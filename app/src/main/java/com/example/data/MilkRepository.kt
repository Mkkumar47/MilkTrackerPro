package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MilkRepository(private val db: AppDatabase) {
    private val recordDao = db.milkRecordDao
    private val configDao = db.milkConfigDao

    val allRecords: Flow<List<MilkRecord>> = recordDao.getAllRecords()
    val configFlow: Flow<MilkConfig> = configDao.getConfigFlow().map { it ?: MilkConfig() }

    suspend fun getRecordForDate(date: String): MilkRecord? = recordDao.getRecordForDate(date)

    fun getRecordsForMonth(yearMonth: String): Flow<List<MilkRecord>> = recordDao.getRecordsForMonth(yearMonth)

    fun getRecordsForYear(year: String): Flow<List<MilkRecord>> = recordDao.getRecordsForYear(year)

    suspend fun insertRecord(record: MilkRecord) {
        recordDao.insertRecord(record)
    }

    suspend fun deleteRecordByDate(date: String) {
        recordDao.deleteRecordByDate(date)
    }

    suspend fun getConfig(): MilkConfig = configDao.getConfig() ?: MilkConfig()

    suspend fun saveConfig(config: MilkConfig) {
        configDao.saveConfig(config)
    }

    suspend fun clearAllData() {
        db.clearAllTables()
        // Save default config back
        configDao.saveConfig(MilkConfig())
    }
    
    suspend fun importBackupRecords(records: List<MilkRecord>) {
        for (rec in records) {
            recordDao.insertRecord(rec)
        }
    }
}
