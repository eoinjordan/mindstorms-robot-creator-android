package com.eoinedge.robotinventor

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "probe_sessions")
data class StoredSession(
    @PrimaryKey val id: String,
    val profileId: String,
    val label: String,
    val notes: String,
    val timestamp: Long,
    val jsonPayload: String
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM probe_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StoredSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StoredSession)

    @Delete
    suspend fun deleteSession(session: StoredSession)
    
    @Query("SELECT * FROM probe_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): StoredSession?
}

@Database(entities = [StoredSession::class], version = 1)
abstract class RobotDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: RobotDatabase? = null

        fun getDatabase(context: Context): RobotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RobotDatabase::class.java,
                    "robot_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
