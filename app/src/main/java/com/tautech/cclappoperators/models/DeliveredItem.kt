package com.tautech.cclappoperators.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.tautech.cclappoperators.models.DeliveryLine

@Entity
data class DeliveredItem(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id")
    var id: Long? = null,
    @ColumnInfo(name = "quantity")
    @SerializedName("quantity")
    var quantity: Int = 0,
    @ColumnInfo(name = "price")
    @SerializedName("price")
    var price: Double = 0.0,
    @Ignore
    @SerializedName("deliveryLine")
    var deliveryLine:  DeliveryLine? = null
){}