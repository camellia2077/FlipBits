package com.bag.audioandroid.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.data.AndroidIntentAudioShareGateway
import com.bag.audioandroid.data.NativeAudioCodecGateway
import com.bag.audioandroid.data.NativeAudioIoGateway
import com.bag.audioandroid.data.AndroidSampleInputTextProvider
import com.bag.audioandroid.data.DefaultSavedAudioRepository
import com.bag.audioandroid.data.MediaStoreAudioExportGateway
import com.bag.audioandroid.data.MediaStoreSavedAudioLibraryGateway
import com.bag.audioandroid.data.NativePlaybackRuntimeGateway
import com.bag.audioandroid.data.PaletteSettingsRepository
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.model.PlaybackSequenceMode
import com.bag.audioandroid.ui.screen.AboutScreen
import com.bag.audioandroid.ui.screen.AudioTabScreen
import com.bag.audioandroid.ui.screen.ConfigTabScreen
import com.bag.audioandroid.ui.screen.LibraryTabScreen
import com.bag.audioandroid.ui.screen.OpenSourceLicensesScreen

@Composable
fun AudioAndroidApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val audioCodecGateway = remember { NativeAudioCodecGateway() }
    val audioIoGateway = remember { NativeAudioIoGateway() }
    val sampleInputTextProvider = remember(appContext) {
        AndroidSampleInputTextProvider(appContext)
    }
    val playbackRuntimeGateway = remember { NativePlaybackRuntimeGateway() }
    val audioExportGateway = remember(appContext, audioIoGateway) {
        MediaStoreAudioExportGateway(appContext, audioIoGateway)
    }
    val savedAudioLibraryGateway = remember(appContext, audioIoGateway) {
        MediaStoreSavedAudioLibraryGateway(appContext, audioIoGateway)
    }
    val audioShareGateway = remember(appContext) {
        AndroidIntentAudioShareGateway(appContext)
    }
    val savedAudioRepository = remember(audioExportGateway, savedAudioLibraryGateway, audioShareGateway) {
        DefaultSavedAudioRepository(
            audioExportGateway = audioExportGateway,
            savedAudioLibraryGateway = savedAudioLibraryGateway,
            audioShareGateway = audioShareGateway
        )
    }
    val paletteSettingsRepository = remember(appContext) {
        PaletteSettingsRepository(appContext)
    }
    val factory = remember(
        audioCodecGateway,
        sampleInputTextProvider,
        paletteSettingsRepository,
        playbackRuntimeGateway,
        savedAudioRepository
    ) {
        AudioAndroidViewModelFactory(
            audioCodecGateway,
            sampleInputTextProvider,
            paletteSettingsRepository,
            playbackRuntimeGateway,
            savedAudioRepository
        )
    }
    val viewModel: AudioAndroidViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSession = uiState.currentSession

    if (uiState.showLicensesPage) {
        MaterialTheme(colorScheme = uiState.selectedPalette.scheme) {
            OpenSourceLicensesScreen(onBack = viewModel::onCloseLicensesPage)
        }
        return
    }

    if (uiState.showAboutPage) {
        MaterialTheme(colorScheme = uiState.selectedPalette.scheme) {
            AboutScreen(
                onBack = viewModel::onCloseAboutPage,
                onOpenLicensesPage = viewModel::onOpenLicensesPage,
                presentationVersion = uiState.presentationVersion,
                coreVersion = uiState.coreVersion
            )
        }
        return
    }

    MaterialTheme(colorScheme = uiState.selectedPalette.scheme) {
        val navigationBarColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
            unselectedTextColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
        )
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    AppTab.entries.forEach { tab ->
                        val selected = tab == uiState.selectedTab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.onTabSelected(tab) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelResId)
                                )
                            },
                            label = { Text(stringResource(tab.labelResId)) },
                            colors = navigationBarColors
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (uiState.selectedTab) {
                AppTab.Config -> ConfigTabScreen(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = viewModel::onLanguageSelected,
                    selectedPalette = uiState.selectedPalette,
                    onPaletteSelected = viewModel::onPaletteSelected,
                    onOpenAboutPage = viewModel::onOpenAboutPage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )

                AppTab.Audio -> AudioTabScreen(
                    transportMode = uiState.transportMode,
                    onTransportModeSelected = viewModel::onTransportModeSelected,
                    inputText = currentSession.inputText,
                    onInputTextChange = viewModel::onInputTextChange,
                    resultText = currentSession.resultText,
                    statusText = currentSession.statusText,
                    playback = uiState.currentPlayback,
                    playbackSequenceMode = uiState.playbackSequenceMode,
                    playbackSampleCount = uiState.currentPlaybackSampleCount,
                    savedAudioItems = uiState.savedAudioItems,
                    showSavedAudioSheet = uiState.showSavedAudioSheet,
                    onEncode = viewModel::onEncode,
                    onTogglePlayback = viewModel::onTogglePlayback,
                    onSkipToPreviousTrack = viewModel::onSkipToPreviousTrack,
                    onSkipToNextTrack = viewModel::onSkipToNextTrack,
                    onPlaybackSequenceModeSelected = viewModel::onPlaybackSequenceModeSelected,
                    canSkipPrevious = uiState.canSkipPrevious,
                    canSkipNext = uiState.canSkipNext,
                    onScrubStarted = viewModel::onScrubStarted,
                    onScrubChanged = viewModel::onScrubChanged,
                    onScrubFinished = viewModel::onScrubFinished,
                    onDecode = viewModel::onDecode,
                    onClear = viewModel::onClear,
                    onClearResult = viewModel::onClearResult,
                    onExportAudio = viewModel::onExportAudio,
                    onShareSavedAudio = uiState.currentSavedAudioItem?.let {
                        viewModel::onShareCurrentSavedAudio
                    },
                    onOpenSavedAudioSheet = viewModel::onOpenSavedAudioSheet,
                    onCloseSavedAudioSheet = viewModel::onCloseSavedAudioSheet,
                    onSavedAudioSelected = viewModel::onSavedAudioSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )

                AppTab.Library -> LibraryTabScreen(
                    savedAudioItems = uiState.savedAudioItems,
                    librarySelection = uiState.librarySelection,
                    statusText = uiState.libraryStatusText,
                    onSelectSavedAudio = viewModel::onSavedAudioSelected,
                    onEnterLibrarySelection = viewModel::onEnterLibrarySelection,
                    onToggleLibrarySelection = viewModel::onToggleLibrarySelection,
                    onSelectAllLibraryItems = viewModel::onSelectAllLibraryItems,
                    onDeleteSelectedSavedAudio = viewModel::onDeleteSelectedSavedAudio,
                    onClearLibrarySelection = viewModel::onClearLibrarySelection,
                    onDeleteSavedAudio = viewModel::onDeleteSavedAudio,
                    onRenameSavedAudio = viewModel::onRenameSavedAudio,
                    onShareSavedAudio = viewModel::onShareSavedAudio,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
    }
}
