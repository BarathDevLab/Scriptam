package com.scriptam.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ScriptEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScriptamDatabase : RoomDatabase() {

    abstract fun scriptDao(): ScriptDao

    companion object {
        @Volatile
        private var INSTANCE: ScriptamDatabase? = null

        fun getInstance(context: Context): ScriptamDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScriptamDatabase::class.java,
                    "scriptam.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
