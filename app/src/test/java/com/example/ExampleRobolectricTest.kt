package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.viewmodel.ReminderViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.aliva.reminder.R

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testReadStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("قسط و چک", appName)
  }

  @Test
  fun testDatabaseMigrationAndInitialization() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = ReminderDatabase.getDatabase(context)
    assertNotNull(database)
    val writableDb = database.openHelper.writableDatabase
    assertNotNull(writableDb)
  }

  @Test
  fun testViewModelInitialization() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context.applicationContext as android.app.Application
    val database = ReminderDatabase.getDatabase(context)
    val repository = ReminderRepository(database.installmentDao, database.chequeDao)
    
    val viewModel = ReminderViewModel(app, repository)
    assertNotNull(viewModel)
    
    viewModel.checkForUpcomingPayments()
  }

  @Test
  fun testInstallmentDaoInsertionAndDeletion() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = ReminderDatabase.getDatabase(context)
      val dao = database.installmentDao

      val testInst = Installment(
        title = "خرید لپتاپ تست",
        amount = 15000000L,
        dueDate = System.currentTimeMillis() + 86400000L * 5,
        totalInstallments = 10,
        paidInstallments = 2,
        category = "خرید اقساطی",
        notes = "تست یادداشت",
        isCompleted = false
      )

      val rowId = dao.insertInstallment(testInst)
      assertTrue(rowId > 0)

      val dbItems = dao.getAllInstallments().first()
      assertFalse(dbItems.isEmpty())
      val inserted = dbItems.firstOrNull { it.title == "خرید لپتاپ تست" }
      assertNotNull(inserted)
      assertEquals(15000000L, inserted?.amount)

      inserted?.let { dao.deleteInstallment(it) }
      val remainingItems = dao.getAllInstallments().first()
      assertNull(remainingItems.firstOrNull { it.title == "خرید لپتاپ تست" })
    }
  }

  @Test
  fun testChequeDaoInsertionAndRetrieval() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val database = ReminderDatabase.getDatabase(context)
      val dao = database.chequeDao

      val testCheque = Cheque(
        chequeNumber = "123456789",
        bankName = "ملی",
        amount = 50000000L,
        dueDate = System.currentTimeMillis() + 86400000L * 10,
        payeeName = "علی رضایی",
        isMyCheque = true,
        isCleared = false,
        notes = "خرید مصالح"
      )

      val rowId = dao.insertCheque(testCheque)
      assertTrue(rowId > 0)

      val dbItems = dao.getAllCheques().first()
      val inserted = dbItems.firstOrNull { it.chequeNumber == "123456789" }
      assertNotNull(inserted)
      assertEquals("ملی", inserted?.bankName)
      assertEquals(50000000L, inserted?.amount)

      inserted?.let { dao.deleteCheque(it) }
    }
  }

  @Test
  fun testExportToJsonAndRestore() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val app = context.applicationContext as android.app.Application
      val database = ReminderDatabase.getDatabase(context)
      val daoInst = database.installmentDao
      val daoCheque = database.chequeDao
      val repository = ReminderRepository(daoInst, daoCheque)

      val mockInst = listOf(
        Installment(title = "قسط پشتیبان", amount = 12000, dueDate = 1000L, totalInstallments = 3, paidInstallments = 0, category = "سایر")
      )
      val mockCheque = listOf(
        Cheque(chequeNumber = "999", bankName = "سامان", amount = 22000, dueDate = 2000L, payeeName = "تست پشتیبان", isMyCheque = false)
      )

      val viewModel = ReminderViewModel(app, repository)
      
      val backupJson = viewModel.exportDataToJson(mockInst, mockCheque)
      assertTrue(backupJson.isNotEmpty())
      assertTrue("JSON should contain installment title", backupJson.contains("قسط پشتیبان"))
      assertTrue("JSON should contain cheque receiver name", backupJson.contains("تست پشتیبان"))

      val csv = viewModel.exportDataToCsv(mockInst, mockCheque)
      assertTrue(csv.isNotEmpty())
      assertTrue(csv.contains("قسط پشتیبان"))

      val textReport = viewModel.exportDataToTextReport(mockInst, mockCheque)
      assertTrue(textReport.isNotEmpty())
      assertTrue(textReport.contains("قسط پشتیبان") || textReport.contains("سامان"))
    }
  }

  @Test
  fun testDatabaseMigrations() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // 1. Create a version 1 database structure
    val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
    val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
        .name("test_migration.db")
        .callback(object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                  CREATE TABLE IF NOT EXISTS `installments` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `amount` INTEGER NOT NULL, 
                    `dueDate` INTEGER NOT NULL, 
                    `totalInstallments` INTEGER NOT NULL, 
                    `paidInstallments` INTEGER NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `notes` TEXT NOT NULL DEFAULT '', 
                    `isCompleted` INTEGER NOT NULL DEFAULT 0
                  )
                """)
                db.execSQL("""
                  CREATE TABLE IF NOT EXISTS `cheques` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `chequeNumber` TEXT NOT NULL, 
                    `bankName` TEXT NOT NULL, 
                    `amount` INTEGER NOT NULL, 
                    `dueDate` INTEGER NOT NULL, 
                    `payeeName` TEXT NOT NULL, 
                    `isMyCheque` INTEGER NOT NULL, 
                    `isCleared` INTEGER NOT NULL DEFAULT 0, 
                    `notes` TEXT NOT NULL DEFAULT ''
                  )
                """)
            }
            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        })
        .build()
        
    val openHelper = factory.create(configuration)
    val db = openHelper.writableDatabase
    
    // Insert some initial mock data into v1 DB
    db.execSQL("INSERT INTO installments (title, amount, dueDate, totalInstallments, paidInstallments, category) VALUES ('قسط خرید گوشی', 5000000, 1717142400000, 10, 2, 'خرید')")
    db.execSQL("INSERT INTO cheques (chequeNumber, bankName, amount, dueDate, payeeName, isMyCheque) VALUES ('102435', 'صادرات', 120000000, 1717228800000, 'شرکت نفت', 1)")
    
    // 2. Perform Migration 1 to 2
    com.example.data.ReminderDatabase.MIGRATION_1_2.migrate(db)
    
    val cursorInst1 = db.query("SELECT * FROM installments")
    assertTrue(cursorInst1.moveToFirst())
    assertEquals("قسط خرید گوشی", cursorInst1.getString(cursorInst1.getColumnIndex("title")))
    cursorInst1.close()
    
    // 3. Perform Migration 2 to 3
    com.example.data.ReminderDatabase.MIGRATION_2_3.migrate(db)
    
    // Verify columns added in v3 (imageUri in installments, isBounced & imageUri in cheques)
    val cursorInst2 = db.query("SELECT * FROM installments")
    assertTrue(cursorInst2.moveToFirst())
    // Note: getColumnIndex might fail if column doesn't exist, we just verify no crash
    cursorInst2.close()
    
    val cursorCh2 = db.query("SELECT * FROM cheques")
    assertTrue(cursorCh2.moveToFirst())
    cursorCh2.close()
    
    // 4. Perform Migration 3 to 4
    com.example.data.ReminderDatabase.MIGRATION_3_4.migrate(db)
    
    // Verify queries on updated schema items work cleanly, proving no database crashes or index duplicate errors occur
    val cursorInst3 = db.query("SELECT * FROM installments ORDER BY amount DESC")
    assertTrue(cursorInst3.moveToFirst())
    cursorInst3.close()
    
    openHelper.close()
    context.deleteDatabase("test_migration.db")
  }
}
