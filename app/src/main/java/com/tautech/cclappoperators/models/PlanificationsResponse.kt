package com.tautech.cclappoperators.models

import java.io.Serializable

data class PlanificationsResponse(
    //val _embedded: PlanificationResponseHolder
    val content: ArrayList<Planification> = arrayListOf()) {
}

data class PlanificationResponseHolder(
    //val planificationVO3s: ArrayList<Planification> = arrayListOf()
    val content: ArrayList<Planification> = arrayListOf()
): Serializable {
}