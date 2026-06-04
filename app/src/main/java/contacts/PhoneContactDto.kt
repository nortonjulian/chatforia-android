package com.chatforia.android.contacts

import kotlinx.serialization.Serializable

data class PhoneContactDto(
    val name: String,
    val phone: String
)

@Serializable
data class ImportContactsRequest(
    val items: List<ImportContactItem>
)

@Serializable
data class ImportContactItem(
    val externalPhone: String,
    val externalName: String? = null,
    val alias: String? = null,
    val favorite: Boolean = false
)

@Serializable
data class ImportContactsResponse(
    val ok: Boolean = false,
    val importedCount: Int = 0,
    val items: List<ContactDto> = emptyList()
)