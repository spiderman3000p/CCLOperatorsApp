package com.tautech.cclappoperators.classes

import android.app.Dialog
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.PlanificationDetailActivityViewModel
import kotlinx.android.synthetic.main.fragment_planification_preview.*
import java.text.NumberFormat
import java.util.*
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.services.CclClient
import kotlinx.android.synthetic.main.activity_planifications.*
import kotlinx.android.synthetic.main.content_scrolling.*
import net.openid.appauth.AuthorizationException
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.collections.ArrayList


class PreviewPlanificationDialog(val function: () -> Unit, val planificationId: Long) : DialogFragment() {
    lateinit  var toolbar: Toolbar
    val TAG = "PREVIEW_DIALOG"
    private var mStateManager: AuthStateManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        mStateManager = AuthStateManager.getInstance(requireContext())
    }

    override fun onStart() {
        super.onStart()
        val dialog: Dialog? = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.getWindow()?.setLayout(width, height)
            dialog.getWindow()?.setWindowAnimations(R.style.AppThemeSlide);
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view: View = inflater.inflate(R.layout.fragment_planification_preview, container, false)
        toolbar = view.findViewById(R.id.toolbar)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener { v: View? -> dismiss() }
        toolbar.title = "Planification Preview"
        toolbar.setTitleTextColor(getColor(requireContext(), R.color.white))
        doneBtn2.setOnClickListener {
            function()
            dismiss()
        }
        // TODO: cargar datos desde la web
        fetchData(this::fetchPlanificationResumeData)
    }

    private fun fetchData(callback: ((String?, String?, AuthorizationException?) -> Unit)) {
        Log.i(TAG, "Fetching data...")
        mStateManager?.current?.performActionWithFreshTokens(mStateManager?.mAuthService!!,
                callback)
    }

    private fun fetchPlanificationResumeData(
            accessToken: String?,
            idToken: String?,
            ex: AuthorizationException?,
    ) {
        val url = "collectionVO3s/search/findByPlanificationId?planificationId=${planificationId}"
        Log.i(TAG, "planifications endpoint: $url")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
                CclDataService::class.java)
        if (mStateManager?.customer == null){
            mStateManager?.loadCustomerAndAddress(requireContext())
        }
        if (dataService != null && accessToken != null) {
            showLoader()
            doAsync {
                try {
                    val call = dataService.getPlanifications(url, mStateManager?.customer?.addressId!!,"Bearer $accessToken").execute()
                    val response = call.body()
                    hideLoader()
                    val allPlanifications = response?.content
                    Log.i(TAG, "planifications response with retrofit: $allPlanifications")
                    if (!allPlanifications.isNullOrEmpty()) {
                        uiThread {

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

    fun drawList(){

    }

    fun formatNumber(number: Double): String{
        val format: NumberFormat = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 0
        format.currency = Currency.getInstance("COP")
        return format.format(number)
    }

    fun hideLoader() {
        activity?.runOnUiThread {
            swiperefresh.isRefreshing = false
            planificationsRv.visibility = View.VISIBLE
        }
    }

    fun showLoader() {
        activity?.runOnUiThread{
            swiperefresh.isRefreshing = true
            messageTv.visibility = View.GONE
            planificationsRv.visibility = View.GONE
        }
    }

    private fun showSnackbar(message: String) {
        activity?.runOnUiThread {
            if(activity?.isFinishing == false && activity?.isDestroyed == false) {
                Snackbar.make(cordinator,
                        message,
                        Snackbar.LENGTH_SHORT)
                        .show()
            }
        }
    }

    fun showAlert(title: String, message: String, exitToLogin: Boolean = false) {
        activity?.runOnUiThread {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", null)
            val dialog: AlertDialog = builder.create();
            if(activity?.isFinishing == false && activity?.isDestroyed == false) {
                dialog.show()
                dialog.setOnDismissListener {
                    if (exitToLogin) {
                        mStateManager?.signOut(requireContext())
                    }
                }
            }
        }
    }

    class Totals(
        var totalPlanificationValue: Double = 0.00,
        var totalDelivered: Double = 0.00,
        var totalUndelivered: Double = 0.00,
        val undeliveredDeliveries: ArrayList<Delivery> = arrayListOf()
    ){}

    companion object {
        fun display(fragmentManager: FragmentManager, function: () -> Unit, planificationId: Long): PreviewPlanificationDialog? {
            val previewDialog = PreviewPlanificationDialog(function, planificationId)
            previewDialog.show(fragmentManager, "preview_dialog")
            return previewDialog
        }
    }
}