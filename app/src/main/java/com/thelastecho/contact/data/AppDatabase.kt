package com.thelastecho.contact.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "contact_metadata")
data class ContactMetadataEntity(
    @PrimaryKey val lookupKey: String,
    val nickname: String = "",
    val displayNameOverride: String? = null,
    val privateNote: String? = null,
    val isFavorite: Boolean = false,
)

@Dao
interface ContactMetadataDao {
    @Query("SELECT * FROM contact_metadata")
    fun observeAll(): Flow<List<ContactMetadataEntity>>

    @Query("SELECT * FROM contact_metadata WHERE lookupKey = :lookupKey LIMIT 1")
    suspend fun findByLookupKey(lookupKey: String): ContactMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: ContactMetadataEntity)

    @Transaction
    suspend fun setFavorite(lookupKey: String, isFavorite: Boolean) {
        val existing = findByLookupKey(lookupKey)
        upsert(
            (existing ?: ContactMetadataEntity(lookupKey = lookupKey))
                .copy(isFavorite = isFavorite),
        )
    }
}

@Database(
    entities = [ContactMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactMetadataDao(): ContactMetadataDao
}
