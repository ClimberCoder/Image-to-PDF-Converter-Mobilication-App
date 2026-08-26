package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PdfDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_documents ORDER BY createdAtTimestamp DESC")
    fun getAllPdfs(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getPdfById(id: Long): PdfDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfDocumentEntity): Long

    @Update
    suspend fun updatePdf(pdf: PdfDocumentEntity)

    @Delete
    suspend fun deletePdf(pdf: PdfDocumentEntity)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deletePdfById(id: Long)

    @Query("SELECT COUNT(*) FROM pdf_documents")
    fun getTotalPdfCount(): Flow<Int>

    @Query("SELECT SUM(pageCount) FROM pdf_documents")
    fun getTotalPagesConverted(): Flow<Int?>

    @Query("SELECT SUM(fileSizeBytes) FROM pdf_documents")
    fun getTotalBytesStored(): Flow<Long?>

    @Query("SELECT * FROM pdf_documents ORDER BY createdAtTimestamp DESC LIMIT :limit")
    fun getRecentPdfs(limit: Int): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE cloudSyncStatus = 'SYNCED' ORDER BY cloudUploadTimestamp DESC")
    fun getSyncedCloudPdfs(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE cloudSyncStatus IN ('UPLOADING', 'FAILED')")
    fun getPendingCloudPdfs(): Flow<List<PdfDocumentEntity>>
}
