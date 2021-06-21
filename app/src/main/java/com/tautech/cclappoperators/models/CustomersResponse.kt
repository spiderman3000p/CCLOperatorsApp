package com.tautech.cclappoperators.models

import java.io.Serializable

data class CustomersResponse(
    val _embedded: CustomersResponseHolder) {
}

data class CustomersResponseHolder(
    val addresses: ArrayList<Customer> = arrayListOf()
): Serializable {
}