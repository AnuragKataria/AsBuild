package com.rbt.survey.data.local.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Entity(tableName = "form_drafts", primaryKeys = ["formId", "fieldId"])
data class FormDraft(
    val formId: Int,
    val fieldId: String,
    val value: String
)

@Dao
interface FormDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: FormDraft)

    @Query("SELECT * FROM form_drafts WHERE formId = :formId")
    suspend fun getDraftsForForm(formId: Int): List<FormDraft>

    @Query("DELETE FROM form_drafts WHERE formId = :formId")
    suspend fun clearDraftsForForm(formId: Int)
}

@Database(entities = [FormDraft::class, OfflineSubmission::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun formDraftDao(): FormDraftDao
    abstract fun offlineSubmissionDao(): OfflineSubmissionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `offline_submissions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `formId` INTEGER NOT NULL, `blockCode` TEXT, `gp` TEXT, `submissionData` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                )
            }
        }

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "survey_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
