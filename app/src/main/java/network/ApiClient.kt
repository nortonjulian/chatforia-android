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
import com.chatforia.android.upload.UploadImageResponse
import com.chatforia.android.numbers.RegulatoryDocumentTransport

class ApiClient(
    private val tokenStorage: TokenStorage
) : ApiTransport, RegulatoryDocumentTransport {
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

    override fun sendRaw(
        request: ApiRequest
    ): String {
        return sendInternal(request) { body -> body }
    }

    fun sendRawWithToken(
        request: ApiRequest,
        authToken: String
    ): String {
        return sendInternal(
            request = request,
            explicitToken = authToken
        ) { body ->
            body
        }
    }

    @PublishedApi
    internal fun <T> sendInternal(
        request: ApiRequest,
        explicitToken: String? = null,
        decode: (String) -> T
    ): T {
        val url = "${Environment.API_BASE_URL}/${request.path.trimStart('/')}"

        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")

        if (request.requiresAuth) {
            val token =
                explicitToken
                    ?.takeIf { it.isNotBlank() }
                    ?: tokenStorage.read()
                    ?: throw Exception("Unauthorized")

            builder.addHeader(
                "Authorization",
                "Bearer $token"
            )
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
            throw ApiException(
                statusCode = response.code,
                responseBody = responseBody
            )
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
            throw ApiException(
                statusCode = response.code,
                responseBody = responseBody
            )
        }

        return json.decodeFromString<UploadImageResponse>(
            if (responseBody.isBlank()) "{}" else responseBody
        )
    }

    fun putBytesToUrl(
        uploadUrl: String,
        mimeType: String,
        bytes: ByteArray
    ) {
        val body = bytes.toRequestBody(mimeType.toMediaType())

        val request = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .addHeader("Content-Type", mimeType)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw Exception("PUT upload failed HTTP ${response.code}: $responseBody")
        }
    }

    fun <T> uploadMultipartTyped(
        path: String,
        fileFieldName: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray,
        requiresAuth: Boolean = true
    ): T {
        val url = "${Environment.API_BASE_URL}/${path.trimStart('/')}"

        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")

        if (requiresAuth) {
            val token = tokenStorage.read()
                ?: throw Exception("Unauthorized")

            builder.addHeader("Authorization", "Bearer $token")
        }

        val fileBody = bytes.toRequestBody(mimeType.toMediaType())

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                fileFieldName,
                filename,
                fileBody
            )
            .build()

        val response = client.newCall(
            builder.post(multipartBody).build()
        ).execute()

        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw ApiException(
                statusCode = response.code,
                responseBody = responseBody
            )
        }

        @Suppress("UNCHECKED_CAST")
        return json.decodeFromString<UploadImageResponse>(
            if (responseBody.isBlank()) "{}" else responseBody
        ) as T
    }


    override fun uploadRegulatoryDocumentRaw(
        e164: String,
        requirementName: String,
        documentType: String,
        attributesJson: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray
    ): String {
        val allowedMimeTypes =
            setOf(
                "image/jpeg",
                "image/png",
                "application/pdf"
            )

        require(mimeType in allowedMimeTypes) {
            "Unsupported regulatory document MIME type."
        }

        require(bytes.size <= 5 * 1024 * 1024) {
            "Regulatory document exceeds the 5 MB limit."
        }

        val token =
            tokenStorage.read()
                ?: throw Exception("Unauthorized")

        val url =
            "${Environment.API_BASE_URL}/numbers/regulatory/documents"

        val textMediaType =
            "text/plain; charset=utf-8".toMediaType()

        val fileBody =
            bytes.toRequestBody(mimeType.toMediaType())

        val multipartBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "e164",
                    null,
                    e164.toRequestBody(textMediaType)
                )
                .addFormDataPart(
                    "requirementName",
                    null,
                    requirementName.toRequestBody(textMediaType)
                )
                .addFormDataPart(
                    "documentType",
                    null,
                    documentType.toRequestBody(textMediaType)
                )
                .addFormDataPart(
                    "attributes",
                    null,
                    attributesJson.toRequestBody(textMediaType)
                )
                .addFormDataPart(
                    "file",
                    filename,
                    fileBody
                )
                .build()

        val request =
            Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader(
                    "Authorization",
                    "Bearer $token"
                )
                .post(multipartBody)
                .build()

        val response =
            client.newCall(request).execute()

        val responseBody =
            response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw ApiException(
                statusCode = response.code,
                responseBody = responseBody
            )
        }

        return if (responseBody.isBlank()) {
            "{}"
        } else {
            responseBody
        }
    }

}
