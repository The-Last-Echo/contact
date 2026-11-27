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
    lateinit var contactRepository: ContactRepository
        private set
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        Logger.setTarget(NoopTarget())

        val passphrase = KeystoreDatabasePassphrase(this).getOrCreate()
        val database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            DATABASE_NAME,
        )
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()

        contactRepository = ContactRepository(
            ContactProviderDataSource(contentResolver),
            database.contactMetadataDao(),
        )
        userPreferencesRepository = UserPreferencesRepository(this)
    }

    private companion object {
        const val DATABASE_NAME = "private-contact-data.db"
    }
}
