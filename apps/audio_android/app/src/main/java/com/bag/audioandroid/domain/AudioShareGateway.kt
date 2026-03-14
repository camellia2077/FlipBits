package com.bag.audioandroid.domain

interface AudioShareGateway {
    fun shareSavedAudio(item: SavedAudioItem): Boolean
}
