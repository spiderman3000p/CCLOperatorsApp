package com.tautech.cclappoperators.models

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import java.io.Serializable

@Entity(primaryKeys = ["deliveryId", "sourcePlanificationId", "targetPlanificationId"])
data class PendingToUploadTransfer(
    @NonNull
    @ColumnInfo(name = "sourcePlanificationId")
    var sourcePlanificationId: Long = 0,
    @NonNull
    @ColumnInfo(name = "targetPlanificationId")
    var targetPlanificationId: Long = 0,
    @NonNull
    @ColumnInfo(name = "deliveryId")
    var deliveryId: Long = 0): Serializable {

}