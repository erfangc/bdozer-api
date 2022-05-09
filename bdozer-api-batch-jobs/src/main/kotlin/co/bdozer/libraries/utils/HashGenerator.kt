package co.bdozer.libraries.utils

import java.security.MessageDigest

object HashGenerator {
    private val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

    fun hash(vararg components: String): String {
        val str = components.joinToString()
        return bytesToHex(digest.digest(str.encodeToByteArray()))
    }

    private fun bytesToHex(hash: ByteArray): String {
        val hexString = StringBuilder(2 * hash.size)
        for (i in hash.indices) {
            val hex = Integer.toHexString(0xff and hash[i].toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

}