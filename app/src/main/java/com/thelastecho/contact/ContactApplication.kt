package com.thelastecho.contact

import android.app.Application
import androidx.room.Room
import com.thelastecho.contact.data.AppDatabase
import com.thelastecho.contact.data.ContactProviderDataSource
import com.thelastecho.contact.data.ContactRepository
import com.thelastecho.contact.data.KeystoreDatabasePassphrase
import com.thelastecho.contact.preferences.UserPreferencesRepository
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class ContactApplication : Application() {
    val contactRepository: ContactRepository by lazy {
        ContactRepository(
            ContactProviderDataSource(contentResolver),
        ) {
            database.contactMetadataDao()
        }
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    private val database: AppDatabase by lazy {
        System.loadLibrary("sqlcipher")
        Logger.setTarget(NoopTarget())

        val passphrase = KeystoreDatabasePassphrase(this).getOrCreate()
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            DATABASE_NAME,
        )
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }

    private companion object {
        const val DATABASE_NAME = "private-contact-data.db"
    }
}
