package com.rbt.survey.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "offline_submissions")
data class OfflineSubmission(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val formId: Int,
    val blockCode: String?,
    val gp: String?,
    val submissionData: String, // JSON representation of the form data
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface OfflineSubmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(submission: OfflineSubmission)

    @Query("SELECT * FROM offline_submissions WHERE formId = :formId ORDER BY timestamp DESC")
    suspend fun getSubmissionsForForm(formId: Int): List<OfflineSubmission>

    @Query("SELECT * FROM offline_submissions WHERE id = :id")
    suspend fun getSubmissionById(id: Int): OfflineSubmission?

    @Query("DELETE FROM offline_submissions WHERE id = :id")
    suspend fun deleteSubmission(id: Int)
}
