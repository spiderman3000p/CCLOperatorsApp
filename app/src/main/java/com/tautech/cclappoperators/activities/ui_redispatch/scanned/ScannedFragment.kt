package com.tautech.cclappoperators.activities.ui_redispatch.scanned

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.*
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.RedispatchActivityViewModel
import com.tautech.cclappoperators.adapters.DeliveryForRedispatchAdapter
import com.tautech.cclappoperators.adapters.PlanificationLineAdapter
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.DeliveryForRedispatch
import kotlinx.android.synthetic.main.fragment_scanned.*

class ScannedFragment : Fragment(), EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    // Variables to hold EMDK related objects
    private var emdkManager: EMDKManager? = null;
    private var barcodeManager: BarcodeManager? = null;
    private var scanner: Scanner? = null;
    private val viewModel: RedispatchActivityViewModel by activityViewModels()
    private var db: AppDatabase? = null
    private var filteredData: MutableList<DeliveryForRedispatch> = mutableListOf()
    val TAG = "SCANNED_FRAGMENT"
    private var mAdapter: DeliveryForRedispatchAdapter? = null
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "on create view")
        val root = inflater.inflate(R.layout.fragment_scanned, container, false)
        db = AppDatabase.getDatabase(requireContext())
        viewModel.processedDeliveries.observe(viewLifecycleOwner, Observer{ redispatchedDeliveries ->
            Log.i(TAG, "redispatched deliveries observed: $redispatchedDeliveries")
            filteredData.clear()
            filteredData.addAll(redispatchedDeliveries)
            mAdapter?.notifyDataSetChanged()
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.actionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.actionBar?.setDisplayShowHomeEnabled(true)
        Log.i(TAG, "on view created")
        mAdapter = DeliveryForRedispatchAdapter(filteredData, null, this.requireContext())
        processedListRv.layoutManager = LinearLayoutManager(this.requireContext())
        processedListRv.adapter = mAdapter
        searchEt4.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                if (searchEt4.text.isNotEmpty()) {
                    searchData(searchEt4.text.toString().toLong())
                }
            }
            false
        }
        initEMDK()
    }

    fun initEMDK() {
        try {
            // Requests the EMDKManager object. This is an asynchronous call and should be called from the main thread.
            // The callback also will receive in the main thread without blocking it until the EMDK resources are ready.
            val results = EMDKManager.getEMDKManager(this.requireContext(), this)
            // Check the return status of getEMDKManager() and update the status TextView accordingly.
            if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
                updateStatus("Barcode request failed!")
            } else {
                updateStatus("Barcode reader initialization is in progress...")
            }
        }catch (e: Exception) {
            //updateStatus("Error loading EMDK Manager")
            Log.e(TAG, "Error loading EMDK Manager")
        }
    }

    private fun initBarcodeManager() {
        // Get the feature object such as BarcodeManager object for accessing the feature.
        barcodeManager = emdkManager?.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
        // Add external scanner connection listener.
        if (barcodeManager == null) {
            showSnackbar("Barcode scanning is not supported")
        }
    }

    private fun initScanner() {
        if (scanner == null) {
            // Get default scanner defined on the device
            scanner = barcodeManager?.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            if (scanner != null) {
                // Implement the DataListener interface and pass the pointer of this object to get the data callbacks.
                scanner?.addDataListener(this)
                // Implement the StatusListener interface and pass the pointer of this object to get the status callbacks.
                scanner?.addStatusListener(this)
                // Hard trigger. When this mode is set, the user has to manually
                // press the trigger on the device after issuing the read call.
                // NOTE: For devices without a hard trigger, use TriggerType.SOFT_ALWAYS.
                scanner?.triggerType = Scanner.TriggerType.HARD
                try {
                    // Enable the scanner
                    // NOTE: After calling enable(), wait for IDLE status before calling other scanner APIs
                    // such as setConfig() or read().
                    scanner?.enable()
                } catch (e: ScannerException) {
                    //updateStatus(e.message)
                    deInitScanner()
                }
            } else {
                updateStatus("Failed to initialize the scanner device.")
            }
        }
    }

    private fun deInitScanner() {
        if (scanner != null) {
            try {
                // Release the scanner
                scanner?.release()
            } catch (e: Exception) {
                updateStatus(e.message)
            }
            scanner = null
        }
    }

    override fun onOpened(_emdkManager: EMDKManager?) {
        // Get a reference to EMDKManager
        emdkManager =  _emdkManager;
        // Get a  reference to the BarcodeManager feature object
        initBarcodeManager();
        // Initialize the scanner
        initScanner();
    }

    override fun onClosed() {
        // The EMDK closed unexpectedly. Release all the resources.
        emdkManager?.release();
        emdkManager = null;
        updateStatus("EMDK closed unexpectedly! Please close and restart the application.");
    }

    override fun onStatus(statusData: StatusData?): Unit {
        // The status will be returned on multiple cases. Check the state and take the action.
        // Get the current state of scanner in background
        val state: StatusData.ScannerStates = statusData?.state ?: StatusData.ScannerStates.IDLE
        var statusStr: String = ""
        // Different states of Scanner
        when (state) {
            StatusData.ScannerStates.IDLE -> {
                // Scanner is idle and ready to change configuration and submit read.
                //statusStr = statusData?.friendlyName + " is   enabled and idle..."
                // Change scanner configuration. This should be done while the scanner is in IDLE state.
                setConfig()
                try {
                    // Starts an asynchronous Scan. The method will NOT turn ON the scanner beam,
                    //but puts it in a  state in which the scanner can be turned on automatically or by pressing a hardware trigger.
                    scanner?.read()
                } catch (e: ScannerException) {
                    Log.e(TAG, "Ocurrio un error con el escaner", e)
                    updateStatus("Ocurrio un error con el escaner")
                }
            }
            StatusData.ScannerStates.WAITING -> {
                // Scanner is waiting for trigger press to scan...
                //statusStr = "Scanner is waiting for trigger press..."
            }
            StatusData.ScannerStates.SCANNING -> {
                // Scanning is in progress...
                //statusStr = "Scanning..."
            }
            StatusData.ScannerStates.DISABLED -> {
                // Scanner is disabled
                //statusStr = statusData?.friendlyName + " is disabled."
            }
            StatusData.ScannerStates.ERROR -> {
                // Error has occurred during scanning
                statusStr = "Occurrio un error durante el escaneo"
            }
        }
        // Updates TextView with scanner state on UI thread.
        updateStatus(statusStr);
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        // The ScanDataCollection object gives scanning result and the collection of ScanData. Check the data and its status.
        var barcodeData: String = ""
        if ((scanDataCollection != null) && (scanDataCollection.result == ScannerResults.SUCCESS)) {
            val scanData: ArrayList<ScanDataCollection.ScanData> =  scanDataCollection.scanData;
            // Iterate through scanned data and prepare the data.
            for (data: ScanDataCollection.ScanData in  scanData) {
                // Get the scanned dataString
                barcodeData =  data.data;
                Log.i("DATA_LOADED", barcodeData);
            }
            // Updates EditText with scanned data and type of label on UI thread.
            searchData(barcodeData.toLong())
        }
    }

    fun searchData(deliveryNumber: Long) {
        Log.i(TAG, "barcode readed: $deliveryNumber")
        var foundDeliveries: List<DeliveryForRedispatch>? = listOf()
        //val foundByIds = db?.deliveryLineDao()?.loadAllByIds(intArrayOf(deliveryLineId))
        if (viewModel.processedDeliveries.value != null && deliveryNumber > 0) {
            foundDeliveries = viewModel.processedDeliveries.value?.filter { d ->
                d.deliveryNumber == deliveryNumber
            }
        }
        if (!foundDeliveries.isNullOrEmpty()) {
            updateStatus("Codigo Encontrado")
            filteredData.clear()
            filteredData.addAll(foundDeliveries)
            activity?.runOnUiThread {
                mAdapter?.notifyDataSetChanged()
            }
        } else {
            updateStatus("Codigo No Encontrado")
        }
    }

    fun showAlert(title: String, message: String, listener: (() -> Unit?)? = null) {
        activity?.runOnUiThread {
            val builder = AlertDialog.Builder(this.requireActivity())
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", DialogInterface.OnClickListener { _, _ ->
                listener?.invoke()
            })
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun setConfig() {
        try {
            // Get scanner config
            val config = scanner?.config
            // Enable haptic feedback
            if (config?.isParamSupported("config.scanParams.decodeHapticFeedback")!!) {
                config.scanParams.decodeHapticFeedback = true
            }
            // Set scanner config
            scanner?.config = config
        } catch (e: ScannerException) {
            updateStatus(e.message)
        }
    }

    private fun updateStatus(status: String?) {
        Log.i(TAG, status ?: "")
        // Update the status text view on UI thread with current scanner state
        showSnackbar("$status")
    }

    private fun showSnackbar(message: String) {
        activity?.runOnUiThread {
            Snackbar.make(certifiedUnitsRv,
                message,
                Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.emdkManager?.release(EMDKManager.FEATURE_TYPE.BARCODE);
        this.emdkManager = null;
    }
}