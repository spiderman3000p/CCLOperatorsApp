package com.tautech.cclappoperators.activities

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.Planification

class PlanificationDetailActivityViewModel() : ViewModel() {
    val planification = MutableLiveData<Planification>()
    val deliveries = MutableLiveData<MutableList<Delivery>>()
}