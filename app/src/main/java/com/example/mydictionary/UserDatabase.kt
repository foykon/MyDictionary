package com.example.mydictionary

import android.content.Context
import android.util.Log
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val score: Int
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY score DESC")
    fun getAllUsersOrderedByScore(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET score = :newScore WHERE id = :userId")
    suspend fun updateUserScore(userId: String, newScore: Int)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Database(entities = [User::class], version = 1)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d("UserDatabase", "Creating database instance")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class UserRepository(private val userDao: UserDao) {
    val allUsers: Flow<List<User>> = userDao.getAllUsersOrderedByScore()

    suspend fun getUser(userId: String): User? {
        return userDao.getUserById(userId)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
        Log.d("UserRepository", "Inserted user: ${user.username}")
    }

    suspend fun updateUserScore(userId: String, newScore: Int) {
        userDao.updateUserScore(userId, newScore)
        Log.d("UserRepository", "Updated score for user $userId to $newScore")
    }

    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }
} 