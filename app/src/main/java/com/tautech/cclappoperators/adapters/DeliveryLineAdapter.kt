package com.tautech.cclappoperators.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.models.DeliveryLine
import com.tautech.cclappoperators.models.PlanificationLine
import kotlinx.android.synthetic.main.last_readed_item.view.*
import java.util.*

class DeliveryLineAdapter(private val dataList: MutableList<DeliveryLine>, val context: Context, val readed: Boolean = true) :
    RecyclerView.Adapter<DeliveryLineAdapter.MyViewHolder>() {

    class MyViewHolder(val itemView: View, val readed: Boolean): RecyclerView.ViewHolder(itemView) {
        fun bindItems(deliveryLine: DeliveryLine, delivery: PlanificationLine? = null) {
            itemView.skuDescriptionTv .text = deliveryLine.description
            itemView.deliveryBtn.text = "Delivery ${deliveryLine.deliveryId}"
            itemView.referenceTv.text = deliveryLine.reference
            itemView.timestampTv.text = Date().toString()
            if (readed) {
                itemView.doneImage.visibility = View.VISIBLE
                itemView.quantitiesTv.visibility = View.VISIBLE
            } else {
                itemView.doneImage.visibility = View.GONE
                itemView.quantitiesTv.visibility = View.GONE
            }
            itemView.quantitiesTv.text = "${deliveryLine.scannedOrder}/${deliveryLine.quantity}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.last_readed_item, parent, false)
        return MyViewHolder(view, readed)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindItems(dataList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}