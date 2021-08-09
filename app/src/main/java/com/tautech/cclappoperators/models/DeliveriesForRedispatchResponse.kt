package com.tautech.cclappoperators.models

import java.io.Serializable

data class DeliveriesForRedispatchResponse(
    val _embedded: DeliveriesForRedispatchHolder) {
}

data class DeliveriesForRedispatchHolder(
    val deliveryVO7s: ArrayList<DeliveryForRedispatch> = arrayListOf()
): Serializable {
}