package com.maryen.app.core.memory

import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Entity(tableName = "memory_items")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long = System.currentTimeMillis(),
    val role: String = "user",   // user | assistant | fact
    val skillId: String? = null,
    val text: String = ""
)

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(item: MemoryItem): Long

    @Query("SELECT * FROM memory_items ORDER BY ts DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MemoryItem>

    @Query("SELECT * FROM memory_items WHERE text LIKE '%' || :term || '%' ORDER BY ts DESC LIMIT :limit")
    suspend fun searchText(term: String, limit: Int): List<MemoryItem>

    @Query("DELETE FROM memory_items")
    suspend fun clearAll()
}

@Database(entities = [MemoryItem::class], version = 1, exportSchema = false)
abstract class MaryenDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile private var INSTANCE: MaryenDatabase? = null

        fun get(ctx: android.content.Context): MaryenDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    MaryenDatabase::class.java,
                    "maryen-memory.db"
                ).build().also { INSTANCE = it }
            }
    }
}

/**
 * Memoria conversazionale v1: SQLite via Room.
 * Nessun embedding vettoriale in questa versione — il "richiamo memoria"
 * (semanticSearch) è una ricerca testuale semplice sulle ultime righe.
 * Questo è il punto di estensione futuro: si potrà sostituire searchText
 * con una vera ricerca per similarità senza toccare Orchestrator.
 */
class MemoryStore(ctx: android.content.Context) {

    private val dao = MaryenDatabase.get(ctx).memoryDao()

    data class Turn(val userText: String, val assistantText: String, val skillUsed: String?)

    suspend fun write(turn: Turn) = withContext(Dispatchers.IO) {
        dao.insert(MemoryItem(role = "user", text = turn.userText))
        dao.insert(MemoryItem(role = "assistant", text = turn.assistantText, skillId = turn.skillUsed))
    }

    suspend fun recent(limit: Int = 8): List<String> = withContext(Dispatchers.IO) {
        dao.recent(limit).reversed().map { "[${it.role}] ${it.text}" }
    }

    /** Ricerca testuale semplice. v2: sostituire con similarità su embeddings. */
    suspend fun semanticSearch(query: String, k: Int = 4): List<String> = withContext(Dispatchers.IO) {
        val terms = query.split(" ").filter { it.length > 3 }.take(3)
        if (terms.isEmpty()) return@withContext emptyList()
        terms.flatMap { dao.searchText(it, k) }
            .distinctBy { it.id }
            .take(k)
            .map { "[${it.role}] ${it.text}" }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }
}
