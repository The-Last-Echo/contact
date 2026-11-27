package com.thelastecho.contact.ui

import com.thelastecho.contact.data.ContactListEntry
import com.thelastecho.contact.data.ContactMetadataEntity
import java.util.Locale

fun filterContacts(
    contacts: List<ContactListEntry>,
    metadata: Map<String, ContactMetadataEntity>,
    query: String,
): List<ContactListEntry> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    if (normalizedQuery.isEmpty()) return contacts

    return contacts.filter { contact ->
        val personal = metadata[contact.lookupKey]
        sequenceOf(
            contact.displayName,
            personal?.nickname,
            personal?.displayNameOverride,
        ).filterNotNull().any { value ->
            value.lowercase(Locale.ROOT).contains(normalizedQuery)
        }
    }
}
