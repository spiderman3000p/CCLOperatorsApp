package com.tautech.cclappoperators.models

import java.io.Serializable

data class PlanificationLinesResponse(
    val _embedded: PlanificationLinesResponseHolder) {
}

data class PlanificationLinesResponseHolder(
    val planificationDeliveryVO1s: ArrayList<Delivery> = arrayListOf()
): Serializable {
}