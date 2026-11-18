package org.matchat.core.matrix.internal

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.matchat.core.model.ConnectionProblem
import java.net.ServerSocket

class ServerProbeTest {

    @Test
    fun `finds the URL in the library's error`() {
        val message = "error sending request for url (https://chats.example.org/.well-known/matrix/client)"
        assertEquals("https://chats.example.org/.well-known/matrix/client", ServerProbe.failedUrl(message))
    }

    @Test
    fun `other failures are not probed`() {
        assertNull(ServerProbe.failedUrl("M_FORBIDDEN: Invalid username or password"))
        assertNull(ServerProbe.failedUrl(null))
    }

    @Test
    fun `a name that does not exist is reported as not found`() = runBlocking {
        // .invalid never resolves (RFC 6761); no real server is contacted.
        val (problem, _) = ServerProbe.probe("http://no-such-server.invalid/")
        assertEquals(ConnectionProblem.NO_ADDRESS, problem)
    }

    @Test
    fun `a closed port is reported as turned away`() = runBlocking {
        val port = ServerSocket(0).use { it.localPort } // free, now closed
        val (problem, _) = ServerProbe.probe("http://127.0.0.1:$port/")
        assertEquals(ConnectionProblem.REFUSED, problem)
    }

    @Test
    fun `a server that answers means only the library failed`() = runBlocking {
        ServerSocket(0).use { server ->
            val answer = Thread {
                server.accept().use { socket ->
                    socket.getInputStream().read(ByteArray(1024))
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n".toByteArray())
                }
            }.apply { start() }
            val (problem, detail) = ServerProbe.probe("http://127.0.0.1:${server.localPort}/")
            answer.join()
            assertEquals(ConnectionProblem.LIBRARY_ONLY, problem)
            assertEquals("Android got HTTP 200", detail)
        }
    }
}
