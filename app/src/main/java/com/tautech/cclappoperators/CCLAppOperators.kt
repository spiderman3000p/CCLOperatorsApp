package com.tautech.cclappoperators

import android.app.Application
import androidx.work.Configuration
import java.util.concurrent.Executors

class CCLAppOperators: Application(), Configuration.Provider {
    override fun getWorkManagerConfiguration() = Configuration.Builder()
        .setExecutor(Executors.newFixedThreadPool(2))
        .build()
}