package com.tautech.cclappoperators.activities.ui_redispatch.scan

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKManager.FEATURE_TYPE
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.*
import com.symbol.emdk.barcode.Scanner
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.RedispatchActivityViewModel
import com.tautech.cclappoperators.adapters.DeliveryForRedispatchAdapter
import com.tautech.cclappoperators.adapters.PlanificationLineAdapter
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.DeliveryForRedispatch
import com.tautech.cclappoperators.models.PendingToUploadRedispatch
import com.tautech.cclappoperators.services.MyWorkerManagerService
import kotlinx.android.synthetic.main.fragment_scan_redispatch.*
import kotlinx.android.synthetic.main.fragment_scan_redispatch.barcodeEt
import kotlinx.android.synthetic.main.fragment_scan_redispatch.constraintLayout
import kotlinx.android.synthetic.main.fragment_scan_redispatch.listLabelTv
import kotlinx.android.synthetic.main.fragment_scan_redispatch.overlayTv
import kotlinx.android.synthetic.main.fragment_scan_redispatch.scannerStatusTv
import kotlinx.android.synthetic.main.fragment_scan_redispatch.triggerBtn
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.collections.ArrayList

class ScanFragment : Fragment(), EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    val TAG = "SCAN_REDISP_FRAGMENT"
    val planificationsStrArray = arrayListOf<String>()
    //lateinit var planificationAdapter: ArrayAdapter<String>
    // Variables to hold EMDK related objects
    private var emdkManager: EMDKManager? = null
    private var scanSuccessBeep: MediaPlayer? = null
    private var scanFailBeep: MediaPlayer? = null
    private var scanErrorBeep: MediaPlayer? = null
    private var scanExistsBeep: MediaPlayer? = null
    private var scanTriggerBeep: MediaPlayer? = null
    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null
    // Variables to hold handlers of UI controls
    private val viewModel: RedispatchActivityViewModel by activityViewModels()
    private var redispatchedDeliveriesShort: MutableList<DeliveryForRedispatch> = mutableListOf()
    private var mAdapter: DeliveryForRedispatchAdapter? = null
    private var mStateManager: AuthStateManager? = null
    private var db: AppDatabase? = null
    //var scannedCounter: Int = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val root = inflater.inflate(R.layout.fragment_scan_redispatch, container, false)
        Log.i(TAG, "on create view...")
        // TODO obtener planificacion id de shared preferences y de la BD
        db = AppDatabase.getDatabase(requireContext())
        mStateManager = AuthStateManager.getInstance(requireContext())
        Log.i(TAG, "Restoring state...")
        scanSuccessBeep = MediaPlayer.create(context, R.raw.success)
        scanFailBeep = MediaPlayer.create(context, R.raw.fail)
        scanTriggerBeep = MediaPlayer.create(context, R.raw.trigger)
        scanExistsBeep = MediaPlayer.create(context, R.raw.exists)
        scanErrorBeep = MediaPlayer.create(context, R.raw.error)
        //Log.i(TAG, "loaded user info: ${mStateManager?.userInfo}")
        Log.i(TAG, "loaded user profile: ${mStateManager?.keycloakUser}")
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "on view created...")
        initUI()
        activity?.actionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.actionBar?.setDisplayShowHomeEnabled(true)
        overlayTv.visibility = View.GONE
        viewModel.processedDeliveries.observe(viewLifecycleOwner, Observer{ redispatchedDeliveries ->
            Log.i(TAG, "actualizando redispatchedDeliveriesShort con ${redispatchedDeliveries}")
            redispatchedDeliveriesShort.clear()
            if (redispatchedDeliveries.size > 5) {
                redispatchedDeliveriesShort.addAll(redispatchedDeliveries.subList(0, 5))
            } else {
                redispatchedDeliveriesShort.addAll(redispatchedDeliveries)
            }
            activity?.runOnUiThread {
                mAdapter?.notifyDataSetChanged()
            }
            updateCounters()
        })
        viewModel.deliveries.observe(viewLifecycleOwner, Observer{ pendingDeliveries ->
            Log.i(TAG, "actualizando pendingDeliveries con ${pendingDeliveries}")
            updateCounters()
        })
        /*viewModel.planifications.observe(viewLifecycleOwner, Observer { _planifications ->
            if (!_planifications.isNullOrEmpty()) {
                /*planificationsStrArray.clear()
                planificationsStrArray.addAll(_planifications.map{
                    "${it.id} (${it.state}): ${it.licensePlate ?: "S/P"} - ${it.label ?: "S/L"} (${it.planificationType})"
                })*/
                //planificationAdapter.notifyDataSetChanged()
                //planificationTv.isEnabled = true
                /*if (viewModel.planification.value != null) {
                    Log.i(TAG, "hay una planificacion seleccionada")
                    val index = viewModel.planifications.value?.
                    indexOfFirst {
                        it.id == viewModel.planification.value?.id
                    } ?: -1
                    if (index > -1) {
                        Log.i(TAG, "planificacion encontrada en el indice $index de la lista")
                        (planificationTv as AutoCompleteTextView).setText(planificationsStrArray.get(index))
                        planificationTv.performCompletion()
                        //planificationTv.listSelection = index
                    }
                }*/
            } else {
                //planificationTv.isEnabled = false
            }
        })*/
        initEMDK()
    }

    fun initEMDK() {
        try {
            // Requests the EMDKManager object. This is an asynchronous call and should be called from the main thread.
            // The callback also will receive in the main thread without blocking it until the EMDK resources are ready.
            val results = EMDKManager.getEMDKManager(this.requireContext(), this)
            // Check the return status of getEMDKManager() and update the status TextView accordingly.
            if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
                updateStatus("Solicitud de lectura fallida!")
            } else {
                updateStatus("Inicializacion en progreso...")
            }
        }catch (e: Exception) {
            //updateStatus("Error loading EMDK Manager")
            Log.e(TAG, "Error loading EMDK Manager")
        }
    }

    private fun showSnackbar(message: String) {
        activity?.runOnUiThread {
            constraintLayout?.let {
                Snackbar.make(it,
                    message,
                    Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    fun updateCounters() {
        activity?.runOnUiThread {
            listLabelTv?.text = getString(R.string.last_transfered_deliveries, viewModel.processedDeliveries.value?.size ?: 0)
        }
    }

    private fun initUI() {
        updateCounters()
        activity?.runOnUiThread {
            mAdapter = DeliveryForRedispatchAdapter(redispatchedDeliveriesShort, null, this.requireContext())
            recyclerViewTransfer?.layoutManager = LinearLayoutManager(this.requireContext())
            recyclerViewTransfer?.adapter = mAdapter
            planificationTv.isEnabled = false
        }
        barcodeEt?.setOnEditorActionListener {v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    triggerBtn?.callOnClick()
                    true
                } else -> false
            }
        }
        barcodeEt?.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                triggerBtn?.callOnClick()
            }
            false
        })
        //planificationAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, planificationsStrArray)
        //planificationTv?.setAdapter(planificationAdapter);
        /*planificationTv?.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val str = planificationAdapter.getItem(position)
            Log.i(TAG, "selected source item: $str")
            if (str != null) {
                viewModel.planificationStr.postValue(str!!)
                val planificationId = str.split(" ").get(0).toLong()
                val selectedPlanification = viewModel.planifications.value?.find {
                    it.id == planificationId
                }
                if (selectedPlanification != null) {
                    viewModel.planification.postValue(selectedPlanification!!)
                }
            }
        }*/
        triggerBtn?.setOnClickListener{_ ->
            if (barcodeEt?.text?.isEmpty() == false) {
                doAsync {
                    searchData(barcodeEt.text.toString().toLong())
                }
            }
        }
    }

    private fun initBarcodeManager() {
        // Get the feature object such as BarcodeManager object for accessing the feature.
        barcodeManager = emdkManager?.getInstance(FEATURE_TYPE.BARCODE) as BarcodeManager
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
                    Log.e(TAG, "Ocurrio una Excepcion ${e.message}", e)
                    updateStatus("Ocurrio un error con el lector")
                    deInitScanner()
                }
            } else {
                updateStatus("La inicializacion del lector ha fallado")
            }
        }
    }

    private fun deInitScanner() {
        if (scanner != null) {
            try {
                // Release the scanner
                scanner?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Ocurrio un error al inicializar el escaner")
                updateStatus(e.message)
            }
            scanner = null
        }
    }

    override fun onOpened(_emdkManager: EMDKManager?) {
        // Get a reference to EMDKManager
        emdkManager =  _emdkManager
        // Get a  reference to the BarcodeManager feature object
        initBarcodeManager()
        // Initialize the scanner
        initScanner()
    }

    override fun onClosed() {
        // The EMDK closed unexpectedly. Release all the resources.
        emdkManager?.release();
        emdkManager = null;
        updateStatus("Se cerro el escaner inesperadamente. Por favor reinicie la app");
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
                statusStr = "Escaner activado y listo para leer..."
                // Change scanner configuration. This should be done while the scanner is in IDLE state.
                setConfig()
                try {
                    // Starts an asynchronous Scan. The method will NOT turn ON the scanner beam,
                    //but puts it in a  state in which the scanner can be turned on automatically or by pressing a hardware trigger.
                    scanner?.read()
                } catch (e: ScannerException) {
                    Log.e(TAG, "Ocurrio una excepcion con el escaner", e)
                    updateStatus("Ocurrio un error inesperado con el escaner")
                }
            }
            StatusData.ScannerStates.WAITING -> {
                // Scanner is waiting for trigger press to scan...
                statusStr = "Escaner esperando por datos..."
            }
            StatusData.ScannerStates.SCANNING -> {
                // Scanning is in progress...
                statusStr = "Escaneando..."
            }
            StatusData.ScannerStates.DISABLED -> {
                // Scanner is disabled
                statusStr = "El escaner esta desactivado"
            }
            StatusData.ScannerStates.ERROR -> {
                // Error has occurred during scanning
                statusStr = "Ha ocurrido un error"
            }
        }
        // Updates TextView with scanner state on UI thread.
        updateStatus(statusStr);
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        // The ScanDataCollection object gives scanning result and the collection of ScanData. Check the data and its status.
        var barcodeData: String = ""
        if ((scanDataCollection != null) && (scanDataCollection.result == ScannerResults.SUCCESS)) {
            val scanData: ArrayList<ScanDataCollection.ScanData> = scanDataCollection.scanData;
            // Iterate through scanned data and prepare the data.
            for (data: ScanDataCollection.ScanData in scanData) {
                // Get the scanned dataString
                barcodeData = data.data
                Log.i("DATA_LOADED", barcodeData)
            }
            // limpiamos input
            activity?.runOnUiThread {
                barcodeEt.text.clear()
                barcodeEt.setText(barcodeData)
            }
            // Updates EditText with scanned data
            searchData(barcodeData.toString().toLong())
        }
    }

    fun searchData(deliveryNumber: Long) {
        // play trigger sound
        /*doAsync {
            scanTriggerBeep?.start()
        }*/
        Log.i(TAG, "barcode readed: $deliveryNumber")
        // primero buscamos si ya fue escaneado
        val delivery = db?.deliveryForRedispatchDao()?.getByDeliveryNumber(deliveryNumber)
        if(delivery == null){
            showSnackbar("Guia no existe")
            return
        }
        val redispatchedDelivery = db?.deliveryForRedispatchDao()?.hasBeenRedispatched(deliveryNumber)
        if (redispatchedDelivery != null) {
            doAsync {
                scanExistsBeep?.start()
            }
            Log.i(TAG, "delivery ya se encuentra redespachado en la BD local: $redispatchedDelivery")
            showSnackbar("Esta guia ya fue escaneada")
            return
        }
        Log.i(TAG, "delivery no se encuentra redespachado en la BD local")
        var foundDelivery: DeliveryForRedispatch? = null
        //var hasBeenRedispatched = false
        foundDelivery = viewModel.deliveries.value?.find { d ->
            d.deliveryNumber == deliveryNumber
        }
        if (foundDelivery != null) {
            doAsync {
                scanSuccessBeep?.start()
                /*db?.planificationDao()?.getById(foundDelivery.planificationId)?.let{
                    val planificationStr = "${it.id} (${it.state}): ${it.licensePlate ?: "S/P"} - ${it.label ?: "S/L"} (${it.planificationType})"
                    uiThread{
                        planificationTv.setText(planificationStr)
                    }
                }*/
            }
            updateStatus("Codigo Encontrado")
            viewModel.deliveries.value?.remove(foundDelivery)
            foundDelivery.deliveryState = "ReDispatched"
            viewModel.processedDeliveries.value?.add(0, foundDelivery)
            viewModel.processedDeliveries.postValue(viewModel.processedDeliveries.value)
            doAsync {
                db?.deliveryDao()?.changeStateById(foundDelivery.id, "ReDispatched")
                val redispatch = PendingToUploadRedispatch()
                redispatch.deliveryId = foundDelivery.id
                //redispatch.sourcePlanificationId = viewModel.planification.value?.id!!
                //redispatch.sourcePlanificationId = foundDelivery.planificationId
                redispatch.newState = "ReDispatched"
                MyWorkerManagerService.enqueUploadSingleRedispatchWork(requireContext(), redispatch)
            }
        } else {
            doAsync {
                scanFailBeep?.start()
            }
            updateStatus("Codigo No Encontrado")
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
            Log.e(TAG, "Error al configurar escaner", e)
            updateStatus("Ocurrio un error al configurar escaner")
        }
    }

    private fun updateStatus(status: String?) {
        Log.i(TAG, status ?: "")
        activity?.runOnUiThread{
            // Update the status text view on UI thread with current scanner state
            scannerStatusTv?.text = status
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emdkManager?.release(FEATURE_TYPE.BARCODE)
        emdkManager = null
        scanTriggerBeep?.release()
        scanTriggerBeep = null
        scanFailBeep?.release()
        scanFailBeep = null
        scanSuccessBeep?.release()
        scanSuccessBeep = null
    }
}