package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class Player(
    val id: String,
    val name: String,
    val colorIndex: Int = 0,
    val isHost: Boolean = false,
    val isReady: Boolean = true,
    val score: Int = 0,
    val isBot: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("colorIndex", colorIndex)
            put("isHost", isHost)
            put("isReady", isReady)
            put("score", score)
            put("isBot", isBot)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): Player {
            return Player(
                id = json.getString("id"),
                name = json.getString("name"),
                colorIndex = json.optInt("colorIndex", 0),
                isHost = json.optBoolean("isHost", false),
                isReady = json.optBoolean("isReady", true),
                score = json.optInt("score", 0),
                isBot = json.optBoolean("isBot", false)
            )
        }
    }
}

val DEFAULT_CATEGORIES = listOf(
    "Nombre",
    "Animal",
    "País / Ciudad",
    "Fruta / Alimento",
    "Cosa / Objeto",
    "Color"
)

val ADDITIONAL_CATEGORIES = listOf(
    "Profesión",
    "Marca",
    "Película / Serie",
    "Verbo",
    "Famoso",
    "Deporte",
    "Instrumento",
    "Cuerpo humano"
)

val GAME_LETTERS = listOf(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
    'L', 'M', 'N', 'O', 'P', 'R', 'S', 'T', 'U', 'V', 'Z'
)

data class GameConfig(
    val categories: List<String> = DEFAULT_CATEGORIES,
    val roundDurationSecs: Int = 60,
    val totalRounds: Int = 5,
    val maxPlayers: Int = 6
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        val catArray = JSONArray()
        categories.forEach { catArray.put(it) }
        json.put("categories", catArray)
        json.put("roundDurationSecs", roundDurationSecs)
        json.put("totalRounds", totalRounds)
        json.put("maxPlayers", maxPlayers)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): GameConfig {
            val catList = mutableListOf<String>()
            val catArray = json.optJSONArray("categories")
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    catList.add(catArray.getString(i))
                }
            } else {
                catList.addAll(DEFAULT_CATEGORIES)
            }
            return GameConfig(
                categories = if (catList.isNotEmpty()) catList else DEFAULT_CATEGORIES,
                roundDurationSecs = json.optInt("roundDurationSecs", 60),
                totalRounds = json.optInt("totalRounds", 5),
                maxPlayers = json.optInt("maxPlayers", 6)
            )
        }
    }
}

data class PlayerAnswers(
    val playerId: String,
    val playerName: String,
    val answers: Map<String, String> // category -> answer word
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("playerId", playerId)
        json.put("playerName", playerName)
        val ansObj = JSONObject()
        answers.forEach { (k, v) -> ansObj.put(k, v) }
        json.put("answers", ansObj)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): PlayerAnswers {
            val pId = json.getString("playerId")
            val pName = json.getString("playerName")
            val ansMap = mutableMapOf<String, String>()
            val ansObj = json.optJSONObject("answers")
            ansObj?.keys()?.forEach { key ->
                ansMap[key] = ansObj.optString(key, "")
            }
            return PlayerAnswers(pId, pName, ansMap)
        }
    }
}

enum class AnswerScoreType(val points: Int, val label: String) {
    UNIQUE(100, "Única (+100)"),
    REPEATED(50, "Repetida (+50)"),
    INVALID(0, "Nula (0)")
}

data class RoundVoteScore(
    val playerId: String,
    val category: String,
    val scoreType: AnswerScoreType = AnswerScoreType.UNIQUE
)

enum class ScreenState {
    HOME,
    LOBBY,
    LETTER_ROULETTE,
    ROUND_PLAYING,
    REVIEW_VOTING,
    ROUND_SUMMARY,
    GAME_OVER
}
