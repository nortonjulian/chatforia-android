package com.chatforia.android.contacts

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.net.URLEncoder

class ContactsRepository(
    private val apiClient: ApiClient
) {
    suspend fun fetchContacts(
        query: String? = null,
        limit: Int = 50,
        cursor: Int? = null
    ): ContactsResponse {
        val params = mutableListOf("limit=$limit")

        if (cursor != null) {
            params.add("cursor=$cursor")
        }

        val trimmedQuery = query?.trim().orEmpty()
        if (trimmedQuery.isNotBlank()) {
            val encoded =
                URLEncoder.encode(
                    trimmedQuery,
                    "UTF-8"
                )

            params.add("q=$encoded")
        }

        val path =
            "contacts" +
                    if (params.isNotEmpty()) {
                        "?${params.joinToString("&")}"
                    } else {
                        ""
                    }

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = path,
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun saveUserContact(
        userId: Int,
        alias: String? = null,
        favorite: Boolean = false
    ): ContactDto {
        val bodyJson =
            apiClient.json.encodeToString(
                SaveUserContactRequest(
                    userId = userId,
                    alias = alias?.trim()?.ifBlank { null },
                    favorite = favorite
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "contacts",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun lookupUser(
        username: String
    ): UserLookupResponse {
        val encoded =
            URLEncoder.encode(
                username.trim(),
                "UTF-8"
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "users/lookup?username=$encoded",
                    method = HttpMethod.GET,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun importContacts(
        phoneContacts: List<PhoneContactDto>
    ): ImportContactsResponse {
        val items =
            phoneContacts.map {
                ImportContactItem(
                    externalPhone = it.phone,
                    externalName = it.name,
                    alias = it.name,
                    favorite = false
                )
            }

        val bodyJson =
            apiClient.json.encodeToString(
                ImportContactsRequest(
                    items = items
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "contacts/import",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun saveExternalContact(
        phone: String,
        externalName: String? = null,
        alias: String? = null,
        favorite: Boolean = false
    ): ContactDto {
        val bodyJson =
            apiClient.json.encodeToString(
                SaveExternalContactRequest(
                    externalPhone = phone.trim(),
                    externalName = externalName?.trim()?.ifBlank { null },
                    alias = alias?.trim()?.ifBlank { null },
                    favorite = favorite
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "contacts",
                    method = HttpMethod.POST,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun updateContact(
        contact: ContactDto,
        alias: String? = null,
        externalName: String? = null,
        favorite: Boolean? = null
    ): ContactDto {
        val userId =
            contact.user?.id ?: contact.userId

        val externalPhone =
            contact.externalPhone

        val bodyJson =
            apiClient.json.encodeToString(
                UpdateContactRequest(
                    userId = userId,
                    externalPhone = if (userId == null) externalPhone else null,
                    alias = alias?.trim()?.ifBlank { null },
                    externalName = externalName?.trim()?.ifBlank { null },
                    favorite = favorite
                )
            )

        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "contacts",
                    method = HttpMethod.PATCH,
                    bodyJson = bodyJson,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun deleteContact(
        contactId: Int
    ) {
        withContext(Dispatchers.IO) {
            apiClient.sendRaw(
                ApiRequest(
                    path = "contacts/$contactId",
                    method = HttpMethod.DELETE,
                    requiresAuth = true
                )
            )
        }
    }

    suspend fun openDirectChat(
        userId: Int
    ): DirectChatRoomResponse {
        return withContext(Dispatchers.IO) {
            apiClient.send(
                ApiRequest(
                    path = "chatrooms/direct/$userId",
                    method = HttpMethod.POST,
                    requiresAuth = true
                )
            )
        }
    }
}

@Serializable
private data class SaveUserContactRequest(
    val userId: Int,
    val alias: String? = null,
    val favorite: Boolean = false
)

@Serializable
private data class SaveExternalContactRequest(
    val externalPhone: String,
    val externalName: String? = null,
    val alias: String? = null,
    val favorite: Boolean = false
)

@Serializable
private data class UpdateContactRequest(
    val userId: Int? = null,
    val externalPhone: String? = null,
    val alias: String? = null,
    val externalName: String? = null,
    val favorite: Boolean? = null
)