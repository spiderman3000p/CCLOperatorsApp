package com.tautech.cclappoperators.activities

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.DeliveryForRedispatch
import com.tautech.cclappoperators.models.Planification

class RedispatchActivityViewModel() : ViewModel() {
    val planificationStr = MutableLiveData<String>()
    //val planification = MutableLiveData<Planification>()
    //val planifications = MutableLiveData<MutableList<Planification>>()
    val deliveries = MutableLiveData<MutableList<DeliveryForRedispatch>>()
    val processedDeliveries = MutableLiveData<MutableList<DeliveryForRedispatch>>()
    val startDate = MutableLiveData<Long?>()
    val endDate = MutableLiveData<Long?>()
    init {
        //this.planifications.postValue(mutableListOf())
        this.deliveries.postValue(mutableListOf())
        this.processedDeliveries.postValue(mutableListOf())
    }
}