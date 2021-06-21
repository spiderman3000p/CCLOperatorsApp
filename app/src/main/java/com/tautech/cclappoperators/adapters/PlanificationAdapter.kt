package com.tautech.cclappoperators.adapters

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.CertificateActivity
import com.tautech.cclappoperators.activities.PlanificationDetailActivity
import com.tautech.cclappoperators.models.Planification
import kotlinx.android.synthetic.main.planification_card_item.view.*

class PlanificationAdapter(private val dataList: List<Planification>, val context: Context):
    RecyclerView.Adapter<PlanificationAdapter.MyViewHolder>() {
    class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun bindItems(planification: Planification) {
            var colorStateList: ColorStateList? = null
            var backgroundColor: Int = 0
            when (planification.state) {
                "Created" -> {
                    backgroundColor = getColor(itemView.context, R.color.created_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.created_bg)
                }
                "Complete" -> {
                    backgroundColor = getColor(itemView.context, R.color.completed_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.completed_bg)
                }
                "Assigned" -> {
                    backgroundColor = getColor(itemView.context, R.color.assigned_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.assigned_bg)
                }
                "Cancelled" -> {
                    backgroundColor = getColor(itemView.context, R.color.cancelled_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.cancelled_bg)
                }
                "Dispatched" -> {
                    backgroundColor = getColor(itemView.context, R.color.dispatched_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.dispatched_bg)
                }
                "OnGoing" -> {
                    backgroundColor = getColor(itemView.context, R.color.ongoing_bg)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.planification_card_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindItems(dataList[position])
        holder.itemView.openBtn.setOnClickListener { view ->
            lateinit var intent: Intent
            // iniciar nuevo activity
            if (dataList[position].planificationType == "National") {
                intent = Intent(view.context, CertificateActivity::class.java).apply {
                    putExtra("planification", dataList[position])
                }
                //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
                view.context.startActivity(intent)
            } else if (dataList[position].planificationType == "Urban") {
                if(dataList[position].state == "OnGoing") {
                    startPlanificationDetailActivity(dataList[position])
                } else {
                    showActionPrompt(dataList[position], view.context)
                }
            }
        }
    }

    fun showActionPrompt(planification: Planification, context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("")
        builder.setTitle(context.getString(R.string.what_to_do))
        builder.setPositiveButton(context.getString(R.string.certificate), DialogInterface.OnClickListener{ dialog, id ->
            startCertificateActivity(planification)
        })
        builder.setNegativeButton(context.getString(R.string.view), DialogInterface.OnClickListener{ dialog, id ->
            startPlanificationDetailActivity(planification)
        })
        val dialog: AlertDialog = builder.create();
        dialog.show();
    }

    fun startCertificateActivity(planification: Planification){
        val intent = Intent(context, CertificateActivity::class.java).apply {
            putExtra("planification", planification)
        }
        //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun startPlanificationDetailActivity(planification: Planification){
        val intent = Intent(context, PlanificationDetailActivity::class.java).apply {
            putExtra("planification", planification)
        }
        //intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}