package com.chatforia.android.contacts

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneContactsReader(
    private val context: Context
) {
    suspend fun readContacts(): List<PhoneContactDto> =
        withContext(Dispatchers.IO) {
            val contacts = mutableListOf<PhoneContactDto>()

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIndex =
                    it.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )

                val numberIndex =
                    it.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex).orEmpty().trim()
                    val phone = it.getString(numberIndex).orEmpty().trim()

                    if (phone.isNotBlank()) {
                        contacts.add(
                            PhoneContactDto(
                                name = name.ifBlank { phone },
                                phone = phone
                            )
                        )
                    }
                }
            }

            contacts.distinctBy {
                it.phone.replace(Regex("[^\\d+]"), "")
            }
        }
}