package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffects
import com.example.model.ADDITIONAL_CATEGORIES
import com.example.model.AnswerScoreType
import com.example.model.DEFAULT_CATEGORIES
import com.example.model.GAME_LETTERS
import com.example.model.GameConfig
import com.example.model.Player
import com.example.model.PlayerAnswers
import com.example.model.ScreenState
import com.example.network.DiscoveredRoom
import com.example.network.LocalNetworkManager
import com.example.network.NetworkDiscovery
import com.example.network.NetworkPacket
import com.example.network.PacketType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StopUiState(
    val currentScreen: ScreenState = ScreenState.HOME,
    val localPlayer: Player = Player(id = UUID.randomUUID().toString().take(8), name = "Tú", colorIndex = 0),
    val players: List<Player> = emptyList(),
    val gameConfig: GameConfig = GameConfig(),
    val roomCode: String = "",
    val hostIp: String = "",
    val isHost: Boolean = false,
    val isSoloOrBotsMode: Boolean = false,
    val availableRooms: List<DiscoveredRoom> = emptyList(),
    val isDiscovering: Boolean = false,
    val connectionStatus: String = "",
    
    // Round State
    val currentRoundNumber: Int = 1,
    val currentLetter: Char = 'A',
    val spinningLetter: Char = 'A',
    val isSpinning: Boolean = false,
    val remainingTimeSeconds: Int = 60,
    val isTimerActive: Boolean = false,
    val playerInputs: Map<String, String> = emptyMap(), // category -> typed word
    val whoCalledStop: String? = null,
    val stopCountdown: Int = 0,
    
    // Voting & Review State
    val allRoundSubmissions: List<PlayerAnswers> = emptyList(),
    // Map: playerId -> (category -> AnswerScoreType)
    val votingScores: Map<String, Map<String, AnswerScoreType>> = emptyMap(),
    val roundPointsEarned: Map<String, Int> = emptyMap(), // playerId -> points in round
    
    // Notification Banner / Snackbar
    val bannerMessage: String? = null
)

class StopGameViewModel(application: Application) : AndroidViewModel(application) {
    private val discovery = NetworkDiscovery(application)
    private val networkManager = LocalNetworkManager()
    private val sounds = SoundEffects(application)

    private val _uiState = MutableStateFlow(StopUiState())
    val uiState: StateFlow<StopUiState> = _uiState.asStateFlow()

    private var roundTimerJob: Job? = null
    private var rouletteJob: Job? = null
    private var stopCountdownJob: Job? = null

    init {
        // Initialize with default player
        val initialPlayer = Player(
            id = UUID.randomUUID().toString().take(8),
            name = "Jugador 1",
            colorIndex = 0,
            isHost = true
        )
        _uiState.update { it.copy(localPlayer = initialPlayer, players = listOf(initialPlayer)) }

        observeIncomingPackets()
        observeDiscoveredRooms()
        observeConnectionErrors()
    }

    private fun observeIncomingPackets() {
        viewModelScope.launch {
            networkManager.incomingPackets.collect { packet ->
                handlePacket(packet)
            }
        }
    }

    private fun observeDiscoveredRooms() {
        viewModelScope.launch {
            discovery.discoveredRooms.collect { room ->
                _uiState.update { state ->
                    val updated = state.availableRooms.filter { it.hostIp != room.hostIp } + room
                    state.copy(availableRooms = updated)
                }
            }
        }
    }

    private fun observeConnectionErrors() {
        viewModelScope.launch {
            networkManager.connectionErrors.collect { err ->
                _uiState.update { it.copy(connectionStatus = err, bannerMessage = err) }
            }
        }
    }

    // --- HOME ACTIONS ---

    fun setPlayerName(name: String) {
        val clean = name.trim().take(15)
        _uiState.update { state ->
            val updatedLocal = state.localPlayer.copy(name = if (clean.isNotEmpty()) clean else "Jugador")
            val updatedPlayers = state.players.map { if (it.id == updatedLocal.id) updatedLocal else it }
            state.copy(localPlayer = updatedLocal, players = updatedPlayers)
        }
    }

    fun setPlayerColor(index: Int) {
        _uiState.update { state ->
            val updatedLocal = state.localPlayer.copy(colorIndex = index)
            val updatedPlayers = state.players.map { if (it.id == updatedLocal.id) updatedLocal else it }
            state.copy(localPlayer = updatedLocal, players = updatedPlayers)
        }
    }

    fun hostGame() {
        val code = discovery.generateRoomCode()
        val localIp = discovery.getLocalIpAddress()
        val hostPlayer = _uiState.value.localPlayer.copy(isHost = true, score = 0)

        _uiState.update {
            it.copy(
                isHost = true,
                isSoloOrBotsMode = false,
                roomCode = code,
                hostIp = localIp,
                localPlayer = hostPlayer,
                players = listOf(hostPlayer),
                currentScreen = ScreenState.LOBBY,
                currentRoundNumber = 1,
                bannerMessage = "Sala creada. Comparte el código $code"
            )
        }

        networkManager.startHost()
        discovery.startBroadcasting(code, hostPlayer.name, 1, _uiState.value.gameConfig.maxPlayers)
    }

    fun startSoloWithBots() {
        val hostPlayer = _uiState.value.localPlayer.copy(isHost = true, score = 0)
        val bot1 = Player(id = "bot_1", name = "Sofi (Bot)", colorIndex = 1, isHost = false, isBot = true)
        val bot2 = Player(id = "bot_2", name = "Juan (Bot)", colorIndex = 2, isHost = false, isBot = true)
        val bot3 = Player(id = "bot_3", name = "Carla (Bot)", colorIndex = 3, isHost = false, isBot = true)

        _uiState.update {
            it.copy(
                isHost = true,
                isSoloOrBotsMode = true,
                roomCode = "SOLO-BOTS",
                hostIp = "127.0.0.1",
                localPlayer = hostPlayer,
                players = listOf(hostPlayer, bot1, bot2, bot3),
                currentScreen = ScreenState.LOBBY,
                currentRoundNumber = 1,
                bannerMessage = "Modo con Bots iniciado. ¡Listo para jugar!"
            )
        }
    }

    fun startDiscovery() {
        _uiState.update { it.copy(isDiscovering = true, availableRooms = emptyList()) }
        discovery.startListening()
    }

    fun stopDiscovery() {
        _uiState.update { it.copy(isDiscovering = false) }
        discovery.stopListening()
    }

    fun joinByRoom(room: DiscoveredRoom) {
        joinByIp(room.hostIp, room.port)
    }

    fun joinByIp(ip: String, port: Int = NetworkDiscovery.DEFAULT_GAME_PORT) {
        val cleanIp = ip.trim()
        if (cleanIp.isEmpty()) {
            _uiState.update { it.copy(bannerMessage = "Ingresa una dirección IP válida") }
            return
        }

        _uiState.update {
            it.copy(
                isHost = false,
                isSoloOrBotsMode = false,
                hostIp = cleanIp,
                connectionStatus = "Conectando a $cleanIp..."
            )
        }

        networkManager.connectToHost(cleanIp, port) { success ->
            if (success) {
                val joinReq = NetworkPacket.createJoinRequest(_uiState.value.localPlayer.copy(isHost = false))
                networkManager.sendToHost(joinReq)
                _uiState.update {
                    it.copy(
                        currentScreen = ScreenState.LOBBY,
                        connectionStatus = "Conectado a la sala",
                        bannerMessage = "¡Te has unido a la sala!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(connectionStatus = "No se pudo conectar a $cleanIp", bannerMessage = "Error de conexión")
                }
            }
        }
    }

    // --- LOBBY ACTIONS ---

    fun toggleCategory(cat: String) {
        if (!_uiState.value.isHost) return
        val current = _uiState.value.gameConfig.categories.toMutableList()
        if (current.contains(cat)) {
            if (current.size > 2) {
                current.remove(cat)
            } else {
                _uiState.update { it.copy(bannerMessage = "Mínimo 2 categorías requeridas") }
                return
            }
        } else {
            if (current.size < 8) {
                current.add(cat)
            } else {
                _uiState.update { it.copy(bannerMessage = "Máximo 8 categorías") }
                return
            }
        }
        val updatedConfig = _uiState.value.gameConfig.copy(categories = current)
        _uiState.update { it.copy(gameConfig = updatedConfig) }
        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(NetworkPacket.createConfigUpdate(updatedConfig))
        }
    }

    fun addCustomCategory(cat: String) {
        val trimmed = cat.trim().capitalizeWords()
        if (trimmed.isEmpty()) return
        if (_uiState.value.gameConfig.categories.contains(trimmed)) return

        val current = _uiState.value.gameConfig.categories.toMutableList()
        if (current.size >= 8) {
            _uiState.update { it.copy(bannerMessage = "Máximo 8 categorías permitidas") }
            return
        }
        current.add(trimmed)
        val updatedConfig = _uiState.value.gameConfig.copy(categories = current)
        _uiState.update { it.copy(gameConfig = updatedConfig) }
        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(NetworkPacket.createConfigUpdate(updatedConfig))
        }
    }

    fun addBotPlayer() {
        if (!_uiState.value.isHost) return
        val currentPlayers = _uiState.value.players
        if (currentPlayers.size >= _uiState.value.gameConfig.maxPlayers) {
            _uiState.update { it.copy(bannerMessage = "Máximo 6 jugadores por partida") }
            return
        }
        val botNames = listOf("Mati", "Sofi", "Juan", "Carla", "Leo", "Valen")
        val unusedName = botNames.firstOrNull { name -> currentPlayers.none { it.name.startsWith(name) } } ?: "Bot ${currentPlayers.size + 1}"
        val botColor = (currentPlayers.size) % 6
        val newBot = Player(
            id = "bot_${UUID.randomUUID().toString().take(6)}",
            name = "$unusedName (Bot)",
            colorIndex = botColor,
            isHost = false,
            isBot = true
        )
        val updated = currentPlayers + newBot
        _uiState.update { it.copy(players = updated) }
        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(NetworkPacket.createPlayerListUpdate(updated))
        }
    }

    fun removePlayer(playerId: String) {
        if (!_uiState.value.isHost) return
        val updated = _uiState.value.players.filter { it.id != playerId }
        _uiState.update { it.copy(players = updated) }
        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(NetworkPacket.createPlayerListUpdate(updated))
        }
    }

    fun setRoundDuration(secs: Int) {
        if (!_uiState.value.isHost) return
        val updatedConfig = _uiState.value.gameConfig.copy(roundDurationSecs = secs)
        _uiState.update { it.copy(gameConfig = updatedConfig) }
        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(NetworkPacket.createConfigUpdate(updatedConfig))
        }
    }

    fun setTotalRounds(rounds: Int) {
        if (!_uiState.value.isHost) return
        val updatedConfig = _uiState.value.gameConfig.copy(totalRounds = rounds)
        _uiState.update { it.copy(gameConfig = updatedConfig) }
        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(NetworkPacket.createConfigUpdate(updatedConfig))
        }
    }

    // --- GAME FLOW ---

    fun startGame() {
        if (!_uiState.value.isHost) return
        if (_uiState.value.players.size < 2 && !_uiState.value.isSoloOrBotsMode) {
            _uiState.update { it.copy(bannerMessage = "Se necesitan al menos 2 jugadores para iniciar") }
            return
        }

        // Reset all player scores
        val resetPlayers = _uiState.value.players.map { it.copy(score = 0) }
        _uiState.update { it.copy(players = resetPlayers, currentRoundNumber = 1) }

        startRouletteForRound(1)
    }

    private fun startRouletteForRound(roundNumber: Int) {
        val selectedLetter = GAME_LETTERS.random()
        _uiState.update {
            it.copy(
                currentRoundNumber = roundNumber,
                currentLetter = selectedLetter,
                currentScreen = ScreenState.LETTER_ROULETTE,
                isSpinning = true,
                playerInputs = emptyMap(),
                whoCalledStop = null,
                stopCountdown = 0,
                allRoundSubmissions = emptyList(),
                votingScores = emptyMap(),
                roundPointsEarned = emptyMap()
            )
        }

        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(NetworkPacket.createStartRoulette(selectedLetter, roundNumber))
        }

        rouletteJob?.cancel()
        rouletteJob = viewModelScope.launch {
            // Animate spinning letter
            var spinCount = 0
            while (spinCount < 18) {
                val randChar = GAME_LETTERS.random()
                _uiState.update { it.copy(spinningLetter = randChar) }
                sounds.playRouletteSpin()
                delay(80L + spinCount * 12L)
                spinCount++
            }
            _uiState.update { it.copy(spinningLetter = selectedLetter, isSpinning = false) }
            sounds.playRoundStart()
            delay(1200)

            // Transition to active round
            startRoundActive(selectedLetter, roundNumber)
        }
    }

    private fun startRoundActive(letter: Char, roundNumber: Int) {
        val duration = _uiState.value.gameConfig.roundDurationSecs
        _uiState.update {
            it.copy(
                currentScreen = ScreenState.ROUND_PLAYING,
                currentLetter = letter,
                currentRoundNumber = roundNumber,
                remainingTimeSeconds = duration,
                isTimerActive = true,
                playerInputs = emptyMap(),
                whoCalledStop = null,
                stopCountdown = 0
            )
        }

        if (!_uiState.value.isSoloOrBotsMode && _uiState.value.isHost) {
            networkManager.broadcastToClients(NetworkPacket.createStartRound(letter, roundNumber, duration))
        }

        startTimer(duration)
    }

    private fun startTimer(duration: Int) {
        roundTimerJob?.cancel()
        roundTimerJob = viewModelScope.launch {
            var timeLeft = duration
            while (timeLeft > 0 && isActive) {
                delay(1000)
                timeLeft--
                _uiState.update { it.copy(remainingTimeSeconds = timeLeft) }
                if (timeLeft <= 5 && timeLeft > 0) {
                    sounds.playTick()
                }
            }
            if (isActive && _uiState.value.whoCalledStop == null) {
                // Time ran out
                triggerStop(callerPlayerId = "system", callerPlayerName = "Tiempo Agotado")
            }
        }
    }

    fun onAnswerChanged(category: String, text: String) {
        val upper = text.trimStart()
        _uiState.update { state ->
            val updated = state.playerInputs.toMutableMap()
            updated[category] = upper
            state.copy(playerInputs = updated)
        }
    }

    fun callStop() {
        val player = _uiState.value.localPlayer
        // Ensure at least 1 field is filled before allowing STOP
        val filledCount = _uiState.value.playerInputs.count { it.value.trim().isNotEmpty() }
        if (filledCount == 0) {
            _uiState.update { it.copy(bannerMessage = "Debes completar al menos una categoría para cantar STOP") }
            return
        }

        triggerStop(player.id, player.name)

        if (!_uiState.value.isSoloOrBotsMode) {
            val packet = NetworkPacket.createStopCalled(player.id, player.name)
            if (_uiState.value.isHost) {
                networkManager.broadcastToClients(packet)
            } else {
                networkManager.sendToHost(packet)
            }
        }
    }

    private fun triggerStop(callerPlayerId: String, callerPlayerName: String) {
        if (_uiState.value.whoCalledStop != null) return // already stopped
        roundTimerJob?.cancel()
        sounds.playStopBuzzer()

        _uiState.update {
            it.copy(
                whoCalledStop = callerPlayerName,
                isTimerActive = false,
                stopCountdown = 3,
                bannerMessage = "¡$callerPlayerName CANTÓ STOP!"
            )
        }

        stopCountdownJob?.cancel()
        stopCountdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(stopCountdown = i) }
                sounds.playTick()
                delay(1000)
            }
            _uiState.update { it.copy(stopCountdown = 0) }

            // Submit local answers
            finalizeAndSubmitAnswers()
        }
    }

    private fun finalizeAndSubmitAnswers() {
        val local = _uiState.value.localPlayer
        val myAnswers = PlayerAnswers(
            playerId = local.id,
            playerName = local.name,
            answers = _uiState.value.playerInputs
        )

        if (_uiState.value.isSoloOrBotsMode) {
            // Generate bot answers
            val botSubmissions = _uiState.value.players.filter { it.isBot }.map { bot ->
                generateBotAnswers(bot, _uiState.value.currentLetter, _uiState.value.gameConfig.categories)
            }
            val all = listOf(myAnswers) + botSubmissions
            processAllAnswers(all)
        } else {
            if (_uiState.value.isHost) {
                // Host collects own and waits for clients
                val existing = _uiState.value.allRoundSubmissions.filter { it.playerId != local.id }
                val updated = existing + myAnswers
                _uiState.update { it.copy(allRoundSubmissions = updated) }
                checkIfAllAnswersReceived(updated)
            } else {
                // Client sends answers to host
                networkManager.sendToHost(NetworkPacket.createSubmitAnswers(myAnswers))
            }
        }
    }

    private fun generateBotAnswers(bot: Player, letter: Char, categories: List<String>): PlayerAnswers {
        val answers = mutableMapOf<String, String>()
        val l = letter.uppercaseChar()
        categories.forEach { cat ->
            // 85% chance bot fills answer
            if ((1..100).random() <= 85) {
                val words = getSampleWords(cat, l)
                if (words.isNotEmpty()) {
                    answers[cat] = words.random()
                } else {
                    answers[cat] = "$l${cat.take(4).lowercase()}"
                }
            } else {
                answers[cat] = ""
            }
        }
        return PlayerAnswers(bot.id, bot.name, answers)
    }

    private fun checkIfAllAnswersReceived(submissions: List<PlayerAnswers>) {
        val requiredCount = _uiState.value.players.size
        if (submissions.size >= requiredCount) {
            processAllAnswers(submissions)
        } else {
            // Auto timeout after 4 seconds to not block
            viewModelScope.launch {
                delay(3500)
                if (_uiState.value.currentScreen == ScreenState.ROUND_PLAYING) {
                    processAllAnswers(_uiState.value.allRoundSubmissions)
                }
            }
        }
    }

    private fun processAllAnswers(submissions: List<PlayerAnswers>) {
        val letter = _uiState.value.currentLetter.uppercaseChar()
        val categories = _uiState.value.gameConfig.categories

        // Automatic scoring suggestions:
        // 1. If empty or doesn't start with letter -> INVALID (0)
        // 2. Count occurrences of trimmed normalized words per category
        // 3. If word is unique among valid answers -> UNIQUE (100)
        // 4. If word is repeated -> REPEATED (50)
        val votes = mutableMapOf<String, MutableMap<String, AnswerScoreType>>()

        categories.forEach { cat ->
            val validAnswersForCat = mutableMapOf<String, MutableList<String>>() // normalized word -> list of playerIds

            submissions.forEach { sub ->
                val word = sub.answers[cat]?.trim() ?: ""
                val normalized = word.lowercase().removeAccents()
                if (word.isNotEmpty() && normalized.first().uppercaseChar() == letter) {
                    validAnswersForCat.getOrPut(normalized) { mutableListOf() }.add(sub.playerId)
                }
            }

            submissions.forEach { sub ->
                val playerMap = votes.getOrPut(sub.playerId) { mutableMapOf() }
                val word = sub.answers[cat]?.trim() ?: ""
                val normalized = word.lowercase().removeAccents()

                if (word.isEmpty() || normalized.isEmpty() || normalized.first().uppercaseChar() != letter) {
                    playerMap[cat] = AnswerScoreType.INVALID
                } else {
                    val count = validAnswersForCat[normalized]?.size ?: 0
                    if (count > 1) {
                        playerMap[cat] = AnswerScoreType.REPEATED
                    } else {
                        playerMap[cat] = AnswerScoreType.UNIQUE
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                allRoundSubmissions = submissions,
                votingScores = votes,
                currentScreen = ScreenState.REVIEW_VOTING,
                bannerMessage = "Revisa y califica las respuestas de cada jugador"
            )
        }

        if (!_uiState.value.isSoloOrBotsMode && _uiState.value.isHost) {
            networkManager.broadcastToClients(NetworkPacket.createAllAnswersSync(submissions))
            broadcastScoresUpdate(votes)
        }
    }

    fun setAnswerVote(playerId: String, category: String, scoreType: AnswerScoreType) {
        val currentVotes = _uiState.value.votingScores.toMutableMap()
        val playerVotes = currentVotes[playerId]?.toMutableMap() ?: mutableMapOf()
        playerVotes[category] = scoreType
        currentVotes[playerId] = playerVotes

        _uiState.update { it.copy(votingScores = currentVotes) }
        sounds.playScoreSuccess()

        if (!_uiState.value.isSoloOrBotsMode && _uiState.value.isHost) {
            broadcastScoresUpdate(currentVotes)
        }
    }

    private fun broadcastScoresUpdate(votes: Map<String, Map<String, AnswerScoreType>>) {
        val root = JSONObject()
        votes.forEach { (pId, catMap) ->
            val pObj = JSONObject()
            catMap.forEach { (cat, st) -> pObj.put(cat, st.name) }
            root.put(pId, pObj)
        }
        networkManager.broadcastToClients(NetworkPacket.createVoteUpdate(root))
    }

    fun finishVotingAndShowSummary() {
        if (!_uiState.value.isHost) return

        val votes = _uiState.value.votingScores
        val roundPoints = mutableMapOf<String, Int>()

        val updatedPlayers = _uiState.value.players.map { player ->
            val playerVotes = votes[player.id] ?: emptyMap()
            val roundTotal = playerVotes.values.sumOf { it.points }
            roundPoints[player.id] = roundTotal
            player.copy(score = player.score + roundTotal)
        }

        val isFinal = _uiState.value.currentRoundNumber >= _uiState.value.gameConfig.totalRounds
        val nextScreen = if (isFinal) ScreenState.GAME_OVER else ScreenState.ROUND_SUMMARY

        _uiState.update {
            it.copy(
                players = updatedPlayers,
                roundPointsEarned = roundPoints,
                currentScreen = nextScreen
            )
        }

        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(
                NetworkPacket.createRoundResults(updatedPlayers, _uiState.value.currentRoundNumber, isFinal)
            )
        }
    }

    fun nextRound() {
        if (!_uiState.value.isHost) return
        val nextRoundNum = _uiState.value.currentRoundNumber + 1
        if (nextRoundNum <= _uiState.value.gameConfig.totalRounds) {
            startRouletteForRound(nextRoundNum)
        } else {
            _uiState.update { it.copy(currentScreen = ScreenState.GAME_OVER) }
        }
    }

    fun restartGame() {
        if (!_uiState.value.isHost) return
        val resetPlayers = _uiState.value.players.map { it.copy(score = 0) }
        _uiState.update {
            it.copy(
                players = resetPlayers,
                currentRoundNumber = 1,
                currentScreen = ScreenState.LOBBY,
                roundPointsEarned = emptyMap(),
                allRoundSubmissions = emptyList(),
                votingScores = emptyMap()
            )
        }

        if (!_uiState.value.isSoloOrBotsMode) {
            networkManager.broadcastToClients(NetworkPacket(PacketType.RESTART_GAME))
        }
    }

    fun returnToHome() {
        discovery.stopBroadcasting()
        discovery.stopListening()
        networkManager.stopAll()
        roundTimerJob?.cancel()
        rouletteJob?.cancel()
        stopCountdownJob?.cancel()

        _uiState.update {
            it.copy(
                currentScreen = ScreenState.HOME,
                players = listOf(it.localPlayer),
                isHost = false,
                isSoloOrBotsMode = false,
                availableRooms = emptyList()
            )
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    // --- PACKET HANDLING ---

    private fun handlePacket(packet: NetworkPacket) {
        when (packet.type) {
            PacketType.JOIN_REQUEST -> {
                if (_uiState.value.isHost) {
                    val pJson = packet.payload.getJSONObject("player")
                    val newPlayer = Player.fromJsonObject(pJson)
                    val current = _uiState.value.players.filter { it.id != newPlayer.id }
                    if (current.size < _uiState.value.gameConfig.maxPlayers) {
                        val updated = current + newPlayer
                        _uiState.update {
                            it.copy(
                                players = updated,
                                bannerMessage = "¡${newPlayer.name} se unió a la partida!"
                            )
                        }
                        networkManager.broadcastToClients(NetworkPacket.createPlayerListUpdate(updated))
                        networkManager.broadcastToClients(NetworkPacket.createConfigUpdate(_uiState.value.gameConfig))
                    }
                }
            }
            PacketType.PLAYER_LIST_UPDATE -> {
                val array = packet.payload.getJSONArray("players")
                val list = mutableListOf<Player>()
                for (i in 0 until array.length()) {
                    list.add(Player.fromJsonObject(array.getJSONObject(i)))
                }
                _uiState.update { state ->
                    val me = list.find { it.id == state.localPlayer.id } ?: state.localPlayer
                    state.copy(players = list, localPlayer = me)
                }
            }
            PacketType.CONFIG_UPDATE -> {
                val cfgJson = packet.payload.getJSONObject("config")
                val config = GameConfig.fromJsonObject(cfgJson)
                _uiState.update { it.copy(gameConfig = config) }
            }
            PacketType.START_LETTER_ROULETTE -> {
                val letter = packet.payload.getString("letter").first()
                val roundNum = packet.payload.getInt("roundNumber")
                _uiState.update {
                    it.copy(
                        currentRoundNumber = roundNum,
                        currentLetter = letter,
                        currentScreen = ScreenState.LETTER_ROULETTE,
                        isSpinning = true,
                        playerInputs = emptyMap(),
                        whoCalledStop = null,
                        stopCountdown = 0
                    )
                }
                sounds.playRoundStart()
            }
            PacketType.START_ROUND -> {
                val letter = packet.payload.getString("letter").first()
                val roundNum = packet.payload.getInt("roundNumber")
                val duration = packet.payload.getInt("durationSecs")
                startRoundActive(letter, roundNum)
            }
            PacketType.STOP_CALLED -> {
                val callerName = packet.payload.getString("callerName")
                val callerId = packet.payload.getString("callerId")
                triggerStop(callerId, callerName)
            }
            PacketType.SUBMIT_ANSWERS -> {
                if (_uiState.value.isHost) {
                    val ansJson = packet.payload.getJSONObject("answers")
                    val sub = PlayerAnswers.fromJsonObject(ansJson)
                    val existing = _uiState.value.allRoundSubmissions.filter { it.playerId != sub.playerId }
                    val updated = existing + sub
                    _uiState.update { it.copy(allRoundSubmissions = updated) }
                    checkIfAllAnswersReceived(updated)
                }
            }
            PacketType.ALL_ANSWERS_SYNC -> {
                val array = packet.payload.getJSONArray("allAnswers")
                val list = mutableListOf<PlayerAnswers>()
                for (i in 0 until array.length()) {
                    list.add(PlayerAnswers.fromJsonObject(array.getJSONObject(i)))
                }
                _uiState.update {
                    it.copy(
                        allRoundSubmissions = list,
                        currentScreen = ScreenState.REVIEW_VOTING
                    )
                }
            }
            PacketType.VOTE_UPDATE -> {
                val votes = mutableMapOf<String, MutableMap<String, AnswerScoreType>>()
                val keys = packet.payload.keys()
                while (keys.hasNext()) {
                    val pId = keys.next()
                    val pObj = packet.payload.getJSONObject(pId)
                    val catMap = mutableMapOf<String, AnswerScoreType>()
                    val catKeys = pObj.keys()
                    while (catKeys.hasNext()) {
                        val cat = catKeys.next()
                        val typeStr = pObj.getString(cat)
                        catMap[cat] = AnswerScoreType.valueOf(typeStr)
                    }
                    votes[pId] = catMap
                }
                _uiState.update { it.copy(votingScores = votes) }
            }
            PacketType.ROUND_RESULTS -> {
                val array = packet.payload.getJSONArray("players")
                val list = mutableListOf<Player>()
                for (i in 0 until array.length()) {
                    list.add(Player.fromJsonObject(array.getJSONObject(i)))
                }
                val isFinal = packet.payload.getBoolean("isGameOver")
                _uiState.update {
                    it.copy(
                        players = list,
                        currentScreen = if (isFinal) ScreenState.GAME_OVER else ScreenState.ROUND_SUMMARY
                    )
                }
            }
            PacketType.RESTART_GAME -> {
                _uiState.update {
                    it.copy(
                        currentRoundNumber = 1,
                        currentScreen = ScreenState.LOBBY
                    )
                }
            }
            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        discovery.stopBroadcasting()
        discovery.stopListening()
        networkManager.stopAll()
        sounds.release()
    }
}

// Helpers for Bot dictionary sample words
private fun getSampleWords(category: String, letter: Char): List<String> {
    val dict = mapOf(
        "Nombre" to mapOf(
            'A' to listOf("Alejandro", "Ana", "Andrés", "Alicia", "Antonio"),
            'B' to listOf("Bruno", "Beatriz", "Bernardo", "Belén", "Benjamín"),
            'C' to listOf("Carlos", "Camila", "Carolina", "Cristian", "Claudia"),
            'D' to listOf("Daniel", "Diana", "David", "Diego", "Daniela"),
            'E' to listOf("Eduardo", "Elena", "Esteban", "Emilia", "Enrique"),
            'F' to listOf("Fernando", "Florencia", "Felipe", "Fabiana", "Francisco"),
            'G' to listOf("Gabriel", "Gabriela", "Gonzalo", "Guillermo", "Gisela"),
            'H' to listOf("Héctor", "Hugo", "Hernán", "Helena", "Horacio"),
            'I' to listOf("Ignacio", "Isabel", "Iván", "Inés", "Irene"),
            'J' to listOf("Juan", "Jorge", "Javier", "Julia", "Joaquín"),
            'L' to listOf("Luis", "Lucía", "Lautaro", "Laura", "Lucas"),
            'M' to listOf("María", "Manuel", "Martín", "Mateo", "Mariana"),
            'N' to listOf("Nicolás", "Natalia", "Noelia", "Néstor", "Nadia"),
            'O' to listOf("Oscar", "Olivia", "Omar", "Osvaldo", "Orlando"),
            'P' to listOf("Pablo", "Paula", "Pedro", "Patricia", "Patricio"),
            'R' to listOf("Rodrigo", "Rosa", "Ricardo", "Ramiro", "Raquel"),
            'S' to listOf("Santiago", "Sofía", "Sebastián", "Silvia", "Sergio"),
            'T' to listOf("Tomás", "Teresa", "Thiago", "Tamara", "Tobías"),
            'U' to listOf("Ulises", "Uriel", "Úrsula", "Urbano"),
            'V' to listOf("Valentina", "Víctor", "Valeria", "Vicente", "Victoria"),
            'Z' to listOf("Zoe", "Zacarías", "Zulema", "Zoraida")
        ),
        "Animal" to mapOf(
            'A' to listOf("Águila", "Abeja", "Alacrán", "Antílope", "Araña"),
            'B' to listOf("Ballena", "Búho", "Búfalo", "Burro", "Babosa"),
            'C' to listOf("Caballo", "Cocodrilo", "Camaleón", "Conejo", "Canguro"),
            'D' to listOf("Delfín", "Dromedario", "Dingo", "Danta"),
            'E' to listOf("Elefante", "Erizo", "Escorpión", "Estrella de mar"),
            'F' to listOf("Flamenco", "Foca", "Faisán", "Ferret"),
            'G' to listOf("Gato", "Gorila", "Gaviota", "Guepardo", "Ganso"),
            'H' to listOf("Hiena", "Hipopótamo", "Hormiga", "Halconcillo"),
            'I' to listOf("Iguana", "Impala", "Insecto"),
            'J' to listOf("Jirafa", "Jaguar", "Jabalí", "Jilguero"),
            'L' to listOf("León", "Lobo", "Leopardo", "Loro", "Llama"),
            'M' to listOf("Mono", "Murciélago", "Morsa", "Mosquito", "Medusa"),
            'N' to listOf("Nutria", "Narval", "Ñandú", "Novillo"),
            'O' to listOf("Oso", "Oveja", "Otorongo", "Orangután", "Ostra"),
            'P' to listOf("Perro", "Pantera", "Pingüino", "Pato", "Puma"),
            'R' to listOf("Ratón", "Rinoceronte", "Rana", "Raya"),
            'S' to listOf("Serpiente", "Salamandra", "Sapo", "Suricata"),
            'T' to listOf("Tigre", "Tiburón", "Tortuga", "Tucán", "Toro"),
            'U' to listOf("Urraca", "Unicornio", "Urón"),
            'V' to listOf("Vaca", "Víbora", "Venado", "Vicuña"),
            'Z' to listOf("Zorro", "Zorrillo", "Zancudo")
        ),
        "País / Ciudad" to mapOf(
            'A' to listOf("Argentina", "Alemania", "Australia", "Ámsterdam", "Atenas"),
            'B' to listOf("Brasil", "Bolivia", "Bélgica", "Bogotá", "Buenos Aires"),
            'C' to listOf("Colombia", "Chile", "Canadá", "Caracas", "Copenhague"),
            'D' to listOf("Dinamarca", "Dublín", "Doha", "Dallas", "Dakar"),
            'E' to listOf("España", "Ecuador", "Egipto", "Edimburgo", "El Salvador"),
            'F' to listOf("Francia", "Finlandia", "Filipinas", "Florencia", "Frankfurt"),
            'G' to listOf("Grecia", "Guatemala", "Ginebra", "Granada", "Georgetown"),
            'H' to listOf("Honduras", "Hungría", "Haití", "Helsinki", "Holanda"),
            'I' to listOf("Italia", "Irlanda", "Islandia", "India", "Indonesia"),
            'J' to listOf("Japón", "Jamaica", "Jordania", "Johannesburgo", "Jerusalén"),
            'L' to listOf("Londres", "Lima", "Lisboa", "La Paz", "Luxemburgo"),
            'M' to listOf("México", "Madrid", "Miami", "Marruecos", "Mónaco"),
            'N' to listOf("Noruega", "Nueva York", "Nicaragua", "Nápoles", "Nueva Zelanda"),
            'O' to listOf("Oslo", "Ottawa", "Orlando", "Oporto", "Omán"),
            'P' to listOf("Perú", "París", "Portugal", "Panamá", "Praga"),
            'R' to listOf("Roma", "Rusia", "Rumania", "Río de Janeiro", "Róterdam"),
            'S' to listOf("Suiza", "Suecia", "Santiago", "Seúl", "Singapur"),
            'T' to listOf("Turquía", "Tokio", "Túnez", "Toronto", "Tailandia"),
            'U' to listOf("Uruguay", "Ucrania", "Uganda", "Ushuaia"),
            'V' to listOf("Venezuela", "Viena", "Valencia", "Venecia", "Vietnam"),
            'Z' to listOf("Zagreb", "Zúrich", "Zambia", "Zaragoza")
        ),
        "Fruta / Alimento" to mapOf(
            'A' to listOf("Arándano", "Aguacate", "Arroz", "Almendra", "Ananá"),
            'B' to listOf("Banana", "Berenjena", "Brócoli", "Bizcocho", "Batata"),
            'C' to listOf("Coco", "Cereza", "Ciruela", "Chocolate", "Carne"),
            'D' to listOf("Durazno", "Dátil", "Donut", "Damascena"),
            'E' to listOf("Espinaca", "Espárrago", "Empanada", "Elote", "Ensalada"),
            'F' to listOf("Fresa", "Frambuesa", "Frutilla", "Fideos", "Flan"),
            'G' to listOf("Guayaba", "Granada", "Galleta", "Garbanzo", "Gelatina"),
            'H' to listOf("Higo", "Huevo", "Hamburguesa", "Helado", "Hongos"),
            'I' to listOf("Icaco", "Ibérico", "Infusión"),
            'J' to listOf("Jamón", "Jengibre", "Jícama", "Jugo"),
            'L' to listOf("Limón", "Lima", "Lechuga", "Lentejas", "Leche"),
            'M' to listOf("Manzana", "Mango", "Melón", "Mandarina", "Mora"),
            'N' to listOf("Naranja", "Nuez", "Nectarina", "Nabo", "Nutella"),
            'O' to listOf("Oliva", "Ostras", "Orégano", "Orejón"),
            'P' to listOf("Plátano", "Pera", "Piña", "Pan", "Papa"),
            'R' to listOf("Rábano", "Remolacha", "Ravioles", "Romero"),
            'S' to listOf("Sandía", "Sopa", "Salchicha", "Sushi", "Salmón"),
            'T' to listOf("Tomate", "Toronja", "Tamarindo", "Taco", "Tarta"),
            'U' to listOf("Uva", "Uchuva"),
            'V' to listOf("Vainilla", "Verdura", "Vino", "Vinagre"),
            'Z' to listOf("Zanahoria", "Zapallo", "Zumo", "Zarzamora")
        ),
        "Cosa / Objeto" to mapOf(
            'A' to listOf("Anillo", "Armario", "Auto", "Avión", "Almohada"),
            'B' to listOf("Botella", "Bolígrafo", "Bicicleta", "Bolsa", "Balón"),
            'C' to listOf("Cama", "Cuchara", "Cuaderno", "Celular", "Computadora"),
            'D' to listOf("Dado", "Disco", "Diario", "Diamante", "Ducha"),
            'E' to listOf("Espejo", "Escoba", "Espada", "Escalera", "Estante"),
            'F' to listOf("Farol", "Foto", "Frasco", "Foco", "Flauta"),
            'G' to listOf("Gorra", "Guitarra", "Guante", "Goma", "Gafas"),
            'H' to listOf("Hacha", "Hueso", "Hilo", "Horno", "Herradura"),
            'I' to listOf("Imán", "Impresora", "Inodoro", "Interruptor"),
            'J' to listOf("Jarrita", "Jabón", "Jarrón", "Juguete", "Joyero"),
            'L' to listOf("Lámpara", "Lápiz", "Libro", "Llave", "Lente"),
            'M' to listOf("Mesa", "Maleta", "Microscopio", "Moneda", "Martillo"),
            'N' to listOf("Navaja", "Nave", "Neumático", "Notebook"),
            'O' to listOf("Olla", "Organizador", "Obelisco", "Órgano"),
            'P' to listOf("Papel", "Peine", "Puerta", "Paraguas", "Pelota"),
            'R' to listOf("Reloj", "Radio", "Regla", "Rueda", "Robot"),
            'S' to listOf("Silla", "Sombrero", "Sofá", "Sobre", "Sartén"),
            'T' to listOf("Taza", "Teléfono", "Teclado", "Tenedor", "Tijera"),
            'U' to listOf("Uniforme", "USB", "Urna", "Utensilio"),
            'V' to listOf("Vaso", "Ventana", "Vela", "Vestido", "Valija"),
            'Z' to listOf("Zapato", "Zapatilla", "Zueco")
        ),
        "Color" to mapOf(
            'A' to listOf("Azul", "Amarillo", "Ámbar", "Añil", "Arena"),
            'B' to listOf("Blanco", "Beige", "Bordo", "Bronce"),
            'C' to listOf("Celeste", "Carmesí", "Café", "Coral", "Cian"),
            'D' to listOf("Dorado", "Durazno"),
            'E' to listOf("Esmeralda", "Escarlata", "Ébano"),
            'F' to listOf("Fucsia"),
            'G' to listOf("Gris", "Granate"),
            'H' to listOf("Hueso", "Humo"),
            'I' to listOf("Índigo", "Ivory"),
            'J' to listOf("Jade", "Jazmín"),
            'L' to listOf("Lila", "Lavanda", "Lima"),
            'M' to listOf("Morado", "Marrón", "Magenta", "Mostaza"),
            'N' to listOf("Negro", "Naranja"),
            'O' to listOf("Ocre", "Oro", "Oliva"),
            'P' to listOf("Plateado", "Púrpura", "Plomo", "Pastel"),
            'R' to listOf("Rojo", "Rosa", "Rubí"),
            'S' to listOf("Salmón", "Sepia"),
            'T' to listOf("Turquesa", "Terracota", "Tabaco"),
            'U' to listOf("Ultramar"),
            'V' to listOf("Verde", "Violeta", "Vainilla"),
            'Z' to listOf("Zafiro")
        )
    )
    return dict[category]?.get(letter) ?: emptyList()
}

private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

private fun String.removeAccents(): String {
    val accents = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u',
        'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ü' to 'U'
    )
    val sb = StringBuilder()
    for (c in this) {
        sb.append(accents[c] ?: c)
    }
    return sb.toString()
}
