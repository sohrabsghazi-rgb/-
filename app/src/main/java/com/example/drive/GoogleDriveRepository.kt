package com.example.drive

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class DriveFileItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedTime: String
) {
    val isSupportedForAiAnalysis: Boolean
        get() = mimeType.startsWith("image/") ||
            mimeType.startsWith("video/") ||
            mimeType == "application/json" ||
            mimeType.startsWith("text/")
}

data class DriveFolderCreated(
    val id: String,
    val name: String,
    val webViewLink: String
)

sealed interface DriveAuthOutcome {
    data class AccessToken(val token: String) : DriveAuthOutcome
    data class ResolutionRequired(val pendingIntent: PendingIntent) : DriveAuthOutcome
    data class Error(val message: String) : DriveAuthOutcome
}

class GoogleDriveRepository {
    companion object {
        const val USER_DRIVE_PACK_FOLDER_ID = "1O1ndOpbuyG0uC0VRs4rW5TwIzaDpplaC"
        private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
        private const val SCOPE_DRIVE_READONLY = "https://www.googleapis.com/auth/drive.readonly"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun authorizeDrive(activity: Activity): DriveAuthOutcome =
        suspendCancellableCoroutine { cont ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(
                    listOf(
                        Scope(SCOPE_DRIVE_FILE),
                        Scope(SCOPE_DRIVE_READONLY)
                    )
                )
                .build()

            Identity.getAuthorizationClient(activity)
                .authorize(request)
                .addOnSuccessListener { result ->
                    if (!cont.isActive) return@addOnSuccessListener
                    val token = result.accessToken
                    val pending = result.pendingIntent
                    when {
                        result.hasResolution() && pending != null ->
                            cont.resume(DriveAuthOutcome.ResolutionRequired(pending))
                        !token.isNullOrBlank() ->
                            cont.resume(DriveAuthOutcome.AccessToken(token))
                        else ->
                            cont.resume(DriveAuthOutcome.Error("Google Drive authorization returned an empty access token."))
                    }
                }
                .addOnFailureListener { exc ->
                    if (!cont.isActive) return@addOnFailureListener
                    cont.resume(
                        DriveAuthOutcome.Error(
                            exc.localizedMessage ?: "Google Drive authorization failed."
                        )
                    )
                }
        }

    fun extractAccessTokenFromIntent(context: Context, data: Intent?): Result<String> {
        return try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (!token.isNullOrBlank()) {
                Result.success(token)
            } else {
                Result.failure(IllegalStateException("Google Drive authorization did not return an access token."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listDriveFolderFiles(
        accessToken: String,
        folderId: String = USER_DRIVE_PACK_FOLDER_ID
    ): Result<List<DriveFileItem>> = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("'$folderId' in parents and trashed = false", "UTF-8")
            val fields = URLEncoder.encode("nextPageToken,files(id,name,mimeType,size,modifiedTime)", "UTF-8")
            val allItems = mutableListOf<DriveFileItem>()
            var pageToken: String? = null

            do {
                val pageParam = if (!pageToken.isNullOrBlank()) {
                    "&pageToken=${URLEncoder.encode(pageToken, "UTF-8")}"
                } else {
                    ""
                }
                val url = "https://www.googleapis.com/drive/v3/files?q=$query&fields=$fields&pageSize=100&supportsAllDrives=true&includeItemsFromAllDrives=true$pageParam"
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(
                            IllegalStateException("Drive list failed (${resp.code}): ${extractDriveError(body)}")
                        )
                    }
                    val json = JSONObject(body)
                    val arr = json.optJSONArray("files") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        allItems.add(
                            DriveFileItem(
                                id = obj.getString("id"),
                                name = obj.optString("name", "untitled"),
                                mimeType = obj.optString("mimeType", "application/octet-stream"),
                                sizeBytes = obj.optString("size", "0").toLongOrNull() ?: 0L,
                                modifiedTime = obj.optString("modifiedTime", "")
                            )
                        )
                    }
                    pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
                }
            } while (pageToken != null)

            Result.success(allItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadDriveFileBytes(
        accessToken: String,
        fileId: String,
        maxBytes: Long = 18L * 1024 * 1024
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&supportsAllDrives=true"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Drive download failed (${resp.code}): ${extractDriveError(err)}")
                    )
                }
                val bytes = resp.body?.bytes()
                    ?: return@withContext Result.failure(IllegalStateException("Empty file stream from Drive."))
                if (bytes.size > maxBytes) {
                    return@withContext Result.failure(
                        IllegalArgumentException("File exceeds ${maxBytes / (1024 * 1024)}MB Gemini inline analysis limit.")
                    )
                }
                Result.success(bytes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDriveFolder(
        accessToken: String,
        folderName: String,
        parentId: String? = null
    ): Result<DriveFolderCreated> = withContext(Dispatchers.IO) {
        try {
            val metadata = JSONObject().apply {
                put("name", folderName)
                put("mimeType", "application/vnd.google-apps.folder")
                if (!parentId.isNullOrBlank()) {
                    put("parents", JSONArray().put(parentId))
                }
            }
            val url = "https://www.googleapis.com/drive/v3/files?fields=id,name,webViewLink&supportsAllDrives=true"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Create folder failed (${resp.code}): ${extractDriveError(body)}")
                    )
                }
                val json = JSONObject(body)
                val id = json.getString("id")
                val link = json.optString("webViewLink").ifBlank {
                    "https://drive.google.com/drive/folders/$id"
                }
                Result.success(
                    DriveFolderCreated(
                        id = id,
                        name = json.optString("name", folderName),
                        webViewLink = link
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFileMultipart(
        accessToken: String,
        file: File,
        mimeType: String,
        parentFolderId: String? = null,
        uploadFileName: String = file.name
    ): Result<DriveFolderCreated> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canRead()) {
                return@withContext Result.failure(IllegalStateException("File is unreadable: ${file.name}"))
            }
            val metadata = JSONObject().apply {
                put("name", uploadFileName)
                if (!parentFolderId.isNullOrBlank()) {
                    put("parents", JSONArray().put(parentFolderId))
                }
            }
            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(
                    metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                )
                .addPart(
                    file.asRequestBody(mimeType.toMediaType())
                )
                .build()

            val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,webViewLink&supportsAllDrives=true"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Upload failed (${resp.code}) for $uploadFileName: ${extractDriveError(body)}")
                    )
                }
                val json = JSONObject(body)
                val id = json.getString("id")
                val link = json.optString("webViewLink").ifBlank {
                    "https://drive.google.com/file/d/$id/view"
                }
                Result.success(
                    DriveFolderCreated(
                        id = id,
                        name = json.optString("name", uploadFileName),
                        webViewLink = link
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun makeFilePublicAndDirectUrl(
        accessToken: String,
        fileId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("role", "reader")
                put("type", "anyone")
            }
            val url = "https://www.googleapis.com/drive/v3/files/$fileId/permissions?supportsAllDrives=true"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("Permission update failed (${resp.code}): ${extractDriveError(body)}")
                    )
                }
                Result.success("https://drive.google.com/uc?export=download&id=$fileId")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getInstalledApkFile(context: Context): File? {
        return try {
            val sourcePath = context.applicationInfo?.sourceDir ?: return null
            val apk = File(sourcePath)
            if (apk.exists() && apk.canRead()) apk else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDriveError(raw: String): String {
        return try {
            val json = JSONObject(raw)
            json.optJSONObject("error")?.optString("message") ?: raw.take(180)
        } catch (_: Exception) {
            raw.take(180).ifBlank { "Unknown Drive error" }
        }
    }
}
