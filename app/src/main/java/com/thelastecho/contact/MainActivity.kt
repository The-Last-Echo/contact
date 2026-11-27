package com.thelastecho.contact

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thelastecho.contact.ui.ContactsHomeScreen
import com.thelastecho.contact.ui.ContactsViewModel
import com.thelastecho.contact.ui.theme.ContactTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ContactApplication
        setContent {
            val viewModel: ContactsViewModel = viewModel(
                factory = ContactsViewModel.Factory(
                    app.contactRepository,
                    app.userPreferencesRepository,
                ),
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            var hasContactsPermission by remember {
                mutableStateOf(hasContactsPermission())
            }
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                hasContactsPermission = granted
            }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasContactsPermission = hasContactsPermission()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(hasContactsPermission) {
                if (hasContactsPermission) viewModel.loadContacts()
            }

            ContactTheme(themeMode = uiState.themeMode) {
                ContactsHomeScreen(
                    state = uiState,
                    hasContactsPermission = hasContactsPermission,
                    onRequestContactsPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    onQueryChanged = viewModel::setSearchQuery,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onThemeModeSelected = viewModel::setThemeMode,
                    onRetry = viewModel::loadContacts,
                )
            }
        }
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
}
