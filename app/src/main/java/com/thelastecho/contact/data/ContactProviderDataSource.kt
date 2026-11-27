package com.thelastecho.contact.data

import android.content.ContentResolver
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class ContactListEntry(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
)

class ContactProviderDataSource(
    private val contentResolver: ContentResolver,
) {
    suspend fun loadContacts(): List<ContactListEntry> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        )
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        ) ?: throw IOException("The contacts provider returned no cursor.")

        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val lookupKeyColumn =
                it.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val displayNameColumn =
                it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            buildList {
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    add(
                        ContactListEntry(
                            id = id,
                            lookupKey = it.getString(lookupKeyColumn) ?: id.toString(),
                            displayName = it.getString(displayNameColumn)
                                ?.takeIf(String::isNotBlank)
                                ?: "Sans nom",
                        ),
                    )
                }
            }
        }
    }
}
