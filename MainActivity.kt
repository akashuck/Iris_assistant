package com.iris.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusView: TextView
    private var isRunning = false
    private val client = OkHttpClient()
    private val openRouterApiKey = "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"
    private val modelId = "nvidia/nemotron-3-nano-30b-a3b:free"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)

        startButton = findViewById(R.id.startBtn)
        stopButton = findViewById(R.id.stopBtn)
        statusView = findViewById(R.id.statusView)

        startButton.setOnClickListener {
            isRunning = true
            statusView.text = "Iris Assistant Started"
            speak("Iris Assistant Started")
        }

        stopButton.setOnClickListener {
            isRunning = false
            statusView.text = "Iris Assistant Stopped"
            speak("Iris Assistant Stopped")
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
    }

    fun performTaskWithOpenRouter(prompt: String) {
        val json = JSONObject()
        json.put("model", modelId)
        json.put("input", prompt)

        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://openrouter.ai/v1/completions")
            .addHeader("Authorization", "Bearer $openRouterApiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { speak("Failed to process request") }
            }
            override fun onResponse(call: Call, response: Response) {
                response.body()?.string()?.let {
                    val resJson = JSONObject(it)
                    val text = resJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getString("text")
                    runOnUiThread { speak(text) }
                }
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}