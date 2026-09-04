package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Transaction::class, Bill::class, Category::class, RecurringRule::class, Budget::class],
    version = 3,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract val financeDao: FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        /**
         * v2 -> v3 (non-destructive):
         *  - adds the `updatedAt` conflict-resolution column (default 0) to the
         *    existing transactions / bills / categories tables, and
         *  - adds the `recurring_rules` and `budgets` tables.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bills ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS recurring_rules (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "amount REAL NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "accountType TEXT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "frequency TEXT NOT NULL, " +
                        "startDateMillis INTEGER NOT NULL, " +
                        "nextRunMillis INTEGER NOT NULL DEFAULT 0, " +
                        "isActive INTEGER NOT NULL DEFAULT 1, " +
                        "note TEXT NOT NULL DEFAULT ''" +
                        ")"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budgets (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "monthlyLimit REAL NOT NULL" +
                        ")"
                )
            }
        }

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
