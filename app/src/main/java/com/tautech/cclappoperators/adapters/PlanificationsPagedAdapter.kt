package com.tautech.cclappoperators.adapters

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.*
import com.tautech.cclappoperators.classes.AuthStateManager
import com.tautech.cclappoperators.classes.Constant
import com.tautech.cclappoperators.classes.PromptPlanificationActionDialog
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.models.Planification
import com.tautech.cclappoperators.services.CclClient
import kotlinx.android.synthetic.main.planification_card_item.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.uiThread
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException

class PlanificationsPagedAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder> {
    var mContext: Context
    var mStateManager: AuthStateManager? = null
    var planifications: ArrayList<Planification?> = arrayListOf()
    var db: AppDatabase? = null
    var viewModel: ViewModel? = null
    var fm: FragmentManager
    constructor(context: Context, stateManager: AuthStateManager? = null,
                _planifications: ArrayList<Planification?>, _db: AppDatabase?,
                _viewModel: UrbanPlanificationsActivityViewModel?, _fm: FragmentManager){
        mContext = context
        mStateManager = stateManager
        planifications = _planifications
        db = _db
        viewModel = _viewModel
        fm = _fm
    }

    constructor(context: Context, stateManager: AuthStateManager? = null,
                _planifications: ArrayList<Planification?>, _db: AppDatabase?,
                _viewModel: PlanificationsActivityViewModel?, _fm: FragmentManager){
        mContext = context
        mStateManager = stateManager
        planifications = _planifications
        db = _db
        viewModel = _viewModel
        fm = _fm
    }
    val TAG = "PLANIF_PAGED_ADAPTER"
    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        fun bindItems(planification: Planification) {
            var colorStateList: ColorStateList? = null
            var backgroundColor: Int = 0
            when (planification.state) {
                "Created" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.created_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.created_bg)
                }
                "Complete" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.completed_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.completed_bg)
                }
                "Assigned" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.assigned_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.assigned_bg)
                }
                "Cancelled" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.cancelled_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.cancelled_bg)
                }
                "Dispatched" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.dispatched_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.dispatched_bg)
                }
                "OnGoing" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.ongoing_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.ongoing_bg)
                }
            }
            itemView.cardView.setCardBackgroundColor(backgroundColor)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                itemView.stateTv.backgroundTintList = colorStateList
            } else {
                itemView.stateTv.setBackgroundColor(backgroundColor)
            }
            itemView.stateTv.text = planification.state
            itemView.titleTv.text = "${planification.licensePlate}-${planification.label ?: ""}"
            var percent: Double = 0.0
            var percentStr = ""
            if (planification.planificationType == "National") {
                if ((planification.totalUnits?.compareTo(0)
                                ?: 0) > 0 && planification.totalCertified ?: 0 > 0
                ) {
                    percent =
                            (100 * (planification.totalCertified ?: 0) / (planification.totalUnits
                                    ?: 1)).toDouble()
                }
                percentStr = String.format("%.2f", percent) + "% (${planification.totalCertified})"
            } else if(planification.planificationType == "Urban") {
                if ((planification.totalUnits?.compareTo(0)
                                ?: 0) > 0 && planification.totalDeliveries ?: 0 > 0
                ) {
                    percent =
                            (100 * (planification.totalDelivered ?: 0) / (planification.totalDeliveries
                                    ?: 1)).toDouble()
                }
                percentStr = String.format("%.2f", percent) + "% (${planification.totalDeliveries})"
            }
            itemView.percentCertifiedTv.text = percentStr
            //itemView.progressBar.setProgress(percent.toInt(), true)
            //itemView.customerTv.text = planification.customerName
            //itemView.addressTv.text = planification.address
            itemView.dateTv.text = planification.dispatchDate
            itemView.qtyTv.text = planification.totalDeliveries.toString()
            //itemView.deliveryLinesTv.text = planification.totalLines.toString()
            itemView.unitsTv.text = planification.totalUnits.toString()
        }
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun addLoadingView() {
        //Add loading item
        doAsync {
            planifications.add(null)
            uiThread {
                notifyItemInserted(planifications.size - 1)
            }
        }
    }

    fun removeLoadingView() {
        //Remove loading item
        if (planifications.size != 0) {
            planifications.removeAt(planifications.size - 1)
            mContext.runOnUiThread {
                notifyItemRemoved(planifications.size)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == Constant.VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(mContext).inflate(R.layout.planification_card_item, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(mContext).inflate(R.layout.progress_loading, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return planifications.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (planifications[position] == null) {
            Constant.VIEW_TYPE_LOADING
        } else {
        return  Constant.VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == Constant.VIEW_TYPE_ITEM && planifications[position] != null) {
            (holder as ItemViewHolder).bindItems(planifications[position]!!)
            holder.itemView.openBtn.setOnClickListener { view ->
                Log.i(TAG, "selected item: ${planifications[position]}")
                showActionPrompt(planifications[position]!!, view.context, fm)
            }
        }
    }

    fun startCertificateActivity(planification: Planification){
        val intent = Intent(mContext, CertificateActivity::class.java).apply {
            putExtra("planification", planification)
        }
        //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
        mContext.startActivity(intent)
    }

    fun startTransferActivity(planification: Planification){
        val intent = Intent(mContext, TransferActivity::class.java).apply {
            putExtra("planification", planification)
        }
        //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
        mContext.startActivity(intent)
    }

    fun startRedispatchActivity(planification: Planification){
        val intent = Intent(mContext, RedispatchActivity::class.java).apply {
            putExtra("planification", planification)
        }
        //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
        (mContext as AppCompatActivity).startActivityForResult(intent, Constant.REDISPATCH_ACTIVITY)
    }

    fun startPlanificationDetailActivity(planification: Planification){
        val intent = Intent(mContext, PlanificationDetailActivity::class.java).apply {
            putExtra("planification", planification)
        }
        //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
        mContext.startActivity(intent)
    }

    fun showActionPrompt(planification: Planification, context: Context, fm: FragmentManager) {

        val options = when(planification.state){
            "Created" -> arrayOf(context.getString(R.string.view), context.getString(R.string.dispatch), context.getString(R.string.certificate))
            "Assigned" -> arrayOf(context.getString(R.string.view), context.getString(R.string.dispatch), context.getString(R.string.certificate))
            "Dispatched" -> arrayOf(context.getString(R.string.view), context.getString(R.string.transfer_guides))
            "OnGoing" -> arrayOf(context.getString(R.string.view))
            "Complete" -> arrayOf(context.getString(R.string.view), context.getString(R.string.redispatch_guides))
            else -> arrayOf(context.getString(R.string.view))
        }
        val listener = DialogInterface.OnClickListener { dialog, which ->
            when(planification.state) {
                "Created" -> {
                    when (which) {
                        0 -> {
                            startPlanificationDetailActivity(planification)
                        }
                        1 -> {
                            showAlert(mContext.getString(R.string.dispatch_route),
                                mContext.getString(R.string.dispatch_route_prompt), this::changePlanificationState, null, planification, "Dispatched")
                        }
                        2 -> {
                            startCertificateActivity(planification)
                        }
                    }
                }
                "Assigned" -> {
                    when (which) {
                        0 -> {
                            startPlanificationDetailActivity(planification)
                        }
                        1 -> {
                            showAlert(mContext.getString(R.string.dispatch_route),
                                mContext.getString(R.string.dispatch_route_prompt), this::changePlanificationState, null, planification, "Dispatched")
                        }
                        2 -> {
                            startCertificateActivity(planification)
                        }
                    }
                }
                "Dispatched" -> {
                    when (which) {
                        0 -> {
                            startPlanificationDetailActivity(planification)
                        }
                        1 -> {
                            startTransferActivity(planification)
                        }
                    }
                }
                "OnGoing" -> {
                    when (which) {
                        0 -> {
                            startPlanificationDetailActivity(planification)
                        }
                    }
                }
                "Complete" -> {
                    when (which) {
                        0 -> {
                            startPlanificationDetailActivity(planification)
                        }
                        1 -> {
                            startRedispatchActivity(planification)
                        }
                    }
                }
                else -> {
                    when (which) {
                        0 -> {
                            startPlanificationDetailActivity(planification)
                        }
                    }
                }
            }
        }
        val dialog = PromptPlanificationActionDialog(options, listener)
        dialog.show(fm, "prompt");
    }

    fun showAlert(title: String, message: String, positiveCallback: ((planification: Planification?, newState: String?) -> Unit)? = null, negativeCallback: ((planification: Planification?) -> Unit)? = null, planification: Planification? = null, newState: String? = null) {
        mContext.runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", DialogInterface.OnClickListener { dialog, id ->
                if (positiveCallback != null) {
                    positiveCallback(planification, newState)
                }
                dialog.dismiss()
            })
            builder.setNegativeButton("Cancelar", DialogInterface.OnClickListener { dialog, id ->
                if (negativeCallback != null) {
                    negativeCallback(planification)
                }
                dialog.dismiss()
            })
            if (!(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && (mContext as Activity).isDestroyed) || !(mContext as Activity).isFinishing) {
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    }

    private fun changePlanificationState(planification: Planification? = null, newState: String? = null) {
        val url = "planification/${planification?.id}/changeState?newState=$newState"
        Log.i(TAG, "constructed endpoint: $url")
        val dataService: CclDataService? = CclClient.getInstance()?.create(
                CclDataService::class.java)
        if (dataService != null && mStateManager?.current?.accessToken != null) {
            Toast.makeText(mContext, mContext.getString(R.string.dispatching_), Toast.LENGTH_SHORT).show()
            doAsync {
                try {
                    val call = dataService.changePlanificationState(url,
                            "Bearer ${mStateManager?.current?.accessToken}")
                            .execute()
                    val response = call.body()
                    Log.i(TAG,
                            "respuesta al cambiar estado de planificacion ${planification?.id}: ${response}")
                    planification?.state = newState
                    Log.i(TAG, "posting value to planification")
                    uiThread {
                        notifyDataSetChanged()
                    }
                    try {
                        db?.planificationDao()?.update(planification!!)
                    } catch (ex: SQLiteException) {
                        Log.e(TAG,
                                "Error actualizando planificacion en la BD local",
                                ex)
                        showAlert(mContext.getString(R.string.database_error),
                                mContext.getString(R.string.database_error_saving_planifications))
                    } catch (ex: SQLiteConstraintException) {
                        Log.e(TAG,
                                "Error actualizando planificacion en la BD local",
                                ex)
                        showAlert(mContext.getString(R.string.database_error),
                                mContext.getString(R.string.database_error_saving_planifications))
                    } catch (ex: Exception) {
                        Log.e(TAG,
                                "Error actualizando planificacion en la BD local",
                                ex)
                        showAlert(mContext.getString(R.string.database_error),
                                mContext.getString(R.string.database_error_saving_planifications))
                    }
                    Log.i(TAG, "finalize planification load response $response")
                } catch (toe: SocketTimeoutException) {
                    Log.e(TAG, "Network error when finalizing planification load", toe)
                    showAlert(mContext.getString(R.string.network_error_title),
                            mContext.getString(R.string.network_error))
                } catch (ioEx: IOException) {
                    Log.e(TAG,
                            "Network error when finalizing planification load",
                            ioEx)
                    showAlert(mContext.getString(R.string.network_error_title),
                            mContext.getString(R.string.network_error))
                } catch (jsonEx: JSONException) {
                    Log.e(TAG, "Failed to parse finalizing planification response", jsonEx)
                    showAlert(mContext.getString(R.string.parsing_error_title),
                            mContext.getString(R.string.parsing_error))
                }
            }
        }
    }
}