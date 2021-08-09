package com.tautech.cclappoperators.models

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity
data class DeliveryForRedispatch(
    @PrimaryKey
    var id: Long = 0,
    var customerAddressId: Long = 0,
    var receiverAddressLatitude: String? = "",
    var receiverAddressLongitude: String? = "",
    var orderDate: String? = "",
    var referenceDocument: String? = "",
    var deliveryState: String? = "",
    var deliveryNumber: Long? = 0,
    var receiverAddress: String? = "",
    var receiverName: String? = "",
    var totalValue: Double? = 0.0,
    var totalVolume: Double? = 0.0,
    var totalWeight: Double? = 0.0,
    var totalLines: Int? = 0,
    var totalQuantity: Int? = 0
): Serializable {

}