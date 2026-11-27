package com.thelastecho.contact.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var searchDraft by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun openSearch(initialQuery: String = "") {
        searchDraft = initialQuery
        onQueryChanged(initialQuery)
        isSearchOpen = true
    }

    fun closeSearch() {
        isSearchOpen = false
        searchDraft = ""
        onQueryChanged("")
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BackHandler(enabled = isSearchOpen, onBack = ::closeSearch)

    ContactsHomeContent(
        state = state,
        hasContactsPermission = hasContactsPermission,
        isSearchOpen = isSearchOpen,
        searchQuery = searchDraft,
        focusRequester = focusRequester,
        onOpenSearch = ::openSearch,
        onCloseSearch = ::closeSearch,
        onQueryChanged = { query ->
            searchDraft = query
            onQueryChanged(query)
        },
        onToggleFavorite = onToggleFavorite,
        onThemeModeSelected = onThemeModeSelected,
        onRequestContactsPermission = onRequestContactsPermission,
        onRetry = onRetry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsHomeContent(
    state: ContactsHomeState,
    hasContactsPermission: Boolean,
    isSearchOpen: Boolean,
    searchQuery: String,
    focusRequester: FocusRequester,
    onOpenSearch: (String) -> Unit,
    onCloseSearch: () -> Unit,
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
    val displayedContacts = if (isSearchOpen) state.searchResults else state.contacts

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Crossfade(
                        targetState = isSearchOpen,
                        animationSpec = tween(durationMillis = 160),
                        label = "contacts-title-search",
                    ) { searching ->
                        if (searching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onQueryChanged,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .padding(vertical = 4.dp),
                                placeholder = { Text("Rechercher un contact") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onQueryChanged("") }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Effacer la recherche",
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                            )
                        } else {
                            Text("Contacts", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                actions = {
                    if (isSearchOpen) {
                        IconButton(onClick = onCloseSearch) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer la recherche")
                        }
                    } else {
                        IconButton(onClick = { onOpenSearch("") }) {
                            Icon(Icons.Default.Search, contentDescription = "Rechercher")
                        }
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
                if (!isSearchOpen && favorites.isNotEmpty()) {
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
                                        paletteSeed = contact.lookupKey,
                                        onClick = {
                                            onOpenSearch(contact.displayName)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = if (isSearchOpen) "search-results-title" else "all-contacts-title") {
                    SectionTitle(
                        text = if (isSearchOpen) "RÉSULTATS" else "TOUS LES CONTACTS",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                if (displayedContacts.isEmpty()) {
                    item(key = "empty-contacts") {
                        MessagePanel(
                            if (isSearchOpen && searchQuery.isNotBlank()) {
                                "Aucun contact ne correspond à cette recherche."
                            } else {
                                "Aucun contact à afficher."
                            },
                        )
                    }
                } else {
                    items(
                        items = displayedContacts,
                        key = ContactListEntry::lookupKey,
                        contentType = { "contact" },
                    ) { contact ->
                        ContactRow(
                            contact = contact,
                            displayName = state.displayNameFor(contact),
                            paletteSeed = contact.lookupKey,
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
    paletteSeed: String,
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
            ContactAvatar(displayName, paletteSeed = paletteSeed)
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
@OptIn(ExperimentalMaterial3Api::class)
private fun FavoriteContact(
    displayName: String,
    paletteSeed: String,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(displayName) } },
        state = rememberTooltipState(),
    ) {
        Column(
            modifier = Modifier
                .width(84.dp)
                .testTag("favorite-$paletteSeed")
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ContactAvatar(displayName, size = 56.dp, paletteSeed = paletteSeed)
            Spacer(Modifier.height(6.dp))
            Text(
                text = displayName,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ContactAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    paletteSeed: String = name,
) {
    val initials = remember(name) {
        name.trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.take(1).uppercase() }
            .ifBlank { "?" }
    }
    val colorScheme = MaterialTheme.colorScheme
    val avatarColors = when (Math.floorMod(paletteSeed.hashCode(), 5)) {
        0 -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        1 -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        2 -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        3 -> colorScheme.errorContainer to colorScheme.onErrorContainer
        else -> colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColors.first),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = avatarColors.second,
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
