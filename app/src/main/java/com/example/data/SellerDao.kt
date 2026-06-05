package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SellerDao {
    @Query("SELECT * FROM sellers ORDER BY name ASC")
    fun getAllSellers(): Flow<List<Seller>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeller(seller: Seller): Long

    @Delete
    suspend fun deleteSeller(seller: Seller)

    @Query("SELECT * FROM sellers WHERE id = :id LIMIT 1")
    suspend fun getSellerById(id: Int): Seller?

    @Query("DELETE FROM sellers")
    suspend fun deleteAllSellers()
}
