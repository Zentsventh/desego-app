package com.desego.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

object OfflineImages {
    fun key(path: String): String = MessageDigest.getInstance("SHA-256").digest(path.toByteArray()).joinToString("") { "%02x".format(it) }
    fun load(context: Context, path: String): Bitmap? {
        if (path.isBlank()) return null
        val name = key(path)
        val directory = File(context.filesDir, "catalog-images").apply { mkdirs() }
        val file = File(directory, name)
        if (file.exists()) BitmapFactory.decodeFile(file.path)?.let { return it }
        runCatching { context.assets.open("catalog-images/$name").use { BitmapFactory.decodeStream(it) } }.getOrNull()?.let { return it }
        return runCatching {
            val base = BackendClient.baseUrl()
            val url = URL(URL(base), path)
            require(url.protocol in listOf("http", "https") && url.host == URI(base).host)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 5000; connection.readTimeout = 5000
                connection.instanceFollowRedirects = false
                require(connection.responseCode == 200 && connection.contentLengthLong <= 5_000_000)
                val bytes = connection.inputStream.use { it.readBytes() }
                require(bytes.size <= 5_000_000)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                file.writeBytes(bytes)
                bitmap
            } finally { connection.disconnect() }
        }.getOrNull()
    }
}
