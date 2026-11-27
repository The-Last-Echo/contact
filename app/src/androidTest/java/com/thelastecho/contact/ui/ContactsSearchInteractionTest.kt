package com.thelastecho.contact.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thelastecho.contact.data.ContactListEntry
import com.thelastecho.contact.data.ContactMetadataEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsSearchInteractionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun delayedSearchStateDoesNotReplaceNewerTypedText() {
        val state = mutableStateOf(ContactsHomeState())
        val emittedQueries = mutableListOf<String>()
        showScreen(state) { query -> emittedQueries += query }

        composeRule.onNodeWithContentDescription("Rechercher").performClick()
        val searchField = composeRule.onNode(hasSetTextAction())
        for (character in "alice") {
            searchField.performTextInput(character.toString())
        }

        assertEquals("alice", emittedQueries.last())
        composeRule.runOnUiThread {
            state.value = state.value.copy(searchQuery = emittedQueries.first())
        }

        searchField.assertTextEquals("alice")
    }

    @Test
    fun immediateSearchStateEchoKeepsRapidInput() {
        val state = mutableStateOf(ContactsHomeState())
        showScreen(state) { query ->
            state.value = state.value.copy(searchQuery = query)
        }

        composeRule.onNodeWithContentDescription("Rechercher").performClick()
        val searchField = composeRule.onNode(hasSetTextAction())
        for (character in "alice") {
            searchField.performTextInput(character.toString())
        }

        searchField.assertTextEquals("alice")
    }

    @Test
    fun openingSearchFocusesTheInput() {
        showScreen(mutableStateOf(ContactsHomeState())) {}

        composeRule.onNodeWithContentDescription("Rechercher").performClick()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun closingAndReopeningSearchStartsWithAnEmptyQuery() {
        val state = mutableStateOf(ContactsHomeState())
        showScreen(state) { query ->
            state.value = state.value.copy(searchQuery = query)
        }

        composeRule.onNodeWithContentDescription("Rechercher").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("sam")
        composeRule.onNodeWithContentDescription("Fermer la recherche").performClick()
        composeRule.onNodeWithContentDescription("Rechercher").performClick()

        composeRule.onAllNodesWithText("sam").assertCountEquals(0)
    }

    @Test
    fun androidBackClosesSearchAndClearsItsQuery() {
        val state = mutableStateOf(ContactsHomeState())
        showScreen(state) { query ->
            state.value = state.value.copy(searchQuery = query)
        }

        composeRule.onNodeWithContentDescription("Rechercher").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("sam")
        closeSoftKeyboard()
        pressBack()

        composeRule.onNodeWithContentDescription("Rechercher").assertExists()
        composeRule.onAllNodesWithText("sam").assertCountEquals(0)
    }

    @Test
    fun favoriteTilesKeepTheSameWidthForShortAndLongNames() {
        val longName = "Alexandra Verylongsurname"
        val contacts = listOf(
            ContactListEntry(1, "short", "Ada"),
            ContactListEntry(2, "long", longName),
        )
        val state = mutableStateOf(
            ContactsHomeState(
                contacts = contacts,
                searchResults = contacts,
                metadata = contacts.associate { contact ->
                    contact.lookupKey to ContactMetadataEntity(
                        lookupKey = contact.lookupKey,
                        isFavorite = true,
                    )
                },
            ),
        )
        showScreen(state) {}

        val shortTile = composeRule.onNodeWithTag("favorite-short").getUnclippedBoundsInRoot()
        val longTile = composeRule.onNodeWithTag("favorite-long").getUnclippedBoundsInRoot()

        assertEquals(
            (shortTile.right - shortTile.left).value,
            (longTile.right - longTile.left).value,
            0.01f,
        )

        composeRule.onNodeWithTag("favorite-long").performTouchInput { longClick() }
        composeRule.onAllNodesWithText(longName).assertCountEquals(3)
    }

    private fun showScreen(
        state: MutableState<ContactsHomeState>,
        onQueryChanged: (String) -> Unit,
    ) {
        composeRule.setContent {
            MaterialTheme {
                ContactsHomeScreen(
                    state = state.value,
                    hasContactsPermission = true,
                    onRequestContactsPermission = {},
                    onQueryChanged = onQueryChanged,
                    onToggleFavorite = {},
                    onThemeModeSelected = {},
                    onRetry = {},
                )
            }
        }
    }
}
