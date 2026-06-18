package com.example.ui.shared

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex

    private var sentences = emptyList<String>()
    private var utteranceSpeed = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            _isInitialized.value = true
            setupProgressListener()
        } else {
            Log.e("TtsManager", "TTS Initialization failed!")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                val index = utteranceId?.toIntOrNull() ?: 0
                _currentSentenceIndex.value = index
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.toIntOrNull() ?: 0
                val nextIndex = index + 1
                if (nextIndex < sentences.size) {
                    speakSentence(nextIndex)
                } else {
                    _isPlaying.value = false
                    _currentSentenceIndex.value = 0
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isPlaying.value = false
                Log.e("TtsManager", "TTS error code: $errorCode")
            }
        })
    }

    fun playText(text: String, startSentenceIdx: Int = 0) {
        if (!_isInitialized.value) return
        
        // Split text into readable sentences
        sentences = text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) return

        stop()
        _isPlaying.value = true
        speakSentence(startSentenceIdx)
    }

    private fun speakSentence(index: Int) {
        if (index >= sentences.size || tts == null) {
            _isPlaying.value = false
            return
        }
        _currentSentenceIndex.value = index
        tts?.setSpeechRate(utteranceSpeed)
        
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        }
        tts?.speak(sentences[index], TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun pause() {
        if (isPlaying.value) {
            tts?.stop()
            _isPlaying.value = false
        }
    }

    fun resume() {
        if (!isPlaying.value && sentences.isNotEmpty()) {
            _isPlaying.value = true
            speakSentence(_currentSentenceIndex.value)
        }
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentSentenceIndex.value = 0
    }

    fun setSpeed(speed: Float) {
        utteranceSpeed = speed
        if (isPlaying.value && sentences.isNotEmpty()) {
            speakSentence(_currentSentenceIndex.value)
        }
    }

    fun getSentenceCount(): Int = sentences.size
    fun getSentences(): List<String> = sentences

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
