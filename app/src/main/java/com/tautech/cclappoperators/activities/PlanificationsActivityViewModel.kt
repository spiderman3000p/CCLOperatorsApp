package com.tautech.cclappoperators.activities

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tautech.cclappoperators.models.Planification
import java.util.*

class PlanificationsActivityViewModel() : ViewModel() {
    val planifications = MutableLiveData<ArrayList<Planification>>()
}