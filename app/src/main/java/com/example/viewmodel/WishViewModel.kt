package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WishDatabase
import com.example.data.WishEntity
import com.example.data.WishRepository
import com.example.receiver.WishNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class WishViewModel(
    application: Application,
    private val repository: WishRepository
) : AndroidViewModel(application) {

    val allWishes: StateFlow<List<WishEntity>> = repository.allWishes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeWish: StateFlow<WishEntity?> = repository.activeWish
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _remainingTimeMs = MutableStateFlow<Long>(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()

    private val _isCreepyMode = MutableStateFlow(false)
    val isCreepyMode: StateFlow<Boolean> = _isCreepyMode.asStateFlow()

    private val _spookyMessage = MutableStateFlow("Whisper your desire to Girigo...")
    val spookyMessage: StateFlow<String> = _spookyMessage.asStateFlow()

    private val _heartbeatRateMs = MutableStateFlow(1000L)
    val heartbeatRateMs: StateFlow<Long> = _heartbeatRateMs.asStateFlow()

    private val _hasJustExpired = MutableStateFlow(false)
    val hasJustExpired: StateFlow<Boolean> = _hasJustExpired.asStateFlow()

    private var timerJob: Job? = null
    private var pulseJob: Job? = null
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private val spookyQuotes = listOf(
        "The pact cannot be undone...",
        "They are watching you.",
        "Girigo is waiting in the dark.",
        "The price must be paid.",
        "Tick tock... your time is fading.",
        "A wish made in blood is sealed in stone.",
        "Did you think you could escape?",
        "The dark waters of the well are rising...",
        "Can you hear the whispers?",
        "Do not close your eyes."
    )

    init {
        viewModelScope.launch {
            activeWish.collect { wish ->
                if (wish != null) {
                    _hasJustExpired.value = false
                    startTimer(wish)
                } else {
                    stopTimer()
                    _remainingTimeMs.value = 0L
                    _isCreepyMode.value = false
                    _spookyMessage.value = "Whisper your desire to Girigo..."
                }
            }
        }
    }

    private fun startTimer(wish: WishEntity) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val timeLeft = wish.expirationTime - now

                if (timeLeft <= 0L) {
                    _remainingTimeMs.value = 0L
                    _isCreepyMode.value = false
                    _hasJustExpired.value = true
                    completeWish(wish)
                    break
                } else {
                    _remainingTimeMs.value = timeLeft
                    val isCreepy = timeLeft <= 5 * 60 * 1000L // 5 minutes
                    _isCreepyMode.value = isCreepy

                    if (isCreepy) {
                        // Dynamically scale heartbeat speed based on time remaining
                        // 5 min (300s) -> 1.5s heartbeat, 10s -> 300ms heartbeat
                        val progress = timeLeft.toDouble() / (5 * 60 * 1000L)
                        val rate = (300 + (progress * 1200)).toLong().coerceIn(200, 1500)
                        _heartbeatRateMs.value = rate

                        // Randomly change spooky quotes
                        if (Random.nextInt(15) == 0) {
                            _spookyMessage.value = spookyQuotes.random()
                        }
                    } else {
                        _spookyMessage.value = "Your wish is granted. But the countdown continues..."
                    }
                }
                delay(500)
            }
        }

        // Start haptic heartbeat if in creepy mode
        pulseJob?.cancel()
        pulseJob = viewModelScope.launch {
            while (true) {
                if (_isCreepyMode.value) {
                    try {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(60)
                    } catch (e: Exception) {
                        Log.e("WishViewModel", "Vibrate failed", e)
                    }
                    delay(_heartbeatRateMs.value)
                } else {
                    delay(2000)
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        pulseJob?.cancel()
    }

    private suspend fun completeWish(wish: WishEntity) {
        val updated = wish.copy(isCompleted = true)
        repository.updateWish(updated)
        WishNotificationHelper.scheduleNotification(
            getApplication(),
            wish.wishText,
            System.currentTimeMillis(),
            "COMPLETED"
        )
    }

    fun makeWish(wishText: String) {
        viewModelScope.launch {
            // Cancel previous active wish if any
            activeWish.value?.let { oldWish ->
                repository.updateWish(oldWish.copy(isCompleted = true))
            }

            val now = System.currentTimeMillis()
            val duration = 24 * 60 * 60 * 1000L // 24 hours
            val expiration = now + duration

            val newWish = WishEntity(
                wishText = wishText,
                creationTime = now,
                expirationTime = expiration,
                isCompleted = false
            )

            repository.insertWish(newWish)

            // Trigger immediate local notification
            WishNotificationHelper.showInstantNotification(getApplication(), wishText)

            // Schedule the alarm for 5 minutes before expiration (23 hours and 55 minutes later)
            val fiveMinutesBefore = expiration - (5 * 60 * 1000L)
            if (fiveMinutesBefore > now) {
                WishNotificationHelper.scheduleNotification(
                    getApplication(),
                    wishText,
                    fiveMinutesBefore,
                    "FIVE_MINUTES"
                )
            }

            // Schedule the alarm for completion
            WishNotificationHelper.scheduleNotification(
                getApplication(),
                wishText,
                expiration,
                "COMPLETED"
            )
        }
    }

    fun simulateTimeRemaining(option: String) {
        val currentActive = activeWish.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newExpiration = when (option) {
                "STANDARD" -> now + (24 * 60 * 60 * 1000L)
                "TEN_MINUTES" -> now + (10 * 60 * 1000L)
                "FIVE_MINUTES_TEN_SECONDS" -> now + (5 * 60 * 1000L + 10 * 1000L)
                "TEN_SECONDS" -> now + (10 * 1000L)
                else -> currentActive.expirationTime
            }

            val updated = currentActive.copy(expirationTime = newExpiration)
            repository.updateWish(updated)

            // Reschedule alarms for simulation
            WishNotificationHelper.cancelAllScheduled(getApplication())

            val fiveMinutesBefore = newExpiration - (5 * 60 * 1000L)
            if (fiveMinutesBefore > now) {
                WishNotificationHelper.scheduleNotification(
                    getApplication(),
                    currentActive.wishText,
                    fiveMinutesBefore,
                    "FIVE_MINUTES"
                )
            }

            WishNotificationHelper.scheduleNotification(
                getApplication(),
                currentActive.wishText,
                newExpiration,
                "COMPLETED"
            )
        }
    }

    fun breakPact() {
        viewModelScope.launch {
            activeWish.value?.let { wish ->
                val updated = wish.copy(isCompleted = true)
                repository.updateWish(updated)
                WishNotificationHelper.cancelAllScheduled(getApplication())
                _spookyMessage.value = "The pact was severed... for now."
            }
        }
    }

    fun dismissExpirationScreen() {
        _hasJustExpired.value = false
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            WishNotificationHelper.cancelAllScheduled(getApplication())
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

class WishViewModelFactory(
    private val application: Application,
    private val repository: WishRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WishViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WishViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
