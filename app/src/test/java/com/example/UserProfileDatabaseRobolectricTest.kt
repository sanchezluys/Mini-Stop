package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.UserProfileDao
import com.example.data.local.UserProfileEntity
import com.example.data.local.UserProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserProfileDatabaseRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: UserProfileDao
    private lateinit var repository: UserProfileRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.userProfileDao()
        repository = UserProfileRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun save_and_retrieve_user_profile() = runBlocking {
        // Initial state should be null
        val initial = repository.userProfile.first()
        assertNull(initial)

        // Save profile
        repository.saveProfile("GamerPro", 3, "/path/to/avatar.jpg")

        // Retrieve and assert
        val saved = repository.userProfile.first()
        assertNotNull(saved)
        assertEquals("GamerPro", saved?.name)
        assertEquals(3, saved?.colorIndex)
        assertEquals("/path/to/avatar.jpg", saved?.avatarUri)

        // Update profile
        repository.saveProfile("GamerProUpdated", 1, null)
        val updated = repository.userProfile.first()
        assertNotNull(updated)
        assertEquals("GamerProUpdated", updated?.name)
        assertEquals(1, updated?.colorIndex)
        assertNull(updated?.avatarUri)
    }
}
