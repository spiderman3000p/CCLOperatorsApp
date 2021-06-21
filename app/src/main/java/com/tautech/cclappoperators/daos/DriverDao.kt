package com.tautech.cclappoperators.daos
import androidx.room.*
import com.tautech.cclappoperators.models.Driver

@Dao
interface DriverDao {
    @Query("SELECT * FROM driver")
    fun getAll(): List<Driver>

    @Query("SELECT * FROM driver WHERE id IN (:driverIds)")
    fun loadAllByIds(driverIds: LongArray): List<Driver>

    @Query("SELECT * FROM driver WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Long?): Driver

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(driver: Driver)

    @Update
    fun update(driver: Driver)

    @Delete
    fun delete(driver: Driver)
}