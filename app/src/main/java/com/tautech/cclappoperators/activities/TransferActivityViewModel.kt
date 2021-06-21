package com.tautech.cclappoperators.activities

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.Planification

class TransferActivityViewModel() : ViewModel() {
    val sourcePlanificationStr = MutableLiveData<String>()
    val targetPlanificationStr = MutableLiveData<String>()
    val sourcePlanification = MutableLiveData<Planification>()
    val targetPlanification = MutableLiveData<Planification>()
    val deliveries = MutableLiveData<MutableList<Delivery>>()
    val planifications = MutableLiveData<MutableList<Planification>>()
    val processedDeliveries = MutableLiveData<MutableList<Delivery>>()

    init {
        this.planifications.postValue(mutableListOf())
        this.deliveries.postValue(mutableListOf())
        this.processedDeliveries.postValue(mutableListOf())
    }
}