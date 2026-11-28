package com.thelastecho.contact.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ContactRepository(
    private val contacts: ContactProviderDataSource,
    private val metadataDao: () -> ContactMetadataDao,
) {
    suspend fun loadContacts(): List<ContactListEntry> = contacts.loadContacts()

    fun observeMetadata(): Flow<List<ContactMetadataEntity>> = flow {
        val dao = withContext(Dispatchers.IO) { metadataDao() }
        emitAll(dao.observeAll())
    }

    suspend fun setFavorite(lookupKey: String, isFavorite: Boolean) {
        val dao = withContext(Dispatchers.IO) { metadataDao() }
        dao.setFavorite(lookupKey, isFavorite)
    }
}
