package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Installment::class, Cheque::class], version = 2, exportSchema = false)
abstract class ReminderDatabase : RoomDatabase() {
    abstract val installmentDao: InstallmentDao
    abstract val chequeDao: ChequeDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_installments_dueDate ON installments (dueDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_installments_isCompleted ON installments (isCompleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cheques_dueDate ON cheques (dueDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cheques_isCleared ON cheques (isCleared)")
            }
        }

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
