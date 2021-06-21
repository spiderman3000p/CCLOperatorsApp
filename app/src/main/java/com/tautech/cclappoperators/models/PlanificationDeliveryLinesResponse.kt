package com.tautech.cclappoperators.models

import java.io.Serializable

data class PlanificationDeliveryLinesResponse(
    val _embedded: PlanificationDeliveryLinesResponseHolder) {
}

data class PlanificationDeliveryLinesResponseHolder(
    val planificationCertificationVO1s: ArrayList<DeliveryLine> = arrayListOf()
): Serializable {
}