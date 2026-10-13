package com.yadav.ai

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(5, 12, 28)
    private val panel = Color.rgb(12, 26, 49)
    private val cyan = Color.rgb(48, 210, 255)
    private val white = Color.WHITE
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var response: TextView
    private lateinit var talkButton: Button
    private lateinit var wakeButton: Button
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var wakeEnabled = false
    private var waitingForCommandAfterWake = false
    private var listening = false

    // Replace these placeholders after deploying the Node backend.
    // Never put your actual AI provider API key in this Android app.
    private val backendChatUrl = "https://YOUR-BACKEND.example.com/chat"
    private val appToken = "CHANGE_ME_APP_TOKEN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
        tts = TextToSpeech(this, this)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            status.text = "SYSTEM READY • TAP TALK OR ENABLE WAKE WORD"
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(20))
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val brand = TextView(this).apply {
            text = "YADAV AI"
            textSize = 28f
            letterSpacing = 0.18f
            setTextColor(cyan)
            gravity = Gravity.CENTER
        }
        content.addView(brand, matchWrap())

        val subtitle = TextView(this).apply {
            text = "PERSONAL VOICE ASSISTANT"
            textSize = 11f
            letterSpacing = 0.16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        content.addView(subtitle, matchWrap())

        val orb = TextView(this).apply {
            text = "◉"
            textSize = 100f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, dp(12))
        }
        content.addView(orb, matchWrap())

        status = TextView(this).apply {
            text = "INITIALIZING..."
            textSize = 12f
            letterSpacing = 0.08f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(22))
        }
        content.addView(status, matchWrap())

        transcript = cardText("YOU: Ready for your command.")
        content.addView(transcript, matchWrap())

        response = cardText("JARVIS: Hello. I am Yadav AI. Tap TALK to begin.")
        val responseParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) }
        content.addView(response, responseParams)

        talkButton = makeButton("🎙  TALK", cyan, bg)
        talkButton.setOnClickListener { startManualListening() }
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
        ).apply { topMargin = dp(22) }
        content.addView(talkButton, buttonParams)

        wakeButton = makeButton("◉  WAKE WORD: OFF", panel, cyan)
        wakeButton.setOnClickListener { toggleWakeWord() }
        val wakeParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
        ).apply { topMargin = dp(10) }
        content.addView(wakeButton, wakeParams)

        val hint = TextView(this).apply {
            text = "Try: “What time is it?” • “Open YouTube” • “Hey Jarvis, tell me a fun fact”"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(8))
        }
        content.addView(hint, matchWrap())

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun cardText(initial: String) = TextView(this).apply {
        text = initial
        textSize = 15f
        setTextColor(white)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundColor(panel)
    }

    private fun makeButton(label: String, color: Int, textColor: Int) = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        setTextColor(textColor)
        setBackgroundColor(color)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.language = Locale.US
        }
    }

    private fun ensureRecognizer(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            sayStatus("Speech recognition service unavailable")
            speak("Speech recognition is not available on this device.")
            return false
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    sayStatus(if (wakeEnabled) "LISTENING FOR HEY JARVIS" else "LISTENING...")
                }
                override fun onBeginningOfSpeech() { sayStatus("HEARING YOU...") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false; sayStatus("PROCESSING...") }
                override fun onError(error: Int) {
                    listening = false
                    if (wakeEnabled) scheduleWakeListen(900)
                    else sayStatus("READY • TAP TALK TO TRY AGAIN")
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val heard = matches?.firstOrNull().orEmpty().trim()
                    if (heard.isBlank()) {
                        if (wakeEnabled) scheduleWakeListen(500)
                        else sayStatus("NO SPEECH DETECTED")
                        return
                    }
                    transcript.text = "YOU: $heard"
                    val lower = heard.lowercase(Locale.ROOT)
                    if (wakeEnabled) {
                        val wakeAt = lower.indexOf("hey jarvis").takeIf { it >= 0 }
                            ?: lower.indexOf("hey yadav").takeIf { it >= 0 }
                        if (!waitingForCommandAfterWake && wakeAt == null) {
                            scheduleWakeListen(400)
                            return
                        }
                        if (wakeAt != null) {
                            val phraseLength = if (lower.substring(wakeAt).startsWith("hey jarvis")) 10 else 9
                            val tail = heard.substring(wakeAt + phraseLength).trim(' ', ',', '.', '!')
                            if (tail.isBlank()) {
                                waitingForCommandAfterWake = true
                                speak("Yes, I am listening.") {
                                    scheduleWakeListen(250)
                                }
                                return
                            }
                            waitingForCommandAfterWake = false
                            handleCommand(tail)
                            return
                        }
                        if (waitingForCommandAfterWake) {
                            waitingForCommandAfterWake = false
                            handleCommand(heard)
                            return
                        }
                    }
                    handleCommand(heard)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        return true
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }
        if (!ensureRecognizer()) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            listening = false
            sayStatus("COULD NOT START MICROPHONE")
            if (wakeEnabled) scheduleWakeListen(1200)
        }
    }

    private fun startManualListening() {
        wakeEnabled = false
        waitingForCommandAfterWake = false
        wakeButton.text = "◉  WAKE WORD: OFF"
        if (ensureRecognizer()) startListening()
    }

    private fun toggleWakeWord() {
        wakeEnabled = !wakeEnabled
        waitingForCommandAfterWake = false
        wakeButton.text = if (wakeEnabled) "◉  WAKE WORD: ON" else "◉  WAKE WORD: OFF"
        if (wakeEnabled) {
            if (ensureRecognizer()) {
                speak("Wake phrase enabled. Say Hey Jarvis.") {
                    scheduleWakeListen(300)
                }
            }
        } else {
            try { recognizer?.cancel() } catch (_: Exception) {}
            sayStatus("WAKE WORD OFF")
        }
    }

    private fun scheduleWakeListen(delay: Long) {
        if (!wakeEnabled || isFinishing) return
        handler.postDelayed({
            if (wakeEnabled && !isFinishing && !listening) startListening()
        }, delay)
    }

    private fun handleCommand(command: String) {
        val c = command.lowercase(Locale.ROOT).trim()
        when {
            c.contains("what time") || c == "time" || c.contains("current time") -> {
                val now = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                respond("The time is $now.")
            }
            c.contains("open youtube") || c == "youtube" -> openApp("com.google.android.youtube", "YouTube")
            c.contains("open whatsapp") || c == "whatsapp" -> openApp("com.whatsapp", "WhatsApp")
            c.contains("open chrome") || c == "chrome" || c.contains("open browser") -> openApp("com.android.chrome", "browser")
            c.contains("open settings") || c == "settings" -> {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    respond("Opening Settings.")
                } catch (_: Exception) { respond("I could not open Settings.") }
            }
            c == "stop listening" || c == "turn off wake word" -> {
                wakeEnabled = false
                wakeButton.text = "◉  WAKE WORD: OFF"
                try { recognizer?.cancel() } catch (_: Exception) {}
                respond("Wake word turned off.")
            }
            else -> askAi(command)
        }
    }

    private fun openApp(packageName: String, label: String) {
        try {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                startActivity(launch)
                respond("Opening $label.")
            } else {
                respond("$label is not installed. You can install it from your app store.")
            }
        } catch (_: ActivityNotFoundException) {
            respond("I could not open $label.")
        } catch (_: Exception) {
            respond("I could not open $label.")
        }
    }

    private fun askAi(prompt: String) {
        if (backendChatUrl.contains("YOUR-BACKEND") || appToken == "CHANGE_ME_APP_TOKEN") {
            respond("My AI backend is not connected yet. Deploy the backend and add its URL and app token in the Android code, then rebuild the APK.")
            return
        }
        sayStatus("ASKING AI...")
        response.text = "JARVIS: Thinking..."
        executor.execute {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(backendChatUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-App-Token", appToken)
                }
                val payload = JSONObject().put("message", prompt).toString()
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val body = stream.bufferedReader().use { it.readText() }
                if (conn.responseCode !in 200..299) throw Exception("Server returned ${conn.responseCode}")
                val answer = JSONObject(body).optString("reply", "I did not receive an answer.")
                runOnUiThread { respond(answer) }
            } catch (e: Exception) {
                runOnUiThread { respond("I could not reach my AI service. Check the internet connection and backend settings.") }
            } finally {
                conn?.disconnect()
            }
        }
    }

    private fun respond(message: String) {
        response.text = "JARVIS: $message"
        sayStatus("RESPONSE READY")
        speak(message) {
            if (wakeEnabled) scheduleWakeListen(300)
        }
    }

    private fun speak(message: String, onDone: (() -> Unit)? = null) {
        if (!ttsReady) {
            onDone?.invoke()
            return
        }
        if (onDone == null) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "yadav-${System.currentTimeMillis()}")
        } else {
            val utteranceId = "yadav-${System.currentTimeMillis()}"
            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId) handler.post { onDone.invoke() }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    if (id == utteranceId) handler.post { onDone.invoke() }
                }
            })
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun sayStatus(message: String) {
        if (::status.isInitialized) status.text = message.uppercase(Locale.ROOT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sayStatus("SYSTEM READY • TAP TALK OR ENABLE WAKE WORD")
        } else if (requestCode == 100) {
            sayStatus("MICROPHONE PERMISSION REQUIRED")
            Toast.makeText(this, "Allow microphone access to use voice commands.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        wakeEnabled = false
        handler.removeCallbacksAndMessages(null)
        try { recognizer?.cancel(); recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        executor.shutdownNow()
        super.onDestroy()
    }
}
