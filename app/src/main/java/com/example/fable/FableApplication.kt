package com.example.fable

import android.app.Application
import androidx.room.Room
import com.example.fable.data.local.DeviceIdentityStore
import com.example.fable.data.local.FableDatabase
import com.example.fable.data.local.SecureSessionStore
import com.example.fable.data.remote.FableApiService
import com.example.fable.data.repository.RemoteAccountRepository
import com.example.fable.data.repository.RoomStoryRepository
import com.example.fable.domain.repository.AccountRepository
import com.example.fable.domain.repository.StoryRepository

class FableApplication : Application() {
    lateinit var sessionStore: SecureSessionStore
        private set
    lateinit var deviceIdentityStore: DeviceIdentityStore
        private set
    lateinit var apiService: FableApiService
        private set
    lateinit var storyRepository: StoryRepository
        private set
    lateinit var accountRepository: AccountRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            applicationContext,
            FableDatabase::class.java,
            "fable.db"
        ).addMigrations(FableDatabase.MIGRATION_1_2).build()
        sessionStore = SecureSessionStore(applicationContext)
        deviceIdentityStore = DeviceIdentityStore(applicationContext)
        apiService = FableApiService(
            baseUrl = BuildConfig.FABLE_API_BASE_URL,
            accessTokenProvider = sessionStore::readAccessToken
        )
        storyRepository = RoomStoryRepository(
            api = apiService,
            dao = database.storyDao(),
            deviceId = deviceIdentityStore.getOrCreateDeviceId(),
            accessTokenProvider = sessionStore::readAccessToken
        )
        accountRepository = RemoteAccountRepository(apiService, sessionStore)
    }
}
