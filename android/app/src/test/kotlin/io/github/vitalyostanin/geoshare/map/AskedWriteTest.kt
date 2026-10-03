package io.github.vitalyostanin.geoshare.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AskedWriteTest {
    // Unconfined runs each write to its end before write() returns.
    private fun asked() = AskedWrite<String>(CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `a write that went through closes the dialog`() {
        val write = asked()

        write.ask("phone")
        write.write { WriteOutcome.DONE }

        assertNull(write.asked)
        assertNull(write.trouble)
        assertFalse(write.busy)
    }

    @Test
    fun `a refused write keeps the dialog open and says so`() {
        val write = asked()

        write.ask("phone")
        write.write { WriteOutcome.REFUSED }

        assertEquals("phone", write.asked)
        assertEquals(WriteOutcome.REFUSED, write.trouble)
        assertFalse(write.busy)
    }

    @Test
    fun `the write is made about what was asked`() {
        val write = asked()
        var wrote: String? = null

        write.ask("phone")
        write.write {
            wrote = it
            WriteOutcome.DONE
        }

        assertEquals("phone", wrote)
    }

    @Test
    fun `asking again forgets the last refusal`() {
        val write = asked()

        write.ask("first")
        write.write { WriteOutcome.REFUSED }
        write.cancel()
        write.ask("second")

        assertEquals("second", write.asked)
        assertNull(write.trouble)
    }

    @Test
    fun `changing the open dialog keeps the last refusal`() {
        val write = asked()

        write.ask("first")
        write.write { WriteOutcome.UNANSWERED }
        write.change("second")

        assertEquals("second", write.asked)
        assertEquals(WriteOutcome.UNANSWERED, write.trouble)
    }

    @Test
    fun `nothing is written while the dialog is closed`() {
        val write = asked()
        var tried = false

        write.write {
            tried = true
            WriteOutcome.DONE
        }

        assertFalse(tried)
    }
}
