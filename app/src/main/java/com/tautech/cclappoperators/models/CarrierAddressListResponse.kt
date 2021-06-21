package com.tautech.cclappoperators.models

import java.io.Serializable

data class CarrierAddressListResponse(
    val _embedded: CarrierAddressListResponseHolder) {
}

data class CarrierAddressListResponseHolder(
    val addresses: ArrayList<Address> = arrayListOf()
): Serializable {
}