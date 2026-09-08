package com.spamblock.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BlockedCallsDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "spamblock.db"
        const val DATABASE_VERSION = 3

        const val TABLE_BLOCKED = "blocked_calls"
        const val COLUMN_ID = "_id"
        const val COLUMN_PHONE = "phone_number"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_REASON = "reason"
        const val COLUMN_SIM_SLOT = "sim_slot"

        @Volatile
        private var instance: BlockedCallsDbHelper? = null

        fun getInstance(context: Context): BlockedCallsDbHelper {
            return instance ?: synchronized(this) {
                instance ?: BlockedCallsDbHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _callsFlow = MutableStateFlow<List<BlockedCall>>(emptyList())
    val callsFlow: StateFlow<List<BlockedCall>> = _callsFlow.asStateFlow()

    init {
        refreshFlow()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_BLOCKED (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_REASON TEXT NOT NULL,
                $COLUMN_SIM_SLOT INTEGER DEFAULT -1
            )
        """.trimIndent()
        db.execSQL(createTable)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_timestamp ON $TABLE_BLOCKED ($COLUMN_TIMESTAMP DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_phone_time ON $TABLE_BLOCKED ($COLUMN_PHONE, $COLUMN_TIMESTAMP DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_BLOCKED ADD COLUMN $COLUMN_SIM_SLOT INTEGER DEFAULT -1")
            } catch (_: Exception) {
                // Ignore if column already exists
            }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_phone_time ON $TABLE_BLOCKED ($COLUMN_PHONE, $COLUMN_TIMESTAMP DESC)")
            } catch (_: Exception) {
                // Ignore if index already exists
            }
        }
    }

    /**
     * Returns count of recently blocked calls from the given number within the windowMillis.
     */
    fun getRecentBlockedCount(rawNumber: String, windowMillis: Long): Int {
        if (rawNumber.isBlank()) return 0
        val minTime = System.currentTimeMillis() - windowMillis
        val normalized = rawNumber.replace(Regex("[^0-9+]"), "")
        val suffix = if (normalized.length >= 7) normalized.takeLast(7) else normalized

        val query = """
            SELECT COUNT(*) FROM $TABLE_BLOCKED 
            WHERE $COLUMN_TIMESTAMP >= ? 
            AND ($COLUMN_PHONE = ? OR $COLUMN_PHONE LIKE ?)
        """.trimIndent()

        val cursor = readableDatabase.rawQuery(
            query,
            arrayOf(minTime.toString(), rawNumber, "%$suffix")
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun insert(phoneNumber: String, reason: String, simSlot: Int = -1): Long {
        val values = ContentValues().apply {
            put(COLUMN_PHONE, phoneNumber)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            put(COLUMN_REASON, reason)
            put(COLUMN_SIM_SLOT, simSlot)
        }
        val id = writableDatabase.insert(TABLE_BLOCKED, null, values)
        refreshFlow()
        return id
    }

    fun getAll(): List<BlockedCall> {
        val list = mutableListOf<BlockedCall>()
        val query = "SELECT $COLUMN_ID, $COLUMN_PHONE, $COLUMN_TIMESTAMP, $COLUMN_REASON, $COLUMN_SIM_SLOT FROM $TABLE_BLOCKED ORDER BY $COLUMN_TIMESTAMP DESC LIMIT 200"
        val cursor = readableDatabase.rawQuery(query, null)
        cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val phoneIdx = cursor.getColumnIndexOrThrow(COLUMN_PHONE)
            val timeIdx = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val reasonIdx = cursor.getColumnIndexOrThrow(COLUMN_REASON)
            val simIdx = cursor.getColumnIndexOrThrow(COLUMN_SIM_SLOT)

            while (cursor.moveToNext()) {
                list.add(
                    BlockedCall(
                        id = cursor.getLong(idIdx),
                        phoneNumber = cursor.getString(phoneIdx),
                        timestamp = cursor.getLong(timeIdx),
                        reason = cursor.getString(reasonIdx),
                        simSlot = cursor.getInt(simIdx)
                    )
                )
            }
        }
        return list
    }

    fun delete(id: Long) {
        writableDatabase.delete(TABLE_BLOCKED, "$COLUMN_ID = ?", arrayOf(id.toString()))
        refreshFlow()
    }

    fun clearAll() {
        writableDatabase.delete(TABLE_BLOCKED, null, null)
        refreshFlow()
    }

    fun getTotalBlockedCount(): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_BLOCKED", null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    fun refreshFlow() {
        _callsFlow.value = getAll()
    }
}
