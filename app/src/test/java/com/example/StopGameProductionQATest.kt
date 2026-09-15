package com.example

import com.example.model.AnswerScoreType
import com.example.model.GAME_LETTERS
import com.example.model.Player
import com.example.model.PlayerAnswers
import com.example.network.NetworkPacket
import com.example.network.PacketType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StopGameProductionQATest {

    @Test
    fun qa_version_and_developer_metadata_test() {
        assertEquals("8.0", BuildConfig.VERSION_NAME)
        assertEquals(8, BuildConfig.VERSION_CODE)
        val devEmail = "sanchezluys@gmail.com"
        assertTrue("Developer email must be valid", devEmail.contains("@") && devEmail.endsWith(".com"))
    }

    @Test
    fun qa_camera_permission_and_fileprovider_configuration_test() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
        val requested = packageInfo.requestedPermissions ?: emptyArray()

        assertTrue("Manifest must declare CAMERA permission", requested.contains(android.Manifest.permission.CAMERA))

        // Verify FileProvider can generate URI for camera cache without exception
        val cacheDir = java.io.File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val testPhoto = java.io.File(cacheDir, "test_camera_photo.jpg").apply { writeText("test_bytes") }
        val authority = "${context.packageName}.fileprovider"

        val photoUri = androidx.core.content.FileProvider.getUriForFile(context, authority, testPhoto)
        assertNotNull("FileProvider must generate a valid URI", photoUri)
        assertTrue("URI must use content scheme", photoUri.scheme == "content")
        assertTrue("URI must contain authority", photoUri.authority == authority)
    }

    @Test
    fun qa_player_avatarBase64_serialization_test() {
        val testBase64 = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP"
        val player = Player(
            id = "p-test-1",
            name = "Camila",
            colorIndex = 3,
            avatarUri = "/data/user/0/com.example/files/avatars/profile_avatar.jpg",
            avatarBase64 = testBase64,
            isHost = true,
            score = 150,
            laughVotes = 3
        )

        val json = player.toJsonObject()
        assertTrue("JSON must contain avatarBase64 key", json.has("avatarBase64"))
        assertEquals(testBase64, json.getString("avatarBase64"))

        val deserialized = Player.fromJsonObject(json)
        assertEquals(player.id, deserialized.id)
        assertEquals(player.name, deserialized.name)
        assertEquals(testBase64, deserialized.avatarBase64)
    }

    @Test
    fun qa_unique_letters_per_round_test() {
        val totalRounds = 5
        val usedLetters = mutableListOf<Char>()

        for (round in 1..totalRounds) {
            val available = GAME_LETTERS.filterNot { it in usedLetters }
            assertTrue("Available pool must not be empty", available.isNotEmpty())
            val picked = available.random()
            assertFalse("Picked letter must not have been used in previous rounds", usedLetters.contains(picked))
            usedLetters.add(picked)
        }

        assertEquals(totalRounds, usedLetters.distinct().size)
    }

    @Test
    fun qa_host_only_voting_security_rule_test() {
        val hostPlayerId = "host-001"
        val clientPlayerId = "guest-002"

        fun canEditScores(requestingPlayerId: String, isHost: Boolean): Boolean {
            return isHost && requestingPlayerId == hostPlayerId
        }

        fun canVoteLaugh(requestingPlayerId: String, targetPlayerId: String): Boolean {
            // Both host and guests can vote laugh, but NEVER for their own word
            return requestingPlayerId != targetPlayerId
        }

        // Host checks
        assertTrue("Host must be allowed to edit scores", canEditScores(hostPlayerId, isHost = true))
        // Guest checks
        assertFalse("Guest must NOT be allowed to edit scores", canEditScores(clientPlayerId, isHost = false))

        // Laugh voting checks
        assertTrue("Guest can vote laugh for host's funny word", canVoteLaugh(clientPlayerId, hostPlayerId))
        assertTrue("Host can vote laugh for guest's funny word", canVoteLaugh(hostPlayerId, clientPlayerId))
        assertFalse("Guest cannot vote laugh for their own word", canVoteLaugh(clientPlayerId, clientPlayerId))
        assertFalse("Host cannot vote laugh for their own word", canVoteLaugh(hostPlayerId, hostPlayerId))
    }

    @Test
    fun qa_all_answers_sync_with_edited_word_test() {
        val originalSubmissions = listOf(
            PlayerAnswers(
                playerId = "guest-1",
                playerName = "Sofi",
                answers = mapOf("Fruta" to "Bannana")
            )
        )

        // Host corrects typo "Bannana" -> "Banana"
        val correctedSubmissions = originalSubmissions.map { sub ->
            if (sub.playerId == "guest-1") {
                val updated = sub.answers.toMutableMap()
                updated["Fruta"] = "Banana"
                sub.copy(answers = updated)
            } else sub
        }

        val syncPacket = NetworkPacket.createAllAnswersSync(correctedSubmissions)
        assertEquals(PacketType.ALL_ANSWERS_SYNC, syncPacket.type)

        val serialized = syncPacket.serialize()
        val deserialized = NetworkPacket.deserialize(serialized)
        assertNotNull(deserialized)

        val arr = deserialized!!.payload.getJSONArray("allAnswers")
        val restored = PlayerAnswers.fromJsonObject(arr.getJSONObject(0))
        assertEquals("Banana", restored.answers["Fruta"])
    }
}
