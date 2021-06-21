package com.tautech.cclappoperators.models

import java.io.Serializable

data class DeliveryDeliveryLinesResponse(
    val _embedded: DeliveryDeliveryLinesResponseHolder) {
}

data class DeliveryDeliveryLinesResponseHolder(
    val planificationDeliveryDetailVO1s: ArrayList<DeliveryLine> = arrayListOf()
): Serializable {
}