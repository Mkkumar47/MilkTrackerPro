package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkRecordDao {
    @Query("SELECT * FROM milk_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<MilkRecord>>

    @Query("SELECT * FROM milk_records WHERE date = :date LIMIT 1")
    suspend fun getRecordForDate(date: String): MilkRecord?

    @Query("SELECT * FROM milk_records WHERE date LIKE :yearMonth || '%' ORDER BY date ASC")
    fun getRecordsForMonth(yearMonth: String): Flow<List<MilkRecord>>

    @Query("SELECT * FROM milk_records WHERE date LIKE :year || '%' ORDER BY date ASC")
    fun getRecordsForYear(year: String): Flow<List<MilkRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MilkRecord)

    @Delete
    suspend fun deleteRecord(record: MilkRecord)

    @Query("DELETE FROM milk_records WHERE date = :date")
    suspend fun deleteRecordByDate(date: String)

    @Query("DELETE FROM milk_records")
    suspend fun deleteAllRecords()
}
