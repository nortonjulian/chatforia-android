package com.chatforia.android.numbers

import com.chatforia.android.network.ApiException
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.ApiTransport
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class NumberRegulatoryRepository(
    private val api: ApiTransport,
    private val documentTransport: RegulatoryDocumentTransport? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun initialize(
        e164: String,
        endUserAttributes: Map<String, String>? = null
    ): RegulatoryInitializeResponseDto =
        postAllowingConflict(
            path = "numbers/regulatory/initialize",
            request =
                RegulatoryInitializeRequest(
                    e164 = e164,
                    endUserAttributes = endUserAttributes
                )
        )

    suspend fun status(
        e164: String
    ): RegulatoryStatusResponseDto =
        post(
            path = "numbers/regulatory/status",
            request = RegulatoryStatusRequest(e164)
        )

    suspend fun documentRequirements(
        e164: String,
        requirementName: String,
        documentType: String
    ): RegulatoryDocumentRequirementsResponseDto =
        postAllowingConflict(
            path = "numbers/regulatory/document-requirements",
            request =
                RegulatoryDocumentRequirementsRequest(
                    e164 = e164,
                    requirementName = requirementName,
                    documentType = documentType
                )
        )

    suspend fun uploadDocument(
        e164: String,
        requirementName: String,
        documentType: String,
        attributes: Map<String, String>,
        filename: String,
        mimeType: String,
        bytes: ByteArray
    ): RegulatoryDocumentResponseDto =
        withContext(Dispatchers.IO) {
            val transport =
                documentTransport
                    ?: throw IllegalStateException(
                        "Regulatory document transport is unavailable."
                    )

            val attributesJson =
                json.encodeToString(attributes)

            try {
                val response =
                    transport.uploadRegulatoryDocumentRaw(
                        e164 = e164,
                        requirementName = requirementName,
                        documentType = documentType,
                        attributesJson = attributesJson,
                        filename = filename,
                        mimeType = mimeType,
                        bytes = bytes
                    )

                json.decodeFromString<RegulatoryDocumentResponseDto>(
                    if (response.isBlank()) "{}" else response
                )
            } catch (error: ApiException) {
                if (error.statusCode != 409) {
                    throw error
                }

                runCatching {
                    json.decodeFromString<RegulatoryDocumentResponseDto>(
                        if (error.responseBody.isBlank()) {
                            "{}"
                        } else {
                            error.responseBody
                        }
                    )
                }.getOrElse {
                    throw error
                }
            }
        }

    suspend fun assemble(
        e164: String,
        email: String
    ): RegulatoryAssembleResponseDto =
        postAllowingConflict(
            path = "numbers/regulatory/assemble",
            request =
                RegulatoryAssembleRequest(
                    e164 = e164,
                    email = email
                )
        )

    suspend fun submit(
        e164: String
    ): RegulatorySubmitResponseDto =
        post(
            path = "numbers/regulatory/submit",
            request = RegulatorySubmitRequest(e164)
        )

    fun supportingDocumentRequirements(
        requirements: RegulatoryRequirementsDto?
    ): List<RegulatoryDocumentRequirement> {
        if (requirements == null) {
            return emptyList()
        }

        val results = mutableListOf<RegulatoryDocumentRequirement>()

        requirements.supportingDocument.forEach { group ->
            val entries =
                when (group) {
                    is JsonArray -> group
                    is JsonObject -> JsonArray(listOf(group))
                    else -> JsonArray(emptyList())
                }

            entries.forEach { element ->
                val decoded =
                    runCatching {
                        json.decodeFromJsonElement<
                            RegulatorySupportingDocumentRequirementDto
                        >(element)
                    }.getOrNull()
                        ?: return@forEach

                val name =
                    decoded.requirementName
                        ?.takeIf { it.isNotBlank() }
                        ?: return@forEach

                if (results.none { it.requirementName == name }) {
                    results +=
                        RegulatoryDocumentRequirement(
                            requirementName = name,
                            type = decoded.type,
                            acceptedDocuments = decoded.acceptedDocuments
                        )
                }
            }
        }

        return results
    }

    private suspend inline fun <
        reified Request,
        reified Response
    > post(
        path: String,
        request: Request
    ): Response =
        withContext(Dispatchers.IO) {
            val body =
                json.encodeToString(request)

            val response =
                api.sendRaw(
                    ApiRequest(
                        path = path,
                        method = HttpMethod.POST,
                        bodyJson = body,
                        requiresAuth = true
                    )
                )

            json.decodeFromString<Response>(
                if (response.isBlank()) "{}" else response
            )
        }

    private suspend inline fun <
        reified Request,
        reified Response
    > postAllowingConflict(
        path: String,
        request: Request
    ): Response =
        withContext(Dispatchers.IO) {
            val body =
                json.encodeToString(request)

            try {
                val response =
                    api.sendRaw(
                        ApiRequest(
                            path = path,
                            method = HttpMethod.POST,
                            bodyJson = body,
                            requiresAuth = true
                        )
                    )

                json.decodeFromString<Response>(
                    if (response.isBlank()) "{}" else response
                )
            } catch (error: ApiException) {
                if (error.statusCode != 409) {
                    throw error
                }

                runCatching {
                    json.decodeFromString<Response>(
                        if (error.responseBody.isBlank()) {
                            "{}"
                        } else {
                            error.responseBody
                        }
                    )
                }.getOrElse {
                    throw error
                }
            }
        }
}
