package com.tautech.cclappoperators.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.io.Serializable

@Entity(primaryKeys = ["deliveryLineId", "planificationId", "index"])
data class PendingToUploadCertification(
    @ColumnInfo(name = "quantity")
    var quantity: Int = 1,
    @ColumnInfo(name = "index")
    var index: Int = 0,
    @ColumnInfo(name = "deliveryLineId")
    var deliveryLineId: Long = 0,
    @ColumnInfo(name = "planificationId")
    var planificationId: Long = 0,
    @ColumnInfo(name = "deliveryId")
    var deliveryId: Long = 0): Serializable {

}