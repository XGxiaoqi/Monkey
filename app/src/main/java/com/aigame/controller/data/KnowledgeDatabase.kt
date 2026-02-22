package com.aigame.controller.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aigame.controller.data.entity.*

/**
 * Room数据库
 */
@Database(
    entities = [
        SkillEntity::class,
        ItemEntity::class,
        GameConfigEntity::class,
        LearnSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KnowledgeDatabase : RoomDatabase() {
    abstract fun skillDao(): SkillDao
    abstract fun itemDao(): ItemDao
    abstract fun gameConfigDao(): GameConfigDao
    abstract fun learnSessionDao(): LearnSessionDao

    companion object {
        private const val DATABASE_NAME = "game_knowledge.db"

        @Volatile
        private var INSTANCE: KnowledgeDatabase? = null

        fun getDatabase(context: Context): KnowledgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KnowledgeDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * 类型转换器
 */
class Converters {
    @TypeConverter
    fun fromByteArray(value: ByteArray?): String? {
        return value?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) }
    }

    @TypeConverter
    fun toByteArray(value: String?): ByteArray? {
        return value?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) }
    }
}
