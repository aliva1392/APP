package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Installment::class, Cheque::class], version = 3, exportSchema = false)
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE installments ADD COLUMN imageUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE cheques ADD COLUMN isBounced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cheques ADD COLUMN imageUri TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
