/*
 * ************************************************************************
 *  AutoUpdate.kt
 * *************************************************************************
 * Copyright © 2024 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.vlc.util

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.Log
import android.util.Patterns
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.videolan.tools.KEY_LAST_UPDATE_TIME
import org.videolan.tools.Settings
import org.videolan.tools.putSingle
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.getUpdateUri
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object AutoUpdate {
    private const val TAG = "AutoUpdate"
    private val http = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
    }

    /**
     * Checks if an update is available in the nightlies
     *
     * @param context [Application] used to get the settings
     * @param skipChecks If true, the checks are skipped
     * @param listener Function called when an update is found
     */
    suspend fun checkUpdate(context: Application, skipChecks:Boolean = false, listener: (String, Date) -> Unit) = withContext(Dispatchers.IO) {
        //limit to debug builds (nightlies are included)
        if (!BuildConfig.DEBUG && !skipChecks) return@withContext

        //check if last update is older than 6 hours

        val settings = Settings.getInstance(context)
        if (!skipChecks && settings.getLong(KEY_LAST_UPDATE_TIME, 0L) > System.currentTimeMillis() - 6 * 3600000) {
            Log.i(TAG, "Last update is less than 6 hours")
            return@withContext
        }
        settings.putSingle(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())

        try {
            val arch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Build.SUPPORTED_ABIS[0]
            } else {
                Build.CPU_ABI
            }

            //get this abi:

            val abiCodes = mapOf(Pair("armeabi-v7a", "armv7"), Pair("arm64-v8a", "arm64"), Pair("x86", "x86"), Pair("x86_64", "x86_64"))
            if (!abiCodes.containsKey(arch)) throw Exception("Unsupported architecture")
            val abi = abiCodes[arch]
            Log.i(TAG, "Checking for update for abi! $abi")


            val buildTime = if (skipChecks) "2000-01-01" else context.getString(R.string.build_time)

            val localFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val webFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
            val buildDate = localFormat.parse(buildTime)
            val url = "http://artifacts.videolan.org/vlc-android/nightly-$abi/"
            val response = http.get(url)
            if (!response.status.isSuccess()) return@withContext
            val body = response.bodyAsText()
                val m = Patterns.WEB_URL.matcher(body)
                var found = false
                while (m.find() && !found) {
                    val decodedUrl = m.group()
                    val splitUrl = decodedUrl.split('-')
                    try {
                        val nightlyDate = webFormat.parse(splitUrl[splitUrl.size - 2])
                        nightlyDate?.let {
                            if (nightlyDate.time > (buildDate?.time ?: Long.MAX_VALUE)) {
                                Log.i(TAG, "Found update: $decodedUrl")
                                withContext(Dispatchers.Main) {
                                    listener.invoke("http://artifacts.videolan.org/vlc-android/nightly-$abi/$decodedUrl", nightlyDate)
                                }
                                found = true

                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Downloads the update and installs it
     *
     * @param context [Application] used for downloading and installing the update
     * @param updateURL URL of the update
     * @param loading Function called when the update is downloading
     */
    suspend fun downloadAndInstall(context: Application, updateURL: String, loading: (Boolean) -> Unit) = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) { loading.invoke(true) }
        download(context, updateURL)
        withContext(Dispatchers.Main) { loading.invoke(false) }
        installAPK(context)
    }

    /**
     * Downloads the update
     *
     * @param context [Application] used for downloading the update
     * @param url URL of the update
     */
    @Throws(IOException::class)
    private suspend fun download(context: Application, url: String) {
        val response: HttpResponse = http.get(url)
        if (!response.status.isSuccess()) throw IOException("Update download failed: HTTP ${response.status.value}")

        val downloadedFile = File(context.cacheDir, "update.apk")
        response.bodyAsChannel().toInputStream().use { input ->
            downloadedFile.outputStream().use { output -> input.copyTo(output) }
        }
    }

    /**
     * Installs the update
     *
     * @param context [Application] used for installing the update
     */
    private fun installAPK(context: Application) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(getUpdateUri(), "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, e.message, e)
        }
    }

    suspend fun clean(context: Application) = withContext(Dispatchers.IO) {
        try {
            val downloadedFile = File(context.cacheDir, "update.apk")
            if (downloadedFile.exists()) downloadedFile.delete() else { }
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }


}
