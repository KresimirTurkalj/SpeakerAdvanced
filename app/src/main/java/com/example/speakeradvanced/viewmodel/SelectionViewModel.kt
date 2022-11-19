package com.example.speakeradvanced.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.speakeradvanced.service.BluetoothFGService
import com.example.speakeradvanced.data.BluetoothSpeaker

class SelectionViewModel : ViewModel() {
    private lateinit var bluetoothBinder : BluetoothFGService.BluetoothBinder
    private val selectedSpeakers = mutableListOf<BluetoothSpeaker>()

    fun getLiveDataSpeakers(): LiveData<List<BluetoothSpeaker>> { return bluetoothBinder.getBindedSpeakersLiveData() }

    fun provideSelectedSpeakers() = bluetoothBinder.selectSpeakersForObservation(selectedSpeakers)

    fun setBluetoothService(binder : BluetoothFGService.BluetoothBinder) {
        bluetoothBinder = binder
    }

    fun updateSelectedSpeakers(bluetoothSpeaker: BluetoothSpeaker) {
        if(selectedSpeakers.contains(bluetoothSpeaker))
            removeSelectedSpeaker(bluetoothSpeaker)
        else
            addSelectedSpeaker(bluetoothSpeaker)
    }

    private fun addSelectedSpeaker(bluetoothSpeaker: BluetoothSpeaker) {
        selectedSpeakers.add(bluetoothSpeaker)
    }

    private fun removeSelectedSpeaker(bluetoothSpeaker: BluetoothSpeaker) {
        selectedSpeakers.remove(bluetoothSpeaker)
    }
}