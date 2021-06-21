package com.tautech.cclappoperators.activities.ui_redispatch.resume

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.tautech.cclappoperators.R
import com.tautech.cclappoperators.activities.RedispatchActivityViewModel
import kotlinx.android.synthetic.main.fragment_resume.*

class ResumeFragment : Fragment() {
    val TAG = "RESUME_REDISP_FRAGMENT"
    private val viewModel: RedispatchActivityViewModel by activityViewModels()
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_resume, container, false)
        viewModel.deliveries.observe(viewLifecycleOwner, Observer{ pendingDeliveries ->
            certifiedDeliveriesTv.text = viewModel.processedDeliveries.value?.size.toString()
            uncertifiedDeliveriesTv.text = pendingDeliveries.size.toString()
        })
        viewModel.processedDeliveries.observe(viewLifecycleOwner, Observer{ redispatchedDeliveries ->
            certifiedDeliveriesTv.text = redispatchedDeliveries.size.toString()
            uncertifiedDeliveriesTv.text = viewModel.deliveries.value?.size.toString()
        })
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.actionBar?.setDisplayHomeAsUpEnabled(true)
        activity?.actionBar?.setDisplayShowHomeEnabled(true)
        deliveryLinesCard.visibility = View.GONE
        certificateLabelTv.text = getString(R.string.redispatched_guides)
        noCertificateLabelTv.text = getString(R.string.unredispatched_guides)
        initCounters()
    }

    fun initCounters(){
        certifiedDeliveriesTv.text = "0"
        uncertifiedDeliveriesTv.text = "0"
        totalDeliveriesTv.text = ((viewModel.processedDeliveries.value?.size ?: 0) + (viewModel.deliveries.value?.size ?: 0)).toString()
    }
}