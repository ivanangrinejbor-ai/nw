package org.catrobat.catroid.apkbuildV3

object Base64Codec {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val DECODE = IntArray(256) { -1 }.also { table ->
        for (i in ALPHABET.indices) table[ALPHABET[i].code] = i
    }

    fun encode(data: ByteArray): String {
        val sb = StringBuilder((data.size + 2) / 3 * 4)
        val len = data.size
        var i = 0
        while (i < len) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < len) data[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < len) data[i + 2].toInt() and 0xFF else -1
            sb.append(ALPHABET[b0 ushr 2])
            sb.append(ALPHABET[((b0 shl 4) or (if (b1 >= 0) b1 ushr 4 else 0)) and 0x3F])
            sb.append(if (b1 >= 0) ALPHABET[((b1 shl 2) or (if (b2 >= 0) b2 ushr 6 else 0)) and 0x3F] else '=')
            sb.append(if (b2 >= 0) ALPHABET[b2 and 0x3F] else '=')
            i += 3
        }
        return sb.toString()
    }

    fun decode(input: String): ByteArray {
        val cleaned = input.filter { it != '=' && it != '\n' && it != '\r' && it != ' ' }
        val out = java.io.ByteArrayOutputStream(cleaned.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (c in cleaned) {
            val v = if (c.code < 256) DECODE[c.code] else -1
            if (v < 0) continue
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}