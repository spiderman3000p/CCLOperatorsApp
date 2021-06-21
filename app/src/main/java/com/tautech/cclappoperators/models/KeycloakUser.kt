package com.tautech.cclappoperators.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import java.io.Serializable
@Entity
data class KeycloakUser(
    @SerializedName("attributes")
    @ColumnInfo(name = "attributes")
        var attributes: Attributes?,
    @SerializedName("email")
        var email: String = "",
    @SerializedName("emailVerified")
        var emailVerified: Boolean = false,
    @SerializedName("firstName")
        var firstName: String = "",
    @SerializedName("lastName")
        var lastName: String = "",
    @SerializedName("username")
        var username: String = ""): Serializable {

}