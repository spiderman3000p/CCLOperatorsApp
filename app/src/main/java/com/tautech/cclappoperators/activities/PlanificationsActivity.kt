package com.tautech.cclappoperators.activities

import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
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
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.snackbar.Snackbar
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.adapters.PlanificationAdapter
import com.tautech.cclappoperators.adapters.PlanificationsPagedAdapter
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.classes.Configuration
import com.tautech.cclappoperators.classes.Constant
import com.tautech.cclappoperators.classes.RecyclerViewLoadMoreScroll
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.interfaces.OnLoadMoreListener
import com.tautech.cclappoperators.models.Planification
import com.tautech.cclappoperators.services.CclClient
import kotlinx.android.synthetic.main.activity_planifications.*
import kotlinx.android.synthetic.main.content_scrolling.*
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.EndSessionResponse
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONException
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException

const val KEY_PROFILE_INFO = "profileInfo"
class PlanificationsActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    val TAG = "PLANIFICATIONS_ACTIVITY"
    var searchStr: String = ""
    private var startRow = 0
    private var endRow = 10
    private var planifications: ArrayList<Planification?> = arrayListOf()
    private var mAdapter: PlanificationsPagedAdapter? = null
    lateinit var scrollListener: RecyclerViewLoadMoreScroll
    lateinit var mLayoutManager: RecyclerView.LayoutManager
    var db: AppDatabase? = null
    private var mStateManager: AuthStateManager? = null
    private val viewModel: PlanificationsActivityViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planifications)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = title
        mStateManager = AuthStateManager.getInstance(this)
        db = AppDatabase.getDatabase(this)
        val config = Configuration.getInstance(this)
        if (config.hasConfigurationChanged()) {
            showAlert("Error", "La configuracion de sesion ha cambiado. Se cerrara su sesion", true)
            return
        }
        initAdapter()
        viewModel.planifications.observe(this, Observer{_planifications ->
            if (_planifications.isNullOrEmpty() && planifications.isEmpty()) {
                messageTv.text = getText(R.string.no_planifications)
                messageTv.visibility = View.VISIBLE
            } else if (_planifications.isNullOrEmpty() && planifications.isNotEmpty()){
                planifications.clear()
            } else if(_planifications.isNotEmpty()){
                planifications.addAll(_planifications)
            }
            mAdapter?.notifyDataSetChanged()
            invalidateOptionsMenu()
        })
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        // user info is retained to survive activity restarts, such as when rotating the
        // device or switching apps. This isn't essential, but it helps provide a less
        // jarring UX when these events occur - data does not just disappear from the view.
        if (mStateManager?.keycloakUser != null) {
            state.putSerializable(KEY_PROFILE_INFO, mStateManager?.keycloakUser)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "on resume...")
        mStateManager?.revalidateSessionData(this)
        Log.i(TAG, "on resume...")
        mStateManager?.revalidateSessionData(this)
        if (mStateManager?.keycloakUser != null) {
            if (viewModel.planifications.value.isNullOrEmpty() && planifications.isNullOrEmpty()) {
                fetchData(this::fetchUserPlanifications)
            } else {
                /*showLoader()
                doAsync {
                    val allPlanifications = db?.planificationDao()?.getAllByType("National")
                    viewModel.planifications.value?.clear()
                    planifications.clear()
                    viewModel.planifications.postValue(allPlanifications?.toMutableList())
                    Log.i(TAG, "planifications loaded from local DB: $allPlanifications")
                    hideLoader()
                }*/
            }
        } else {
            showAlert("User Data Error", "Some user data are wrong or empty.", true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun fetchData(callback: ((String?, String?, AuthorizationException?) -> Unit)) {
        Log.i(TAG, "Fetching data...")
        mStateManager?.current?.performActionWithFreshTokens(mStateManager?.mAuthService!!,
            callback)
    }

    private fun fetchUserPlanifications(
            accessToken: String?,
            idToken: String?,
            ex: AuthorizationException?,
    ) {
        var url = "planification/byOperator/2;planificationType-filterType=text;planificationType-type=equals;planificationType-filter=national;"
        if (searchStr.isNotEmpty()){
            url += "licensePlate-filterType=text;licensePlate-type=contains;licensePlate-filter=${searchStr};"
        }
        url += "startRow=$startRow;endRow=$endRow;sort-dispatchDate=desc;"
        Log.i(TAG, "planifications endpoint: $url")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
                CclDataService::class.java)
        if (dataService != null && accessToken != null) {
            showLoader()
            doAsync {
                try {
                    val call = dataService.getPlanifications(url, mStateManager?.customer?.addressId!!,"Bearer $accessToken").execute()
                    val response = call.body()
                    hideLoader()
                    val allPlanifications = response?.content
                    viewModel.planifications.postValue(allPlanifications)
                    Log.i(TAG, "planifications response with retrofit: $allPlanifications")
                    if (!allPlanifications.isNullOrEmpty()) {
                        try {
                            db?.planificationDao()?.insertAll(allPlanifications)
                        } catch (ex: SQLiteException) {
                            Log.e(TAG,
                                    "Error saving planifications to local dabase",
                                    ex)
                            showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                        } catch (ex: SQLiteConstraintException) {
                            Log.e(TAG,
                                    "Error saving planifications to local dabase",
                                    ex)
                            showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                        } catch (ex: Exception) {
                            Log.e(TAG,
                                    "Error saving planifications to local dabase",
                                    ex)
                            showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                        }
                        uiThread {
                            messageTv.visibility = View.GONE
                        }
                    }
                } catch (ioEx: IOException) {
                    hideLoader()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTv.text = getText(R.string.network_error)
                        messageTv.visibility = View.VISIBLE
                        Log.e(TAG, "Network error when querying planifications endpoint", ioEx)
                    }

                } catch (jsonEx: JSONException) {
                    hideLoader()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTv.text = getText(R.string.parse_planifications_error)
                        messageTv.visibility = View.VISIBLE
                        Log.e(TAG, "Failed to parse planifications response", jsonEx)
                    }
                } catch (e: Exception) {
                    hideLoader()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTv.text = getText(R.string.unknown_error)
                        messageTv.visibility = View.VISIBLE
                        Log.e(TAG, "Unknown exception: ", e)
                    }
                } catch (e: SocketTimeoutException) {
                    hideLoader()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTv.text = getText(R.string.unknown_error)
                        messageTv.visibility = View.VISIBLE
                        Log.e(TAG, "Socket timeout exception: ", e)
                    }
                }
            }
        }
    }

    private fun fetchMorePlanifications(
            accessToken: String?,
            idToken: String?,
            ex: AuthorizationException?,
    ) {
        //var url = "planification/list/2;customerAddressId-filterType=number;customerAddressId-type=equals;customerAddressId-filter=${mStateManager?.customer?.addressId};planificationType-filterType=text;planificationType-type=equals;planificationType-filter=national;"
        var url = "planification/byOperator/2;planificationType-filterType=text;planificationType-type=equals;planificationType-filter=national;"
        if (searchStr.isNotEmpty()){
            url += "licensePlate-filterType=text;licensePlate-type=contains;licensePlate-filter=${searchStr};"
        }
        url += "startRow=$startRow;endRow=$endRow;sort-planificationDate=desc;"
        Log.i(TAG, "planifications endpoint: $url")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
                CclDataService::class.java)
        mAdapter?.addLoadingView()
        if (mStateManager?.customer == null){
            mStateManager?.loadCustomerAndAddress(this)
        }
        if (dataService != null && accessToken != null && mStateManager?.customer?.addressId != null) {
            doAsync {
                try {
                    val call = dataService.getPlanifications(url, mStateManager?.customer?.addressId!!,"Bearer $accessToken").execute()
                    val response = call.body()
                    val allPlanifications = response?.content
                    //val allPlanifications = response?._embedded?.content
                    mAdapter?.removeLoadingView()
                    scrollListener.setLoaded()
                    planifications.addAll(allPlanifications ?: arrayListOf())
                    uiThread {
                        mAdapter?.notifyDataSetChanged()
                    }
                    Log.i(TAG, "planifications response with retrofit: $allPlanifications")
                    if (!allPlanifications.isNullOrEmpty()) {
                        try {
                            db?.planificationDao()?.insertAll(allPlanifications)
                        } catch (ex: SQLiteException) {
                            Log.e(TAG,
                                    "Error saving planifications to local dabase",
                                    ex)
                            showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                        } catch (ex: SQLiteConstraintException) {
                            Log.e(TAG,
                                    "Error saving planifications to local dabase",
                                    ex)
                            showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                        } catch (ex: Exception) {
                            Log.e(TAG,
                                    "Error saving planifications to local dabase",
                                    ex)
                            showAlert(getString(R.string.database_error), getString(R.string.database_error_saving_planifications))
                        }
                        uiThread {
                            messageTv.visibility = View.GONE
                        }
                    }
                } catch (ioEx: IOException) {
                    mAdapter?.addLoadingView()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTv.text = getText(R.string.network_error)
                        messageTv.visibility = View.VISIBLE
                        Log.e(TAG, "Network error when querying planifications endpoint", ioEx)
                    }

                } catch (jsonEx: JSONException) {
                    mAdapter?.addLoadingView()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTv.text = getText(R.string.parse_planifications_error)
                        messageTv.visibility = View.VISIBLE
                        Log.e(TAG, "Failed to parse planifications response", jsonEx)
                    }
                } catch (e: Exception) {
                    mAdapter?.addLoadingView()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTv.text = getText(R.string.unknown_error)
                        messageTv.visibility = View.VISIBLE
                        Log.e(TAG, "Unknown exception: ", e)
                    }
                } catch (e: SocketTimeoutException) {
                    mAdapter?.addLoadingView()
                    showSnackbar(getString(R.string.error_fetching_planifications))
                    uiThread {
                        messageTv.text = getText(R.string.unknown_error)
                        messageTv.visibility = View.VISIBLE
                        Log.e(TAG, "Socket timeout exception: ", e)
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        runOnUiThread {
            if(!isFinishing || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && !isDestroyed)) {
                Snackbar.make(cordinator,
                    message,
                    Snackbar.LENGTH_SHORT)
                    .show()
            }
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
        if (requestCode == Constant.TRANSFER_ACTIVITY || requestCode == Constant.REDISPATCH_ACTIVITY && data?.hasExtra("planification") == true) {
            val planification = data?.getSerializableExtra("planification") as Planification
            var foundPlanification = planifications.find {
                it?.id == planification.id
            }
            if(foundPlanification != null) {
                foundPlanification = planification
            }
            mAdapter?.notifyDataSetChanged()
        }
    }

    fun initAdapter() {
        runOnUiThread{
            swiperefresh.setOnRefreshListener {
                startRow = 0
                endRow = 10
                viewModel.planifications.value?.clear()
                planifications.clear()
                mAdapter?.notifyDataSetChanged()
                fetchData(this::fetchUserPlanifications)
            }
            mAdapter = PlanificationsPagedAdapter(this, mStateManager, planifications, db, viewModel, supportFragmentManager)
            mLayoutManager = LinearLayoutManager(this)
            planificationsRv.layoutManager = mLayoutManager
            planificationsRv.adapter = mAdapter
            scrollListener = RecyclerViewLoadMoreScroll(mLayoutManager as LinearLayoutManager)
            scrollListener.setOnLoadMoreListener(object : OnLoadMoreListener {
                override fun onLoadMore() {
                    startRow = endRow
                    endRow += 10
                    fetchData(this@PlanificationsActivity::fetchMorePlanifications)
                }
            })
            planificationsRv.addOnScrollListener(scrollListener)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        /*startActivity(Intent(this, DashboardActivity2::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()*/
        onBackPressed()
        return true
    }

    fun hideLoader() {
        runOnUiThread {
            swiperefresh.isRefreshing = false
            planificationsRv.visibility = View.VISIBLE
        }
    }

    fun showLoader() {
        runOnUiThread{
            swiperefresh.isRefreshing = true
            messageTv.visibility = View.GONE
            planificationsRv.visibility = View.GONE
        }
    }

    fun showAlert(title: String, message: String, exitToLogin: Boolean = false) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", null)
            val dialog: AlertDialog = builder.create();
            if(!isFinishing && !isDestroyed) {
                dialog.show();
                dialog.setOnDismissListener {
                    if (exitToLogin) {
                        mStateManager?.signOut(this)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.default_menu, menu)
        val searchMenuItem = menu.findItem(R.id.search)
        searchMenuItem.isVisible = true
        val searchView = searchMenuItem.actionView as androidx.appcompat.widget.SearchView
        searchView.setQuery(searchStr, false)
        searchView.setOnQueryTextListener( object: SearchView.OnQueryTextListener,
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Toast like print
                Log.i(TAG, "query submit $query")
                if( ! searchView.isIconified()) {
                    searchView.setIconified(true)
                }
                searchStr = query ?: ""
                startRow = 0
                endRow = 10
                viewModel.planifications.value?.clear()
                planifications.clear()
                runOnUiThread {
                    mAdapter?.notifyDataSetChanged()
                }
                fetchData(this@PlanificationsActivity::fetchUserPlanifications)
                /*filterPlanifications(query?.toLowerCase())
                searchMenuItem.collapseActionView()*/
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                Log.i(TAG, "text change query $s")
                return true
            }
        })
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
            R.id.search -> {

                true
            }
            R.id.clear_filter -> {
                startRow = 0
                endRow = 10
                searchStr = ""
                viewModel.planifications.value?.clear()
                planifications.clear()
                mAdapter?.notifyDataSetChanged()
                fetchData(this::fetchUserPlanifications)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        searchStr.let {
            val clearFilterMenuItem = menu?.findItem(R.id.clear_filter)
            clearFilterMenuItem?.isVisible = it.isNotEmpty()
        }
        return true
    }

    override fun onRefresh() {
        searchStr = ""
    }

    private fun filterPlanifications(query: String?) {
        Log.i(TAG, "filtering planifications by $query...")
        planifications.clear()
        val filtered = viewModel.planifications.value?.filter{
            Log.i(TAG, "examining planification ${it.id}: $it")
            query == null || it.dispatchDate?.toLowerCase()?.contains(query) == true ||
                    it.label?.toLowerCase()?.contains(query) == true || it.planificationType?.toLowerCase()?.contains(query) == true ||
                    it.state?.toLowerCase()?.contains(query) == true || it.licensePlate?.toLowerCase()?.contains(query) == true
        } ?: arrayListOf()
        Log.i(TAG, "filtered results ${filtered.size}")
        planifications.addAll(filtered)
        searchStr = query ?: ""
        mAdapter?.notifyDataSetChanged()
        invalidateOptionsMenu()
    }
}