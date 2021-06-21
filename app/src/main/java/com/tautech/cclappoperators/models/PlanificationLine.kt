package com.tautech.cclappoperators.models

import androidx.room.*
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(foreignKeys = [ForeignKey(entity = Planification::class,
    parentColumns = ["id"],
    childColumns = ["planificationId"],
    onDelete = ForeignKey.CASCADE,
    onUpdate = ForeignKey.CASCADE)])
data class PlanificationLine(
    @PrimaryKey
    @SerializedName("id")
    var id: Long = 0,
    @ColumnInfo(name = "planificationId")
    @SerializedName("planificationId")
    var planificationId: Long? = 0,
    @ColumnInfo(name = "cityDestination")
    @SerializedName("cityDestination")
    var cityDestination: String? = "",
    @ColumnInfo(name = "cityOrigin")
    @SerializedName("cityOrigin")
    var cityOrigin: String? = "",
    @ColumnInfo(name = "cityReceiverName")
    @SerializedName("cityReceiverName")
    var cityReceiverName: String? = "",
    @ColumnInfo(name = "receiverAddressLatitude")
    @SerializedName("receiverAddressLatitude")
    var receiverAddressLatitude: String? = "",
    @ColumnInfo(name = "receiverAddressLongitude")
    @SerializedName("receiverAddressLongitude")
    var receiverAddressLongitude: String? = "",
    @ColumnInfo(name = "citySenderName")
    @SerializedName("citySenderName")
    var citySenderName: String? = "",
    @ColumnInfo(name = "deliveryDate")
    @SerializedName("deliveryDate")
    var deliveryDate: String? = "",
    @ColumnInfo(name = "deliveryState")
    @SerializedName("deliveryState")
    var deliveryState: String? = "",
    @ColumnInfo(name = "deliveryNumber")
    @SerializedName("deliveryNumber")
    var deliveryNumber: Long? = 0,
    @ColumnInfo(name = "deliveryZoneCode")
    @SerializedName("deliveryZoneCode")
    var deliveryZoneCode: String? = "",
    @ColumnInfo(name = "detail")
    @SerializedName("detail")
    @Ignore
    var detail: ArrayList<DeliveryLine> = arrayListOf(),
    @ColumnInfo(name = "notes")
    @SerializedName("notes")
    var notes: String = "",
    @ColumnInfo(name = "receiverAddress")
    @SerializedName("receiverAddress")
    var receiverAddress: String? = "",
    @ColumnInfo(name = "receiverIdentificationNumber")
    @SerializedName("receiverIdentificationNumber")
    var receiverIdentificationNumber: String? = "",
    @ColumnInfo(name = "receiverIdentificationType")
    @SerializedName("receiverIdentificationType")
    var receiverIdentificationType: String? = "",
    @ColumnInfo(name = "receiverName")
    @SerializedName("receiverName")
    var receiverName: String? = "",
    @ColumnInfo(name = "receiverPhone")
    @SerializedName("receiverPhone")
    var receiverPhone: String? = "",
    @ColumnInfo(name = "referenceDocument")
    @SerializedName("referenceDocument")
    
    var referenceDocument: String? = "",
    @ColumnInfo(name = "senderAddress")
    @SerializedName("senderAddress")
    
    var senderAddress: String? = "",
    @ColumnInfo(name = "senderIdentificationNumber")
    @SerializedName("senderIdentificationNumber")
    
    var senderIdentificationNumber: String? = "",
    @ColumnInfo(name = "senderIdentificationType")
    @SerializedName("senderIdentificationType")
    
    var senderIdentificationType: String? = "",
    @ColumnInfo(name = "senderName")
    @SerializedName("senderName")
    
    var senderName: String? = "",
    @ColumnInfo(name = "senderPhone")
    @SerializedName("senderPhone")
    
    var senderPhone: String? = "",
    @ColumnInfo(name = "totalDeclared")
    @SerializedName("totalDeclared")
    
    var totalDeclared: Double? = 0.0,
    @ColumnInfo(name = "totalWeight")
    @SerializedName("totalWeight")
    
    var totalWeight: Double? = 0.0,
    @ColumnInfo(name = "totalQuantity")
    @SerializedName("totalQuantity")
    
    var totalQuantity: Int? = 0,
    @SerializedName("totalDelivered")
    @ColumnInfo(name = "totalDelivered")
    var totalDelivered: Int? = 0): Serializable {

}