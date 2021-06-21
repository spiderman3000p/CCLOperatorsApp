package com.tautech.cclappoperators.daos

import androidx.room.*
import com.tautech.cclappoperators.models.PendingToUploadCertification
import com.tautech.cclappoperators.models.PendingToUploadTransfer

@Dao
interface PendingToUploadTransferDao {
    @Query("SELECT * FROM pendingtouploadtransfer")
    fun getAll(): List<PendingToUploadTransfer>

    @Query("SELECT COUNT(*) FROM pendingtouploadtransfer")
    fun count(): Long

    @Query("SELECT * FROM pendingtouploadtransfer WHERE sourcePlanificationId = CAST(:planificationId AS NUMERIC)")
    fun getAllBySourcePlanification(planificationId: Long): List<PendingToUploadTransfer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(redispatch: PendingToUploadTransfer)

    @Update
    fun update(redispatch: PendingToUploadTransfer)

    @Delete
    fun delete(redispatch: PendingToUploadTransfer)
}