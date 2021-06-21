package com.tautech.cclappoperators.services

import retrofit2.Retrofit

class KeycloakClient {
    companion object {
        var retrofit: Retrofit? = null
        val BASE_URL: String = "https://keycloak.cclauth.com/auth/realms/cclrealm/"
        fun getInstance(): Retrofit? {
            if (retrofit == null) {
                val okHttpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .readTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .writeTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .build()
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
            }
            return retrofit
        }
    }
}