package com.toffice.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.toffice.app.data.document.DocumentDao
import com.toffice.app.data.document.DocumentEntity
import com.toffice.app.data.task.Task
import com.toffice.app.data.task.TaskDao

@Database(
    entities = [Task::class, DocumentEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun documentDao(): DocumentDao

    companion object {
        const val NAME = "t_office.db"
    }
}
