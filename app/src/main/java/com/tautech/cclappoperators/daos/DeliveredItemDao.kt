package com.tautech.cclappoperators.daos

import androidx.room.*
import com.tautech.cclappoperators.models.DeliveredItem

@Dao
interface DeliveredItemDao {
    @Query("SELECT * FROM delivereditem")
    fun getAll(): List<DeliveredItem>

    @Query("SELECT * FROM delivereditem WHERE id IN (:ids)")
    fun loadAllByIds(ids: LongArray): List<DeliveredItem>

    @Query("SELECT * FROM delivereditem WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Long?): DeliveredItem

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: DeliveredItem)

    @Update
    fun update(item: DeliveredItem)

    @Delete
    fun delete(item: DeliveredItem)
}