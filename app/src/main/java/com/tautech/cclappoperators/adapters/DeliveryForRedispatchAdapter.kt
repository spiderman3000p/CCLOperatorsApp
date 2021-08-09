package com.tautech.cclappoperators.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.models.DeliveryForRedispatch
import com.tautech.cclappoperators.models.Planification
import kotlinx.android.synthetic.main.planification_card_item.view.*

class DeliveryForRedispatchAdapter(private val dataList: MutableList<DeliveryForRedispatch>, val planification: Planification?, val context: Context):
    RecyclerView.Adapter<DeliveryForRedispatchAdapter.MyViewHolder>() {
    class MyViewHolder(val itemView: View, val _planification: Planification?): RecyclerView.ViewHolder(itemView) {
        val openButton: Button
        init {
            openButton = itemView.findViewById(R.id.openBtn)
            openButton.visibility = View.GONE
            /*if (_planification?.state == "OnGoing" || _planification?.state == "UnDelivered") {
                openButton.visibility = View.VISIBLE
            }*/
        }
        fun bindItems(delivery: DeliveryForRedispatch? = null) {
            Log.i("Planif Line Adapter", "delivery: $delivery")
            itemView.unitsTv?.visibility = View.GONE
            var colorStateList: ColorStateList? = null
            var backgroundColor: Int = 0
            when (delivery?.deliveryState) {
                "Created" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.created_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.created_bg)
                }
                "Planned" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.planned_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.planned_bg)
                }
                "OnGoing" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.ongoing_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.ongoing_bg)
                }
                "InDepot" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.indepot_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.indepot_bg)
                }
                "Delivered" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.delivered_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.delivered_bg)
                }
                "UnDelivered" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.undelivered_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.undelivered_bg)
                }
                "Partial" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.partial_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.partial_bg)
                }
                "ReDispatched" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.redispatched_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.redispatched_bg)
                }
                "Cancelled" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, R.color.cancelled_bg)
                    colorStateList = ContextCompat.getColorStateList(itemView.context, R.color.cancelled_bg)
                }
            }
            itemView.cardView.setCardBackgroundColor(backgroundColor)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                itemView.stateTv.backgroundTintList = colorStateList
            } else {
                itemView.stateTv.setBackgroundColor(backgroundColor)
            }
            itemView.stateTv.text = delivery?.deliveryState
            itemView.titleTv?.text = "${delivery?.id}-${delivery?.referenceDocument ?: ""}"
            val operation = when(_planification?.planificationType){
                "National" -> itemView.context.getString(R.string.certified)
                else -> itemView.context.getString(R.string.delivered)
            }
            val percent = 0.0/*when(_planification?.planificationType){
                "National" -> (100 * (delivery?.totalCertified ?: 0) / (delivery?.totalQuantity ?: 1)).toDouble()
                else -> (100 * (delivery?.totalDelivered ?: 0) / (delivery?.totalQuantity ?: 1)).toDouble()
            }*/
            val percentStr = String.format("%.2f", percent) + "% " + operation
            itemView.percentCertifiedTv?.text = percentStr
            //itemView.progressBar.setProgress(percent.toInt(), true)
            itemView.customerTv?.text = delivery?.receiverName
            itemView.addressTv?.text = delivery?.receiverAddress
            itemView.customerTv?.visibility = View.VISIBLE
            itemView.addressTv?.visibility = View.VISIBLE
            itemView.dateTv?.text = delivery?.orderDate
            itemView.qtyTv?.text = delivery?.totalQuantity.toString()
            itemView.deliveryLinesTv?.text = delivery?.totalQuantity.toString()
            //itemView.unitsTv.text = planification.totalUnits.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.planification_card_item, parent, false)
        return MyViewHolder(view, planification)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindItems(dataList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}