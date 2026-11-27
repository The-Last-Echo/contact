package com.thelastecho.contact.ui

import com.thelastecho.contact.data.ContactListEntry
import com.thelastecho.contact.data.ContactMetadataEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactFilterTest {
    private val contacts = listOf(
        ContactListEntry(1, "lookup-1", "Alice Martin"),
        ContactListEntry(2, "lookup-2", "Noah Bernard"),
    )

    @Test
    fun emptySearchKeepsAllContacts() {
        assertEquals(contacts, filterContacts(contacts, emptyMap(), "  "))
    }

    @Test
    fun searchMatchesPersonalNicknameWithoutChangingProviderData() {
        val metadata = mapOf(
            "lookup-2" to ContactMetadataEntity(
                lookupKey = "lookup-2",
                nickname = "Nono",
            ),
        )

        assertEquals(listOf(contacts[1]), filterContacts(contacts, metadata, " NONO "))
        assertEquals("Noah Bernard", contacts[1].displayName)
    }
}
