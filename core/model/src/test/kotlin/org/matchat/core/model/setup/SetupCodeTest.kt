package org.matchat.core.model.setup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SetupCodeTest {

    private val key = "EsTj 3yST y93F SLpB jJsz eAXc 2XzA ygD3 w69H fGaN TKBj jXEd"

    private fun ok(text: String) = (SetupCodeParser.parse(text) as SetupCodeParser.Result.Ok).code

    @Test
    fun `every field of a v1 link`() {
        val code = ok("matchat:add?v=1&server=matrix.example.org&user=sam&pass=p%40ss&key=EsTj%203ySTy93F")
        assertEquals(SetupCode("matrix.example.org", "sam", "p@ss", "EsTj 3ySTy93F"), code)
    }

    @Test
    fun `a plus in a password stays a plus`() {
        assertEquals("a+b c", ok("matchat:add?v=1&user=sam&pass=a+b%20c").password)
    }

    @Test
    fun `the https form reads what is after the hash`() {
        val code = ok("https://example.org/matchat#add?v=1&user=sam&server=example.org")
        assertEquals("sam", code.user)
        assertEquals("example.org", code.server)
    }

    @Test
    fun `no version means 1, a newer version asks for an update`() {
        assertEquals("sam", ok("matchat:add?user=sam").user)
        assertEquals(SetupCodeParser.Result.TooNew, SetupCodeParser.parse("matchat:add?v=2&user=sam"))
    }

    @Test
    fun `unknown keys are ignored, empty fields left out`() {
        val code = ok("matchat:add?v=1&user=sam&color=blue&pass=")
        assertEquals("sam", code.user)
        assertNull(code.password)
    }

    @Test
    fun `other QR codes are not setup codes`() {
        listOf("", "https://example.org", "busycall:add?v=1&user=1", "hello", "matchat:add?v=1").forEach {
            assertEquals(SetupCodeParser.Result.NotASetupCode, SetupCodeParser.parse(it), it)
        }
    }

    @Test
    fun `toString never shows the password or key`() {
        val shown = SetupCode("s", "u", "hunter2", key).toString()
        assertFalse(shown.contains("hunter2"))
        assertFalse(shown.contains("EsTj"))
    }

    @Test
    fun `an address from a plain address, a matrix to link, or a setup code`() {
        assertEquals("@sam:example.org", ScannedValue.address("@sam:example.org"))
        assertEquals("@sam:example.org", ScannedValue.address("https://matrix.to/#/@sam:example.org"))
        assertEquals("@sam:example.org", ScannedValue.address("https://matrix.to/#/%40sam%3Aexample.org"))
        assertEquals("@sam:example.org", ScannedValue.address("matchat:add?v=1&user=sam&server=https://example.org/"))
        assertEquals("@sam:example.org", ScannedValue.address("matchat:add?v=1&user=%40sam%3Aexample.org"))
        assertNull(ScannedValue.address("https://example.org"))
        assertNull(ScannedValue.address("sam"))
    }

    @Test
    fun `a recovery key from the plain key or a setup code`() {
        assertEquals(key, ScannedValue.recoveryKey(key))
        assertEquals(key, ScannedValue.recoveryKey("matchat:add?v=1&key=" + key.replace(" ", "%20")))
        assertNull(ScannedValue.recoveryKey("matchat:add?v=1&user=sam"))
        assertNull(ScannedValue.recoveryKey("https://example.org/not-a-key"))
        assertTrue(ScannedValue.recoveryKey("short") == null)
    }
}
