package com.tautech.cclappoperators.activities

import android.content.Intent
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.services.CclClient
import com.tautech.cclappoperators.services.MyWorkerManagerService
import kotlinx.android.synthetic.main.activity_dashboard.*
import net.openid.appauth.*
import retrofit2.Retrofit

class DashboardActivity: AppCompatActivity() {
    val TAG = "DASHBOARD_ACTIVITY"
    var db: AppDatabase? = null
    private var mStateManager: AuthStateManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
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
        urbanBtnTv.setOnClickListener{
            val intent = Intent(this, UrbanPlanificationsActivity::class.java)
            //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            //finish()
        }
        nationalBtnTv.setOnClickListener{
            val intent = Intent(this, PlanificationsActivity::class.java)
            startActivity(intent)
        }
        transferDeliveriesBtnTv.setOnClickListener{
            val intent = Intent(this, TransferActivity::class.java)
            startActivity(intent)
        }
        redispatchDeliveriesBtnTv.setOnClickListener{
            val intent = Intent(this, RedispatchActivity::class.java)
            startActivity(intent)
        }
        Log.i(TAG, "carrier: ${mStateManager?.carrierPartner}")
        Log.i(TAG, "carrier address: ${mStateManager?.customerAddress}")
        Log.i(TAG, "customer: ${mStateManager?.customer}")
        MyWorkerManagerService.uploadFailedRedispatches(this)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        /*Log.i(TAG, "Pausando la actividad. Guardando datos...")
        // user info is retained to survive activity restarts, such as when rotating the
        // device or switching apps. This isn't essential, but it helps provide a less
        // jarring UX when these events occur - data does not just disappear from the view.
        if (mStateManager?.userInfo != null) {
            Log.i(TAG, "Guardando keycloak user ${mStateManager?.userInfo}")
            state.putSerializable(KEY_PROFILE_INFO, mStateManager?.keycloakUser)
        }
        if (mStateManager?.driverInfo != null) {
            Log.i(TAG, "Guardando driver info ${mStateManager?.driverInfo}")
            state.putSerializable(KEY_DRIVER_INFO, mStateManager?.driverInfo)
        }
        if (mStateManager?.userInfo != null) {
            Log.i(TAG, "Guardando user info ${mStateManager?.userInfo}")
            state.putString(KEY_USER_INFO, mStateManager?.userInfo.toString())
        }*/
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
                        true)
                } else {
                    mStateManager?.signOut(this)
                    val mainIntent = Intent(this,
                        LoginActivity::class.java)
                    mainIntent.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(mainIntent)
                    finish()
                }
            } else {
                Log.e(TAG, "Error al intentar finalizar sesion", ex)
                showAlert("Error",
                    "No se pudo finalizar la sesion remota",
                    true)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.default_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.default_action -> {
                Log.i(TAG, "solicitando logout...")
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun hideLoader() {
        runOnUiThread {
            progressBar2.visibility = View.GONE
        }
    }

    fun showLoader() {
        runOnUiThread{
            messageTv2.visibility = View.GONE
            progressBar2.visibility = View.VISIBLE
        }
    }

    fun showAlert(title: String, message: String, exitToLogin: Boolean = false) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", null)
            val dialog: AlertDialog = builder.create();
            if (!isFinishing && !isDestroyed) {
                dialog.show();
                dialog.setOnDismissListener {
                    if (exitToLogin) {
                        mStateManager?.signOut(this)
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                Snackbar.make(container,
                    message,
                    Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }
}