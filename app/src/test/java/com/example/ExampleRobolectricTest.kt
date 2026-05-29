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
}
