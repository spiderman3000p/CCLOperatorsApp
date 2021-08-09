package com.tautech.cclappoperators.daos

import androidx.room.*
import com.tautech.cclappoperators.models.DeliveryForRedispatch
import com.tautech.cclappoperators.models.DeliveryLine

@Dao
interface DeliveryForRedispatchDao {
    @Query("SELECT * FROM deliveryforredispatch")
    fun getAll(): List<DeliveryForRedispatch>

    @Query("SELECT * FROM deliveryforredispatch WHERE orderDate = :date")
    fun getAll(date: String): List<DeliveryForRedispatch>

    @Query("SELECT * FROM deliveryforredispatch WHERE id IN (:deliveryIds)")
    fun loadAllByIds(deliveryIds: LongArray): List<DeliveryForRedispatch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(deliveryforredispatch: DeliveryForRedispatch)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(deliveries: MutableList<DeliveryForRedispatch>)

    @Update()
    fun update(deliveryforredispatch: DeliveryForRedispatch)

    @Delete
    fun delete(deliveryforredispatch: DeliveryForRedispatch)

    @Query("DELETE FROM deliveryforredispatch")
    fun deleteAll()

    @Query("SELECT * FROM deliveryforredispatch WHERE id = CAST(:id AS NUMERIC)")
    fun getById(id: Long?): DeliveryForRedispatch?

    @Query("SELECT * FROM deliveryforredispatch WHERE deliveryNumber = CAST(:deliveryNumber AS NUMERIC)")
    fun getByDeliveryNumber(deliveryNumber: Long): DeliveryForRedispatch?

    @Query("SELECT * FROM deliveryforredispatch WHERE id IN (:ids)")
    fun getAllByIds(ids: LongArray): List<DeliveryForRedispatch>

    @Query("SELECT A.* FROM deliveryforredispatch AS A WHERE A.deliveryNumber = CAST(:deliveryNumber AS NUMERIC) AND A.id IN (SELECT B.deliveryId FROM pendingtouploadredispatch B WHERE B.deliveryId = A.id)")
    fun hasBeenRedispatched(deliveryNumber: Long): DeliveryForRedispatch

    @Query("SELECT A.* FROM deliveryforredispatch AS A WHERE A.deliveryNumber = CAST(:deliveryNumber AS NUMERIC) AND A.id IN (SELECT B.deliveryId FROM pendingtouploadtransfer B WHERE B.deliveryId = A.id)")
    fun hasBeenTransfered(deliveryNumber: Long): DeliveryForRedispatch
}