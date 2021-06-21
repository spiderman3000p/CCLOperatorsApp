package com.tautech.cclappoperators.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(primaryKeys = ["planificationId", "id", "deliveryId", "index"])
data class DeliveryLine(
    @SerializedName("id")
    @ColumnInfo(name = "id")
    var id: Long = 0,
    @SerializedName("packetType")
    @ColumnInfo(name = "packetType")
    var packetType: String = "",
    @SerializedName("price")
    @ColumnInfo(name = "price")
    var price: Double = 0.0,
    @SerializedName("quantity")
    @ColumnInfo(name = "quantity")
    var quantity: Int = 0,
    @SerializedName("deliveredQuantity")
    @ColumnInfo(name = "deliveredQuantity")
    var deliveredQuantity: Int = 0,
    @SerializedName("reference")
    @ColumnInfo(name = "reference")
    var reference: String = "",
    @SerializedName("description")
    @ColumnInfo(name = "description")
    var description: String = "",
    @SerializedName("weight")
    @ColumnInfo(name = "weight")
    var weight: Double = 0.0,
    @SerializedName("deliveryId")
    @ColumnInfo(name = "deliveryId")
    var deliveryId: Long = 0,
    @SerializedName("planificationId")
    @ColumnInfo(name = "planificationId")
    var planificationId: Long = 0,
    @SerializedName("uploaded")
    @ColumnInfo(name = "uploaded")
    var uploaded: Boolean = false,
    @SerializedName("certified")
    @ColumnInfo(name = "certified")
    var certified: Int = 0,
    @SerializedName("delivered")
    @ColumnInfo(name = "delivered")
    var delivered: Boolean? = null,
    @SerializedName("index")
    @ColumnInfo(name = "index")
    var index: Int = 0,
    @SerializedName("scannedOrder")
    @ColumnInfo(name = "scannedOrder")
    var scannedOrder: Int? = null) : Serializable {

}