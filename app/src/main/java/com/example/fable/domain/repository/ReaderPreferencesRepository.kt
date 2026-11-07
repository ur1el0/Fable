package com.example.fable.domain.repository

import com.example.fable.domain.model.ReaderPreferences

interface ReaderPreferencesRepository {
    fun read(): ReaderPreferences
    fun save(preferences: ReaderPreferences)
}
