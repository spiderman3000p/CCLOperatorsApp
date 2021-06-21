package com.tautech.cclappoperators.daos

import androidx.room.*
import com.tautech.cclappoperators.models.Planification

@Dao
interface PlanificationDao {
    @Query("SELECT * FROM planification ORDER BY dispatchDate DESC")
    fun getAll(): List<Planification>

    @Query("SELECT * FROM planification WHERE dispatchDate = :date ORDER BY dispatchDate DESC")
    fun getAll(date: String): List<Planification>

    @Query("SELECT * FROM planification WHERE planificationType = :type AND driverId = CAST(:driverId AS NUMERIC) ORDER BY dispatchDate DESC")
    fun getAllByTypeAndDriver(type: String, driverId: Long?): List<Planification>

    @Query("SELECT * FROM planification WHERE planificationType = :type ORDER BY id DESC")
    fun getAllByType(type: String): List<Planification>

    @Query("SELECT * FROM planification WHERE id IN (:planificationIds) ORDER BY dispatchDate DESC")
    fun loadAllByIds(planificationIds: LongArray): List<Planification>

    @Query("SELECT * FROM planification WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Long): Planification

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(planification: Planification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(planificationCertifications: List<Planification>)

    @Update
    fun update(planification: Planification)

    @Delete
    fun delete(planification: Planification)

    @Query("UPDATE planification SET planificationState=:state WHERE id = CAST(:planificationId AS NUMERIC)")
    fun updateState(planificationId: Long?, state: String)
}