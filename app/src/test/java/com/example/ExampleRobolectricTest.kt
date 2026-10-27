package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.PaymentTransaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CantaPagos", appName)
    }

    @Test
    fun `test database transaction insertion and total`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val dao = db.paymentDao()

        val item = PaymentTransaction(
            wallet = "Nequi",
            amount = 15000L,
            formattedAmount = "$ 15.000",
            timestamp = System.currentTimeMillis()
        )
        dao.insertTransaction(item)

        val total = dao.getTotalForShift(0L).first()
        assertEquals(15000L, total)
    }
}
