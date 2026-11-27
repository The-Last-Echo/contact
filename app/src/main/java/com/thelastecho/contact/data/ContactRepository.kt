package com.thelastecho.contact.data

import kotlinx.coroutines.flow.Flow

class ContactRepository(
    private val contacts: ContactProviderDataSource,
    private val metadata: ContactMetadataDao,
) {
    suspend fun loadContacts(): List<ContactListEntry> = contacts.loadContacts()

    fun observeMetadata(): Flow<List<ContactMetadataEntity>> = metadata.observeAll()

    suspend fun setFavorite(lookupKey: String, isFavorite: Boolean) =
        metadata.setFavorite(lookupKey, isFavorite)
}
