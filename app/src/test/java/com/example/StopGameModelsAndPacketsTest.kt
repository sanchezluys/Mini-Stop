package com.example

import com.example.model.AnswerScoreType
import com.example.model.GameConfig
import com.example.model.Player
import com.example.model.PlayerAnswers
import com.example.network.NetworkPacket
import com.example.network.PacketType
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StopGameModelsAndPacketsTest {

    @Test
    fun player_serialization_includes_laughVotes() {
        val player = Player(
            id = "player-123",
            name = "Carlos",
            colorIndex = 2,
            avatarUri = "/data/user/0/com.example/files/avatars/profile.jpg",
            isHost = true,
            isReady = true,
            score = 250,
            isBot = false,
            laughVotes = 4
        )

        val json = player.toJsonObject()
        val deserialized = Player.fromJsonObject(json)

        assertEquals(player.id, deserialized.id)
        assertEquals(player.name, deserialized.name)
        assertEquals(player.colorIndex, deserialized.colorIndex)
        assertEquals(player.avatarUri, deserialized.avatarUri)
        assertEquals(player.isHost, deserialized.isHost)
        assertEquals(player.score, deserialized.score)
        assertEquals(player.isBot, deserialized.isBot)
        assertEquals(4, deserialized.laughVotes)
    }

    @Test
    fun laughVoteToggle_packet_serialization() {
        val packet = NetworkPacket.createLaughVoteToggle(
            targetPlayerId = "player-2",
            category = "Fruta",
            voterId = "player-1"
        )

        assertEquals(PacketType.LAUGH_VOTE_TOGGLE, packet.type)
        assertEquals("player-1", packet.senderId)
        assertEquals("player-2", packet.payload.getString("targetPlayerId"))
        assertEquals("Fruta", packet.payload.getString("category"))
        assertEquals("player-1", packet.payload.getString("voterId"))

        val serialized = packet.serialize()
        val deserialized = NetworkPacket.deserialize(serialized)

        assertNotNull(deserialized)
        assertEquals(PacketType.LAUGH_VOTE_TOGGLE, deserialized!!.type)
        assertEquals("player-2", deserialized.payload.getString("targetPlayerId"))
    }

    @Test
    fun laughVotesSync_packet_serialization() {
        val roundLaughs = mapOf(
            "player-2" to mapOf(
                "Fruta" to listOf("player-1", "player-3")
            )
        )
        val tournamentLaughs = mapOf(
            "player-2" to 5,
            "player-1" to 2
        )

        val packet = NetworkPacket.createLaughVotesSync(roundLaughs, tournamentLaughs)
        assertEquals(PacketType.LAUGH_VOTES_SYNC, packet.type)

        val serialized = packet.serialize()
        val deserialized = NetworkPacket.deserialize(serialized)

        assertNotNull(deserialized)
        val roundObj = deserialized!!.payload.getJSONObject("roundLaughVotes")
        val p2CatObj = roundObj.getJSONObject("player-2")
        val votersArr = p2CatObj.getJSONArray("Fruta")
        assertEquals(2, votersArr.length())
        assertEquals("player-1", votersArr.getString(0))

        val tournObj = deserialized.payload.getJSONObject("tournamentLaughs")
        assertEquals(5, tournObj.getInt("player-2"))
        assertEquals(2, tournObj.getInt("player-1"))
    }

    @Test
    fun scoring_calculation_rule_test() {
        // Unique = 100, Repeated = 50, Invalid = 0
        assertEquals(100, AnswerScoreType.UNIQUE.points)
        assertEquals(50, AnswerScoreType.REPEATED.points)
        assertEquals(0, AnswerScoreType.INVALID.points)

        val sampleScores = mapOf(
            "Nombre" to AnswerScoreType.UNIQUE,
            "Animal" to AnswerScoreType.REPEATED,
            "Cosa" to AnswerScoreType.INVALID
        )
        val totalRoundScore = sampleScores.values.sumOf { it.points }
        assertEquals(150, totalRoundScore)
    }

    @Test
    fun comedy_winner_selection_test() {
        val players = listOf(
            Player(id = "p1", name = "Ana", score = 300, laughVotes = 2),
            Player(id = "p2", name = "Luis", score = 200, laughVotes = 6),
            Player(id = "p3", name = "Sofia", score = 250, laughVotes = 6),
            Player(id = "p4", name = "Pedro", score = 150, laughVotes = 1)
        )

        val maxLaughs = players.maxOfOrNull { it.laughVotes } ?: 0
        assertEquals(6, maxLaughs)

        val comedyWinners = players.filter { it.laughVotes == maxLaughs }
        assertEquals(2, comedyWinners.size)
        assertTrue(comedyWinners.any { it.name == "Luis" })
        assertTrue(comedyWinners.any { it.name == "Sofia" })
    }
}
