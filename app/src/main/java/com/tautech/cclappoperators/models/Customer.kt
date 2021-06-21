package com.tautech.cclappoperators.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Customer(
    @ColumnInfo(name = "carrierAddressId")
    var carrierAddressId: Long?,
    @ColumnInfo(name = "customerId")
    var customerId: Long?,
    @ColumnInfo(name = "identificationType")
    var identificationType: String?,
    @ColumnInfo(name = "identificationNumber")
    var identificationNumber: String?,
    @ColumnInfo(name = "name")
    var name: String?,
    @ColumnInfo(name = "phone")
    var phone: String?,
    @ColumnInfo(name = "email")
    var email: String?,
    @ColumnInfo(name = "contactEmail")
    var contactEmail: String?,
    @ColumnInfo(name = "contactName")
    var contactName: String?,
    @ColumnInfo(name = "contactPhone")
    var contactPhone: String?,
    @ColumnInfo(name = "addressId")
    var addressId: Long?,
    @ColumnInfo(name = "address")
    var address: String?,
    @ColumnInfo(name = "complement")
    var complement: String?,
    @ColumnInfo(name = "addressType")
    var addressType: String?,
    @ColumnInfo(name = "description")
    var description: String?,
    @ColumnInfo(name = "latitude")
    var latitude: Double?,
    @ColumnInfo(name = "locality")
    var locality: String?,
    @ColumnInfo(name = "longitude")
    var longitude: Double?,
    @ColumnInfo(name = "neighborhood")
    var neighborhood: String?,
    @ColumnInfo(name = "normalizedAddress")
    var normalizedAddress: String?,
    @ColumnInfo(name = "normalizedAddressComplement")
    var normalizedAddressComplement: String?,
    @ColumnInfo(name = "normalizedAddressNoMeter")
    var normalizedAddressNoMeter: String?,
    @ColumnInfo(name = "cityId")
    var cityId: Long?,
    @ColumnInfo(name = "showPaymentMethod")
    var showPaymentMethod: Boolean?,
    @ColumnInfo(name = "showUrbanPlanifications")
    var showUrbanPlanifications: Boolean?,
    @ColumnInfo(name = "showNationalPlanifications")
    var showNationalPlanifications: Boolean?,
    @ColumnInfo(name = "showPrintLabelsOptions")
    var showPrintLabelsOptions: Boolean?,
    @ColumnInfo(name = "showPrintGuidesOptions")
    var showPrintGuidesOptions: Boolean?,
    @ColumnInfo(name = "deliveryTimeThreshold")
    var deliveryTimeThreshold: Int?,
    @ColumnInfo(name = "syncBeetrack")
    var syncBeetrack: Boolean?
)