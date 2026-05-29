package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ReminderDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("قسط و چک", appName)
  }

  @Test
  fun `test database migration and initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = ReminderDatabase.getDatabase(context)
    assertNotNull(database)
    // Force open database and trigger onCreate/onUpgrade
    val writableDb = database.openHelper.writableDatabase
    assertNotNull(writableDb)
  }

  @Test
  fun `test viewModel initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context.applicationContext as android.app.Application
    val database = ReminderDatabase.getDatabase(context)
    val repository = com.example.data.ReminderRepository(database.installmentDao, database.chequeDao)
    
    // Instantiate ViewModel
    val viewModel = com.example.ui.viewmodel.ReminderViewModel(app, repository)
    assertNotNull(viewModel)
    
    // Trigger checkForUpcomingPayments manually
    viewModel.checkForUpcomingPayments()
  }
}
