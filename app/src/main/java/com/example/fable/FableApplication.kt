package com.example.fable

import android.app.Application
import androidx.room.Room
import com.example.fable.data.local.FableDatabase
import com.example.fable.data.remote.FableApiService
import com.example.fable.data.repository.RoomStoryRepository
import com.example.fable.domain.repository.StoryRepository

class FableApplication : Application() {
    lateinit var storyRepository: StoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            FableDatabase::class.java,
            "fable.db"
        ).build()
        storyRepository = RoomStoryRepository(
            api = FableApiService(BuildConfig.FABLE_API_BASE_URL),
            dao = database.storyDao()
        )
    }
}
