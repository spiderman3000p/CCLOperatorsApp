package com.tautech.cclappoperators.models

import java.io.Serializable

data class CustomerAddressListResponse(
    val _embedded: CustomerAddressListResponseHolder) {
}

data class CustomerAddressListResponseHolder(
    val allowedAddressVO1s: ArrayList<Customer> = arrayListOf()
): Serializable {
}