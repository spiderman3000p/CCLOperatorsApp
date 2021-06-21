package com.tautech.cclappoperators.daos

import androidx.room.*
import com.tautech.cclappoperators.models.PendingToUploadCertification

@Dao
interface PendingToUploadCertificationDao {
    @Query("SELECT * FROM pendingtouploadcertification")
    fun getAll(): List<PendingToUploadCertification>

    @Query("SELECT COUNT(*) FROM pendingtouploadcertification")
    fun count(): Long

    @Query("SELECT * FROM pendingtouploadcertification WHERE planificationId = CAST(:planificationId AS NUMERIC)")
    fun getAllByPlanification(planificationId: Long): List<PendingToUploadCertification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(certification: PendingToUploadCertification)

    @Update
    fun update(certification: PendingToUploadCertification)

    @Delete
    fun delete(certification: PendingToUploadCertification)
}