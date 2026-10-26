package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.InventoryLog
import com.example.data.model.Medication
import com.example.data.model.RefillSubscription
import com.example.data.model.SaleRecord

@Database(
    entities = [
        Medication::class,
        RefillSubscription::class,
        SaleRecord::class,
        InventoryLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PharmacyDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun refillDao(): RefillDao
    abstract fun saleDao(): SaleDao
    abstract fun inventoryLogDao(): InventoryLogDao

    companion object {
        @Volatile
        private var INSTANCE: PharmacyDatabase? = null

        fun getDatabase(context: Context): PharmacyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PharmacyDatabase::class.java,
                    "mejan_pharmacy_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
