package com.tautech.cclappoperators.daos

import androidx.room.*
import com.tautech.cclappoperators.models.Delivery
import com.tautech.cclappoperators.models.DeliveryLine

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM delivery")
    fun getAll(): List<Delivery>

    @Query("SELECT * FROM delivery WHERE orderDate = :date")
    fun getAll(date: String): List<Delivery>

    @Query("SELECT * FROM deliveryline WHERE deliveryId = CAST(:deliveryId AS NUMERIC)")
    fun getLines(deliveryId: Long): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline WHERE deliveryId = CAST(:deliveryId AS NUMERIC) GROUP BY id")
    fun getGroupedLines(deliveryId: Long): List<DeliveryLine>

    @Query("SELECT * FROM delivery WHERE planificationId = CAST(:planificationId AS NUMERIC)")
    fun getAllByPlanification(planificationId: Long): List<Delivery>

    @Query("SELECT * FROM delivery WHERE deliveryid IN (:deliveryIds)")
    fun loadAllByIds(deliveryIds: LongArray): List<Delivery>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(delivery: Delivery)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(deliveries: MutableList<Delivery>)

    @Update()
    fun update(delivery: Delivery)

    @Delete
    fun delete(delivery: Delivery)

    @Query("DELETE FROM delivery")
    fun deleteAll()

    @Query("SELECT * FROM delivery WHERE deliveryId = CAST(:id AS NUMERIC)")
    fun getById(id: Long?): Delivery

    @Query("SELECT * FROM delivery WHERE deliveryId = CAST(:id AS NUMERIC) AND planificationId = CAST(:planificationId AS NUMERIC)")
    fun getByIdAndPlanification(id: Long?, planificationId: Long?): Delivery

    @Query("SELECT * FROM delivery WHERE deliveryId IN (:ids)")
    fun getAllByIds(ids: LongArray): List<Delivery>

    @Query("SELECT A.* FROM delivery AS A WHERE A.deliveryNumber = CAST(:deliveryNumber AS NUMERIC) AND A.deliveryId IN (SELECT B.deliveryId FROM pendingtouploadredispatch B WHERE B.sourcePlanificationId = CAST(:planificationId AS NUMERIC) AND B.deliveryId = A.deliveryId)")
    fun hasBeenRedispatched(deliveryNumber: Long, planificationId: Long): Delivery

    @Query("SELECT A.* FROM delivery AS A WHERE A.deliveryNumber = CAST(:deliveryNumber AS NUMERIC) AND A.deliveryId IN (SELECT B.deliveryId FROM pendingtouploadtransfer B WHERE B.sourcePlanificationId = CAST(:planificationId AS NUMERIC) AND B.deliveryId = A.deliveryId)")
    fun hasBeenTransfered(deliveryNumber: Long, planificationId: Long): Delivery

    @Query("SELECT A.* FROM delivery AS A WHERE A.planificationId = CAST(:planificationId AS NUMERIC) AND A.deliveryId IN (SELECT B.deliveryId FROM pendingtouploadredispatch AS B WHERE B.sourcePlanificationId = CAST(:planificationId AS NUMERIC) AND B.`deliveryId` = A.deliveryId)")
    fun getAllRedispatchedByPlanification(planificationId: Long): List<Delivery>

    @Query("SELECT A.* FROM delivery AS A WHERE A.planificationId = CAST(:planificationId AS NUMERIC) AND A.deliveryId NOT IN (SELECT B.deliveryId FROM pendingtouploadredispatch AS B WHERE B.sourcePlanificationId = CAST(:planificationId AS NUMERIC) AND B.`deliveryId` = A.`deliveryId`)")
    fun getAllPendingBySourcePlanification(planificationId: Long): List<Delivery>
}