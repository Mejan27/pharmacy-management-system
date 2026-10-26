package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InventoryLog
import com.example.data.model.RefillSubscription
import com.example.data.model.SaleRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RefillDao {
    @Query("SELECT * FROM refill_subscriptions ORDER BY nextRefillDateEpoch ASC")
    fun getAllRefills(): Flow<List<RefillSubscription>>

    @Query("SELECT * FROM refill_subscriptions WHERE status = 'ACTIVE' ORDER BY nextRefillDateEpoch ASC")
    fun getActiveRefills(): Flow<List<RefillSubscription>>

    @Query("SELECT * FROM refill_subscriptions WHERE id = :id")
    suspend fun getRefillById(id: Long): RefillSubscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefill(refill: RefillSubscription): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(refills: List<RefillSubscription>)

    @Update
    suspend fun updateRefill(refill: RefillSubscription)

    @Delete
    suspend fun deleteRefill(refill: RefillSubscription)

    @Query("SELECT COUNT(*) FROM refill_subscriptions")
    suspend fun getCount(): Int
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sale_records ORDER BY timestampEpoch DESC")
    fun getAllSales(): Flow<List<SaleRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sales: List<SaleRecord>)

    @Query("SELECT COUNT(*) FROM sale_records")
    suspend fun getCount(): Int
}

@Dao
interface InventoryLogDao {
    @Query("SELECT * FROM inventory_logs ORDER BY timestampEpoch DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<InventoryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: InventoryLog): Long
}
