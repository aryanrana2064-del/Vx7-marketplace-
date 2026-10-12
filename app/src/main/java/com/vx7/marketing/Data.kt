package com.vx7.marketing

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "",
    @ColumnInfo(defaultValue = "''") val notes: String = ""
)

@Entity
data class CollectionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val shop: String = "",
    @ColumnInfo(defaultValue = "''") val location: String = "",
    val note: String = ""
)

/** productName is a snapshot so history stays correct when a product is renamed or deleted. */
@Entity(
    foreignKeys = [ForeignKey(entity = CollectionRecord::class, parentColumns = ["id"], childColumns = ["recordId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recordId")]
)
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unit: String,
    @ColumnInfo(defaultValue = "''") val note: String = ""
)

@Entity
data class QuickNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class RecordRow(@Embedded val record: CollectionRecord, val entryCount: Int)
data class EntryRow(@Embedded val entry: Entry, val createdAt: Long, val shop: String)

@Dao
interface ProductDao {
    @Query("SELECT * FROM Product WHERE name LIKE '%' || :q || '%' OR category LIKE '%' || :q || '%' ORDER BY name COLLATE NOCASE")
    fun search(q: String): Flow<List<Product>>

    @Query("SELECT * FROM Product")
    suspend fun allOnce(): List<Product>

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(p: Product): Long
    @Update suspend fun update(p: Product)
    @Delete suspend fun delete(p: Product)
}

@Dao
abstract class RecordDao {
    @Query(
        "SELECT r.*, (SELECT COUNT(*) FROM Entry e WHERE e.recordId = r.id) AS entryCount FROM CollectionRecord r " +
            "WHERE r.createdAt BETWEEN :from AND :to AND (:q = '' OR r.shop LIKE '%' || :q || '%' " +
            "OR r.id IN (SELECT recordId FROM Entry WHERE productName LIKE '%' || :q || '%')) " +
            "ORDER BY r.createdAt DESC LIMIT :limit"
    )
    abstract fun search(q: String, from: Long, to: Long, limit: Int): Flow<List<RecordRow>>

    @Query("SELECT e.*, r.createdAt AS createdAt, r.shop AS shop FROM Entry e JOIN CollectionRecord r ON r.id = e.recordId WHERE r.createdAt BETWEEN :from AND :to ORDER BY r.createdAt DESC, e.id")
    abstract fun entryRows(from: Long, to: Long): Flow<List<EntryRow>>

    @Query("SELECT * FROM CollectionRecord WHERE id = :id") abstract fun recordFlow(id: Long): Flow<CollectionRecord?>
    @Query("SELECT * FROM CollectionRecord WHERE id = :id") abstract suspend fun get(id: Long): CollectionRecord?
    @Query("SELECT * FROM Entry WHERE recordId = :id ORDER BY id") abstract fun entriesFlow(id: Long): Flow<List<Entry>>
    @Query("SELECT * FROM Entry WHERE recordId = :id ORDER BY id") abstract suspend fun entries(id: Long): List<Entry>
    @Query("SELECT * FROM CollectionRecord ORDER BY createdAt") abstract suspend fun allRecords(): List<CollectionRecord>
    @Query("SELECT * FROM Entry ORDER BY id") abstract suspend fun allEntries(): List<Entry>

    @Insert abstract suspend fun insertRecord(r: CollectionRecord): Long
    @Update abstract suspend fun updateRecord(r: CollectionRecord)
    @Insert abstract suspend fun insertEntries(e: List<Entry>)
    @Query("DELETE FROM Entry WHERE recordId = :id") abstract suspend fun deleteEntries(id: Long)
    @Query("DELETE FROM CollectionRecord WHERE id = :id") abstract suspend fun delete(id: Long)

    @Transaction
    open suspend fun saveNew(r: CollectionRecord, es: List<Entry>): Long {
        val id = insertRecord(r.copy(id = 0))
        insertEntries(es.map { it.copy(id = 0, recordId = id) })
        return id
    }

    @Transaction
    open suspend fun saveEdit(r: CollectionRecord, es: List<Entry>) {
        updateRecord(r)
        deleteEntries(r.id)
        insertEntries(es.map { it.copy(id = 0, recordId = r.id) })
    }
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM QuickNote ORDER BY createdAt DESC") fun all(): Flow<List<QuickNote>>
    @Query("SELECT * FROM QuickNote") suspend fun allOnce(): List<QuickNote>
    @Insert suspend fun add(n: QuickNote)
    @Delete suspend fun delete(n: QuickNote)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE Product ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE CollectionRecord ADD COLUMN location TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE Entry ADD COLUMN note TEXT NOT NULL DEFAULT ''")
    }
}

@Database(entities = [Product::class, CollectionRecord::class, Entry::class, QuickNote::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun products(): ProductDao
    abstract fun records(): RecordDao
    abstract fun notes(): NoteDao

    companion object {
        @Volatile private var instance: AppDb? = null
        fun get(c: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "vx7-marketing.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { instance = it }
        }
    }
}
