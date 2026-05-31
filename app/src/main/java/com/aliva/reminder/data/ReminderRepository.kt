package com.aliva.reminder.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val installmentDao: InstallmentDao,
    private val chequeDao: ChequeDao
) {
    val allInstallments: Flow<List<Installment>> = installmentDao.getAllInstallments()
    val allCheques: Flow<List<Cheque>> = chequeDao.getAllCheques()

    suspend fun insertInstallment(installment: Installment): Long {
        return installmentDao.insertInstallment(installment)
    }

    suspend fun deleteInstallment(installment: Installment) {
        installmentDao.deleteInstallment(installment)
    }

    suspend fun getInstallmentById(id: Int): Installment? {
        return installmentDao.getInstallmentById(id)
    }

    suspend fun insertCheque(cheque: Cheque): Long {
        return chequeDao.insertCheque(cheque)
    }

    suspend fun deleteCheque(cheque: Cheque) {
        chequeDao.deleteCheque(cheque)
    }

    suspend fun getChequeById(id: Int): Cheque? {
        return chequeDao.getChequeById(id)
    }
}
