package com.tautech.cclappoperators.activities

/*import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase*/

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.tautech.cclappoperators.classes.AuthStateManager
import kotlinx.android.synthetic.main.activity_login.*
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.models.Address
import com.tautech.cclappoperators.models.CarrierPartner
import com.tautech.cclappoperators.models.Customer
import net.openid.appauth.*
import net.openid.appauth.browser.VersionedBrowserMatcher
import java.util.concurrent.CountDownLatch


class LoginActivity : AppCompatActivity() {
    val TAG = "LOGIN_ACTIVITY"
    private val mUsePendingIntents: Boolean = false
    private val EXTRA_FAILED = "failed"
    private val RC_AUTH = 100
    private lateinit var mAuthStateManager: AuthStateManager
    private var mClientId: String? = null
    private var mAuthRequest: AuthorizationRequest? = null
    private var mAuthIntent: CustomTabsIntent? = null
    private var mAuthIntentLatch = CountDownLatch(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        //Splash
        /*Thread.sleep(2000)
        setTheme(R.style.AppTheme)*/

        super.onCreate(savedInstanceState)
        //supportActionBar?.hide()
        setContentView(R.layout.activity_login)
        //analytics event
        /*val analytics: FirebaseAnalytics = Firebase.analytics
        analytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, "InitScreen")
        }*/
        /*FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            // Log and toast
            Log.d(TAG, token)
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })*/
        val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        if (!sharedPref.contains("firstRun")) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        mAuthStateManager = AuthStateManager.getInstance(this)
        if (!mAuthStateManager.mConfiguration.isValid) {
            //displayError(mConfiguration.getConfigurationError(), false)
            Log.e(TAG, "Error en configuracion de auth manager")
            //return
        }
        if (mAuthStateManager.mConfiguration.hasConfigurationChanged()) {
            // discard any existing authorization state due to the change of configuration
            Log.i(TAG, "Configuration change detected, discarding old state")
            mAuthStateManager.replace(AuthState())
            mAuthStateManager.mConfiguration.acceptConfiguration()
        }
        val resp = EndSessionResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        if (resp != null) {
            // authorization completed
            Log.i(TAG, "Logout successfull")
            mAuthStateManager.signOut(this)
        } else if (ex != null) {
            // authorization failed, check ex for more details
            Log.e(TAG, "Logout failed", ex)
            Toast.makeText(this, "Error al finalizar sesion", Toast.LENGTH_SHORT).show()
        }
        loginBtn.setOnClickListener {
           // doLogin()
            //getAccessToken()
            doAuth()
        }
        initializeAppAuth()
        if (mAuthStateManager.getCurrent().isAuthorized && !mAuthStateManager.mConfiguration.hasConfigurationChanged()) {
            Log.i(TAG, "User is already authenticated, proceeding to token activity")
            doAuth()
            return
        }
    }

    private fun createAuthRequest() {
        Log.i(TAG, "Creating auth request for login")
        val authRequestBuilder = AuthorizationRequest.Builder(
            mAuthStateManager.current.authorizationServiceConfiguration!!,
            mAuthStateManager.mConfiguration.clientId!!,
            ResponseTypeValues.CODE,
            mAuthStateManager.mConfiguration.redirectUri)
            .setScope(mAuthStateManager.mConfiguration.scope)
        val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        if (sharedPref.contains("isLoggedIn") && !sharedPref.getBoolean("isLoggedIn", false)) {
            authRequestBuilder.setPromptValues("login")
        }
        mAuthRequest = authRequestBuilder.build()
    }

    private fun recreateAuthorizationService() {
        if (mAuthStateManager.mAuthService != null) {
            Log.i(TAG, "Discarding existing AuthService instance")
            mAuthStateManager.mAuthService?.dispose()
        }
        mAuthStateManager.mAuthService = createAuthorizationService()
    }

    private fun createAuthorizationService(): AuthorizationService {
        Log.i(TAG, "Creating authorization service")
        val appAuthConfig = AppAuthConfiguration.Builder()
            .setBrowserMatcher(net.openid.appauth.browser.BrowserWhitelist(
                VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB))
            .build()
        return AuthorizationService(this, appAuthConfig)
    }

    private fun initializeAppAuth() {
        Log.i(TAG, "Initializing AppAuth")
        recreateAuthorizationService()
        if (mAuthStateManager.current.authorizationServiceConfiguration != null) {
            // configuration is already created, skip to client initialization
            Log.i(TAG, "auth config already established")
            initializeClient()
            return
        }

        // if we are not using discovery, build the authorization service configuration directly
        // from the static configuration values.

        // if we are not using discovery, build the authorization service configuration directly
        // from the static configuration values.
        if (mAuthStateManager.mConfiguration.discoveryUri == null) {
            Log.i(TAG, "Creating auth config from res/raw/auth_config.json")
            val config = AuthorizationServiceConfiguration(
                mAuthStateManager.mConfiguration.authEndpointUri!!,
                mAuthStateManager.mConfiguration.tokenEndpointUri!!,
                mAuthStateManager.mConfiguration.registrationEndpointUri,
                mAuthStateManager.mConfiguration.endSessionEndpoint)
            Log.i(TAG, "end session uri on config login ${config.endSessionEndpoint}")
            mAuthStateManager.replace(AuthState(config))
            Log.i(TAG,
                "end session uri on mAuthState login ${mAuthStateManager.current.authorizationServiceConfiguration?.endSessionEndpoint}")
            initializeClient()
            return
        }
    }

    private fun initializeClient() {
        if (mAuthStateManager.mConfiguration.clientId != null) {
            Log.i(TAG, "Using static client ID: " + mAuthStateManager.mConfiguration.clientId)
            // use a statically configured client ID
            mClientId = mAuthStateManager.mConfiguration.clientId
            initializeAuthRequest()
            return
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "on activity result")
        if (resultCode == RESULT_CANCELED) {
            Log.e(TAG, "Auth canceled")
        } else if (resultCode == RESULT_OK && requestCode == RC_AUTH){
            val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean("isLoggedIn", true)
                commit()
            }
            loginBtn.visibility = View.GONE
            /*if (sharedPref.contains("carrierPartnerJSON") && sharedPref.contains("customerJSON") && sharedPref.contains("addressJSON")){
                mAuthStateManager.carrierPartner = Gson().fromJson(sharedPref.getString("carrierPartnerJSON", null), CarrierPartner::class.java)
                mAuthStateManager.customer = Gson().fromJson(sharedPref.getString("customerJSON", null), Customer::class.java)
                mAuthStateManager.carrierAddress = Gson().fromJson(sharedPref.getString("addressJSON", null), Address::class.java)
            }
            var intent: Intent
            if (mAuthStateManager.customer == null || mAuthStateManager.carrierAddress == null || mAuthStateManager.carrierPartner == null || mAuthStateManager.keycloakUser == null) {*/
                intent = Intent(this, InitializeActivity::class.java)
            /*} else {
                intent = Intent(this, DashboardActivity2::class.java)
            }*/
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtras(data!!.extras!!)
            startActivity(intent)
            finish()
        }
    }


    private fun initializeAuthRequest() {
        createAuthRequest()
        warmUpBrowser()
    }

    private fun warmUpBrowser() {
        mAuthIntentLatch = CountDownLatch(1)
        Log.i(TAG, "Warming up browser instance for auth request")
        val intentBuilder =
            mAuthStateManager.mAuthService?.createCustomTabsIntentBuilder(mAuthRequest?.toUri())
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            intentBuilder?.setToolbarColor(getColor(R.color.material_on_primary_emphasis_medium))
        }
        mAuthIntent = intentBuilder?.build()
        mAuthIntentLatch.countDown()
    }

    private fun doAuth() {
        try {
            mAuthIntentLatch.await()
        } catch (ex: InterruptedException) {
            Log.w(TAG, "Interrupted while waiting for auth intent")
        }
        if (mUsePendingIntents) {
            val completionIntent = Intent(this, PlanificationsActivity::class.java)
            val cancelIntent = Intent(this, LoginActivity::class.java)
            cancelIntent.putExtra(EXTRA_FAILED, true)
            cancelIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            mAuthStateManager.mAuthService?.performAuthorizationRequest(
                mAuthRequest!!,
                PendingIntent.getActivity(this, 0, completionIntent, 0),
                PendingIntent.getActivity(this, 0, cancelIntent, 0),
                mAuthIntent!!)
        } else {
            val intent = mAuthStateManager.mAuthService?.getAuthorizationRequestIntent(
                mAuthRequest!!,
                mAuthIntent!!)
            startActivityForResult(intent, RC_AUTH)
        }
    }
}