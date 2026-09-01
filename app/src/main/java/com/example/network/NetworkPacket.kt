package com.example.network

import com.example.model.GameConfig
import com.example.model.Player
import com.example.model.PlayerAnswers
import org.json.JSONArray
import org.json.JSONObject

enum class PacketType {
    JOIN_REQUEST,
    JOIN_RESPONSE,
    PLAYER_LIST_UPDATE,
    CONFIG_UPDATE,
    START_LETTER_ROULETTE,
    START_ROUND,
    STOP_CALLED,
    SUBMIT_ANSWERS,
    ALL_ANSWERS_SYNC,
    VOTE_UPDATE,
    ROUND_RESULTS,
    NEXT_ROUND_TRIGGER,
    RESTART_GAME,
    HEARTBEAT,
    LAUGH_VOTE_TOGGLE,
    LAUGH_VOTES_SYNC
}

data class NetworkPacket(
    val type: PacketType,
    val senderId: String = "",
    val payload: JSONObject = JSONObject()
) {
    fun serialize(): String {
        val root = JSONObject()
        root.put("type", type.name)
        root.put("senderId", senderId)
        root.put("payload", payload)
        return root.toString()
    }

    companion object {
        fun deserialize(jsonStr: String): NetworkPacket? {
            return try {
                val root = JSONObject(jsonStr)
                val typeStr = root.getString("type")
                val type = PacketType.valueOf(typeStr)
                val senderId = root.optString("senderId", "")
                val payload = root.optJSONObject("payload") ?: JSONObject()
                NetworkPacket(type, senderId, payload)
            } catch (e: Exception) {
                null
            }
        }

        fun createJoinRequest(player: Player): NetworkPacket {
            val payload = JSONObject().apply {
                put("player", player.toJsonObject())
            }
            return NetworkPacket(PacketType.JOIN_REQUEST, player.id, payload)
        }

        fun createPlayerListUpdate(players: List<Player>): NetworkPacket {
            val array = JSONArray()
            players.forEach { array.put(it.toJsonObject()) }
            val payload = JSONObject().apply {
                put("players", array)
            }
            return NetworkPacket(PacketType.PLAYER_LIST_UPDATE, "", payload)
        }

        fun createConfigUpdate(config: GameConfig): NetworkPacket {
            val payload = JSONObject().apply {
                put("config", config.toJsonObject())
            }
            return NetworkPacket(PacketType.CONFIG_UPDATE, "", payload)
        }

        fun createStartRoulette(letter: Char, roundNumber: Int): NetworkPacket {
            val payload = JSONObject().apply {
                put("letter", letter.toString())
                put("roundNumber", roundNumber)
            }
            return NetworkPacket(PacketType.START_LETTER_ROULETTE, "", payload)
        }

        fun createStartRound(letter: Char, roundNumber: Int, durationSecs: Int): NetworkPacket {
            val payload = JSONObject().apply {
                put("letter", letter.toString())
                put("roundNumber", roundNumber)
                put("durationSecs", durationSecs)
            }
            return NetworkPacket(PacketType.START_ROUND, "", payload)
        }

        fun createStopCalled(playerId: String, playerName: String): NetworkPacket {
            val payload = JSONObject().apply {
                put("callerId", playerId)
                put("callerName", playerName)
            }
            return NetworkPacket(PacketType.STOP_CALLED, playerId, payload)
        }

        fun createSubmitAnswers(answers: PlayerAnswers): NetworkPacket {
            val payload = JSONObject().apply {
                put("answers", answers.toJsonObject())
            }
            return NetworkPacket(PacketType.SUBMIT_ANSWERS, answers.playerId, payload)
        }

        fun createAllAnswersSync(allAnswers: List<PlayerAnswers>): NetworkPacket {
            val array = JSONArray()
            allAnswers.forEach { array.put(it.toJsonObject()) }
            val payload = JSONObject().apply {
                put("allAnswers", array)
            }
            return NetworkPacket(PacketType.ALL_ANSWERS_SYNC, "", payload)
        }

        fun createVoteUpdate(scoresJson: JSONObject): NetworkPacket {
            return NetworkPacket(PacketType.VOTE_UPDATE, "", scoresJson)
        }

        fun createRoundResults(players: List<Player>, roundNumber: Int, isGameOver: Boolean): NetworkPacket {
            val array = JSONArray()
            players.forEach { array.put(it.toJsonObject()) }
            val payload = JSONObject().apply {
                put("players", array)
                put("roundNumber", roundNumber)
                put("isGameOver", isGameOver)
            }
            return NetworkPacket(PacketType.ROUND_RESULTS, "", payload)
        }

        fun createLaughVoteToggle(targetPlayerId: String, category: String, voterId: String): NetworkPacket {
            val payload = JSONObject().apply {
                put("targetPlayerId", targetPlayerId)
                put("category", category)
                put("voterId", voterId)
            }
            return NetworkPacket(PacketType.LAUGH_VOTE_TOGGLE, voterId, payload)
        }

        fun createLaughVotesSync(
            roundLaughVotes: Map<String, Map<String, List<String>>>,
            tournamentLaughs: Map<String, Int>
        ): NetworkPacket {
            val root = JSONObject()
            val roundObj = JSONObject()
            roundLaughVotes.forEach { (targetId, catMap) ->
                val catObj = JSONObject()
                catMap.forEach { (cat, voters) ->
                    val arr = JSONArray()
                    voters.forEach { arr.put(it) }
                    catObj.put(cat, arr)
                }
                roundObj.put(targetId, catObj)
            }
            root.put("roundLaughVotes", roundObj)

            val tournObj = JSONObject()
            tournamentLaughs.forEach { (pId, count) ->
                tournObj.put(pId, count)
            }
            root.put("tournamentLaughs", tournObj)

            return NetworkPacket(PacketType.LAUGH_VOTES_SYNC, "", root)
        }
    }
}
