package com.example.engine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket

data class ApkHttpServerInfo(
    val port: Int,
    val localUrl: String,
    val lanUrl: String?,
    val webAppLocalUrl: String,
    val webAppLanUrl: String?,
    val apkSizeMb: Float
)

/**
 * Lightweight embedded HTTP server for serving BOTH:
 * 1) Full Offline PWA WebApp Studio at "/" (index.html, manifest.webmanifest, sw.js)
 * 2) Direct APK installer download at "/NeonVFX_Studio_Full_16K_3D.apk"
 * over localhost and same-Wi-Fi LAN (PC & other mobile devices).
 */
class ApkLocalHttpServer {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    fun start(context: Context, scope: CoroutineScope): Result<ApkHttpServerInfo> {
        stop()
        val appContext = context.applicationContext
        val sourcePath = appContext.applicationInfo?.sourceDir
            ?: return Result.failure(IllegalStateException("APK source file unavailable."))
        val apkFile = File(sourcePath)
        if (!apkFile.exists() || !apkFile.canRead()) {
            return Result.failure(IllegalStateException("Cannot read APK file on this device."))
        }

        val socket = runCatching { ServerSocket(8787) }.getOrElse {
            runCatching { ServerSocket(0) }.getOrElse { err ->
                return Result.failure(err)
            }
        }
        serverSocket = socket
        val port = socket.localPort
        val lanIp = findLanIpv4Address()

        serverJob = scope.launch(Dispatchers.IO) {
            while (isActive && !socket.isClosed) {
                try {
                    val client = socket.accept()
                    launch(Dispatchers.IO) {
                        client.use { conn ->
                            val input = conn.getInputStream().bufferedReader()
                            val requestLine = input.readLine().orEmpty()
                            while (true) {
                                val line = input.readLine() ?: break
                                if (line.isBlank()) break
                            }
                            val parts = requestLine.split(" ")
                            val method = parts.getOrNull(0).orEmpty()
                            val path = parts.getOrNull(1)?.substringBefore('?').orEmpty()
                            val out = BufferedOutputStream(conn.getOutputStream())

                            if (method != "GET" && method != "HEAD") {
                                out.write("HTTP/1.1 405 Method Not Allowed\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
                                out.flush()
                                return@use
                            }

                            when (path) {
                                "/", "/index.html", "/webapp", "/webapp/" -> {
                                    serveAssetBytes(appContext, "webapp/index.html", "text/html; charset=utf-8", method == "GET", out)
                                }
                                "/manifest.webmanifest" -> {
                                    serveAssetBytes(appContext, "webapp/manifest.webmanifest", "application/manifest+json; charset=utf-8", method == "GET", out)
                                }
                                "/sw.js" -> {
                                    serveAssetBytes(appContext, "webapp/sw.js", "application/javascript; charset=utf-8", method == "GET", out)
                                }
                                "/NeonVFX_Studio_Full_16K_3D.apk", "/app.apk" -> {
                                    val headers = buildString {
                                        append("HTTP/1.1 200 OK\r\n")
                                        append("Content-Type: application/vnd.android.package-archive\r\n")
                                        append("Content-Disposition: attachment; filename=\"NeonVFX_Studio_Full_16K_3D.apk\"\r\n")
                                        append("Content-Length: ${apkFile.length()}\r\n")
                                        append("Connection: close\r\n\r\n")
                                    }
                                    out.write(headers.toByteArray(Charsets.US_ASCII))
                                    if (method == "GET") {
                                        apkFile.inputStream().use { fis ->
                                            val buf = ByteArray(64 * 1024)
                                            while (true) {
                                                val read = fis.read(buf)
                                                if (read <= 0) break
                                                out.write(buf, 0, read)
                                            }
                                        }
                                    }
                                    out.flush()
                                }
                                else -> {
                                    out.write("HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
                                    out.flush()
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    if (socket.isClosed) break
                }
            }
        }

        val fileName = "NeonVFX_Studio_Full_16K_3D.apk"
        return Result.success(
            ApkHttpServerInfo(
                port = port,
                localUrl = "http://127.0.0.1:$port/$fileName",
                lanUrl = lanIp?.let { "http://$it:$port/$fileName" },
                webAppLocalUrl = "http://127.0.0.1:$port/",
                webAppLanUrl = lanIp?.let { "http://$it:$port/" },
                apkSizeMb = apkFile.length() / (1024f * 1024f)
            )
        )
    }

    private fun serveAssetBytes(
        context: Context,
        assetPath: String,
        contentType: String,
        includeBody: Boolean,
        out: BufferedOutputStream
    ) {
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        val headers = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-cache\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(headers.toByteArray(Charsets.US_ASCII))
        if (includeBody) {
            out.write(bytes)
        }
        out.flush()
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun findLanIpv4Address(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
