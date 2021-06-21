package com.tautech.cclappoperators.interfaces

import com.tautech.cclappoperators.models.*
import okhttp3.MultipartBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.*

interface CclDataService {

    @GET
    @Headers("Content-Type: application/json")
    fun getPlanifications(
        @Url url: String,
        @Header("address-id") addressId: Long,
        @Header("Authorization") authorization: String
    ): Call<PlanificationsResponse>

    @GET
    @Headers("Content-Type: application/json")
    fun getPlanificationLines(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): Call<PlanificationLinesResponse>

    @GET
    @Headers("Content-Type: application/json")
    fun getPlanificationDeliveryLines(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): Call<PlanificationDeliveryLinesResponse>

    @POST
    @Headers("Content-Type: application/json")
    fun saveCertifiedDeliveryLine(
        @Body certifiedDeliveryLine: CertificationToUpload,
        @Header("Authorization") authorization: String,
        @Url url: String = "planificationCertifications"
    ): Call<Void>

    @GET
    @Headers("Content-Type: application/json")
    fun getPlanificationsCertifiedLines(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): Call<PlanificationCertificationsResponse>

    @POST
    @Headers("Content-Type: application/json")
    fun changePlanificationState(
        @Url url: String,
        @Header("Authorization") authorization: String): Call<Void>

    @POST
    @Headers("Content-Type: application/json")
    fun changeDeliveryState(
        @Url url: String,
        @Header("Authorization") authorization: String): Call<Void>

    @GET
    @Headers("Content-Type: application/json")
    fun getCarrierPartner(
            @Url url: String,
            @Header("Authorization") authorization: String
    ): Call<CarrierPartner>

    @GET
    @Headers("Content-Type: application/json")
    fun getCarrierAddressList(
            @Url url: String,
            @Header("Authorization") authorization: String
    ): Call<CarrierAddressListResponse>

    @GET
    @Headers("Content-Type: application/json")
    fun getCustomerAddressList(
            @Url url: String,
            @Header("Authorization") authorization: String
    ): Call<CustomerAddressListResponse>
}