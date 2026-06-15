package com.rbt.survey.data.local.db

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Entity(tableName = "form_drafts", primaryKeys = ["formId", "fieldId", "gp"])
data class FormDraft(
    val formId: Int,
    val fieldId: String,
    val gp: String,
    val value: String
)

@Entity(tableName = "cached_forms")
data class CachedForm(
    @PrimaryKey val formId: Int,
    val formName: String,
    val formCode: String,
    val description: String?,
    val isActive: Boolean
)

@Entity(tableName = "cached_form_details", primaryKeys = ["formId", "blockCode"])
data class CachedFormDetail(
    val formId: Int,
    val blockCode: String, // Use empty string for no block
    val detailJson: String // JSON representation of FormDetailData
)

@Entity(tableName = "pending_file_uploads")
data class PendingFileUpload(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val formId: Int,
    val fieldId: String,
    val uploadedBy: String,
    val uriString: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "location_queue")
data class LocationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Int,
    val userName: String,
    val email: String,

    val lat: Double,
    val lng: Double,

    val accuracy: Double?,
    val altitude: Double?,
    val speed: Double?,
    val heading: Double?,

    val deviceType: String,
    val recordedAt: String
)

@Dao
interface LocationDao {

    @Insert
    suspend fun insert(location: LocationEntity)

    @Query("SELECT * FROM location_queue ORDER BY id ASC")
    suspend fun getAllLocations(): List<LocationEntity>

    @Delete
    suspend fun delete(location: LocationEntity)
}

@Dao
interface FormDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: FormDraft)

    @Query("SELECT * FROM form_drafts WHERE formId = :formId AND gp = :gp")
    suspend fun getDraftsForForm(formId: Int, gp: String): List<FormDraft>

    @Query("DELETE FROM form_drafts WHERE formId = :formId AND gp = :gp")
    suspend fun clearDraftsForForm(formId: Int, gp: String)
}

@Dao
interface CachedFormDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForms(forms: List<CachedForm>)

    @Query("SELECT * FROM cached_forms")
    suspend fun getAllForms(): List<CachedForm>
}

@Dao
interface CachedFormDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormDetail(detail: CachedFormDetail)

    @Query("SELECT * FROM cached_form_details WHERE formId = :formId AND blockCode = :blockCode")
    suspend fun getFormDetail(formId: Int, blockCode: String): CachedFormDetail?

    @Query("SELECT blockCode FROM cached_form_details WHERE formId = :formId")
    suspend fun getDownloadedBlockCodes(formId: Int): List<String>
}

@Dao
interface PendingFileUploadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: PendingFileUpload)

    @Query("SELECT * FROM pending_file_uploads ORDER BY timestamp ASC")
    suspend fun getAllPendingUploads(): List<PendingFileUpload>

    @Query("DELETE FROM pending_file_uploads WHERE id = :id")
    suspend fun delete(id: Int)
}

@Entity(tableName = "cached_block_assignments")
data class CachedBlockAssignment(
    @PrimaryKey val userId: String,
    val assignmentsJson: String // JSON of List<Assignment>
)

@Entity(tableName = "cached_block_summaries", primaryKeys = ["userId", "formId"])
data class CachedBlockSummary(
    val userId: String,
    val formId: Int,
    val summaryJson: String // JSON of List<BlockSummary>
)

@Dao
interface CachedBlockAssignmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: CachedBlockAssignment)

    @Query("SELECT * FROM cached_block_assignments WHERE userId = :userId")
    suspend fun getByUserId(userId: String): CachedBlockAssignment?
}

@Dao
interface CachedBlockSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: CachedBlockSummary)

    @Query("SELECT * FROM cached_block_summaries WHERE userId = :userId AND formId = :formId")
    suspend fun getSummary(userId: String, formId: Int): CachedBlockSummary?
}

@Entity(tableName = "cached_uploaded_submissions", primaryKeys = ["formId", "submissionId"])
data class CachedUploadedSubmission(
    val formId: Int,
    val submissionId: Int,
    val submissionJson: String // JSON of SubmissionItem
)

@Dao
interface CachedUploadedSubmissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(submissions: List<CachedUploadedSubmission>)

    @Query("SELECT * FROM cached_uploaded_submissions WHERE formId = :formId")
    suspend fun getByFormId(formId: Int): List<CachedUploadedSubmission>

    @Query("DELETE FROM cached_uploaded_submissions WHERE formId = :formId")
    suspend fun deleteByFormId(formId: Int)
}

@Database(
    entities = [
        FormDraft::class,
        OfflineSubmission::class,
        CachedForm::class,
        CachedFormDetail::class,
        PendingFileUpload::class,
        CachedBlockAssignment::class,
        CachedBlockSummary::class,
        CachedUploadedSubmission::class,
        LocationEntity::class
    ],
    version = 7
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun formDraftDao(): FormDraftDao
    abstract fun offlineSubmissionDao(): OfflineSubmissionDao
    abstract fun cachedFormDao(): CachedFormDao
    abstract fun cachedFormDetailDao(): CachedFormDetailDao
    abstract fun pendingFileUploadDao(): PendingFileUploadDao
    abstract fun cachedBlockAssignmentDao(): CachedBlockAssignmentDao
    abstract fun cachedBlockSummaryDao(): CachedBlockSummaryDao
    abstract fun cachedUploadedSubmissionDao(): CachedUploadedSubmissionDao
    abstract fun locationDao(): LocationDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_forms` (`formId` INTEGER NOT NULL, `formName` TEXT NOT NULL, `formCode` TEXT NOT NULL, `description` TEXT, `isActive` INTEGER NOT NULL, PRIMARY KEY(`formId`))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_form_details` (`formId` INTEGER NOT NULL, `detailJson` TEXT NOT NULL, PRIMARY KEY(`formId`))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pending_file_uploads` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `formId` INTEGER NOT NULL, `fieldId` TEXT NOT NULL, `uploadedBy` TEXT NOT NULL, `uriString` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_block_assignments` (`userId` TEXT NOT NULL, `assignmentsJson` TEXT NOT NULL, PRIMARY KEY(`userId`))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_block_summaries` (`userId` TEXT NOT NULL, `formId` INTEGER NOT NULL, `summaryJson` TEXT NOT NULL, PRIMARY KEY(`userId`, `formId`))"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_uploaded_submissions` (`formId` INTEGER NOT NULL, `submissionId` INTEGER NOT NULL, `submissionJson` TEXT NOT NULL, PRIMARY KEY(`formId`, `submissionId`))"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // We need to change primary key of cached_form_details. 
                // Since it's a cache, we can just drop and recreate.
                database.execSQL("DROP TABLE IF EXISTS `cached_form_details`")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_form_details` (`formId` INTEGER NOT NULL, `blockCode` TEXT NOT NULL, `detailJson` TEXT NOT NULL, PRIMARY KEY(`formId`, `blockCode`))"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `location_queue` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `userId` INTEGER NOT NULL,
                `userName` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `lat` REAL NOT NULL,
                `lng` REAL NOT NULL,
                `accuracy` REAL,
                `altitude` REAL,
                `speed` REAL,
                `heading` REAL,
                `deviceType` TEXT NOT NULL,
                `recordedAt` TEXT NOT NULL
            )
            """.trimIndent()
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,MIGRATION_6_7)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
