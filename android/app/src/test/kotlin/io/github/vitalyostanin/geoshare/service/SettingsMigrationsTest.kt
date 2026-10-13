package io.github.vitalyostanin.geoshare.service

import android.content.Context
import android.content.SharedPreferences
import io.github.vitalyostanin.geoshare.log.AppLog
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** What is done to the settings kept by an older or a newer build. */
class SettingsMigrationsTest {
    private val wasToSystemLog = AppLog.toSystemLog
    private val kept = mutableMapOf<String, Int>()
    private val written = mutableListOf<Int>()

    @Before
    fun keepOffTheSystemLog() {
        AppLog.toSystemLog = false
    }

    @After
    fun putItBack() {
        AppLog.toSystemLog = wasToSystemLog
    }

    private fun device(): Context {
        val editor = mockk<SharedPreferences.Editor>()
        every { editor.putInt(any(), any()) } answers {
            kept[firstArg()] = secondArg()
            written += secondArg<Int>()
            editor
        }
        every { editor.commit() } returns true
        val file = mockk<SharedPreferences> {
            every { contains(any()) } answers { kept.containsKey(firstArg()) }
            every { getInt(any(), any()) } answers { kept[firstArg()] ?: secondArg() }
            every { edit() } returns editor
        }

        return mockk { every { getSharedPreferences("schema", any()) } returns file }
    }

    @Test
    fun `no number is taken for the current one without a step`() {
        val ran = mutableListOf<Int>()

        SettingsMigrations.run(device(), 3, listOf({ ran += 1 }, { ran += 2 }))

        assertEquals(3, kept["version"])
        assertEquals(emptyList<Int>(), ran)
    }

    @Test
    fun `a lower version runs the steps in order and the number follows`() {
        val ran = mutableListOf<Int>()
        kept["version"] = 1

        SettingsMigrations.run(device(), 3, listOf({ ran += 1 }, { ran += 2 }))

        assertEquals(listOf(1, 2), ran)
        assertEquals(listOf(2, 3), written)
        assertEquals(3, kept["version"])
    }

    @Test
    fun `a higher version is left as it is`() {
        val ran = mutableListOf<Int>()
        kept["version"] = 5

        SettingsMigrations.run(device(), 3, listOf({ ran += 1 }, { ran += 2 }))

        assertEquals(5, kept["version"])
        assertEquals(emptyList<Int>(), written)
        assertEquals(emptyList<Int>(), ran)
    }
}
