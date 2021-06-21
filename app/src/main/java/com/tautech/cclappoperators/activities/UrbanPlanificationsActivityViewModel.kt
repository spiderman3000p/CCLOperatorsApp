package com.tautech.cclappoperators.activities

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tautech.cclappoperators.models.Planification

class UrbanPlanificationsActivityViewModel() : ViewModel() {
    val planifications = MutableLiveData<ArrayList<Planification>>()
}