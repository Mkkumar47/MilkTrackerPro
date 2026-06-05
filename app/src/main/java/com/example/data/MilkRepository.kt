package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MilkRepository(private val db: AppDatabase) {
    private val recordDao = db.milkRecordDao
    private val configDao = db.milkConfigDao
    private val sellerDao = db.sellerDao
    private val paymentDao = db.paymentDao

    val allRecords: Flow<List<MilkRecord>> = recordDao.getAllRecords()
    val configFlow: Flow<MilkConfig> = configDao.getConfigFlow().map { it ?: MilkConfig() }
    val allSellers: Flow<List<Seller>> = sellerDao.getAllSellers()
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments()

    suspend fun insertSeller(seller: Seller): Long = sellerDao.insertSeller(seller)
    suspend fun deleteSeller(seller: Seller) = sellerDao.deleteSeller(seller)
    suspend fun getSellerById(id: Int): Seller? = sellerDao.getSellerById(id)
    suspend fun deleteAllSellers() = sellerDao.deleteAllSellers()

    suspend fun insertPayment(payment: Payment) = paymentDao.insertPayment(payment)
    suspend fun deletePayment(payment: Payment) = paymentDao.deletePayment(payment)
    suspend fun deletePaymentById(id: Int) = paymentDao.deletePaymentById(id)
    suspend fun deleteAllPayments() = paymentDao.deleteAllPayments()

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

    suspend fun deleteAllRecords() {
        recordDao.deleteAllRecords()
    }
    
    suspend fun importBackupRecords(records: List<MilkRecord>) {
        for (rec in records) {
            recordDao.insertRecord(rec)
        }
    }
}
