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
import android.widget.LinearLayout
import android.widget.TextView
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
import com.tautech.cclappoperators.classes.Configuration
import com.tautech.cclappoperators.classes.PreviewPlanificationDialog
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.Planification
import com.tautech.cclappoperators.services.CclClient
import kotlinx.android.synthetic.main.activity_planification_detail.*
import kotlinx.android.synthetic.main.fragment_planification_preview.*
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


class PlanificationDetailActivity : AppCompatActivity() {
    private var newState: String = ""
    val TAG = "PLANIF_DETAIL_ACTIVITY"
    var loadingData = false
    var planification: Planification? = null
    private var mStateManager: AuthStateManager? = null
    var db: AppDatabase? = null
    private val viewModel: PlanificationDetailActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planification_detail)
        val navView: BottomNavigationView = findViewById(R.id.nav_view2)
        val navController = findNavController(R.id.nav_host_fragment_urban)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_deliveries, R.id.navigation_finished, R.id.navigation_resume_urban))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        supportFragmentManager.beginTransaction()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        mStateManager = AuthStateManager.getInstance(this)
        val config = Configuration.getInstance(this)
        try {
            db = AppDatabase.getDatabase(this)
        } catch(ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        if (config.hasConfigurationChanged()) {
            showAlert("Error", "La configuracion de sesion ha cambiado. Se cerrara su sesion", this::signOut)
            return
        }
        if (!mStateManager!!.current.isAuthorized) {
            showAlert("Sesion expirada", "Su sesion ha expirado", this::signOut)
            return
        }
        viewModel.planification.observe(this, Observer{_planification ->
            if (_planification != null) {
                planification = _planification
                invalidateOptionsMenu()
                if (!loadingData) {
                    loadingData = true
                    if (viewModel.deliveries.value.isNullOrEmpty()) {
                        doAsync {
                            fetchPlanificationDataReq()
                        }
                    } else {
                        doAsync {
                            loadDeliveriesFromLocal()
                        }
                    }
                }
            }
        })
        viewModel.deliveries.observe(this, Observer{_deliveries ->
            if(!_deliveries.isNullOrEmpty()){
                invalidateOptionsMenu()
            }
        })
        val extras = intent.extras
        if (extras != null) {
            // TODO obtener planificacion id de shared preferences y luego la planificacion de la BD
            if (extras.containsKey("planification")) {
                planification = extras.getSerializable("planification") as Planification
                Log.i(TAG, "planification recibido en extras ${planification?.id}")
            } else {
                Log.i(TAG, "no se recibio ninguna planificacion. enviando a planificaciones")
                finish()
            }
        } else {
            Log.i(TAG, "no se recibieron datos")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume()...")
        mStateManager?.revalidateSessionData(this)
        loadPlanification(planification?.id)
    }

    private fun loadPlanification(planificationId: Long?){
        doAsync {
            val _planification = db?.planificationDao()?.getById(planificationId!!)
            viewModel.planification.postValue(_planification)
            Log.i(TAG, "planification loaded from local DB: $_planification")
        }
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        // user info is retained to survive activity restarts, such as when rotating the
        // device or switching apps. This isn't essential, but it helps provide a less
        // jarring UX when these events occur - data does not just disappear from the view.
        if (planification != null) {
            state.putSerializable("planification", planification)
        }
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

    fun hideViews() {
        runOnUiThread {
            if (layout2 != null) {
                layout2.visibility = View.GONE
            }
        }
    }

    fun showViews() {
        runOnUiThread {
            if (layout2 != null) {
                layout2.visibility = View.VISIBLE
            }
        }
    }

    private fun loadDeliveriesFromLocal(){
        loadingData = true
        showLoader()
        hideViews()
        doAsync {
            try {
                Log.i(TAG, "fetching planification ${planification?.id} deliveries from local DB")
                val deliveries = db?.deliveryDao()?.getAllByPlanification(planification?.id!!)
                if (!deliveries.isNullOrEmpty()) {
                    Log.i(TAG, "deliveries cargadas de BD local $deliveries")
                    viewModel.deliveries.postValue(deliveries.toMutableList())
                }
            } catch(ex: Exception) {
                Log.e(TAG, "Excepcion al cargar deliveries de la BD local", ex)
            } finally {
                loadingData = false
                hideLoader()
                showViews()
            }
        }
    }

    private fun fetchPlanificationData(
        accessToken: String?,
        idToken: String?,
        ex: AuthorizationException?,
    ) {
        if (ex != null) {
            Log.e(TAG, "ocurrio una excepcion mientras se recuperaban datos de la planificacion", ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", this::signOut)
            }
            return
        }
        hideViews()
        showLoader()
        val url = "planificationDeliveryVO1s/search/findByPlanificationId?planificationId=${planification?.id}"
        Log.i(TAG, "planification data endpoint: ${url}")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    Log.i(TAG, "fetching planification ${planification?.id} deliveries")
                    val call = dataService.getPlanificationLines(url, "Bearer $accessToken").execute()
                    val response = call.body()
                    showViews()
                    if (response != null && response._embedded.planificationDeliveryVO1s.isNotEmpty()) {
                        Log.i(TAG, "deliveries cargadas de internet ${response._embedded.planificationDeliveryVO1s}")
                        viewModel.deliveries.postValue(response._embedded.planificationDeliveryVO1s)
                        db?.deliveryDao()?.insertAll(response._embedded.planificationDeliveryVO1s)
                    }
                } catch(toe: SocketTimeoutException) {
                    showRetryMessage("Network error fetching user planification data",
                        this@PlanificationDetailActivity::fetchPlanificationDataReq)
                    showSnackbar("Fetching user planification data failed")
                    Log.e(TAG, "Network error when querying planification data endpoint", toe)
                } catch (ioEx: IOException) {
                    showRetryMessage("Network error fetching user planification data",
                        this@PlanificationDetailActivity::fetchPlanificationDataReq)
                    showSnackbar("Fetching user planification data failed")
                    Log.e(TAG, "Network error when querying planification data endpoint", ioEx)
                } catch (jsonEx: JSONException) {
                    showRetryMessage("Error parsing user planification data",
                        this@PlanificationDetailActivity::fetchPlanificationDataReq)
                    Log.e(TAG, "Failed to parse planification data response", jsonEx)
                    showSnackbar("Failed to parse planification data")
                } catch (e: Exception) {
                    showRetryMessage("Fetching user planification data failed",
                        this@PlanificationDetailActivity::fetchPlanificationDataReq)
                    showSnackbar("Fetching planification data failed")
                    Log.e(TAG, "Unknown exception: ", e)
                } finally {
                    loadingData = false
                    hideLoader()
                }
            }
        }
    }

    fun fetchPlanificationDataReq() {
        Log.i(TAG, "recuperando deliveries y delivery lines desde la BD remota...")
        fetchData(this::fetchPlanificationData)
    }

    private fun showRetryMessage(message: String, callback: () -> Unit) {
        runOnUiThread{
            messageHomeTv2?.text = message
            retryBtn2?.setOnClickListener {
                callback.invoke()
            }
            retryLayout2?.visibility = View.VISIBLE
        }
    }

    fun showAlert(title: String, message: String) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", null)
            val dialog: AlertDialog = builder.create();
            dialog.show();
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_planification, menu)
        menu.findItem(R.id.startRoute).isVisible = false
        menu.findItem(R.id.endRoute).isVisible = false
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
            "Created" -> {
                menu?.findItem(R.id.startRoute)?.isVisible = false
                menu?.findItem(R.id.endRoute)?.isVisible = false
            }
            "Dispatched" -> {
                menu?.findItem(R.id.startRoute)?.isVisible = true
                menu?.findItem(R.id.endRoute)?.isVisible = false
            }
            "OnGoing" -> {
                menu?.findItem(R.id.startRoute)?.isVisible = false
                menu?.findItem(R.id.endRoute)?.isVisible = true
            }
            "Cancelled" -> {
                menu?.findItem(R.id.startRoute)?.isVisible = false
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
                showAlert(getString(R.string.cancel_planification), getString(R.string.cancel_planification_prompt), this::changePlanificationState)
            }
            "OnGoing" -> {
                showAlert(getString(R.string.start_route), getString(R.string.start_route_prompt), this::changePlanificationState)
            }
            "Complete" -> {
                showAlert(getString(R.string.complete_route), getString(R.string.complete_route_prompt), this::showFinalizationPreview)
            }
        }
    }

    fun showFinalizationPreview(){
        PreviewPlanificationDialog.display(supportFragmentManager, this::changePlanificationState, planification?.id!!)
    }

    fun changePlanificationState() {
        fetchData(this::changePlanificationState)
    }

    private fun changePlanificationState(accessToken: String?, idToken: String?, ex: AuthorizationException?) {
        if (ex != null) {
            Log.e(TAG, "ocurrio una excepcion al cambiar estado de planificacion", ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", this::signOut)
            }
            return
        }
        hideViews()
        showLoader()
        showSnackbar("Solicitando cambio de estado...")
        val url = "planification/${planification?.id}/changeState?newState=$newState"
        //Log.i(TAG_PLANIFICATIONS, "constructed user endpoint: $userInfoEndpoint")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    val call = dataService.changePlanificationState(url,
                        "Bearer $accessToken")
                        .execute()
                    val response = call.body()
                    Log.i(TAG, "respuesta al cambiar estado de planificacion ${planification?.id}: ${response}")
                    showViews()
                    planification?.state = newState
                    viewModel.planification.postValue(planification)
                    try {
                        db?.planificationDao()?.update(planification!!)
                    } catch (ex: SQLiteException) {
                        Log.e(TAG,
                            "Error actualizando planificacion en la BD local",
                            ex)
                        showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                    } catch (ex: SQLiteConstraintException) {
                        Log.e(TAG,
                            "Error actualizando planificacion en la BD local",
                            ex)
                        showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                    } catch (ex: Exception) {
                        Log.e(TAG,
                            "Error actualizando planificacion en la BD local",
                            ex)
                        showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                    }
                    Log.i(TAG, "finalize planification load response $response")
                } catch(toe: SocketTimeoutException) {
                    Log.e(TAG, "Network error when finalizing planification load", toe)
                    showAlert(getString(R.string.network_error_title), getString(R.string.network_error))
                } catch (ioEx: IOException) {
                    Log.e(TAG,
                        "Network error when finalizing planification load",
                        ioEx)
                    showAlert(getString(R.string.network_error_title), getString(R.string.network_error))
                } catch (jsonEx: JSONException) {
                    Log.e(TAG, "Failed to parse finalizing planification response", jsonEx)
                    showAlert(getString(R.string.parsing_error_title), getString(R.string.parsing_error))
                } finally {
                    hideLoader()
                }
            }
        }
    }

    private fun fetchData(callback: ((String?, String?, AuthorizationException?) -> Unit)) {
        Log.i(TAG, "oning data...$callback")
        mStateManager?.current?.performActionWithFreshTokens(mStateManager?.mAuthService!!, callback)
    }

    fun showAlert(title: String, message: String, positiveCallback: (() -> Unit)? = null, negativeCallback: (() -> Unit)? = null) {
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
            if (!(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && this.isDestroyed) || !this.isFinishing) {
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    }

    fun hideLoader() {
        runOnUiThread {
            progressBar3.visibility = View.GONE
            retryLayout2?.visibility = View.GONE
        }
    }

    fun showLoader() {
        runOnUiThread{
            progressBar3.visibility = View.VISIBLE
            retryLayout2?.visibility = View.GONE
        }
    }

    private fun showSnackbar(message: String) {
        runOnUiThread {
            if(!(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && this.isDestroyed) && !this.isFinishing) {
                Snackbar.make(contentView!!,
                    message,
                    Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }
}