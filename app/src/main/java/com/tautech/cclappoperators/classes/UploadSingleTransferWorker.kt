package com.tautech.cclappoperators.classes

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.hasKeyWithValueOfType
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.models.*
import com.tautech.cclappoperators.services.CclClient
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException

class UploadSingleTransferWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "UPLOAD_SINGLE_TRANSFER_WORKER"
    private val MAX_REINTENT = 3
    private var failedRequestsCounter = 0
    var db: AppDatabase? = null
    private var mStateManager: AuthStateManager? = null

    override fun doWork(): Result {
        mStateManager = AuthStateManager.getInstance(appContext)
        try {
            db = AppDatabase.getDatabase(appContext)
        } catch(ex: SQLiteDatabaseLockedException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteAccessPermException) {
            Log.e(TAG, "Database error found", ex)
        } catch (ex: SQLiteCantOpenDatabaseException) {
            Log.e(TAG, "Database error found", ex)
        }
        if (mStateManager?.current?.isAuthorized != true) {
            Log.e(TAG, "No hay autorizacion para el usuario. Sesion de usuario ha finalizado")
            return Result.failure()
        }
        Log.i(TAG, "input data en work transfer: $inputData")
        if (inputData.getLong("deliveryId", 0) > 0 &&
            inputData.getLong("sourcePlanificationId", 0) > 0 &&
            inputData.getLong("targetPlanificationId", 0) > 0) {
            val sourcePlanificationId = inputData.getLong("sourcePlanificationId", 0)
            val targetPlanificationId = inputData.getLong("targetPlanificationId", 0)
            val deliveryId = inputData.getLong("deliveryId", 0)
            val pendingToUploadTransfer = PendingToUploadTransfer(sourcePlanificationId = sourcePlanificationId, targetPlanificationId = targetPlanificationId, deliveryId = deliveryId)
            Log.i(TAG, "pendingToUploadTransfer: $pendingToUploadTransfer")
            val dataService: CclDataService? = CclClient.getInstance(appContext)?.create(
                CclDataService::class.java)
            if (pendingToUploadTransfer != null) {
                if (dataService != null && mStateManager?.current?.accessToken != null) {
                    Log.i(TAG, "transfiriendo guia $pendingToUploadTransfer")
                    try {
                        val url = "planification/moveDelivery?deliveryId=$deliveryId&sourcePlanificationId=$sourcePlanificationId&targetPlanificationId=$targetPlanificationId"
                        val call = dataService.changeDeliveryState(url,
                                "Bearer ${mStateManager?.current?.accessToken}")
                                .execute()
                        if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                            Log.e(TAG, "upload transfer error response: ${call.errorBody()}")
                            return if (failedRequestsCounter < MAX_REINTENT) {
                                failedRequestsCounter++
                                Result.retry()
                            } else {
                                Log.i(TAG, "guardando transferencia en la tabla de requests fallidas $pendingToUploadTransfer")
                                db?.pendingToUploadTransferDao()?.insert(pendingToUploadTransfer)
                                Log.i(TAG, "Transferencia guardada en la BD remota con exito")
                                Result.failure()
                            }
                        } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                            db?.pendingToUploadTransferDao()?.delete(pendingToUploadTransfer)
                            return Result.success()
                        }
                    } catch (toe: SocketTimeoutException) {
                        Log.e(TAG, "Network error when uploading transfer $pendingToUploadTransfer", toe)
                        return if (failedRequestsCounter < MAX_REINTENT) {
                            failedRequestsCounter++
                            Result.retry()
                        } else {
                            db?.pendingToUploadTransferDao()?.insert(pendingToUploadTransfer)
                            Result.failure()
                        }
                    } catch (ioEx: IOException) {
                        Log.e(TAG,
                                "Network error when uploading certification $pendingToUploadTransfer",
                                ioEx)
                        return if (failedRequestsCounter < MAX_REINTENT) {
                            failedRequestsCounter++
                            Result.retry()
                        } else {
                            db?.pendingToUploadTransferDao()?.insert(pendingToUploadTransfer)
                            Result.failure()
                        }
                    }
                } else {
                    Log.e(TAG, "El cliente http o la autenticacion de usuario son invalidos. No se puede realizar el proceso")
                }
            } else {
                Log.e(TAG, "el objeto de transferencia es invalido")
            }
        } else {
            Log.e(TAG, "No se recibieron datos validos para transferir delivery")
        }
        return Result.failure()
    }
}