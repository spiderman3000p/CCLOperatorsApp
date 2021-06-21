package com.tautech.cclappoperators.daos

import androidx.room.*
import com.tautech.cclappoperators.models.DeliveryLine

@Dao
interface DeliveryLineDao {
    @Query("SELECT * FROM deliveryline")
    fun getAll(): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline WHERE deliveryId = CAST(:deliveryId AS NUMERIC)")
    fun getByDelivery(deliveryId: Long): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline where uploaded = 0")
    fun getAllToUpload(): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline WHERE id IN (:ids)")
    fun loadAllByIds(ids: LongArray): List<DeliveryLine>

    @Query("SELECT COUNT(*) > 0 FROM deliveryline WHERE id = CAST(:id AS NUMERIC) AND `index` = CAST(:index AS NUMERIC)")
    fun exists(id: Long, index: Long): Boolean

    @Query("SELECT COUNT(*) FROM deliveryline AS A WHERE planificationId = CAST(:id AS NUMERIC) AND A.id IN (SELECT deliveryLineId FROM certification WHERE planificationId = A.planificationId AND `index` = A.`index` AND deliveryLineId = A.id)")
    fun countCertified(id: Long): Long

    @Query("SELECT * FROM deliveryline WHERE id = CAST(:id AS NUMERIC)")
    fun get(id: Long): DeliveryLine

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(deliveryLine: DeliveryLine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(deliveryLines: MutableList<DeliveryLine>)

    @Update
    fun update(deliveryLine: DeliveryLine)

    @Delete
    fun delete(deliveryLine: DeliveryLine)

    @Query("DELETE FROM deliveryLine")
    fun deleteAll()

    @Query("SELECT * FROM deliveryline WHERE planificationId = CAST(:planificationId AS NUMERIC)")
    fun getAllByPlanification(planificationId: Long): List<DeliveryLine>

    @Query("SELECT A.* FROM deliveryline AS A WHERE A.id = CAST(:id AS NUMERIC) AND A.`index` = CAST(:index AS NUMERIC) AND A.id IN (SELECT B.deliveryLineId FROM certification B WHERE B.planificationId = A.planificationId AND B.`index` = CAST(:index AS NUMERIC) AND B.id = CAST(:id AS NUMERIC))")
    fun hasBeenCertified(id: Long, index: Int?): DeliveryLine

    @Query("SELECT A.* FROM deliveryline AS A WHERE A.planificationId = CAST(:planificationId AS NUMERIC) AND A.id IN (SELECT B.deliveryLineId FROM certification AS B WHERE B.planificationId = CAST(:planificationId AS NUMERIC) AND B.`index` = A.`index` AND B.deliveryLineId = A.id)")
    fun getAllCertifiedByPlanification(planificationId: Long): List<DeliveryLine>

    @Query("SELECT A.* FROM deliveryline AS A WHERE A.planificationId = CAST(:planificationId AS NUMERIC) AND A.id NOT IN (SELECT B.deliveryLineId FROM certification AS B WHERE B.planificationId = CAST(:planificationId AS NUMERIC) AND B.`index` = A.`index` AND B.deliveryLineId = A.id)")
    fun getAllPendingByPlanification(planificationId: Long): List<DeliveryLine>

    @Query("SELECT * FROM deliveryline WHERE id IN (:ids)")
    fun getAllByIds(ids: LongArray): List<DeliveryLine>
}