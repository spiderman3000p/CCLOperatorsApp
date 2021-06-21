package com.tautech.cclappoperators.models

import java.io.Serializable

data class Attributes(
    var carrier: List<Long>? = null,
    var driverId: List<Long>? = null,
    var userType: List<String>? = null): Serializable {
}