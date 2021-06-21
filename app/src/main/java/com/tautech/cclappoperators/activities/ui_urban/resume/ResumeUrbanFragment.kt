package com.tautech.cclappoperators.activities.ui_urban.resume

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.PlanificationDetailActivity
import com.tautech.cclappoperators.activities.PlanificationDetailActivityViewModel
import kotlinx.android.synthetic.main.fragment_resume_urban.*

class ResumeUrbanFragment : Fragment() {
    val TAG = "RESUME_URBAN_FRAGMENT"
    private val viewModel: PlanificationDetailActivityViewModel by activityViewModels()
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_resume_urban, container, false)
        viewModel.deliveries.observe(viewLifecycleOwner, Observer{
            initCounters()
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as PlanificationDetailActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as PlanificationDetailActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        initCounters()
    }

    fun getTotalDeliveredDeliveryLines(): Int{
        return viewModel.deliveries.value?.fold(0, {acc, delivery ->
            if (delivery.deliveryState == "Delivered") {
                acc + (delivery.totalQuantity ?: 0)
            }else {
                0
            }
        }) ?: 0
    }

    fun getTotalDeliveredDeliveries(): Int{
        return viewModel.deliveries.value?.count{delivery ->
            delivery.deliveryState == "Delivered"
        } ?: 0
    }

    fun initCounters(){
        val totalDeliveryLines = viewModel.planification.value?.totalUnits ?: 0
        val totalDeliveries = viewModel.planification.value?.totalDeliveries ?: 0
        val totalDeliveredDeliveries = getTotalDeliveredDeliveries()
        val totalDeliveredDeliveryLines = getTotalDeliveredDeliveryLines()
        deliveredDeliveriesTv?.text = totalDeliveredDeliveries.toString()
        undeliveredDeliveriesTv?.text = (totalDeliveries - totalDeliveredDeliveries).toString()
        deliveredDeliveryLinesTv?.text = totalDeliveredDeliveryLines.toString()
        undeliveredDeliveryLinesTv?.text = (totalDeliveryLines - totalDeliveredDeliveryLines).toString()
        totalDeliveriesTv2?.text = totalDeliveries.toString()
        totalDeliveryLinesTv2?.text = totalDeliveryLines.toString()
    }
}