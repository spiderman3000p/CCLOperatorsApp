package com.tautech.cclappoperators.activities.ui_redispatch.pending

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
import com.symbol.emdk.barcode.*
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.RedispatchActivityViewModel
import com.tautech.cclappoperators.adapters.DeliveryForRedispatchAdapter
import com.tautech.cclappoperators.adapters.PlanificationLineAdapter
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.DeliveryForRedispatch
import kotlinx.android.synthetic.main.fragment_pending.*

class PendingFragment : Fragment(), EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    // Variables to hold EMDK related objects
    private var emdkManager: EMDKManager? = null;
    private var barcodeManager: BarcodeManager? = null;
    private var scanner: Scanner? = null;
    private val viewModel: RedispatchActivityViewModel by activityViewModels()
    private var db: AppDatabase? = null
    private val filteredData: MutableList<DeliveryForRedispatch> = mutableListOf()
    val TAG = "PENDING_FRAG_REDISPATCH"
    private var mAdapter: DeliveryForRedispatchAdapter? = null
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "on create view")
        val root = inflater.inflate(R.layout.fragment_pending, container, false)
        db = AppDatabase.getDatabase(requireContext())
        viewModel.deliveries.observe(viewLifecycleOwner, Observer{ pendingDeliveries ->
            Log.i(TAG, "pending deliveries observed: $pendingDeliveries")
            filteredData.clear()
            filteredData.addAll(pendingDeliveries)
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
        pendingDeliveryLinesRv.layoutManager = LinearLayoutManager(this.requireContext())
        pendingDeliveryLinesRv.adapter = mAdapter
        searchEt4.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                if (searchEt4.text.isNotEmpty()) {
                    searchData(searchEt4.text.toString().toLong())
                } else {
                    filteredData.clear()
                    filteredData.addAll(viewModel.deliveries.value!!)
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
        }catch (e: Exception) {
            Log.e(TAG, "Error loading EMDK Manager", e)
        }
    }

    private fun initBarcodeManager() {
        // Get the feature object such as BarcodeManager object for accessing the feature.
        barcodeManager = emdkManager?.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
        // Add external scanner connection listener.
        if (barcodeManager == null) {
            Log.e(TAG, "Barcode scanning is not supported")
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
                statusStr = statusData?.friendlyName + " is   enabled and idle..."
                // Change scanner configuration. This should be done while the scanner is in IDLE state.
                setConfig()
                try {
                    // Starts an asynchronous Scan. The method will NOT turn ON the scanner beam,
                    //but puts it in a  state in which the scanner can be turned on automatically or by pressing a hardware trigger.
                    scanner?.read()
                } catch (e: ScannerException) {
                    updateStatus(e.message);
                }
            }
            StatusData.ScannerStates.WAITING -> {
                // Scanner is waiting for trigger press to scan...
                statusStr = "Scanner is waiting for trigger press..."
            }
            StatusData.ScannerStates.SCANNING -> {
                // Scanning is in progress...
                statusStr = "Scanning..."
            }
            StatusData.ScannerStates.DISABLED -> {
                // Scanner is disabled
                statusStr = statusData?.friendlyName + " is disabled."
            }
            StatusData.ScannerStates.ERROR -> {
                // Error has occurred during scanning
                statusStr = "An error has occurred."
            }
        }
        // Updates TextView with scanner state on UI thread.
        updateStatus(statusStr);
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        // The ScanDataCollection object gives scanning result and the collection of ScanData. Check the data and its status.
        var dataStr: String = ""
        var barcodeData: String
        if ((scanDataCollection != null) && (scanDataCollection.result == ScannerResults.SUCCESS)) {
            val scanData: ArrayList<ScanDataCollection.ScanData> =  scanDataCollection.scanData;
            // Iterate through scanned data and prepare the data.
            for (data: ScanDataCollection.ScanData in  scanData) {
                // Get the scanned dataString
                barcodeData =  data.data;
                Log.i("DATA_LOADED", barcodeData);
                // Get the type of label being scanned
                val labelType: ScanDataCollection.LabelType = data.labelType;
                // Concatenate barcode data and label type
                dataStr = "$barcodeData  $labelType";
            }
            // Updates EditText with scanned data and type of label on UI thread.
            searchData(dataStr.toLong())
        }
    }

    fun searchData(deliveryNumber: Long) {
        Log.i(TAG, "barcode readed: $deliveryNumber")
        var foundDeliveries = listOf<DeliveryForRedispatch>()
        foundDeliveries = viewModel.deliveries.value!!.filter { d ->
            d.deliveryNumber == deliveryNumber
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
    }

    private fun showSnackbar(message: String) {
        activity?.runOnUiThread {
            Snackbar.make(constraintLayout2,
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