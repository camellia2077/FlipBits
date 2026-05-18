package com.bag.audioandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.bag.audioandroid.ui.AppTabDebugScenario
import com.bag.audioandroid.ui.AudioAndroidApp
import com.bag.audioandroid.ui.EncodeProgressDebugScenario
import com.bag.audioandroid.ui.FlashDebugScenario
import com.bag.audioandroid.ui.MiniDebugScenario
import com.bag.audioandroid.ui.SavedAudioDebugScenario
import com.bag.audioandroid.ui.SettingsImportDebugScenario

class MainActivity : AppCompatActivity() {
    private val debugScenarioState = mutableStateOf<FlashDebugScenario?>(null)
    private val miniDebugScenarioState = mutableStateOf<MiniDebugScenario?>(null)
    private val encodeProgressDebugScenarioState = mutableStateOf<EncodeProgressDebugScenario?>(null)
    private val savedAudioDebugScenarioState = mutableStateOf<SavedAudioDebugScenario?>(null)
    private val appTabDebugScenarioState = mutableStateOf<AppTabDebugScenario?>(null)
    private val settingsImportDebugScenarioState = mutableStateOf<SettingsImportDebugScenario?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debugScenarioState.value = FlashDebugScenario.fromIntent(intent)
        miniDebugScenarioState.value = MiniDebugScenario.fromIntent(intent)
        encodeProgressDebugScenarioState.value = EncodeProgressDebugScenario.fromIntent(intent)
        savedAudioDebugScenarioState.value = SavedAudioDebugScenario.fromIntent(intent)
        appTabDebugScenarioState.value = AppTabDebugScenario.fromIntent(intent)
        settingsImportDebugScenarioState.value = SettingsImportDebugScenario.fromIntent(intent)
        setContent {
            AudioAndroidApp(
                debugScenario = debugScenarioState.value,
                miniDebugScenario = miniDebugScenarioState.value,
                encodeProgressDebugScenario = encodeProgressDebugScenarioState.value,
                savedAudioDebugScenario = savedAudioDebugScenarioState.value,
                appTabDebugScenario = appTabDebugScenarioState.value,
                settingsImportDebugScenario = settingsImportDebugScenarioState.value,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        debugScenarioState.value = FlashDebugScenario.fromIntent(intent)
        miniDebugScenarioState.value = MiniDebugScenario.fromIntent(intent)
        encodeProgressDebugScenarioState.value = EncodeProgressDebugScenario.fromIntent(intent)
        savedAudioDebugScenarioState.value = SavedAudioDebugScenario.fromIntent(intent)
        appTabDebugScenarioState.value = AppTabDebugScenario.fromIntent(intent)
        settingsImportDebugScenarioState.value = SettingsImportDebugScenario.fromIntent(intent)
    }
}
