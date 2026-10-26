package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.formula.FormulaEvaluator
import org.junit.Assert.assertEquals
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
        assertEquals("SerialLab", appName)
    }

    @Test
    fun `test formula evaluation`() {
        val res1 = FormulaEvaluator.evaluate("[x]*2000/32768", 16384.0)
        assertEquals(1000.0, res1, 0.01)

        val res2 = FormulaEvaluator.evaluate("if([x]>1000, [x]*1, [x]*2)", 500.0)
        assertEquals(1000.0, res2, 0.01)

        val res3 = FormulaEvaluator.evaluate("Round([x]*3.3/4096, 2)", 2048.0)
        assertEquals(1.65, res3, 0.01)
    }
}
