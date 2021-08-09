package com.tautech.cclappoperators.activities.ui_transfer.scan

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKManager.FEATURE_TYPE
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.*
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.TransferActivityViewModel
import com.tautech.cclappoperators.adapters.PlanificationLineAdapter
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.PendingToUploadTransfer
import com.tautech.cclappoperators.services.CclClient
import com.tautech.cclappoperators.services.MyWorkerManagerService
import kotlinx.android.synthetic.main.fragment_scan_transfer.*
import kotlinx.android.synthetic.main.fragment_scan_transfer.barcodeEt
import kotlinx.android.synthetic.main.fragment_scan_transfer.constraintLayout
import kotlinx.android.synthetic.main.fragment_scan_transfer.listLabelTv
import kotlinx.android.synthetic.main.fragment_scan_transfer.overlayTv
import kotlinx.android.synthetic.main.fragment_scan_transfer.recyclerViewTransfer
import kotlinx.android.synthetic.main.fragment_scan_transfer.scannerStatusTv
import kotlinx.android.synthetic.main.fragment_scan_transfer.triggerBtn
import org.jetbrains.anko.doAsync

class ScanFragment : Fragment(), EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    val TAG = "SCAN_REDISP_FRAGMENT"
    val planificationsStrArray = arrayListOf<String>()
    lateinit var sourcePlanificationAdapter: ArrayAdapter<String>
    lateinit var targetPlanificationAdapter: ArrayAdapter<String>
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
    private val viewModel: TransferActivityViewModel by activityViewModels()
    private var transferedDeliveriesShort: MutableList<Delivery> = mutableListOf()
    private var mAdapter: PlanificationLineAdapter? = null
    private var mStateManager: AuthStateManager? = null
    private var db: AppDatabase? = null
    var dataService: CclDataService? = null
    //var scannedCounter: Int = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val root = inflater.inflate(R.layout.fragment_scan_transfer, container, false)
        Log.i(TAG, "on create view...")
        // TODO obtener planificacion id de shared preferences y de la BD
        db = AppDatabase.getDatabase(requireContext())
        dataService = CclClient.getInstance(requireContext())?.create(
            CclDataService::class.java)
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
        activity?.actionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.actionBar?.setDisplayShowHomeEnabled(true)
        initUI()
        overlayTv.visibility = View.GONE
        viewModel.processedDeliveries.observe(viewLifecycleOwner, Observer{ transferedDeliveries ->
            Log.i(TAG, "actualizando transferedDeliveriesShort con ${transferedDeliveries}")
            transferedDeliveriesShort.clear()
            if (transferedDeliveries.size > 5) {
                transferedDeliveriesShort.addAll(transferedDeliveries.subList(0, 5))
            } else {
                transferedDeliveriesShort.addAll(transferedDeliveries)
            }
            mAdapter?.notifyDataSetChanged()
            updateCounters()
        })
        viewModel.deliveries.observe(viewLifecycleOwner, Observer{ pendingDeliveries ->
            Log.i(TAG, "actualizando deliveries con ${pendingDeliveries}")
            updateCounters()
        })
        sourcePlanificationAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, planificationsStrArray)
        targetPlanificationAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, planificationsStrArray)
        sourcePlanificationTv2.setAdapter(sourcePlanificationAdapter);
        sourcePlanificationTv2.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val str = sourcePlanificationAdapter.getItem(position)
            Log.i(TAG, "selected source item: $str")
            if (str != null) {
                viewModel.sourcePlanificationStr.postValue(str!!)
                val planificationId = str.split(" ").get(0).toLong()
                val selectedSourcePlanification = viewModel.planifications.value?.find {
                    it.id == planificationId
                }
                if (selectedSourcePlanification != null) {
                    viewModel.sourcePlanification.postValue(selectedSourcePlanification!!)
                }
            }
        }
        targetPlanificationTv.setAdapter(targetPlanificationAdapter);
        targetPlanificationTv.onItemClickListener = object: AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val str = targetPlanificationAdapter.getItem(position)
                Log.i(TAG, "selected target item: $str")
                if (str != null) {
                    viewModel.targetPlanificationStr.postValue(str!!)
                    val planificationId = str.split(" ").get(0).toLong()
                    val selectedTargetPlanification = viewModel.planifications.value?.find {
                        it.id == planificationId
                    }
                    if (selectedTargetPlanification != null) {
                        viewModel.targetPlanification.postValue(selectedTargetPlanification)
                    }
                }
            }
        }
        triggerBtn.setOnClickListener{_ ->
            Log.i(TAG, "source planification: ${viewModel.sourcePlanification.value}")
            Log.i(TAG, "target planification: ${viewModel.targetPlanification.value}")
            Log.i(TAG, "barcode: ${barcodeEt.text}")
            if (barcodeEt.text.isNotEmpty()
            ) {
                if (viewModel.sourcePlanification.value != null &&
                        viewModel.targetPlanification.value != null &&
                        viewModel.sourcePlanification.value != viewModel.targetPlanification.value) {
                    doAsync {
                        searchData(barcodeEt.text.toString().toLong())
                    }
                } else {
                    showSnackbar("Verifique que tiene planificaciones seleccionadas y sean distintas")
                }
            }
        }
        viewModel.planifications.observe(viewLifecycleOwner, Observer { _planifications ->
            Log.i(TAG, "planificaciones observadas: $_planifications")
            if (!_planifications.isNullOrEmpty()) {
                planificationsStrArray.clear()
                planificationsStrArray.addAll(_planifications.map{
                    "${it.id} (${it.state}): ${it.licensePlate ?: "S/P"} - ${it.label ?: "S/L"} (${it.planificationType})"
                })
                sourcePlanificationAdapter.notifyDataSetChanged()
                targetPlanificationAdapter.notifyDataSetChanged()
                sourcePlanificationTv2.isEnabled = true
                targetPlanificationTv.isEnabled = true
                if (viewModel.sourcePlanificationStr.value != null){
                    sourcePlanificationTv2.setText(viewModel.sourcePlanificationStr.value)
                }
                if (viewModel.targetPlanificationStr.value != null){
                    targetPlanificationTv.setText(viewModel.targetPlanificationStr.value)
                }
                if (viewModel.sourcePlanification.value != null) {
                    Log.i(TAG, "hay una planificacion seleccionada")
                    val index = viewModel.planifications.value?.
                    indexOfFirst {
                        it.id == viewModel.sourcePlanification.value?.id
                    } ?: -1
                    if (index > -1) {
                        Log.i(TAG, "planificacion encontrada en el indice $index de la lista")
                        (sourcePlanificationTv2 as AutoCompleteTextView).setText(planificationsStrArray.get(index))
                        sourcePlanificationTv2.performCompletion()
                        //planificationTv.listSelection = index
                    }
                }
            } else {
                sourcePlanificationTv2.isEnabled = false
                targetPlanificationTv.isEnabled = false
            }
        })
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
        sourcePlanificationTv2.isEnabled = false
        targetPlanificationTv.isEnabled = false
        activity?.runOnUiThread {
            mAdapter = PlanificationLineAdapter(transferedDeliveriesShort, null, this.requireContext())
            recyclerViewTransfer.layoutManager = LinearLayoutManager(this.requireContext())
            recyclerViewTransfer.adapter = mAdapter
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
            if(viewModel.sourcePlanification.value != null && viewModel.targetPlanification.value != null) {
                // Updates EditText with scanned data
                searchData(barcodeData.toLong())
            } else {
                doAsync {
                    scanFailBeep?.start()
                }
                Log.i(TAG, "Seleccione planificacion de origen y destino primero")
                showSnackbar("Seleccione planificacion de origen y destino primero")
                return
            }
        }
    }

    fun searchData(deliveryNumber: Long) {
        // play trigger sound
        /*doAsync {
            scanTriggerBeep?.start()
        }*/
        Log.i(TAG, "barcode readed: $deliveryNumber")
        // primero buscamos si ya fue escaneado
        val exists = db?.deliveryDao()?.hasBeenTransfered(deliveryNumber, viewModel.sourcePlanification.value?.id!!)
        if (exists != null) {
            doAsync {
                scanExistsBeep?.start()
            }
            Log.i(TAG, "delivery ya se encuentra redespachado en la BD local: $exists")
            showSnackbar("Esta guia ya fue escaneada")
            return
        } else {
            Log.i(TAG, "delivery no se encuentra redespachado en la BD local")
        }
        var foundDelivery: Delivery? = null
        var hasBeenTransfered: Boolean
        foundDelivery = viewModel.deliveries.value?.find { d ->
            d.deliveryNumber == deliveryNumber
        }
        hasBeenTransfered = viewModel.processedDeliveries.value?.find { d ->
            d.deliveryNumber == deliveryNumber
        } != null
        if (foundDelivery != null && !hasBeenTransfered) {
            doAsync {
                scanSuccessBeep?.start()
            }
            updateStatus("Codigo Encontrado")
            viewModel.deliveries.value?.remove(foundDelivery)
            foundDelivery.planificationId = viewModel.targetPlanification.value?.id!!
            viewModel.processedDeliveries.value?.add(0, foundDelivery)
            viewModel.processedDeliveries.postValue(viewModel.processedDeliveries.value)
            doAsync {
                db?.deliveryDao()?.update(foundDelivery)
                val transfer = PendingToUploadTransfer()
                transfer.deliveryId = foundDelivery.deliveryId
                transfer.sourcePlanificationId = viewModel.sourcePlanification.value?.id!!
                transfer.targetPlanificationId = viewModel.targetPlanification.value?.id!!
                Log.i(TAG, "transferencia a enviar a workmanager: $transfer")
                MyWorkerManagerService.enqueUploadSingleTransferWork(requireContext(), transfer)
            }
        } else if (hasBeenTransfered) {
            doAsync {
                scanExistsBeep?.start()
            }
            updateStatus("Codigo Ya Escaneado")
        } else if (!hasBeenTransfered) {
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
        scanErrorBeep?.release()
        scanErrorBeep = null
        scanExistsBeep?.release()
        scanExistsBeep = null
    }
}