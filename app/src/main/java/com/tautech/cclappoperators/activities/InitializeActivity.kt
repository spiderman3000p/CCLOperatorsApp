package com.tautech.cclappoperators.activities

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.annotation.MainThread
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.interfaces.KeycloakDataService
import com.tautech.cclappoperators.models.Customer
import com.tautech.cclappoperators.models.Address
import com.tautech.cclappoperators.models.CarrierPartner
import com.tautech.cclappoperators.services.CclClient
import com.tautech.cclappoperators.services.KeycloakClient
import kotlinx.android.synthetic.main.activity_initialize.*
import net.openid.appauth.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.format.DateTimeFormat
import org.json.JSONException
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException

class InitializeActivity : AppCompatActivity() {
    private var carrierAddresses: ArrayList<Address> = arrayListOf()
    private var customers: ArrayList<Customer> = arrayListOf()
    private var spinnerCustomers: ArrayList<String> = arrayListOf()
    private var spinnerCarrierAddresses: ArrayList<String> = arrayListOf()
    private var isLoadingCustomerAddresses: Boolean = false
    private var isLoadingCarriers: Boolean = false
    private var isLoadingCustomers: Boolean = false
    val TAG = "INITIALIZE_ACTIVITY"
    var db: AppDatabase? = null
    private var mStateManager: AuthStateManager? = null
    private var isLoadingUserProfile: Boolean = false
    private var carrierAddressesAdapter: ArrayAdapter<String>? = null
    private var customersAdapter: ArrayAdapter<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize)
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
        // the stored AuthState is incomplete, so check if we are currently receiving the result of
        // the authorization flow from the browser.
        val response = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)
        when {
            response?.authorizationCode != null -> {
                // authorization code exchange is required
                Log.i(TAG, "exchanging token...")
                mStateManager?.updateAfterAuthorization(response, ex)
                exchangeAuthorizationCode(response)
            }
            ex != null -> {
                Log.e(TAG, "Authorization flow failed: " + ex.message, ex)
                mStateManager?.refreshAccessToken()
            }
            mStateManager?.current?.isAuthorized == true -> {
                displayAuthorized()
            }
        }
        carrierAddressSp.isEnabled = false
        customerSp.isEnabled = false
        carrierAddressesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerCarrierAddresses)
        customersAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerCustomers)
        carrierAddressesAdapter?.setDropDownViewResource(android.R.layout.select_dialog_item)
        customersAdapter?.setDropDownViewResource(android.R.layout.select_dialog_item)
        carrierAddressSp.adapter = carrierAddressesAdapter
        customerSp.adapter = customersAdapter
        carrierAddressSp.onItemSelectedListener = object: OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val carrierDescription = carrierAddressesAdapter?.getItem(position)
                mStateManager?.customerAddress = carrierAddresses.find {
                    it.description == carrierDescription
                }
                //mStateManager?.customerAddress = carrierAddresses.get(position)
                Log.i(TAG, "carrier address selected ${mStateManager?.customerAddress}")
                fetchData(this@InitializeActivity::fetchCustomers)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        customerSp.onItemSelectedListener = object: OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.i(TAG, "customer position: $position")
                val customerName = customersAdapter?.getItem(position)
                Log.i(TAG, "customer name: $customerName")
                mStateManager?.customer = customers.find {
                    it.name == customerName
                }
                Log.i(TAG, "customer address selected ${mStateManager?.customer}")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
        submitBtn.setOnClickListener {
            if(mStateManager?.keycloakUser != null && mStateManager?.customerAddress != null && mStateManager?.customer != null) {
                val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
                val gson = Gson()
                Log.i(TAG, "customerAddress final seleccionado: ${mStateManager?.customerAddress}")
                Log.i(TAG, "customer final seleccionado: ${mStateManager?.customer}")
                with(sharedPref.edit()) {
                    putString("keycloakUserJSON", gson.toJson(mStateManager?.keycloakUser))
                    putString("customerJSON", gson.toJson(mStateManager?.customer))
                    putString("addressJSON", gson.toJson(mStateManager?.customerAddress))
                    commit()
                }
                val intent = Intent(this, DashboardActivity2::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            } else {
                showAlert("Error", "Invalid data")
            }
        }
    }

    private fun displayAuthorized() {
        val state = mStateManager!!.current
        //Log.i(TAG, "token: ${state.idToken ?: "no id token returned"}")
        if (state.accessToken == null) {
            Log.e(TAG, "token: ${state.accessToken ?: "no access token returned"}")
            mStateManager?.refreshAccessToken()
            return
        } else {
            val expiresAt = state.accessTokenExpirationTime
            when {
                expiresAt == null -> {
                    Log.i(TAG, "no access token expiry")
                }
                expiresAt < System.currentTimeMillis() -> {
                    Log.i(TAG, "access token expired")
                    showSnackbar("Access token expired")
                    mStateManager?.refreshAccessToken()
                }
                else -> {
                    Log.i(TAG, "access token expires at: ${
                        DateTimeFormat
                            .forPattern("yyyy-MM-dd HH:mm:ss ZZ").print(expiresAt)
                    }")
                    when {
                        mStateManager?.keycloakUser == null && !isLoadingUserProfile -> {
                            Log.i(TAG,
                                "No hay datos de keycloak user guardados, solicitando keycloak user data...")
                            fetchData(this::fetchUserProfile)
                        }
                        mStateManager?.carrierPartner == null && !isLoadingCarriers -> {
                            Log.i(TAG,
                                "No hay datos de carriers guardados, solicitando carriers...")
                            fetchData(this::fetchCarriers)
                        }
                        carrierAddresses.isNullOrEmpty() && !isLoadingCustomers -> {
                            Log.i(TAG,
                                    "No hay carrier addresses guardados, solicitando carrier addresses...")
                            fetchData(this::fetchCarrierAddressList)
                        }
                        mStateManager?.keycloakUser != null -> {
                            hideLoader()
                        }
                    }
                }
            }
        }
    }

    @MainThread
    private fun exchangeAuthorizationCode(authorizationResponse: AuthorizationResponse) {
        showLoader()
        mStateManager?.performTokenRequest(
            authorizationResponse.createTokenExchangeRequest()
        ) { tokenResponse: TokenResponse?, authException: AuthorizationException? ->
            handleCodeExchangeResponse(tokenResponse,
                authException)
        }
    }

    @MainThread
    private fun handleCodeExchangeResponse(
        tokenResponse: TokenResponse?,
        authException: AuthorizationException?,
    ) {
        mStateManager!!.updateAfterTokenResponse(tokenResponse, authException)
        if (!mStateManager!!.current.isAuthorized) {
            showAlert("Error", "Authorization Code exchange failed ", true)
        } else {
            /*val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
            if (sharedPref.contains("carrierPartnerJSON") && sharedPref.contains("customerJSON") && sharedPref.contains("addressJSON")){
                mStateManager?.carrierPartner = Gson().fromJson(sharedPref.getString("carrierPartnerJSON", null), CarrierPartner::class.java)
                mStateManager?.customer = Gson().fromJson(sharedPref.getString("customerJSON", null), Customer::class.java)
                mStateManager?.customerAddress = Gson().fromJson(sharedPref.getString("addressJSON", null), Address::class.java)
                val intent = Intent(this, DashboardActivity2::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            } else {*/
                displayAuthorized()
            //}
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

    private fun fetchUserProfile(
        accessToken: String?, idToken: String?, ex: AuthorizationException?,
    ) {
        if (ex != null) {
            Log.e(TAG,
                "ocurrio una excepcion mientras se recuperaban detalles del perfil de usuario",
                ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", true)
            }
            return
        }
        val userProfileEndpoint = mStateManager?.mConfiguration?.userProfileEndpointUri.toString()
        Log.i(TAG, "constructed user profile endpoint: $userProfileEndpoint")
        val dataService: KeycloakDataService? = KeycloakClient.getInstance()?.create(
            KeycloakDataService::class.java)
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    isLoadingUserProfile = true
                    showLoader()
                    val call = dataService.getUserProfile(userProfileEndpoint,
                        "Bearer $accessToken")
                        .execute()
                    val response = call.body()
                    isLoadingUserProfile = false
                    hideLoader()
                    Log.i(TAG, "user profile response $response")
                    if (response != null) {
                        mStateManager?.keycloakUser = response
                        val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
                        val gson = Gson()
                        with(sharedPref.edit()) {
                            putString("keycloakUserJSON", gson.toJson(response))
                            commit()
                        }
                        Log.i(TAG, "user profile fetched ${mStateManager?.keycloakUser}")
                        if (!mStateManager?.keycloakUser?.attributes?.userType?.get(0)?.toLowerCase()
                                .equals("operator")
                        ) {
                            showAlert("Error", "Tu usuario no es de tipo operador")
                            return@doAsync
                        }
                        if (mStateManager?.keycloakUser?.attributes?.carrier?.get(0) == null) {
                            showAlert("Error", "Tu usuario no esta asignado a ningun carrier")
                            return@doAsync
                        }
                        displayAuthorized()
                    }
                } catch (toe: SocketTimeoutException) {
                    hideLoader()
                    showSnackbar(getString(R.string.fetching_profile_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } catch (ioEx: IOException) {
                    Log.e(TAG,
                        "Network error when querying user profile endpoint",
                        ioEx)
                    showSnackbar(getString(R.string.fetching_profile_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } catch (jsonEx: JSONException) {
                    Log.e(TAG, "Failed to parse user profile response", jsonEx)
                    showSnackbar(getString(R.string.fetching_profile_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } finally {
                    isLoadingUserProfile = true
                }
            }
        }
    }

    private fun fetchCarriers(
        accessToken: String?, idToken: String?, ex: AuthorizationException?,
    ) {
        if (ex != null) {
            Log.e(TAG,
                "ocurrio una excepcion mientras se recuperaban carriers",
                ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", true)
            }
            return
        }
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    isLoadingCarriers = true
                    showLoader()
                    val call = dataService.getCarrierPartner("carrierPartners/${mStateManager?.keycloakUser?.attributes?.carrier?.get(0)}", "Bearer $accessToken")
                        .execute()
                    val response = call.body()
                    isLoadingCarriers = false
                    hideLoader()
                    Log.i(TAG, "carrier partners response $response")
                    if (response != null) {
                        mStateManager?.carrierPartner = response
                        val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("carrierPartnerJSON", Gson().toJson(mStateManager?.carrierPartner))
                            commit()
                        }
                        Log.i(TAG, "carrier partner fetched ${mStateManager?.carrierPartner}")
                        displayAuthorized()
                    }
                } catch (toe: SocketTimeoutException) {
                    hideLoader()
                    showSnackbar(getString(R.string.fetching_carriers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } catch (ioEx: IOException) {
                    Log.e(TAG,
                        "Network error when querying carrier partners endpoint",
                        ioEx)
                    showSnackbar(getString(R.string.fetching_carriers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } catch (jsonEx: JSONException) {
                    Log.e(TAG, "Failed to parse carrier partners response", jsonEx)
                    showSnackbar(getString(R.string.fetching_carriers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } finally {
                    isLoadingCarriers = true
                }
            }
        }
    }

    private fun fetchCarrierAddressList(
        accessToken: String?, idToken: String?, ex: AuthorizationException?,
    ) {
        if (ex != null) {
            Log.e(TAG,
                "ocurrio una excepcion mientras se recuperaban customers",
                ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", true)
            }
            return
        }
        val dataService: CclDataService? = CclClient.getInstance()?.create(
            CclDataService::class.java)
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    isLoadingCustomers = true
                    showLoader()
                    val url = "carrierPartners/${mStateManager?.keycloakUser?.attributes?.carrier?.get(0)}/addressList"
                    val call = dataService.getCarrierAddressList(url, "Bearer $accessToken")
                        .execute()
                    val response = call.body()
                    isLoadingCustomers = false
                    hideLoader()
                    Log.i(TAG, "carrier address list response $response")
                    if (response != null && !response._embedded.addresses.isNullOrEmpty()) {
                        carrierAddresses = response._embedded.addresses
                        spinnerCarrierAddresses.clear()
                        spinnerCarrierAddresses.addAll(response._embedded.addresses.map{
                            it.description ?: "Desconocido"
                        })
                        uiThread {
                            carrierAddressesAdapter?.notifyDataSetChanged()
                            carrierAddressSp.isEnabled = true
                            carrierAddressSp.setSelection(0, true)
                            val view = carrierAddressSp.getChildAt(0)
                            val itemId = carrierAddressSp.adapter.getItemId(0)
                            Log.i(TAG, "carrierAddressSp tiene ${customerSp.childCount} hijos")
                            Log.i(TAG, "view $view")
                            Log.i(TAG, "itemId $itemId")
                            Log.i(TAG, "performing click carrier spinner...")
                            if (!carrierAddressSp.performItemClick(view, 0, itemId)){
                                Log.e(TAG, "there isn't click listener for carrier spinner...")
                            }
                            val carrierDescription = carrierAddressesAdapter?.getItem(0)
                            mStateManager?.customerAddress = carrierAddresses.find {
                                it.description == carrierDescription
                            }
                        }
                    } else {
                        showAlert("Error", "No hay carriers asignados")
                    }
                } catch (toe: SocketTimeoutException) {
                    Log.e(TAG,
                        "Network error when querying carrier address list endpoint",
                        toe)
                    showSnackbar(getString(R.string.fetching_customers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } catch (ioEx: IOException) {
                    Log.e(TAG,
                        "Network error when querying carrier address list endpoint",
                        ioEx)
                    showSnackbar(getString(R.string.fetching_customers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } catch (jsonEx: JSONException) {
                    Log.e(TAG, "Failed to parse carrier address list response", jsonEx)
                    showSnackbar(getString(R.string.fetching_customers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } finally {
                    isLoadingCustomers = false
                }
            }
        }
    }

    private fun fetchCustomers(
            accessToken: String?, idToken: String?, ex: AuthorizationException?,
    ) {
        if (ex != null) {
            Log.e(TAG,
                    "ocurrio una excepcion mientras se recuperaban direcciones",
                    ex)
            if (ex.type == 2 && ex.code == 2002 && ex.error == "invalid_grant") {
                showAlert("Sesion expirada", "Su sesion ha expirado", true)
            }
            return
        }
        val dataService: CclDataService? = CclClient.getInstance()?.create(
                CclDataService::class.java)
        if (dataService != null && accessToken != null) {
            doAsync {
                try {
                    isLoadingCustomerAddresses = true
                    showLoader()
                    val selectedCustomer = carrierAddresses.get(carrierAddressSp.selectedItemPosition)
                    val url = "allowedAddressVO1s/search/findByCarrierAddressId?carrierAddressId=${selectedCustomer.id}"
                    val call = dataService.getCustomerAddressList(url, "Bearer $accessToken")
                            .execute()
                    val response = call.body()
                    isLoadingCustomerAddresses = false
                    hideLoader()
                    Log.i(TAG, "customers response $response")
                    if (response != null && !response._embedded.allowedAddressVO1s.isNullOrEmpty()) {
                        customers = response._embedded.allowedAddressVO1s
                        spinnerCustomers.clear()
                        spinnerCustomers.addAll(response._embedded.allowedAddressVO1s.map{
                            it.name ?: "Desconocido"
                        })
                        uiThread {
                            customersAdapter?.notifyDataSetChanged()
                            customerSp.isEnabled = true
                            customerSp.setSelection(0, true)
                            val view = customerSp.getChildAt(0)
                            val itemId = customerSp.adapter.getItemId(0)
                            Log.i(TAG, "customerSp tiene ${customerSp.childCount} hijos")
                            Log.i(TAG, "view $view")
                            Log.i(TAG, "itemId $itemId")
                            Log.i(TAG, "performing click customer spinner...")
                            if (!customerSp.performItemClick(view, 0, itemId)){
                                Log.e(TAG, "there isn't click listener for customer spinner...")
                            }
                            val name = customersAdapter?.getItem(0)
                            mStateManager?.customer = customers.find {
                                it.name == name
                            }
                        }
                    } else {
                        showAlert("Error", "No hay customers asignados")
                    }
                } catch (toe: SocketTimeoutException) {
                    Log.e(TAG,
                            "Network error when querying customers endpoint",
                            toe)
                    showSnackbar(getString(R.string.fetching_customers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } catch (ioEx: IOException) {
                    Log.e(TAG,
                            "Network error when querying customers endpoint",
                            ioEx)
                    showSnackbar(getString(R.string.fetching_customers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } catch (jsonEx: JSONException) {
                    Log.e(TAG, "Failed to parse customers response", jsonEx)
                    showSnackbar(getString(R.string.fetching_customers_error),Snackbar.LENGTH_INDEFINITE, getString(R.string.retry), this@InitializeActivity::displayAuthorized)
                } finally {
                    isLoadingCustomerAddresses = false
                }
            }
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

    private fun showSnackbar(message: String, length: Int = Snackbar.LENGTH_SHORT, actionStr: String? = null, action: (() -> Unit)? = null) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                val snack = Snackbar.make(container2,
                    message,
                    length)
                if (action != null){
                    val str = actionStr ?: "OK"
                    snack.setAction(str, View.OnClickListener {
                        action()
                    })
                }
                snack.show()
            }
        }
    }

    fun hideLoader() {
        runOnUiThread {
            progressBar.visibility = View.GONE
        }
    }

    fun showLoader() {
        runOnUiThread{
            progressBar.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        if (mStateManager?.current?.accessToken != null) {
            displayAuthorized()
        }
    }

    private fun fetchData(callback: ((String?, String?, AuthorizationException?) -> Unit)) {
        Log.i(TAG, "Fetching data...")
        showLoader()
        mStateManager!!.current.performActionWithFreshTokens(mStateManager!!.mAuthService,
            callback)
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
}