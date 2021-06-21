package com.tautech.cclappoperators.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.models.DeliveryLine
import kotlinx.android.synthetic.main.delivery_line_item.view.*
import kotlinx.android.synthetic.main.last_readed_item.view.referenceTv
import kotlinx.android.synthetic.main.last_readed_item.view.skuDescriptionTv

class DeliveryLineItemAdapter(private val dataList: MutableList<DeliveryLine>, val editable: Boolean = false, val context: Context):
    RecyclerView.Adapter<DeliveryLineItemAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View, editable: Boolean = false): RecyclerView.ViewHolder(itemView), SeekBar.OnSeekBarChangeListener {
        var deliveryLine: DeliveryLine? = null
        fun bindItems(_deliveryLine: DeliveryLine, editable: Boolean = false) {
            deliveryLine = _deliveryLine
            itemView.skuDescriptionTv .text = deliveryLine?.description
            itemView.referenceTv.text = deliveryLine?.reference
            if (!editable) {
                itemView.quantitySeekBar.visibility = View.GONE
            }
            itemView.quantitySeekBar.setOnSeekBarChangeListener(this)
            itemView.quantityTv.text = "${deliveryLine?.deliveredQuantity}/${deliveryLine?.quantity}"
            (itemView.quantitySeekBar as SeekBar).max = deliveryLine?.quantity ?: 0
            (itemView.quantitySeekBar as SeekBar).progress = deliveryLine?.deliveredQuantity ?: 0
        }
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                if (deliveryLine != null) {
                    if (progress >= deliveryLine?.quantity ?: 0) {
                        deliveryLine?.deliveredQuantity = deliveryLine?.quantity ?: 0
                        itemView.quantityTv.text =
                            "${deliveryLine?.quantity}/${deliveryLine?.quantity}"
                        seekBar?.progress = deliveryLine?.quantity ?: 0
                    } else {
                        itemView.quantityTv.text = "${progress}/${deliveryLine?.quantity}"
                        deliveryLine?.deliveredQuantity = progress
                    }
                }
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.delivery_line_item, parent, false)
        return MyViewHolder(view, editable)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindItems(dataList[position], editable)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}