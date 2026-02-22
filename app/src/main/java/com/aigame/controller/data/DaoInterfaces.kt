package com.aigame.controller.data

import androidx.room.*
import com.aigame.controller.data.entity.*

/**
 * 技能DAO
 */
@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY learnTime DESC")
    suspend fun getAll(): List<SkillEntity>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getById(id: String): SkillEntity?

    @Query("SELECT * FROM skills WHERE name LIKE :name")
    suspend fun searchByName(name: String): List<SkillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(skill: SkillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(skills: List<SkillEntity>)

    @Update
    suspend fun update(skill: SkillEntity)

    @Delete
    suspend fun delete(skill: SkillEntity)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM skills")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM skills")
    suspend fun getCount(): Int
}

/**
 * 物品DAO
 */
@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY learnTime DESC")
    suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE name LIKE :name")
    suspend fun searchByName(name: String): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getCount(): Int
}

/**
 * 游戏配置DAO
 */
@Dao
interface GameConfigDao {
    @Query("SELECT * FROM game_configs")
    suspend fun getAll(): List<GameConfigEntity>

    @Query("SELECT * FROM game_configs WHERE id = :id")
    suspend fun getById(id: String): GameConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: GameConfigEntity)

    @Delete
    suspend fun delete(config: GameConfigEntity)
}

/**
 * 学习会话DAO
 */
@Dao
interface LearnSessionDao {
    @Query("SELECT * FROM learn_sessions ORDER BY startTime DESC")
    suspend fun getAll(): List<LearnSessionEntity>

    @Query("SELECT * FROM learn_sessions WHERE id = :id")
    suspend fun getById(id: Long): LearnSessionEntity?

    @Insert
    suspend fun insert(session: LearnSessionEntity): Long

    @Update
    suspend fun update(session: LearnSessionEntity)

    @Query("SELECT * FROM learn_sessions WHERE status = 'running' LIMIT 1")
    suspend fun getRunningSession(): LearnSessionEntity?
}
