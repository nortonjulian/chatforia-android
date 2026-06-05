package com.chatforia.android.network

import com.chatforia.android.auth.TokenStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.chatforia.android.upload.UploadImageResponse

class ApiClient(
    private val tokenStorage: TokenStorage
) {
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(Environment.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Environment.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    inline fun <reified T> send(
        request: ApiRequest
    ): T {
        return sendInternal(request) { body ->
            json.decodeFromString<T>(body)
        }
    }

    fun sendRaw(
        request: ApiRequest
    ): String {
        return sendInternal(request) { body -> body }
    }

    @PublishedApi
    internal fun <T> sendInternal(
        request: ApiRequest,
        decode: (String) -> T
    ): T {
        val url = "${Environment.API_BASE_URL}/${request.path.trimStart('/')}"

        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")

        if (request.requiresAuth) {
            val token = tokenStorage.read()
                ?: throw Exception("Unauthorized")

            builder.addHeader("Authorization", "Bearer $token")
        }

        val mediaType = "application/json".toMediaType()

        val body = request.bodyJson?.toRequestBody(mediaType)

        when (request.method) {
            HttpMethod.GET -> builder.get()

            HttpMethod.POST -> builder.post(
                body ?: "{}".toRequestBody(mediaType)
            )

            HttpMethod.PATCH -> builder.patch(
                body ?: "{}".toRequestBody(mediaType)
            )

            HttpMethod.DELETE -> {
                if (body != null) {
                    builder.delete(body)
                } else {
                    builder.delete()
                }
            }
        }

        val response = client.newCall(builder.build()).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $responseBody")
        }

        return decode(
            if (responseBody.isBlank()) "{}" else responseBody
        )
    }

    fun uploadMultipart(
        path: String,
        fileFieldName: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray,
        requiresAuth: Boolean = true
    ): UploadImageResponse {
        val url = "${Environment.API_BASE_URL}/${path.trimStart('/')}"

        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")

        if (requiresAuth) {
            val token = tokenStorage.read()
                ?: throw Exception("Unauthorized")

            builder.addHeader("Authorization", "Bearer $token")
        }

        val fileBody =
            bytes.toRequestBody(mimeType.toMediaType())

        val multipartBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    fileFieldName,
                    filename,
                    fileBody
                )
                .build()

        val response =
            client.newCall(
                builder
                    .post(multipartBody)
                    .build()
            ).execute()

        val responseBody =
            response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: $responseBody")
        }

        return json.decodeFromString<UploadImageResponse>(
            if (responseBody.isBlank()) "{}" else responseBody
        )
    }
}