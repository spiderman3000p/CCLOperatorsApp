package com.tautech.cclappoperators.activities

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.*
import android.os.Bundle
import android.util.Log
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
import com.tautech.cclappoperators.models.Planification
import com.tautech.cclappoperators.services.CclClient
import com.tautech.cclappoperators.services.MyWorkerManagerService
import kotlinx.android.synthetic.main.activity_main.layout
import kotlinx.android.synthetic.main.activity_main.progressBar3
import kotlinx.android.synthetic.main.activity_main.retryBtn
import kotlinx.android.synthetic.main.activity_main.retryLayout
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

class TransferActivity : AppCompatActivity() {
    val TAG = "TRANSFER_ACTIVITY"
    private var mStateManager: AuthStateManager? = null
    var db: AppDatabase? = null
    private val viewModel: TransferActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)
        val navView: BottomNavigationView = findViewById(R.id.nav_view4)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.navigation_scan_transfer,
            R.id.navigation_transfered,
            R.id.navigation_deliveries,
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
        viewModel.sourcePlanification.observe(this, Observer { _planification ->
            if (_planification != null) {
                fetchSourcePlanificationLinesReq()
                Log.i(TAG, "Planification origen observada: ${_planification.id}")
            } else {
                viewModel.deliveries.postValue(arrayListOf())
            }
        })
        val extras = intent.extras
        if (extras != null) {
            // TODO obtener planificacion id de shared preferences y luego la planificacion de la BD
            if (extras.containsKey("planification")) {
                val planification = extras.getSerializable("planification") as Planification
                Log.i(TAG, "posting value to planification")
                viewModel.sourcePlanification.postValue(planification)
            } else {
                Log.i(TAG, "no se recibio ninguna planificacion.")
            }
        } else {
            Log.i(TAG, "no se recibieron datos en el intent")
        }
        MyWorkerManagerService.uploadFailedTransfers(this)
        fetchData(this::fetchUserPlanifications)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        // user info is retained to survive activity restarts, such as when rotating the
        // device or switching apps. This isn't essential, but it helps provide a less
        // jarring UX when these events occur - data does not just disappear from the view.
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

    private fun fetchSourcePlanificationLines(
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
        val planificationId = viewModel.sourcePlanification.value?.id
        val url = "planificationDeliveryVO1s/search/findByPlanificationId?planificationId=${planificationId}"
        Log.i(TAG, "planification lines endpoint: ${url}")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        showLoader()
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    Log.i(TAG,
                        "fetching planification lines for planification ${planificationId}")
                    val call = dataService.getPlanificationLines(url, "Bearer $accessToken").execute()
                    val response = call.body()
                    Log.i(TAG,
                        "respuesta de lineas de planificacion ${planificationId}: $response")
                    val deliveries = response?._embedded?.planificationDeliveryVO1s?.filter{
                        it.deliveryState == "Planned" || it.deliveryState == "ReDispatched"
                    } ?: arrayListOf()
                    hideLoader()
                    if (!deliveries.isNullOrEmpty()) {
                        Log.i(TAG,
                            "deliveries de planificacion filtrados: ${deliveries.toMutableList()}")
                    } else {
                        showSnackbar("La planificacion de origen no tiene guias validas")
                    }
                    viewModel.deliveries.postValue(deliveries.toMutableList())
                } catch (toe: SocketTimeoutException) {
                    hideLoader()
                    showRetryMessage("Network error fetching user planification lines",
                        this@TransferActivity::fetchSourcePlanificationLinesReq)
                    showSnackbar("Fetching user planification lines failed")
                    Log.e(TAG, "Network error when querying planification lines endpoint", toe)
                } catch (ioEx: IOException) {
                    hideLoader()
                    showRetryMessage("Network error fetching user planification lines",
                        this@TransferActivity::fetchSourcePlanificationLinesReq)
                    showSnackbar("Fetching user planification lines failed")
                    Log.e(TAG, "Network error when querying planification lines endpoint", ioEx)
                } catch (jsonEx: JSONException) {
                    hideLoader()
                    showRetryMessage("Error parsing user planification lines",
                        this@TransferActivity::fetchSourcePlanificationLinesReq)
                    Log.e(TAG, "Failed to parse planification lines response", jsonEx)
                    showSnackbar("Failed to parse planification lines")
                } catch (e: Exception) {
                    hideLoader()
                    showRetryMessage("Fetching user planification lines failed",
                        this@TransferActivity::fetchSourcePlanificationLinesReq)
                    showSnackbar("Fetching planification lines failed")
                    Log.e(TAG, "Unknown exception: ", e)
                }
            }
        }
    }

    fun fetchSourcePlanificationLinesReq() {
        fetchData(this::fetchSourcePlanificationLines)
    }

    private fun fetchUserPlanifications(
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
            doAsync {
                try {
                    val call = dataService.getPlanifications(url, mStateManager?.customer?.addressId!!,"Bearer $accessToken").execute()
                    val response = call.body()
                    hideLoader()
                    val allPlanifications = response?.content?.filter {
                        it.state == "Created" || it.state == "Dispatched" || it.state == "Complete"
                    } ?: arrayListOf()
                    viewModel.planifications.postValue(allPlanifications.toMutableList())
                    Log.i(TAG, "planifications response with retrofit: $allPlanifications")
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
    }

    private fun showRetryMessage(message: String, callback: () -> Unit) {
        runOnUiThread{
            messageTransferTv?.text = message
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