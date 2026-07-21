package org.catrobat.catroid.network

interface NetworkService {
    fun httpGet(url: String): String
    fun httpPost(url: String, body: String): String
    fun httpPut(url: String, body: String): String
    fun httpDelete(url: String): String
    fun httpHead(url: String): String
    fun httpPatch(url: String, body: String): String
    fun httpOptions(url: String): String
}
