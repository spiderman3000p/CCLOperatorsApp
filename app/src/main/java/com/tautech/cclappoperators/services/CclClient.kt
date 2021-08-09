package com.tautech.cclappoperators.services

import android.content.Context
import com.tautech.cclappoperators.R
import retrofit2.Retrofit

class CclClient {
    companion object {
        var retrofit: Retrofit? = null
        //val BASE_URL: String = "https://api.dev.ccl-express.com/"
        fun getBaseUrl(context: Context): String{
            val url = context.getString(R.string.api_url)
            return url
        }
        fun getInstance(context: Context): Retrofit? {
            if (retrofit == null) {
                val okHttpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .readTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .writeTimeout(2, java.util.concurrent.TimeUnit.MINUTES)
                    .build()
                retrofit = Retrofit.Builder()
                    .baseUrl(getBaseUrl(context))
                    .client(okHttpClient)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
            }
            return retrofit
        }
    }
}