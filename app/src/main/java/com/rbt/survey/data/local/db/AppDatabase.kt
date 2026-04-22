package com.rbt.survey.data.local.db

import androidx.room.*

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

@Database(entities = [FormDraft::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun formDraftDao(): FormDraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "survey_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
