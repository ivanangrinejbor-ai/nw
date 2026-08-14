package org.catrobat.catroid.content

import android.util.Log
import java.net.InetAddress
import java.net.UnknownHostException
import okhttp3.Dns

class CustomDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return Dns.SYSTEM.lookup(hostname)
    }
}
