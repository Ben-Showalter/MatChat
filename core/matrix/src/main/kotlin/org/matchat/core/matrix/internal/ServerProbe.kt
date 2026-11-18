package org.matchat.core.matrix.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.matchat.core.model.ConnectionProblem
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * After the Matrix library fails with "error sending request for url (…)",
 * which hides the reason, fetches that same URL once with Android's own
 * networking and says which step fails. Only the address the library just
 * tried, the one the user typed or scanned, is fetched: no discovery
 * (AGENTS.md §0). Nothing is sent but a plain GET; the body is not read.
 */
internal object ServerProbe {

    private val FAILED_URL = Regex("""error sending request for url \(([^)\s]+)\)""")

    /** The URL inside the library's "error sending request" message, or null
     *  when [message] is some other failure. */
    fun failedUrl(message: String?): String? =
        message?.let { FAILED_URL.find(it)?.groupValues?.get(1) }

    suspend fun probe(url: String): Pair<ConnectionProblem, String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false
            }
            ConnectionProblem.LIBRARY_ONLY to "Android got HTTP ${connection.responseCode}"
        } catch (e: UnknownHostException) {
            ConnectionProblem.NO_ADDRESS to describe(e)
        } catch (e: SocketTimeoutException) {
            ConnectionProblem.TIMED_OUT to describe(e)
        } catch (e: ConnectException) {
            ConnectionProblem.REFUSED to describe(e)
        } catch (e: SSLException) {
            ConnectionProblem.SECURE_CONNECTION to describe(e)
        } catch (e: IOException) {
            ConnectionProblem.OTHER to describe(e)
        } catch (e: IllegalArgumentException) {
            ConnectionProblem.OTHER to describe(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun describe(e: Exception): String =
        "${e.javaClass.simpleName}: ${e.message.orEmpty()}".take(MAX_DETAIL)

    private const val TIMEOUT_MS = 15_000
    private const val MAX_DETAIL = 160
}
