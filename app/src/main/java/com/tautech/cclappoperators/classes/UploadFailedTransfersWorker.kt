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
import com.tautech.cclappoperators.models.PendingToUploadRedispatch
import com.tautech.cclappoperators.models.PendingToUploadTransfer
import com.tautech.cclappoperators.services.CclClient
import com.tautech.cclappoperators.services.MyWorkerManagerService
import retrofit2.Retrofit

class UploadFailedTransfersWorker
    (val appContext: Context,
    val workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    private val TAG = "UPLOAD_FAILED_REDISPATCHES_WORKER"
    var db: AppDatabase? = null
    private var retrofitClient: Retrofit? = null
    private var mStateManager: AuthStateManager? = null
    private var pendingTransfers: List<PendingToUploadTransfer>? = null

    fun initAll(){
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
            //showAlert(getString(R.string.error), getString(R.string.unauthorized_user))
            Log.e(TAG, "No hay autorizacion para el usuario. Sesion de usuario ha finalizado")
        }
    }

    override fun doWork(): Result {
        initAll()
        pendingTransfers = db?.pendingToUploadTransferDao()?.getAll()
        if (!pendingTransfers.isNullOrEmpty()) {
            val dataService: CclDataService? = CclClient.getInstance()?.create(
                CclDataService::class.java)
            if (dataService != null && mStateManager?.current?.accessToken != null) {
                for(pendingTransfer in pendingTransfers!!) {
                    MyWorkerManagerService.enqueUploadSingleTransferWork(appContext, pendingTransfer)
                }
            } else {
                Log.e(TAG, "El cliente http o la autenticacion de usuario son invalidos. No se puede realizar el proceso")
            }
        }
        return Result.success()
    }
}