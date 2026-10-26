package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE currentStock <= minThreshold ORDER BY currentStock ASC")
    fun getLowStockMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE expiryDateEpoch <= :thresholdEpoch ORDER BY expiryDateEpoch ASC")
    fun getExpiringSoonMedications(thresholdEpoch: Long): Flow<List<Medication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medications: List<Medication>)

    @Update
    suspend fun updateMedication(medication: Medication)

    @Query("UPDATE medications SET currentStock = :newStock WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("SELECT COUNT(*) FROM medications")
    suspend fun getCount(): Int
}
