package com.tautech.cclappoperators.models

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import java.io.Serializable

@Entity(primaryKeys = ["deliveryId", "sourcePlanificationId", "newState"])
data class PendingToUploadRedispatch(
    @NonNull
    @ColumnInfo(name = "sourcePlanificationId")
    var sourcePlanificationId: Long = 0,
    @NonNull
    @ColumnInfo(name = "newState")
    var newState: String = "",
    @NonNull
    @ColumnInfo(name = "deliveryId")
    var deliveryId: Long = 0): Serializable {

}