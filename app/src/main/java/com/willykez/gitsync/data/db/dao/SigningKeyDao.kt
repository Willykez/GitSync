package com.willykez.gitsync.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.willykez.gitsync.data.db.entity.SigningKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SigningKeyDao {

    @Query("SELECT * FROM signing_keys ORDER BY name ASC")
    fun getAllFlow(): Flow<List<SigningKeyEntity>>

    @Query("SELECT * FROM signing_keys ORDER BY name ASC")
    suspend fun getAll(): List<SigningKeyEntity>

    @Query("SELECT * FROM signing_keys WHERE id = :id")
    suspend fun getById(id: Long): SigningKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: SigningKeyEntity): Long

    @Update
    suspend fun update(key: SigningKeyEntity)

    @Delete
    suspend fun delete(key: SigningKeyEntity)
}
