package com.tautech.cclappoperators.activities

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.classes.Constant
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.models.Planification
import com.tautech.cclappoperators.services.CclClient
import com.tautech.cclappoperators.services.MyWorkerManagerService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.layout
import kotlinx.android.synthetic.main.activity_main.messageHomeTv
import kotlinx.android.synthetic.main.activity_main.progressBar3
import kotlinx.android.synthetic.main.activity_main.retryBtn
import kotlinx.android.synthetic.main.activity_main.retryLayout
import kotlinx.android.synthetic.main.activity_redispatch.*
import kotlinx.android.synthetic.main.activity_transfer.*
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.EndSessionResponse
import org.jetbrains.anko.contentView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONException
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*


class RedispatchActivity : AppCompatActivity() {
    val TAG = "REDISPATCH_ACTIVITY"
    val KEY_PLANIFICATION_INFO = "planification"
    var planification: Planification? = null
    private var mStateManager: AuthStateManager? = null
    var db: AppDatabase? = null
    private val viewModel: RedispatchActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_redispatch)
        val navView: BottomNavigationView = findViewById(R.id.nav_view4)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_scan,
            R.id.navigation_scanned,
            R.id.navigation_pending,
            R.id.navigation_resume))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        mStateManager = AuthStateManager.getInstance(this)
        try {
            db = AppDatabase.getDatabase(this)
        } catch (ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        if (!mStateManager!!.current.isAuthorized) {
            showAlert("Sesion expirada", "Su sesion ha expirado", this::signOut)
            return
        }
        /*viewModel.planification.observe(this, Observer { _planification ->
            if (_planification != null) {
                planification = _planification
                //invalidateOptionsMenu()
                Log.i(TAG, "Planification observada: ${_planification.id}")
                fetchPlanificationLinesReq()
            }
        })*/
        viewModel.endDate.observe(this, {timestamp ->
            val endDate =  timestamp?.let {
                SimpleDateFormat("yyyy-MM-dd").format(Date(it))
            } ?: ""
            val startDate = viewModel.endDate.value?.let {
                SimpleDateFormat("yyyy-MM-dd").format(Date(it))
            } ?: ""
            dateFilterTv.text = "$startDate a $endDate"
            if(startDate.isNotEmpty() && endDate.isNotEmpty()){
                fetchPlanificationLinesReq()
            }
        })
        val extras = intent.extras
        if (extras != null) {
            // TODO obtener planificacion id de shared preferences y luego la planificacion de la BD
            if (extras.containsKey("planification")) {
                planification = extras.getSerializable("planification") as Planification
                Log.i(TAG, "posting value to planification")
                //viewModel.planification.postValue(planification)
            } else {
                Log.i(TAG, "no se recibio ninguna planificacion.")
            }
        } else {
            Log.i(TAG, "no se recibieron datos en el intent")
        }
        //fetchPlanifications()
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        // user info is retained to survive activity restarts, such as when rotating the
        // device or switching apps. This isn't essential, but it helps provide a less
        // jarring UX when these events occur - data does not just disappear from the view.
        /*if (viewModel.planification.value != null) {
            state.putSerializable(KEY_PLANIFICATION_INFO, viewModel.planification.value)
        }*/
        if (planification != null) {
            state.putSerializable(KEY_PLANIFICATION_INFO, planification)
        }
    }

    override fun onResume(){
        super.onResume()
        mStateManager?.revalidateSessionData(this)
    }

    override fun onDestroy(){
        super.onDestroy()

    }

    private fun signOut() {
        if (mStateManager != null && mStateManager?.current != null && mStateManager?.mConfiguration?.redirectUri != null &&
            mStateManager?.current?.idToken != null && mStateManager?.current?.authorizationServiceConfiguration != null) {
            val endSessionRequest = EndSessionRequest.Builder(
                mStateManager?.current?.authorizationServiceConfiguration!!,
                mStateManager?.current?.idToken!!,
                mStateManager?.mConfiguration?.redirectUri!!
            ).build()
            if (endSessionRequest != null) {
                val authService = AuthorizationService(this)
                val endSessionIntent = authService.getEndSessionRequestIntent(endSessionRequest)
                startActivityForResult(endSessionIntent, AuthStateManager.RC_END_SESSION)
            } else {
                showSnackbar("Error al intentar cerrar sesion")
            }
        } else {
            Log.i(TAG,
                "mStateManager?.mConfiguration?.redirectUri: ${mStateManager?.mConfiguration?.redirectUri}")
            Log.i(TAG, "mStateManager?.current?.idToken: ${mStateManager?.current?.idToken}")
            Log.i(TAG,
                "mStateManager?.current?.authorizationServiceConfiguration: ${mStateManager?.current?.authorizationServiceConfiguration}")
            showSnackbar("Error al intentar cerrar sesion")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthStateManager.RC_END_SESSION) {
            val resp: EndSessionResponse = EndSessionResponse.fromIntent(data!!)!!
            val ex = AuthorizationException.fromIntent(data)
            Log.i(TAG, "logout response: $resp")
            if (resp != null) {
                if (ex != null) {
                    Log.e(TAG, "Error al intentar finalizar sesion", ex)
                    showAlert("Error",
                        "No se pudo finalizar la sesion",
                        this::signOut)
                } else {
                    mStateManager?.signOut(this)
                    val mainIntent = Intent(this,
                        LoginActivity::class.java)
                    mainIntent.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(mainIntent)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        finishAndRemoveTask()
                    } else {
                        finish()
                    }
                }
            } else {
                Log.e(TAG, "Error al intentar finalizar sesion", ex)
                showAlert("Error",
                    "No se pudo finalizar la sesion remota",
                    this::signOut)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val mIntent = Intent().putExtra("planification", planification)
        setResult(Constant.REDISPATCH_ACTIVITY, mIntent)
        onBackPressed()
        return true
    }
/*
    private fun checkRedispatches(){
        doAsync {
            Log.i(TAG, "checking certifications and delivery lines....")
            val pendingToUploadRedispatches = db?.pendingToUploadCertificationDao()?.getAll()
            if (!pendingToUploadRedispatches.isNullOrEmpty()) {
                Log.i(TAG,
                    "Hay guias redespachadas pendientes por subir: $pendingToUploadRedispatches")
                val pendingToUploadRedispatchesDeliveries =
                    db?.deliveryDao()?.getAllByIds(pendingToUploadRedispatches.map {
                        it.deliveryId
                    }.toLongArray())
                if (!pendingToUploadRedispatchesDeliveries.isNullOrEmpty()) {
                    pendingToUploadRedispatchesDeliveries.filter{ delivery ->
                        // filtramos las delivery lines certificada pendientes por subir que no esten en la lista de certificaciones
                        viewModel.processedDeliveries.value?.contains(delivery) != true
                    }.let { definitiveDeliveries ->
                        if (definitiveDeliveries.isNotEmpty()) {
                            Log.i(TAG,
                                "deliveries pendientes por subir que no estan en la lista de redespachadas: $definitiveDeliveries")
                            viewModel.processedDeliveries.value?.addAll(
                                definitiveDeliveries)
                            viewModel.processedDeliveries.postValue(viewModel.processedDeliveries.value)
                            if (!viewModel.deliveries.value.isNullOrEmpty()) {
                                Log.i(TAG,
                                    "pending deliveries hasta ahora: ${viewModel.deliveries.value}")
                                viewModel.deliveries.value?.removeAll(
                                    definitiveDeliveries)
                                Log.i(TAG,
                                    "pending deliveries excluidos los ya redespachados: ${viewModel.deliveries.value}")
                                viewModel.deliveries.postValue(viewModel.deliveries.value)
                            }
                        }
                    }
                } else {
                    Log.i(TAG,
                        "No hay deliveries que coincidan con los deliveries redespachados pendientes por subir")
                }
            } else {
                Log.i(TAG, "No hay deliveries redespachados pendientes por subir")
            }
            val unRedispatchedDeliveries = db?.deliveryDao()?.getAllPendingBySourcePlanification(
                planification?.id!!)
            Log.i(TAG,
                "unRedispatchedDeliveries tiene ${unRedispatchedDeliveries?.size} elementos: $unRedispatchedDeliveries")
            val redispatchedDeliveries = db?.deliveryDao()?.getAllRedispatchedByPlanification(
                planification?.id!!)
            Log.i(TAG,
                "redispatchedDeliveryies tiene ${redispatchedDeliveries?.size} elementos: $redispatchedDeliveries")
            if (!redispatchedDeliveries.isNullOrEmpty()){
                Log.i(TAG,
                    "deliveries redespachados hasta ahora (${redispatchedDeliveries.size}) $redispatchedDeliveries")
                if (viewModel.processedDeliveries.value.isNullOrEmpty()) {
                    viewModel.processedDeliveries.postValue(redispatchedDeliveries.toMutableList())
                } else {
                    Log.i(TAG, "agregando al final de lista de deliveries redespachados")
                    redispatchedDeliveries.filter{ delivery ->
                        // filtramos las deliveries que no esten en la lista de redespachos
                        viewModel.processedDeliveries.value?.contains(delivery) != true
                    }.let{ filteredDeliveries ->
                        Log.i(TAG,
                            "deliveries redespachados que no estan en la lista de redespachados $filteredDeliveries")
                        if (filteredDeliveries.isNotEmpty()){
                            Log.i(TAG,
                                "se agregaran ${filteredDeliveries.size} elementos a la lista de pendientes")
                            viewModel.processedDeliveries.value?.addAll(filteredDeliveries.toMutableList())
                            viewModel.processedDeliveries.postValue(viewModel.processedDeliveries.value)
                        }
                    }
                }
                Log.i(TAG,
                    "deliveries redespachados hasta ahora (${redispatchedDeliveries.size}) $redispatchedDeliveries")
                if (!viewModel.deliveries.value.isNullOrEmpty()) {
                    Log.i(TAG,
                        "removiendo deliveries ya redespachadas de la lista de deliveries pendientes")
                    viewModel.deliveries.value?.removeAll {
                        redispatchedDeliveries.contains(it)
                    }
                    Log.i(TAG,
                        "deliveries pendientes exceptuando los ya redespachados: ${viewModel.deliveries.value}")
                }
            }
            if (viewModel.deliveries.value.isNullOrEmpty() && !unRedispatchedDeliveries.isNullOrEmpty()){
                Log.i(TAG,
                    "la lista de pendings esta vacia y hay ${unRedispatchedDeliveries.size} deliveries no redespachados: $unRedispatchedDeliveries")
                viewModel.deliveries.postValue(unRedispatchedDeliveries.toMutableList())
            } else if (!viewModel.deliveries.value.isNullOrEmpty() && !unRedispatchedDeliveries.isNullOrEmpty()){
                Log.i(TAG,
                    "la lista de pendings no esta vacia y hay ${unRedispatchedDeliveries.size} deliveries no certificados $unRedispatchedDeliveries")
                unRedispatchedDeliveries.filter{ delivery ->
                        // filtramos las delivery lines que no esten en la lista de pendientes
                        viewModel.deliveries.value?.contains(delivery) != true
                    }.let{ filteredDeliveries ->
                    Log.i(TAG,
                        "deliveries no redespachados que no estan en la lista de pendientes (${filteredDeliveries.size}) $filteredDeliveries")
                    if (filteredDeliveries.isNotEmpty()){
                        Log.i(TAG,
                            "se agregaran ${filteredDeliveries.size} elementos a la lista de pendientes")
                        viewModel.deliveries.value?.addAll(filteredDeliveries.toMutableList())
                        viewModel.deliveries.postValue(viewModel.deliveries.value)
                    }
                }
            }
        }
    }
*/
    private fun hideViews() {
        runOnUiThread {
            if (layout != null) {
                layout.visibility = View.GONE
            }
        }
    }

    private fun showViews() {
        runOnUiThread {
            if (layout != null) {
                layout.visibility = View.VISIBLE
            }
        }
    }

    private fun fetchPlanificationLines(
        accessToken: String?,
        idToken: String?,
        ex: AuthorizationException?,
    ) {
        if (ex != null) {
            Log.e(TAG, "ocurrio una excepcion mientras se recuperaban lineas de planificacion", ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", this::signOut)
            }
            return
        }
        //val url = "delivery/label/planifications"
        //val url = "planificationDeliveryVO1s/search/findByPlanificationId?planificationId=${planification?.id}"
        val startDate = SimpleDateFormat("yyyy-MM-dd").format(Date(viewModel.startDate.value!!))
        val endDate = SimpleDateFormat("yyyy-MM-dd").format(Date(viewModel.endDate.value!!))
        val url = "deliveryVO7s/search/findByCustomerAddressIdAndOrderDateBetween?customerAddressId=${mStateManager?.customer?.addressId}&start=$startDate&end=$endDate"
        Log.i(TAG, "planification lines endpoint: ${url}")
        val dataService: CclDataService? = CclClient.getInstance(this)?.create(
            CclDataService::class.java)
        showLoader()
        showSnackbar("cargando guias...")
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    Log.i(TAG,
                        "fetching planification lines for planification ${planification?.id}")
                    val call = dataService.getDeliveriesForRedispatch(url, "Bearer $accessToken").execute()
                    val response = call.body()
                    Log.i(TAG,
                            "respuesta cargar deliveries: $response")
                    val deliveries = response?._embedded?.deliveryVO7s?.filter{
                        it.deliveryState == "UnDelivered" || it.deliveryState == "Partial"
                    } ?: arrayListOf()
                    hideLoader()
                    Log.i(TAG,"lineas filtradas ${deliveries}")
                    if (deliveries.isNullOrEmpty()) {
                        showSnackbar("No hay guias para cargar")
                    }
                    db?.deliveryForRedispatchDao()?.deleteAll()
                    db?.deliveryForRedispatchDao()?.insertAll(deliveries.toMutableList())
                    viewModel.deliveries.postValue(deliveries.toMutableList())
                } catch (toe: SocketTimeoutException) {
                    hideLoader()
                    showRetryMessage("Network error fetching user planification lines",
                        this@RedispatchActivity::fetchPlanificationLinesReq)
                    showSnackbar("Fetching user planification lines failed")
                    Log.e(TAG, "Network error when querying planification lines endpoint", toe)
                } catch (ioEx: IOException) {
                    hideLoader()
                    showRetryMessage("Network error fetching user planification lines",
                        this@RedispatchActivity::fetchPlanificationLinesReq)
                    showSnackbar("Fetching user planification lines failed")
                    Log.e(TAG, "Network error when querying planification lines endpoint", ioEx)
                } catch (jsonEx: JSONException) {
                    hideLoader()
                    showRetryMessage("Error parsing user planification lines",
                        this@RedispatchActivity::fetchPlanificationLinesReq)
                    Log.e(TAG, "Failed to parse planification lines response", jsonEx)
                    showSnackbar("Failed to parse planification lines")
                } catch (e: Exception) {
                    hideLoader()
                    showRetryMessage("Fetching user planification lines failed",
                        this@RedispatchActivity::fetchPlanificationLinesReq)
                    showSnackbar("Fetching planification lines failed")
                    Log.e(TAG, "Unknown exception: ", e)
                }
            }
        }
    }

    private fun fetchPlanificationLinesReq() {
        fetchData(this::fetchPlanificationLines)
    }

    /*private fun fetchPlanifications() {
        fetchData(this::fetchPlanifications)
    }

    private fun fetchPlanifications(
            accessToken: String?,
            idToken: String?,
            ex: AuthorizationException?,
    ) {
        //var url = "planification/list/2;customerAddressId-filterType=number;customerAddressId-type=equals;customerAddressId-filter=${mStateManager?.customer?.addressId};planificationType-filterType=text;planificationType-type=equals;planificationType-filter=urban;"
        var url = "planification/byOperator/2;startRow=0;endRow=50000;sort-dispatchDate=desc;"
        Log.i(TAG, "planifications endpoint: $url")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
                CclDataService::class.java)
        if (mStateManager?.customer == null){
            mStateManager?.loadCustomerAndAddress(this)
        }
        if (dataService != null && accessToken != null && mStateManager?.customer?.addressId != null) {
            showLoader()
            showSnackbar("Cargando lista de planificaciones...")
            doAsync {
                try {
                    val call = dataService.getPlanifications(url, mStateManager?.customer?.addressId!!,"Bearer $accessToken").execute()
                    val response = call.body()
                    hideLoader()
                    Log.i(TAG, "planifications response with retrofit: $response")
                    val allPlanifications = response?.content?.filter {
                        it.state == "Complete"
                    } ?: arrayListOf()
                    Log.i(TAG, "planifications response filtered: $allPlanifications")
                    viewModel.planifications.postValue(allPlanifications.toMutableList())
                } catch (ioEx: IOException) {
                    hideLoader()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTransferTv?.text = getText(R.string.network_error)
                        messageTransferTv?.visibility = View.VISIBLE
                        Log.e(TAG, "Network error when querying planifications endpoint", ioEx)
                    }

                } catch (jsonEx: JSONException) {
                    hideLoader()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTransferTv?.text = getText(R.string.parse_planifications_error)
                        messageTransferTv?.visibility = View.VISIBLE
                        Log.e(TAG, "Failed to parse planifications response", jsonEx)
                    }
                } catch (e: Exception) {
                    hideLoader()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTransferTv?.text = getText(R.string.unknown_error)
                        messageTransferTv?.visibility = View.VISIBLE
                        Log.e(TAG, "Unknown exception: ", e)
                    }
                } catch (e: SocketTimeoutException) {
                    hideLoader()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTransferTv?.text = getText(R.string.unknown_error)
                        messageTransferTv?.visibility = View.VISIBLE
                        Log.e(TAG, "Socket timeout exception: ", e)
                    }
                }
            }
        }
    }*/

    private fun showRetryMessage(message: String, callback: () -> Unit) {
        runOnUiThread{
            messageHomeTv?.text = message
            retryBtn?.setOnClickListener {
                callback.invoke()
            }
            retryLayout?.visibility = View.VISIBLE
        }
    }

    private fun fetchData(callback: ((String?, String?, AuthorizationException?) -> Unit)) {
        Log.i(TAG, "Fetching user planifications...")
        try {
            mStateManager?.current?.performActionWithFreshTokens(mStateManager?.mAuthService!!,
                callback)
        }catch (ex: AuthorizationException) {
            Log.e(TAG, "error fetching data", ex)
        }
    }

    private fun showAlert(
        title: String,
        message: String,
        positiveCallback: (() -> Unit)? = null,
        negativeCallback: (() -> Unit)? = null,
    ) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", DialogInterface.OnClickListener { dialog, id ->
                if (positiveCallback != null) {
                    positiveCallback()
                }
                dialog.dismiss()
            })
            builder.setNegativeButton("Cancelar", DialogInterface.OnClickListener { dialog, id ->
                if (negativeCallback != null) {
                    negativeCallback()
                }
                dialog.dismiss()
            })
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun hideLoader() {
        runOnUiThread {
            progressBar3.visibility = View.GONE
            retryLayout?.visibility = View.GONE
        }
    }

    private fun showLoader() {
        runOnUiThread{
            progressBar3.visibility = View.VISIBLE
            retryLayout?.visibility = View.GONE
        }
    }

    private fun showSnackbar(message: String) {
        runOnUiThread {
            Snackbar.make(contentView!!,
                message,
                Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.activity_redispatch, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.calendarBtn -> {
                Log.i(TAG, "mostrando calendario...")
                val builder = MaterialDatePicker.Builder.dateRangePicker()
                val now = Calendar.getInstance()
                builder.setSelection(androidx.core.util.Pair(now.timeInMillis, now.timeInMillis))
                val picker = builder.build()
                picker.show(supportFragmentManager, picker.toString())
                picker.addOnNegativeButtonClickListener {
                    picker.dismiss()
                }
                picker.addOnPositiveButtonClickListener {
                    Log.i(TAG, "The selected date range is ${it.first} - ${it.second}")
                    viewModel.startDate.setValue(it.first)
                    viewModel.endDate.setValue(it.second)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return true
    }
}