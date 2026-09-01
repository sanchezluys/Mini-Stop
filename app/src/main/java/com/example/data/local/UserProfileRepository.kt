package com.example.data.local

import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val dao: UserProfileDao) {
    val userProfile: Flow<UserProfileEntity?> = dao.getUserProfile()

    suspend fun saveProfile(name: String, colorIndex: Int, avatarUri: String?) {
        dao.saveUserProfile(
            UserProfileEntity(
                id = 1,
                name = name,
                colorIndex = colorIndex,
                avatarUri = avatarUri
            )
        )
    }
}
