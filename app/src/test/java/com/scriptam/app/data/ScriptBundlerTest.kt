package com.scriptam.app.data

import com.scriptam.app.data.bundle.ScriptBundler
import com.scriptam.app.data.db.ScriptEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ScriptBundlerTest {

    @Test
    fun testExportAndImportBundle() {
        val originalScript = ScriptEntity(
            id = 42L,
            fileName = "crypto_tracker.js",
            title = "Crypto Tracker Widget",
            accentColor = 0xFFF7931A
        )
        val originalCode = "const btc = await Native.fetch('https://api.coindesk.com'); console.log(btc);"

        val byteOut = ByteArrayOutputStream()
        ScriptBundler.exportScript(originalScript, originalCode, byteOut)

        val zipBytes = byteOut.toByteArray()
        assertTrue("Exported ZIP bytes must not be empty", zipBytes.isNotEmpty())

        val imported = ScriptBundler.importScript(ByteArrayInputStream(zipBytes))
        assertEquals(originalScript.title, imported.title)
        assertEquals(originalScript.accentColor, imported.accentColor)
        assertEquals(originalCode, imported.code)
    }
}
