package com.willykez.gitsync.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.willykez.gitsync.data.db.entity.SshKeyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SshKeyDao {

    @Query("SELECT * FROM ssh_keys ORDER BY name ASC")
    fun getAllFlow(): Flow<List<SshKeyEntity>>

    @Query("SELECT * FROM ssh_keys ORDER BY name ASC")
    suspend fun getAll(): List<SshKeyEntity>

    @Query("SELECT * FROM ssh_keys WHERE id = :id")
    suspend fun getById(id: Long): SshKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(key: SshKeyEntity): Long

    @Update
    suspend fun update(key: SshKeyEntity)

    @Delete
    suspend fun delete(key: SshKeyEntity)
}
