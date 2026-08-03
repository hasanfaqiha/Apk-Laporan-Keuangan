package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionEntity::class, BillEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getDatabase(androidxContext: android.content.Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    androidxContext.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database_v2_unused"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
