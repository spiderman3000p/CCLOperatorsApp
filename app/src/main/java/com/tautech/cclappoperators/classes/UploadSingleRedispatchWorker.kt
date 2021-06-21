package com.tautech.cclappoperators.classes

import android.content.Context
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tautech.cclappoperators.database.AppDatabase
import com.tautech.cclappoperators.interfaces.CclDataService
import com.tautech.cclappoperators.models.Certification
import com.tautech.cclappoperators.models.CertificationToUpload
import com.tautech.cclappoperators.models.PendingToUploadCertification
import com.tautech.cclappoperators.models.PendingToUploadRedispatch
import com.tautech.cclappoperators.services.CclClient
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException

class UploadSingleRedispatchWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "UPLOAD_SINGLE_REDISPATCH_WORKER"
    private val MAX_REINTENT = 3
    private var failedRequestsCounter = 0
    var db: AppDatabase? = null
    private var retrofitClient: Retrofit? = null
    private var mStateManager: AuthStateManager? = null

    override fun doWork(): Result {
        retrofitClient = CclClient.getInstance()
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
        if (!mStateManager!!.current.isAuthorized) {
            Log.e(TAG, "No hay autorizacion para el usuario. Sesion de usuario ha finalizado")
            return Result.failure()
        }
        if (inputData.hasKeyWithValueOfType("planificationId", Long::class.java)) {
            val planificationId = inputData.getLong("planificationId", 0)
            val pendingToUploadDelivery = db?.deliveryDao()?.getByIdAndPlanification(inputData.getLong("deliveryId", 0), planificationId)
            val pendingToUploadRedispatch = PendingToUploadRedispatch(sourcePlanificationId = planificationId, newState = "ReDispatched", deliveryId = pendingToUploadDelivery?.deliveryId!!)
            val dataService: CclDataService? = CclClient.getInstance()?.create(
                CclDataService::class.java)
            if (dataService != null && mStateManager?.current?.accessToken != null) {
                Log.i(TAG, "modificando estado de guia $pendingToUploadDelivery")
                    try {
                        val url = "delivery/${pendingToUploadDelivery.deliveryId}/changeState?newState=ReDispatched"
                        val call = dataService.changeDeliveryState(url,
                            "Bearer ${mStateManager?.current?.accessToken}")
                            .execute()
                        if (call.code() == 500 || call.code() == 400 || call.code() == 404 || call.code() == 403 || call.code() == 401) {
                            Log.e(TAG, "upload certification error response: ${call.errorBody()}")
                            return if (failedRequestsCounter < MAX_REINTENT) {
                                failedRequestsCounter++
                                Result.retry()
                            } else {
                                Log.i(TAG, "guardando redispatch en la tabla de requests fallidas $pendingToUploadDelivery")
                                db?.pendingToUploadRedispatchDao()?.insert(pendingToUploadRedispatch)
                                Log.i(TAG, "Redispatch guardada en la BD remota con exito")
                                Result.failure()
                            }
                        } else if (call.code() == 200 || call.code() == 201 || call.code() == 202) {
                            pendingToUploadDelivery?.deliveryState = "ReDispatched"
                            db?.deliveryDao()?.update(pendingToUploadDelivery!!)
                            db?.pendingToUploadRedispatchDao()?.delete(pendingToUploadRedispatch)
                            return Result.success()
                        }
                    } catch(toe: SocketTimeoutException) {
                        Log.e(TAG, "Network error when uploading redispatch $pendingToUploadRedispatch", toe)
                        return if (failedRequestsCounter < MAX_REINTENT) {
                            failedRequestsCounter++
                            Result.retry()
                        } else {
                            db?.pendingToUploadRedispatchDao()?.insert(pendingToUploadRedispatch)
                            Result.failure()
                        }
                    } catch (ioEx: IOException) {
                        Log.e(TAG,
                            "Network error when uploading certification $pendingToUploadRedispatch",
                            ioEx)
                        return if (failedRequestsCounter < MAX_REINTENT) {
                            failedRequestsCounter++
                            Result.retry()
                        } else {
                            db?.pendingToUploadRedispatchDao()?.insert(pendingToUploadRedispatch)
                            Result.failure()
                        }
                    }
                } else {
                Log.e(TAG, "El cliente http o la autenticacion de usuario son invalidos. No se puede realizar el proceso")
            }
        }
        return Result.failure()
    }
}