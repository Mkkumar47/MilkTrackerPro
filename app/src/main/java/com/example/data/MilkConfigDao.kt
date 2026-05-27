package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkConfigDao {
    @Query("SELECT * FROM milk_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<MilkConfig?>

    @Query("SELECT * FROM milk_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): MilkConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: MilkConfig)
}
