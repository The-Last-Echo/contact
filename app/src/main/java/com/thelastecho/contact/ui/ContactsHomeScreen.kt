package com.thelastecho.contact.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thelastecho.contact.data.ContactListEntry
import com.thelastecho.contact.preferences.ThemeMode

@Composable
fun ContactsHomeScreen(
    state: ContactsHomeState,
    hasContactsPermission: Boolean,
    onRequestContactsPermission: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggleFavorite: (ContactListEntry) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onRetry: () -> Unit,
) {
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isSearchOpen) {
        isSearchOpen = false
    }

    if (isSearchOpen) {
        ContactsSearchScreen(
            state = state,
            hasContactsPermission = hasContactsPermission,
            onBack = { isSearchOpen = false },
            onQueryChanged = onQueryChanged,
            onToggleFavorite = onToggleFavorite,
            onRequestContactsPermission = onRequestContactsPermission,
            onRetry = onRetry,
        )
    } else {
        ContactsHomeContent(
            state = state,
            hasContactsPermission = hasContactsPermission,
            onOpenSearch = { isSearchOpen = true },
            onQueryChanged = onQueryChanged,
            onToggleFavorite = onToggleFavorite,
            onThemeModeSelected = onThemeModeSelected,
            onRequestContactsPermission = onRequestContactsPermission,
            onRetry = onRetry,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsHomeContent(
    state: ContactsHomeState,
    hasContactsPermission: Boolean,
    onOpenSearch: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggleFavorite: (ContactListEntry) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onRetry: () -> Unit,
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }
    val favorites = remember(state.contacts, state.metadata) {
        state.contacts.filter { state.metadata[it.lookupKey]?.isFavorite == true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Rechercher")
                    }
                    Box {
                        IconButton(onClick = { themeMenuExpanded = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Apparence")
                        }
                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false },
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label()) },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = state.themeMode == mode,
                                            onClick = null,
                                        )
                                    },
                                    onClick = {
                                        onThemeModeSelected(mode)
                                        themeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            !hasContactsPermission -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                PermissionPrompt(onRequestContactsPermission)
            }
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                MessagePanel("Chargement de vos contacts…")
            }
            state.loadFailed -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                ErrorPanel(onRetry)
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 8.dp,
                    end = 20.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (favorites.isNotEmpty()) {
                    item(key = "favorites") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionTitle("FAVORIS")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                            ) {
                                items(
                                    items = favorites,
                                    key = ContactListEntry::lookupKey,
                                ) { contact ->
                                    FavoriteContact(
                                        displayName = state.displayNameFor(contact),
                                        onClick = {
                                            onQueryChanged(contact.displayName)
                                            onOpenSearch()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "all-contacts-title") {
                    SectionTitle(
                        text = "TOUS LES CONTACTS",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                if (state.contacts.isEmpty()) {
                    item(key = "empty-contacts") {
                        MessagePanel("Aucun contact à afficher.")
                    }
                } else {
                    items(
                        items = state.contacts,
                        key = ContactListEntry::lookupKey,
                        contentType = { "contact" },
                    ) { contact ->
                        ContactRow(
                            contact = contact,
                            displayName = state.displayNameFor(contact),
                            isFavorite = state.metadata[contact.lookupKey]?.isFavorite == true,
                            onToggleFavorite = { onToggleFavorite(contact) },
                        )
                    }
                }

                if (state.updateFailed) {
                    item(key = "update-error") {
                        Text(
                            text = "Impossible d’enregistrer ce changement. Réessayez.",
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsSearchScreen(
    state: ContactsHomeState,
    hasContactsPermission: Boolean,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggleFavorite: (ContactListEntry) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recherche") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                placeholder = { Text("Rechercher un contact") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer la recherche")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )

            when {
                !hasContactsPermission -> PermissionPrompt(onRequestContactsPermission)
                state.isLoading -> MessagePanel("Chargement de vos contacts…")
                state.loadFailed -> ErrorPanel(onRetry)
                state.searchResults.isEmpty() -> MessagePanel(
                    if (state.searchQuery.isBlank()) {
                        "Aucun contact à afficher."
                    } else {
                        "Aucun contact ne correspond à cette recherche."
                    },
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(
                        items = state.searchResults,
                        key = ContactListEntry::lookupKey,
                        contentType = { "contact" },
                    ) { contact ->
                        ContactRow(
                            contact = contact,
                            displayName = state.displayNameFor(contact),
                            isFavorite = state.metadata[contact.lookupKey]?.isFavorite == true,
                            onToggleFavorite = { onToggleFavorite(contact) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun PermissionPrompt(onRequestContactsPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Vos contacts, à votre rythme", style = MaterialTheme.typography.titleLarge)
        Text(
            "Autorisez l’accès pour afficher les contacts du téléphone. " +
                "Ils restent dans leur source d’origine.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRequestContactsPermission) {
            Text("Autoriser l’accès aux contacts")
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactListEntry,
    displayName: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContactAvatar(displayName)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Géré par Contacts Android",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FavoriteContact(
    displayName: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 88.dp, max = 164.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactAvatar(displayName, size = 58.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = displayName,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ContactAvatar(name: String, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.take(1).uppercase() }
            .ifBlank { "?" }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MessagePanel(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(vertical = 24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorPanel(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Impossible de lire les contacts. Vérifiez l’autorisation puis réessayez.",
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry) { Text("Réessayer") }
    }
}

@Composable
private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Selon le système"
    ThemeMode.LIGHT -> "Clair"
    ThemeMode.DARK -> "Sombre"
}

private fun ContactsHomeState.displayNameFor(contact: ContactListEntry): String =
    metadata[contact.lookupKey]
        ?.displayNameOverride
        ?.takeIf(String::isNotBlank)
        ?: metadata[contact.lookupKey]?.nickname?.takeIf(String::isNotBlank)
        ?: contact.displayName
