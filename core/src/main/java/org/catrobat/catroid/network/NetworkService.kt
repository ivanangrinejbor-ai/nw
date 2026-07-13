package org.catrobat.catroid.network

/**
 * Platform-independent HTTP/network surface used by web-request bricks.
 *
 * Android and desktop each provide their own implementation so that
 * sprite action code can make simple HTTP requests without depending
 * on Android's [android.os.AsyncTask] or Volley/OkHttp directly.
 */
interface NetworkService {
    /**
     * Perform a synchronous HTTP GET and return the response body as a string.
     * Timeout should be handled internally (default ~10 s).
     */
    fun httpGet(url: String): String

    /**
     * Perform a synchronous HTTP POST with the given body and return the
     * response body as a string.
     */
    fun httpPost(url: String, body: String): String

    /**
     * Perform a synchronous HTTP PUT with the given body and return the
     * response body as a string.
     */
    fun httpPut(url: String, body: String): String

    /**
     * Perform a synchronous HTTP DELETE and return the response body as a string.
     */
    fun httpDelete(url: String): String
}
