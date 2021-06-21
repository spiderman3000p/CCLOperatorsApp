package com.tautech.cclappoperators.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.tautech.cclappoperators.classes.*
import com.tautech.cclappoperators.models.PendingToUploadCertification
import com.tautech.cclappoperators.models.PendingToUploadRedispatch
import com.tautech.cclappoperators.models.PendingToUploadTransfer
import java.io.File
import java.util.concurrent.TimeUnit

class MyWorkerManagerService {
    companion object{
        private val TAG = "MY_WORKER_MANAGER_SERVICE"
        val filesToUpload: MutableMap<String, File> = mutableMapOf()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueUploadSingleCertificationWork(context: Context, certification: PendingToUploadCertification){
            Log.i(TAG, "encolando work para subir certification $certification")
            val data = workDataOf(
                "deliveryId" to certification.deliveryId,
                "planificationId" to certification.planificationId,
                "deliveryLineId" to certification.deliveryLineId,
                "index" to certification.index,
                "quantity" to certification.quantity
            )
            val uploadWorkRequest =
                OneTimeWorkRequestBuilder<UploadSingleCertificationWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag("uploadCertificationsRequest-${certification.deliveryId}-${certification.deliveryLineId}-${certification.index}")
                    .setInputData(data)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork("uploadCertificationsRequest-${certification.deliveryId}-${certification.deliveryLineId}-${certification.index}", ExistingWorkPolicy.KEEP, uploadWorkRequest)
        }

        fun uploadFailedCertifications(context: Context){
            Log.i(TAG, "iniciando trabajo para buscar subidas de certificaciones fallidas")
            val uploadWorkRequest =
                PeriodicWorkRequestBuilder<UploadFailedCertificationsWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag("uploadFailedCertificationsRequest")
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork("uploadFailedCertificationsRequest", ExistingPeriodicWorkPolicy.KEEP, uploadWorkRequest)
        }

        fun uploadFailedRedispatches(context: Context){
            Log.i(TAG, "iniciando trabajo para buscar subidas de redespachos fallidas")
            val uploadWorkRequest =
                PeriodicWorkRequestBuilder<UploadFailedRedispatchesWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag("uploadFailedRedispatchRequest")
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork("uploadFailedRedispatchRequest", ExistingPeriodicWorkPolicy.KEEP, uploadWorkRequest)
        }

        fun uploadFailedTransfers(context: Context){
            Log.i(TAG, "iniciando trabajo para buscar subidas de transferencias fallidas")
            val uploadWorkRequest =
                PeriodicWorkRequestBuilder<UploadFailedTransfersWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag("uploadFailedTransfersRequest")
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork("uploadFailedTransfersRequest", ExistingPeriodicWorkPolicy.KEEP, uploadWorkRequest)
        }

        fun enqueUploadSingleRedispatchWork(context: Context, redispatch: PendingToUploadRedispatch){
            Log.i(TAG, "encolando work para subir redespacho $redispatch")
            val data = workDataOf(
                "deliveryId" to redispatch.deliveryId,
                "planificationId" to redispatch.sourcePlanificationId,
                "newState" to redispatch.newState
            )
            val uploadWorkRequest =
                OneTimeWorkRequestBuilder<UploadSingleRedispatchWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag("uploadRedispatchRequest-${redispatch.deliveryId}-${redispatch.sourcePlanificationId}-${redispatch.newState}")
                    .setInputData(data)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork("uploadRedispatchRequest-${redispatch.deliveryId}-${redispatch.sourcePlanificationId}-${redispatch.newState}", ExistingWorkPolicy.KEEP, uploadWorkRequest)
        }

        fun enqueUploadSingleTransferWork(context: Context, transfer: PendingToUploadTransfer){
            Log.i(TAG, "encolando work para subir transferencia $transfer")
            val data = workDataOf(
                "deliveryId" to transfer.deliveryId,
                "sourcePlanificationId" to transfer.sourcePlanificationId,
                "targetPlanificationId" to transfer.targetPlanificationId,
            )
            val uploadWorkRequest =
                OneTimeWorkRequestBuilder<UploadSingleTransferWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .addTag("uploadTransferRequest-${transfer.deliveryId}-${transfer.sourcePlanificationId}-${transfer.targetPlanificationId}")
                    .setInputData(data)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork("uploadTransferRequest-${transfer.deliveryId}-${transfer.sourcePlanificationId}-${transfer.targetPlanificationId}", ExistingWorkPolicy.KEEP, uploadWorkRequest)
        }
    }
}