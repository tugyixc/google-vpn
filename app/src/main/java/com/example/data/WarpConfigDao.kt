package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WarpConfigDao {
    @Query("SELECT * FROM warp_configs ORDER BY timestamp DESC")
    fun getAllConfigs(): Flow<List<WarpConfigEntity>>

    @Query("SELECT * FROM warp_configs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestConfig(): Flow<WarpConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: WarpConfigEntity): Long

    @Query("DELETE FROM warp_configs WHERE id = :id")
    suspend fun deleteConfigById(id: Int)

    @Query("DELETE FROM warp_configs")
    suspend fun deleteAllConfigs()
}
