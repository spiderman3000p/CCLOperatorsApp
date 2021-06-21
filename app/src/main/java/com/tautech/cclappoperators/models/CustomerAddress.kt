package com.tautech.cclappoperators.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Address(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "abbreviatedAddress")
    val abbreviatedAddress: String?,
    @ColumnInfo(name = "address")
    val address: String?,
    @ColumnInfo(name = "addressType")
    val addressType: String?,
    @ColumnInfo(name = "cityCodeError")
    val cityCodeError: String?,
    @ColumnInfo(name = "cityError")
    val cityError: String?,
    @ColumnInfo(name = "complement")
    val complement: String?,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "effectiveAddress")
    val effectiveAddress: String?,
    @ColumnInfo(name = "georeferenced")
    val georeferenced: Boolean?,
    @ColumnInfo(name = "latitude")
    val latitude: Double?,
    @ColumnInfo(name = "locality")
    val locality: String?,
    @ColumnInfo(name = "longitude")
    val longitude: Double?,
    @ColumnInfo(name = "neighborhood")
    val neighborhood: String?,
    @ColumnInfo(name = "normalizedAddress")
    val normalizedAddress: String?,
    @ColumnInfo(name = "normalizedAddressComplement")
    val normalizedAddressComplement: String?,
    @ColumnInfo(name = "normalizedAddressNoMeter")
    val normalizedAddressNoMeter: String?,
)