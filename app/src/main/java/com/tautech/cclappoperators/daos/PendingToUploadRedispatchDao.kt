package com.tautech.cclappoperators.daos

import androidx.room.*
import com.tautech.cclappoperators.models.PendingToUploadCertification
import com.tautech.cclappoperators.models.PendingToUploadRedispatch

@Dao
interface PendingToUploadRedispatchDao {
    @Query("SELECT * FROM pendingtouploadredispatch")
    fun getAll(): List<PendingToUploadRedispatch>

    @Query("SELECT COUNT(*) FROM pendingtouploadredispatch")
    fun count(): Long

    @Query("SELECT * FROM pendingtouploadredispatch WHERE sourcePlanificationId = CAST(:planificationId AS NUMERIC)")
    fun getAllBySourcePlanification(planificationId: Long): List<PendingToUploadRedispatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(redispatch: PendingToUploadRedispatch)

    @Update
    fun update(redispatch: PendingToUploadRedispatch)

    @Delete
    fun delete(redispatch: PendingToUploadRedispatch)
}