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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.models.DeliveryLine
import com.tautech.cclappoperators.models.Planification
import com.tautech.cclappoperators.models.PlanificationLine
import com.tautech.cclappoperators.services.CclClient
import com.tautech.cclappoperators.services.MyWorkerManagerService
import kotlinx.android.synthetic.main.activity_main.*
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.EndSessionResponse
import org.jetbrains.anko.contentView
import org.jetbrains.anko.doAsync
import org.json.JSONException
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException


class CertificateActivity : AppCompatActivity() {
    private var newState: String = ""
    val TAG = "MAIN_ACTIVITY"
    val KEY_PLANIFICATION_INFO = "planification"
    var planification: Planification? = null
    private var mStateManager: AuthStateManager? = null
    private var routeStarted: Boolean = false
    var db: AppDatabase? = null
    private val viewModel: CertificateActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        viewModel.planification.observe(this, Observer { _planification ->
            if (_planification != null) {
                planification = _planification
                invalidateOptionsMenu()
                Log.i(TAG, "Planification cargada en observer: ${planification?.id}")
            }
        })
        val extras = intent.extras
        if (extras != null) {
            // TODO obtener planificacion id de shared preferences y luego la planificacion de la BD
            if (extras.containsKey("planification")) {
                planification = extras.getSerializable("planification") as Planification
                Log.i(TAG, "posting value to planification")
                viewModel.planification.postValue(planification)
                doAsync {
                    fetchData(this@CertificateActivity::fetchPlanificationLines)
                }
                doAsync {
                    fetchData(this@CertificateActivity::fetchPlanificationDeliveryLines)
                }
            } else {
                Log.i(TAG, "no se recibio ninguna planificacion. enviando a planificaciones")
                finish()
            }
        } else {
            Log.i(TAG, "no se recibieron datos")
            finish()
        }
        MyWorkerManagerService.uploadFailedCertifications(this)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        // user info is retained to survive activity restarts, such as when rotating the
        // device or switching apps. This isn't essential, but it helps provide a less
        // jarring UX when these events occur - data does not just disappear from the view.
        if (viewModel.planification.value != null) {
            state.putSerializable(KEY_PLANIFICATION_INFO, viewModel.planification.value)
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
        onBackPressed()
        return true
    }

    private fun fetchCertifiedLines(
        accessToken: String?,
        idToken: String?,
        ex: AuthorizationException?,
    ) {
        if (ex != null) {
            Log.e(TAG, "ocurrio una excepcion mientras se recuperaban las certificaciones", ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", this::signOut)
            }
            return
        }
        Log.i(TAG, "fetching certified delivery lines for planification ${planification?.id}...")
        val url = "planificationCertifications/search/findByPlanificationId?planificationId=${planification?.id}"
        //Log.i(TAG_PLANIFICATIONS, "constructed user endpoint: $userInfoEndpoint")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        if (dataService != null && accessToken != null) {
            try {
                doAsync {
                    showLoader()
                    val call = dataService.getPlanificationsCertifiedLines(url,
                        "Bearer $accessToken")
                        .execute()
                    val response = call.body()
                    //showViews()
                    hideLoader()
                    Log.i(TAG,
                        "Fetched certified lines: ${response?._embedded?.planificationCertifications}")
                    if (!response?._embedded?.planificationCertifications.isNullOrEmpty()) {
                        try {
                            db?.certificationDao()?.deleteAllByPlanification(planification?.id!!)
                            db?.certificationDao()?.insertAll(response?._embedded?.planificationCertifications!!)
                            Log.i(TAG,
                                "Se guardaron ${response?._embedded?.planificationCertifications?.size} certificaciones en la BD local")
                        } catch (ex: SQLiteException) {
                            Log.e(TAG,
                                "Error actualizando planificacion en la BD local",
                                ex)
                            showAlert(getString(R.string.database_error),
                                getString(R.string.database_error_saving_planifications))
                        } catch (ex: SQLiteConstraintException) {
                            Log.e(TAG,
                                "Error actualizando planificacion en la BD local",
                                ex)
                            showAlert(getString(R.string.database_error),
                                getString(R.string.database_error_saving_planifications))
                        } catch (ex: Exception) {
                            Log.e(TAG,
                                "Error actualizando planificacion en la BD local",
                                ex)
                            showAlert(getString(R.string.database_error),
                                getString(R.string.database_error_saving_planifications))
                        }
                    } else {
                        Log.i(TAG, "No hay lineas certificadas en la BD remota")
                    }
                    checkCertifications()
                }
            } catch (toe: SocketTimeoutException) {
                hideLoader()
                Log.e(TAG, "Network error when finalizing planification load", toe)
            } catch (ioEx: IOException) {
                hideLoader()
                Log.e(TAG,
                    "Network error when finalizing planification load",
                    ioEx)
            } catch (jsonEx: JSONException) {
                hideLoader()
                Log.e(TAG, "Failed to parse finalizing planification response", jsonEx)
            }
        }
    }

    fun checkCertifications(){
        doAsync {
            Log.i(TAG, "checking certifications and delivery lines....")
            val pendingToUploadCertifications = db?.pendingToUploadCertificationDao()?.getAll()
            if (!pendingToUploadCertifications.isNullOrEmpty()) {
                Log.i(TAG,
                    "Hay deliveryLines certificados pendientes por subir: $pendingToUploadCertifications")
                val pendingToUploadCertifiedDeliveryLines =
                    db?.deliveryLineDao()?.getAllByIds(pendingToUploadCertifications.map {
                        it.deliveryLineId
                    }.toLongArray())
                if (!pendingToUploadCertifiedDeliveryLines.isNullOrEmpty()) {
                    pendingToUploadCertifiedDeliveryLines.filter{ deliveryLine ->
                        // filtramos las delivery lines certificada pendientes por subir que no esten en la lista de certificaciones
                        viewModel.certifiedDeliveryLines.value?.contains(deliveryLine) != true
                    }.let { definitiveLines ->
                        if (definitiveLines.isNotEmpty()) {
                            Log.i(TAG,
                                "delivery lines pendientes por subir que no estan en la lista de certificadas: $definitiveLines")
                            viewModel.certifiedDeliveryLines.value?.addAll(
                                definitiveLines)
                            Log.i(TAG, "posting value to certifiedDeliveryLines")
                            viewModel.certifiedDeliveryLines.postValue(viewModel.certifiedDeliveryLines.value)
                            if (!viewModel.pendingDeliveryLines.value.isNullOrEmpty()) {
                                Log.i(TAG,
                                    "pending delivery lines hasta ahora: ${viewModel.pendingDeliveryLines.value}")
                                viewModel.pendingDeliveryLines.value?.removeAll(
                                    definitiveLines)
                                Log.i(TAG,
                                    "pending delivery lines excluidos los ya certificados: ${viewModel.pendingDeliveryLines.value}")
                                Log.i(TAG, "posting value to pendingDeliveryLines")
                                viewModel.pendingDeliveryLines.postValue(viewModel.pendingDeliveryLines.value)
                            }
                        }
                    }
                } else {
                    Log.i(TAG,
                        "No hay delivery lines que coincidan con los delivery lines certificados pendientes por subir")
                }
            } else {
                Log.i(TAG, "No hay deliveryLines certificados pendientes por subir")
            }
            val uncertifiedDeliveryLines = db?.deliveryLineDao()?.getAllPendingByPlanification(
                planification?.id!!)
            Log.i(TAG,
                "uncertifiedDeliveryLines tiene ${uncertifiedDeliveryLines?.size} elementos: $uncertifiedDeliveryLines")
            val certifiedDeliveryLines = db?.deliveryLineDao()?.getAllCertifiedByPlanification(
                planification?.id!!)
            Log.i(TAG,
                "certifiedDeliveryLines tiene ${certifiedDeliveryLines?.size} elementos: $certifiedDeliveryLines")
            if (!certifiedDeliveryLines.isNullOrEmpty()){
                Log.i(TAG,
                    "delivery lines certificados hasta ahora (${certifiedDeliveryLines.size}) $certifiedDeliveryLines")
                if (viewModel.certifiedDeliveryLines.value.isNullOrEmpty()) {
                    Log.i(TAG, "creando lista de certified delivery lines")
                    Log.i(TAG, "posting value to certifiedDeliveryLines")
                    viewModel.certifiedDeliveryLines.postValue(certifiedDeliveryLines.toMutableList())
                } else {
                    Log.i(TAG, "agregando al final de lista de certified delivery lines")
                    certifiedDeliveryLines.filter{ deliveryLine ->
                        // filtramos las delivery lines que no esten en la lista de certificaciones
                        viewModel.certifiedDeliveryLines.value?.contains(deliveryLine) != true
                    }.let{ filteredDeliveryLines ->
                        Log.i(TAG,
                            "delivery lines certificadas que no estan en la lista de certificadas $filteredDeliveryLines")
                        if (filteredDeliveryLines.isNotEmpty()){
                            Log.i(TAG,
                                "se agregaran ${filteredDeliveryLines.size} elementos a la lista de pendientes")
                            viewModel.certifiedDeliveryLines.value?.addAll(filteredDeliveryLines.toMutableList())
                            Log.i(TAG, "posting value to certifiedDeliveryLines")
                            viewModel.certifiedDeliveryLines.postValue(viewModel.certifiedDeliveryLines.value)
                        }
                    }
                }
                Log.i(TAG,
                    "delivery lines certificados hasta ahora (${certifiedDeliveryLines.size}) $certifiedDeliveryLines")
                if (!viewModel.pendingDeliveryLines.value.isNullOrEmpty()) {
                    Log.i(TAG,
                        "removiendo delivery lines ya certificados de la lista de delivery lines pendientes")
                    viewModel.pendingDeliveryLines.value?.removeAll {
                        certifiedDeliveryLines.contains(it)
                    }
                    Log.i(TAG,
                        "delivery lines pendientes exceptuando los ya certificados: ${viewModel.pendingDeliveryLines.value}")
                }
            }
            if (viewModel.pendingDeliveryLines.value.isNullOrEmpty() && !uncertifiedDeliveryLines.isNullOrEmpty()){
                Log.i(TAG,
                    "la lista de pendings esta vacia y hay ${uncertifiedDeliveryLines.size} delivery lines no certificados: $uncertifiedDeliveryLines")
                Log.i(TAG, "posting value to pendingDeliveryLines")
                viewModel.pendingDeliveryLines.postValue(uncertifiedDeliveryLines.toMutableList())
            } else if (!viewModel.pendingDeliveryLines.value.isNullOrEmpty() && !uncertifiedDeliveryLines.isNullOrEmpty()){
                Log.i(TAG,
                    "la lista de pendings no esta vacia y hay ${uncertifiedDeliveryLines.size} delivery lines no certificados $uncertifiedDeliveryLines")
                uncertifiedDeliveryLines.filter{ deliveryLine ->
                        // filtramos las delivery lines que no esten en la lista de pendientes
                        viewModel.pendingDeliveryLines.value?.contains(deliveryLine) != true
                    }.let{ filteredDeliveryLines ->
                    Log.i(TAG,
                        "delivery lines no certificados que no estan en la lista de pendientes (${filteredDeliveryLines.size}) $filteredDeliveryLines")
                    if (filteredDeliveryLines.isNotEmpty()){
                        Log.i(TAG,
                            "se agregaran ${filteredDeliveryLines.size} elementos a la lista de pendientes")
                        viewModel.pendingDeliveryLines.value?.addAll(filteredDeliveryLines.toMutableList())
                        Log.i(TAG, "posting value to pendingDeliveryLines")
                        viewModel.pendingDeliveryLines.postValue(viewModel.pendingDeliveryLines.value)
                    }
                }
            }
        }
    }

    fun hideViews() {
        runOnUiThread {
            if (layout != null) {
                layout.visibility = View.GONE
            }
        }
    }

    fun showViews() {
        runOnUiThread {
            if (layout != null) {
                layout.visibility = View.VISIBLE
            }
        }
    }

    fun fetchPlanificationLinesReq() {
        fetchData(this::fetchPlanificationLines)
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
        val url = "planificationDeliveryVO1s/search/findByPlanificationId?planificationId=${planification?.id}"
        Log.i(TAG, "planification lines endpoint: ${url}")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        var deliveryMap: HashMap<String, PlanificationLine>? = hashMapOf()
        hideViews()
        showLoader()
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    Log.i(TAG,
                        "fetching planification lines for planification ${planification?.id}")
                    val call = dataService.getPlanificationLines(url, "Bearer $accessToken").execute()
                    val response = call.body()
                    showViews()
                    hideLoader()
                    if (!response?._embedded?.planificationDeliveryVO1s.isNullOrEmpty()) {
                        Log.i(TAG,
                            "lineas cargadas de internet ${response?._embedded?.planificationDeliveryVO1s}")
                        db?.deliveryDao()?.insertAll(response?._embedded?.planificationDeliveryVO1s!!)
                        Log.i(TAG,
                            "deliveries de planificacion: ${response?._embedded?.planificationDeliveryVO1s}")
                        Log.i(TAG, "posting value to planificationLines")
                        viewModel.planificationLines.postValue(response?._embedded?.planificationDeliveryVO1s)
                    }
                } catch (toe: SocketTimeoutException) {
                    hideLoader()
                    showRetryMessage("Network error fetching user planification lines",
                        this@CertificateActivity::fetchPlanificationLinesReq)
                    showSnackbar("Fetching user planification lines failed")
                    Log.e(TAG, "Network error when querying planification lines endpoint", toe)
                } catch (ioEx: IOException) {
                    hideLoader()
                    showRetryMessage("Network error fetching user planification lines",
                        this@CertificateActivity::fetchPlanificationLinesReq)
                    showSnackbar("Fetching user planification lines failed")
                    Log.e(TAG, "Network error when querying planification lines endpoint", ioEx)
                } catch (jsonEx: JSONException) {
                    hideLoader()
                    showRetryMessage("Error parsing user planification lines",
                        this@CertificateActivity::fetchPlanificationLinesReq)
                    Log.e(TAG, "Failed to parse planification lines response", jsonEx)
                    showSnackbar("Failed to parse planification lines")
                } catch (e: Exception) {
                    hideLoader()
                    showRetryMessage("Fetching user planification lines failed",
                        this@CertificateActivity::fetchPlanificationLinesReq)
                    showSnackbar("Fetching planification lines failed")
                    Log.e(TAG, "Unknown exception: ", e)
                }
            }
        }
    }

    private fun fetchPlanificationDeliveryLines(
        accessToken: String?,
        idToken: String?,
        ex: AuthorizationException?,
    ) {
        if (ex != null) {
            Log.e(TAG,
                "ocurrio una excepcion mientras se recuperaban los delivery lines de planificacion",
                ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", this::signOut)
            }
            return
        }
        //val url = "delivery/label/planifications"
        val url = "planificationCertificationVO1s/search/findByPlanificationId?planificationId=${planification?.id}"
        Log.i(TAG, "planification lines endpoint: ${url}")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        hideViews()
        showLoader()
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    Log.i(TAG, "fetching delivery lines for planification ${planification?.id}")
                    val call = dataService.getPlanificationDeliveryLines(url, "Bearer $accessToken").execute()
                    val response = call.body()
                    showViews()
                    hideLoader()
                    Log.i(TAG, "reponse delivery lines: $response")
                    if (!response?._embedded?.planificationCertificationVO1s.isNullOrEmpty()) {
                        val deliveryLines = mutableListOf<DeliveryLine>()
                        response?._embedded?.planificationCertificationVO1s?.forEach { deliveryLine ->
                            for (j in 0 until deliveryLine.quantity) {
                                deliveryLines.add(deliveryLine.copy(index = j,
                                    deliveryId = deliveryLine.deliveryId,
                                    scannedOrder = j + 1,
                                    planificationId = planification?.id!!))
                            }
                        }
                        Log.i(TAG, "delivery lines cargadas de internet y parseadas $deliveryLines")
                        if (deliveryLines.isNotEmpty()) {
                            db?.deliveryLineDao()?.insertAll(deliveryLines)
                            doAsync { checkCertifications()}
                        }
                    } else {
                        Log.e(TAG, "la planificacion no tiene delivery lines")
                    }
                    fetchData(this@CertificateActivity::fetchCertifiedLines)
                } catch (toe: SocketTimeoutException) {
                    hideLoader()
                    showRetryMessage("Network error fetching user planification lines",
                        this@CertificateActivity::fetchPlanificationDeliveryLinesReq)
                    showSnackbar("Fetching user planification lines failed")
                    Log.e(TAG, "Network error when querying planification lines endpoint", toe)
                } catch (ioEx: IOException) {
                    hideLoader()
                    showRetryMessage("Network error fetching user planification lines",
                        this@CertificateActivity::fetchPlanificationDeliveryLinesReq)
                    showSnackbar("Fetching user planification lines failed")
                    Log.e(TAG, "Network error when querying planification lines endpoint", ioEx)
                } catch (jsonEx: JSONException) {
                    hideLoader()
                    showRetryMessage("Error parsing user planification lines",
                        this@CertificateActivity::fetchPlanificationDeliveryLinesReq)
                    Log.e(TAG, "Failed to parse planification lines response", jsonEx)
                    showSnackbar("Failed to parse planification lines")
                } catch (e: Exception) {
                    hideLoader()
                    showRetryMessage("Fetching user planification lines failed",
                        this@CertificateActivity::fetchPlanificationDeliveryLinesReq)
                    showSnackbar("Fetching planification lines failed")
                    Log.e(TAG, "Unknown exception: ", e)
                }
            }
        }
    }

    fun fetchPlanificationDeliveryLinesReq() {
        fetchData(this::fetchPlanificationDeliveryLines)
    }

    private fun showRetryMessage(message: String, callback: () -> Unit) {
        runOnUiThread{
            messageHomeTv?.text = message
            retryBtn?.setOnClickListener {
                callback.invoke()
            }
            retryLayout?.visibility = View.VISIBLE
        }
    }

   override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_planification, menu)
        menu.findItem(R.id.startRoute).isVisible = false
        menu.findItem(R.id.endRoute).isVisible = false

        when(planification?.state) {
            "Dispatched" -> {
                menu.findItem(R.id.startRoute).isVisible = true
                menu.findItem(R.id.endRoute).isVisible = false
            }
            "OnGoing" -> {
                menu.findItem(R.id.startRoute).isVisible = false
                menu.findItem(R.id.endRoute).isVisible = true
            }
            "Cancelled" -> {
                menu.findItem(R.id.startRoute).isVisible = true
                menu.findItem(R.id.endRoute).isVisible = false
            }
            "Complete" -> {
                menu.findItem(R.id.startRoute).isVisible = false
                menu.findItem(R.id.endRoute).isVisible = false
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.startRoute -> {
                askForChangeState("OnGoing")
                true
            }
            R.id.endRoute -> {
                askForChangeState("Complete")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        when(planification?.state) {
            "Dispatched" -> {
                menu?.findItem(R.id.startRoute)?.isVisible = true
                menu?.findItem(R.id.endRoute)?.isVisible = false
            }
            "OnGoing" -> {
                menu?.findItem(R.id.startRoute)?.isVisible = false
                menu?.findItem(R.id.endRoute)?.isVisible = true
            }
            "Cancelled" -> {
                menu?.findItem(R.id.startRoute)?.isVisible = true
                menu?.findItem(R.id.endRoute)?.isVisible = false
            }
            "Complete" -> {
                menu?.findItem(R.id.startRoute)?.isVisible = false
                menu?.findItem(R.id.endRoute)?.isVisible = false
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    fun askForChangeState(state: String) {
        newState = state
        when(state) {
            "Cancelled" -> {
                showAlert(getString(R.string.cancel_planification),
                    getString(R.string.cancel_planification_prompt),
                    this::changePlanificationState)
            }
            "OnGoing" -> {
                showAlert(getString(R.string.start_route),
                    getString(R.string.start_route_prompt),
                    this::changePlanificationState)
            }
            "Complete" -> {
                showAlert(getString(R.string.complete_route),
                    getString(R.string.complete_route_prompt),
                    this::changePlanificationState)
            }
        }
    }

    fun changePlanificationState() {
        fetchData(this::changePlanificationState)
    }

    private fun changePlanificationState(
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
        hideViews()
        showLoader()
        showSnackbar("Solicitando finalizacion de ruta...")
        val url = "planification/${planification?.id}/changeState?newState=$newState"
        //Log.i(TAG_PLANIFICATIONS, "constructed user endpoint: $userInfoEndpoint")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    /*val call = dataService.finalizePlanificationLoad(url,
                        "Bearer $accessToken")
                        .execute()*/
                    val call = dataService.changePlanificationState(url,
                        "Bearer $accessToken")
                        .execute()
                    val response = call.body()
                    Log.i(TAG,
                        "respuesta al cambiar estado de planificacion ${planification?.id}: ${response}")
                    hideLoader()
                    showViews()
                    routeStarted = true
                    planification?.state = newState
                    Log.i(TAG, "posting value to planification")
                    viewModel.planification.postValue(planification)
                    try {
                        db?.planificationDao()?.update(planification!!)
                    } catch (ex: SQLiteException) {
                        Log.e(TAG,
                            "Error actualizando planificacion en la BD local",
                            ex)
                        showAlert(getString(R.string.database_error),
                            getString(R.string.database_error_saving_planifications))
                    } catch (ex: SQLiteConstraintException) {
                        Log.e(TAG,
                            "Error actualizando planificacion en la BD local",
                            ex)
                        showAlert(getString(R.string.database_error),
                            getString(R.string.database_error_saving_planifications))
                    } catch (ex: Exception) {
                        Log.e(TAG,
                            "Error actualizando planificacion en la BD local",
                            ex)
                        showAlert(getString(R.string.database_error),
                            getString(R.string.database_error_saving_planifications))
                    }
                    Log.i(TAG, "finalize planification load response $response")
                } catch (toe: SocketTimeoutException) {
                    Log.e(TAG, "Network error when finalizing planification load", toe)
                    showAlert(getString(R.string.network_error_title),
                        getString(R.string.network_error))
                } catch (ioEx: IOException) {
                    Log.e(TAG,
                        "Network error when finalizing planification load",
                        ioEx)
                    showAlert(getString(R.string.network_error_title),
                        getString(R.string.network_error))
                } catch (jsonEx: JSONException) {
                    Log.e(TAG, "Failed to parse finalizing planification response", jsonEx)
                    showAlert(getString(R.string.parsing_error_title),
                        getString(R.string.parsing_error))
                }
            }
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

    fun showAlert(
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

    fun hideLoader() {
        runOnUiThread {
            progressBar3.visibility = View.GONE
            retryLayout?.visibility = View.GONE
        }
    }

    fun showLoader() {
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
}