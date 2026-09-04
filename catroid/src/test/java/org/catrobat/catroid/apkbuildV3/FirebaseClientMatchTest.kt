package org.catrobat.catroid.apkbuildV3

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseClientMatchTest {

    private fun clientEntry(packageName: String?): JSONObject {
        val client = JSONObject()
        val clientInfo = JSONObject()
        clientInfo.put("mobilesdk_app_id", "1:123:android:abc")
        if (packageName != null) {
            val androidInfo = JSONObject()
            androidInfo.put("package_name", packageName)
            clientInfo.put("android_client_info", androidInfo)
        }
        client.put("client_info", clientInfo)
        return client
    }

    private fun clientsArray(vararg packages: String?): JSONArray {
        val array = JSONArray()
        for (pkg in packages) {
            array.put(clientEntry(pkg))
        }
        return array
    }

    @Test
    fun matchClient_exactMatchReturnsClient() {
        val clients = clientsArray("com.example.a", "com.example.game", "com.example.b")

        val match = FirebaseConfigManager.matchClient(clients, "com.example.game")

        assertTrue(match.client != null)
        assertEquals(
            "com.example.game",
            match.client!!
                .getJSONObject("client_info")
                .getJSONObject("android_client_info")
                .getString("package_name")
        )
        assertEquals(
            listOf("com.example.a", "com.example.game", "com.example.b"),
            match.foundPackages
        )
    }

    @Test
    fun matchClient_mismatchReturnsNullAndFoundPackages() {
        val clients = clientsArray("com.example.a", "com.example.b")

        val match = FirebaseConfigManager.matchClient(clients, "com.example.game")

        assertNull(match.client)
        assertEquals(listOf("com.example.a", "com.example.b"), match.foundPackages)
    }

    @Test
    fun matchClient_skipsClientsWithoutPackageInfo() {
        val array = JSONArray()
        array.put(JSONObject().put("client_info", JSONObject()))
        array.put(clientEntry("com.example.game"))
        array.put(clientEntry(null))

        val match = FirebaseConfigManager.matchClient(array, "com.example.game")

        assertTrue(match.client != null)
        assertEquals(listOf("com.example.game"), match.foundPackages)
    }

    @Test
    fun matchClient_emptyArrayReturnsNullAndEmptyList() {
        val match = FirebaseConfigManager.matchClient(JSONArray(), "com.example.game")

        assertNull(match.client)
        assertTrue(match.foundPackages.isEmpty())
    }

    @Test
    fun matchClient_firstMatchWins() {
        val first = clientEntry("com.example.game")
        val second = clientEntry("com.example.game")
        val array = JSONArray()
        array.put(first)
        array.put(second)

        val match = FirebaseConfigManager.matchClient(array, "com.example.game")

        assertTrue(match.client === first)
        assertEquals(listOf("com.example.game"), match.foundPackages)
    }
}
