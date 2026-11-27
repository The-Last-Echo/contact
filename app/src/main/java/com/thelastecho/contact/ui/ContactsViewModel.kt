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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactsHomeState(
    val contacts: List<ContactListEntry> = emptyList(),
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

    val uiState = combine(
        combine(contacts, contactRepository.observeMetadata()) { contactEntries, metadataEntries ->
            contactEntries to metadataEntries
        },
        combine(searchQuery, userPreferencesRepository.themeMode) { query, themeMode ->
            query to themeMode
        },
        combine(isLoading, loadFailed, updateFailed) { loading, loadError, updateError ->
            Triple(loading, loadError, updateError)
        },
    ) { (contactEntries, metadataEntries), (query, themeMode), status ->
        val metadata = metadataEntries
            .associateBy(ContactMetadataEntity::lookupKey)
        ContactsHomeState(
            contacts = filterContacts(
                contacts = contactEntries,
                metadata = metadata,
                query = query,
            ),
            metadata = metadata,
            searchQuery = query,
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
