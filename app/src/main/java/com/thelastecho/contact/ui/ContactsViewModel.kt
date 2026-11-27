package com.thelastecho.contact.ui

import android.database.SQLException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thelastecho.contact.data.ContactListEntry
import com.thelastecho.contact.data.ContactMetadataEntity
import com.thelastecho.contact.data.ContactRepository
import com.thelastecho.contact.preferences.ThemeMode
import com.thelastecho.contact.preferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactsHomeState(
    val contacts: List<ContactListEntry> = emptyList(),
    val searchResults: List<ContactListEntry> = emptyList(),
    val metadata: Map<String, ContactMetadataEntity> = emptyMap(),
    val searchQuery: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val updateFailed: Boolean = false,
)

class ContactsViewModel(
    private val contactRepository: ContactRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val contacts = MutableStateFlow<List<ContactListEntry>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private val loadFailed = MutableStateFlow(false)
    private val updateFailed = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val contactContent = combine(
        combine(contacts, contactRepository.observeMetadata()) { contactEntries, metadataEntries ->
            contactEntries to metadataEntries
        },
        searchQuery,
    ) { (contactEntries, metadataEntries), query ->
        ContactContent(
            contacts = contactEntries,
            metadata = metadataEntries.associateBy(ContactMetadataEntity::lookupKey),
            query = query,
        )
    }.mapLatest { content ->
        withContext(Dispatchers.Default) {
            content.copy(
                searchResults = filterContacts(
                    contacts = content.contacts,
                    metadata = content.metadata,
                    query = content.query,
                ),
            )
        }
    }

    val uiState = combine(
        contactContent,
        userPreferencesRepository.themeMode,
        combine(isLoading, loadFailed, updateFailed) { loading, loadError, updateError ->
            Triple(loading, loadError, updateError)
        },
    ) { content, themeMode, status ->
        ContactsHomeState(
            contacts = content.contacts,
            searchResults = content.searchResults,
            metadata = content.metadata,
            searchQuery = content.query,
            themeMode = themeMode,
            isLoading = status.first,
            loadFailed = status.second,
            updateFailed = status.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactsHomeState())

    fun loadContacts() {
        viewModelScope.launch {
            isLoading.value = true
            loadFailed.value = false
            try {
                contacts.value = contactRepository.loadContacts()
            } catch (_: SecurityException) {
                loadFailed.value = true
            } catch (_: java.io.IOException) {
                loadFailed.value = true
            } finally {
                isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleFavorite(contact: ContactListEntry) {
        viewModelScope.launch {
            updateFailed.value = false
            try {
                contactRepository.setFavorite(
                    lookupKey = contact.lookupKey,
                    isFavorite = uiState.value.metadata[contact.lookupKey]?.isFavorite != true,
                )
            } catch (_: SQLException) {
                updateFailed.value = true
            } catch (_: java.io.IOException) {
                updateFailed.value = true
            }
        }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(themeMode)
        }
    }

    private data class ContactContent(
        val contacts: List<ContactListEntry>,
        val metadata: Map<String, ContactMetadataEntity>,
        val query: String,
        val searchResults: List<ContactListEntry> = emptyList(),
    )

    class Factory(
        private val contactRepository: ContactRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ContactsViewModel::class.java))
            return ContactsViewModel(contactRepository, userPreferencesRepository) as T
        }
    }
}
