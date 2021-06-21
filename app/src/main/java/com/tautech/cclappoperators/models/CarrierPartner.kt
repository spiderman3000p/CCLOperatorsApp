package com.tautech.cclappoperators.models

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

data class CarrierPartner(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long?,
    @ColumnInfo(name = "identificationType")
    var identificationType: String?,
    @ColumnInfo(name = "identificationNumber")
    var identificationNumber: String?,
    @ColumnInfo(name = "name")
    var name: String?,
    @ColumnInfo(name = "email")
    var email: String?,
    @ColumnInfo(name = "phone")
    var phone: String?,
    @ColumnInfo(name = "contactName")
    var contactName: String?,
    @ColumnInfo(name = "contactPhone")
    var contactPhone: String?,
    @ColumnInfo(name = "contactEmail")
    var contactEmail: String?,
    @ColumnInfo(name = "showPaymentMethod")
    var showPaymentMethod: Boolean?,
    @ColumnInfo(name = "packagingLicense")
    var packagingLicense: String?,
    @ColumnInfo(name = "licenseTransport")
    var licenseTransport: String?
)