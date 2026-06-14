package com.toffice.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.toffice.app.data.task.Task
import com.toffice.app.data.task.TaskDao

@Database(
    entities = [Task::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val NAME = "t_office.db"
    }
}
