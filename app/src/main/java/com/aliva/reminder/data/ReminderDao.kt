package com.aliva.reminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY dueDate ASC")
    fun getAllInstallments(): Flow<List<Installment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: Installment): Long

    @Delete
    suspend fun deleteInstallment(installment: Installment)

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallmentById(id: Int): Installment?
}

@Dao
interface ChequeDao {
    @Query("SELECT * FROM cheques ORDER BY dueDate ASC")
    fun getAllCheques(): Flow<List<Cheque>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheque(cheque: Cheque): Long

    @Delete
    suspend fun deleteCheque(cheque: Cheque)

    @Query("SELECT * FROM cheques WHERE id = :id")
    suspend fun getChequeById(id: Int): Cheque?
}
