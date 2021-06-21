package com.tautech.cclappoperators.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import java.io.Serializable
@Entity
data class CertificationToUpload(
    @SerializedName("quantity")
    @ColumnInfo(name = "quantity")
    var quantity: Int = 0,
    @SerializedName("index")
    @ColumnInfo(name = "index")
    var index: Int? = 0,
    @SerializedName("planification")
    @ColumnInfo(name = "planification")
    var planification: String = "",
    @SerializedName("delivery")
    @ColumnInfo(name = "delivery")
    var delivery: String = "",
    @SerializedName("deliveryLine")
    @ColumnInfo(name = "deliveryLine")
    var deliveryLine: String = ""): Serializable {

}