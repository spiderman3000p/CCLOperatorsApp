package com.tautech.cclappoperators.activities.ui_urban.detail

import android.content.DialogInterface
import android.database.sqlite.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.PlanificationDetailActivity
import com.tautech.cclappoperators.activities.PlanificationDetailActivityViewModel
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.classes.Configuration
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.services.CclClient
import kotlinx.android.synthetic.main.fragment_planification_detail.*
import kotlinx.android.synthetic.main.fragment_planification_detail.view.*
import net.openid.appauth.AuthorizationException
import retrofit2.Retrofit

class PlanificationDetailFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    val TAG = "PLANIF_DETAIL_FRAGMENT"
    private val viewModel: PlanificationDetailActivityViewModel by activityViewModels()
    private var retrofitClient: Retrofit? = null
    private var mStateManager: AuthStateManager? = null
    var db: AppDatabase? = null
    var totalDeliveriesDelivered = 0
    var totalDeliveryLinesDelivered = 0
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_planification_detail, container, false)
        retrofitClient = CclClient.getInstance()
        mStateManager = AuthStateManager.getInstance(requireContext())
        val config = Configuration.getInstance(requireContext())
        try {
            db = AppDatabase.getDatabase(requireContext())
        } catch(ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        if (config.hasConfigurationChanged()) {
            showAlert("Error", "La configuracion de sesion ha cambiado. Se cerrara su sesion", this::signOut)
        }
        if (!mStateManager!!.current.isAuthorized) {
            showAlert("Sesion expirada", "Su sesion ha expirado", this::signOut)
        }
        viewModel.deliveries.observe(viewLifecycleOwner, Observer{ deliveries ->
            calculateTotals()
        })
        viewModel.planification.observe(viewLifecycleOwner, Observer{planification ->
            root.planificationStateChip.text = planification.state
            root.planificationStateChip.chipBackgroundColor = when(planification.state) {
                "Created" -> ContextCompat.getColorStateList(requireContext(), R.color.created_bg)
                "Dispatched" -> ContextCompat.getColorStateList(requireContext(), R.color.dispatched_bg)
                "Cancelled" -> ContextCompat.getColorStateList(requireContext(), R.color.cancelled_bg)
                "Complete" -> ContextCompat.getColorStateList(requireContext(), R.color.completed_bg)
                "OnGoing" -> ContextCompat.getColorStateList(requireContext(), R.color.ongoing_bg)
                else -> ContextCompat.getColorStateList(requireContext(), R.color.created_bg)
            }
            root.dateTv.text = planification.dispatchDate
            root.planificationTypeTv.text = planification.planificationType
            root.planificationLabelTv.text = planification.label.let {
                if (it.isNullOrEmpty()) {
                    getString(R.string.no_label_planification)
                } else {
                    it
                }
            }
            /*root.planificationCustomerTv.text = planification.customerName
            root.planificationDriverTv.text = planification.driverName*/
            root.planificationDriverTv.visibility = View.GONE
            root.planificationCustomerTv.visibility = View.GONE
            root.planificationVehicleTv.text = planification.licensePlate
            root.totalWeightChip.text = "%.2f".format(planification.totalWeight ?: 0) + " kg"
            root.totalValueChip.text = "%.2f".format(planification.totalValue ?: 0) + " $"
            refreshTotals()
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as PlanificationDetailActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as PlanificationDetailActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    fun getCompletedDeliveryLinesProgress(): Int {
        return (totalDeliveryLinesDelivered * 100) / (viewModel.planification.value?.totalUnits ?: 1)
    }

    fun getCompletedDeliveriesProgress(): Int {
        return (totalDeliveriesDelivered * 100) / (viewModel.planification.value?.totalDeliveries ?: 1)
    }

    fun calculateTotals(){
        totalDeliveriesDelivered = 0
        totalDeliveryLinesDelivered = 0
        viewModel.deliveries.value?.forEach {
            if (it.deliveryState == "Delivered"){
                totalDeliveriesDelivered++
                totalDeliveryLinesDelivered += it.totalQuantity ?: 0
            }
        }
        refreshTotals()
    }

    private fun refreshTotals(){
        planificationCompletedProgressTv.text = "${getCompletedDeliveryLinesProgress()}% ${getString(R.string.completed)}"
        planificationCompletedProgressBar.progress = getCompletedDeliveryLinesProgress()
        totalItemsChip.text = "${totalDeliveriesDelivered}/${viewModel.planification.value?.totalDeliveries ?: 0}"
        deliveriesCompletedProgressTv.text = "${getCompletedDeliveriesProgress()}% ${getString(R.string.completed)}"
        deliveriesCompletedProgressBar.progress = getCompletedDeliveriesProgress()
        deliveryLinesCountChip.text = "${totalDeliveryLinesDelivered}/${viewModel.planification.value?.totalUnits ?: 0}"
        deliveryLinesCompletedProgressTv.text = "${getCompletedDeliveryLinesProgress()}% ${getString(R.string.completed)}"
        deliveryLinesCompletedProgressBar.progress = getCompletedDeliveryLinesProgress()
    }

    private fun fetchData(callback: ((String?, String?, AuthorizationException?) -> Unit)) {
        Log.i(TAG, "Fetching data...$callback")
        mStateManager?.current?.performActionWithFreshTokens(mStateManager?.mAuthService!!, callback)
    }

    private fun signOut() {
        mStateManager?.signOut(requireContext())
    }

    fun showAlert(title: String, message: String) {
        activity?.runOnUiThread {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", null)
            val dialog: AlertDialog = builder.create();
            dialog.show();
        }
    }

    fun showAlert(title: String, message: String, positiveCallback: (() -> Unit)? = null, negativeCallback: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", DialogInterface.OnClickListener{ dialog, id ->
            if (positiveCallback != null) {
                positiveCallback()
            }
            dialog.dismiss()
        })
        builder.setNegativeButton("Cancelar", DialogInterface.OnClickListener{ dialog, id ->
            if (negativeCallback != null) {
                negativeCallback()
            }
            dialog.dismiss()
        })
        if(this.activity?.isFinishing == false) {
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    override fun onRefresh() {

    }
}