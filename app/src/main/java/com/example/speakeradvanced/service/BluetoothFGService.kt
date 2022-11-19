package com.example.speakeradvanced.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.speakeradvanced.R
import com.example.speakeradvanced.data.BluetoothSpeaker
import com.example.speakeradvanced.ui.StartActivity
import com.example.speakeradvanced.utils.BuildUtils
import com.example.speakeradvanced.utils.Constants.Companion.OBSERVE_INTERVAL
import com.example.speakeradvanced.utils.Constants.Companion.SCAN_INTERVAL

class BluetoothFGService : LifecycleService() {

	inner class BluetoothBinder : Binder() {
		fun getSelectedSpeakerLiveData(): MutableLiveData<List<BluetoothSpeaker>> = listOfSelectedBluetoothSpeakers
		fun getBindedSpeakersLiveData(): MutableLiveData<List<BluetoothSpeaker>> = listOfBindedBluetoothSpeakers

		fun selectSpeakersForObservation(listOfSpeakers: List<BluetoothSpeaker>) {
			listOfSpeakers.forEachIndexed { index: Int, speaker: BluetoothSpeaker ->
				speaker.setIntervalForGatt(OBSERVE_INTERVAL)
				this@BluetoothFGService.let { service ->
					speaker.getRssi().observe(service) {
						if (service.isClosestSpeaker(it)) {
							service.switchToSpeakerAt(index)
						}
					}
				}
			}
			listOfSelectedBluetoothSpeakers.postValue(listOfSpeakers)
		}
	}

	private fun switchToSpeakerAt(index: Int) {
		TODO("Not yet implemented")
	}

	private fun isClosestSpeaker(it: Int?): Boolean {
		return it != null && it > currentClosestRssi
	}

	private var currentClosestRssi = Int.MAX_VALUE
	private val binder = BluetoothBinder()
	private val NOTIFICATION_NUMBER = 1
	private val A2DP_UUID: ParcelUuid = ParcelUuid.fromString("0000110b-0000-1000-8000-00805f9b34fb")

	private val listOfBindedBluetoothSpeakers = MutableLiveData<List<BluetoothSpeaker>>()
	private val listOfSelectedBluetoothSpeakers = MutableLiveData<List<BluetoothSpeaker>>()
	private var notStarted = true

	private val bluetoothManager: BluetoothManager by lazy { getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }

	override fun onBind(intent: Intent): IBinder {
		super.onBind(intent)
		Log.d(this.javaClass.name,"Service started")
		if (notStarted) {
			notStarted = false
			if (BuildUtils.isVersion(8)) {
				setupForeground()
			}
			updateBindedA2dpDevices()
		}
		return binder
	}

	@SuppressLint("NewApi")
	private fun setupForeground() {
		val name = getString(R.string.title_notification)
		val descriptionText = getString(R.string.message_notification)
		val importance = NotificationManager.IMPORTANCE_HIGH
		val channel = NotificationChannel(getString(R.string.bluetooth_service), name, importance).apply {
			description = descriptionText
		}

		val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.createNotificationChannel(channel)

		val pendingIntent: PendingIntent = Intent(this, StartActivity::class.java).let { notificationIntent ->
			PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
		}

		val notification: Notification = Notification.Builder(baseContext, getString(R.string.bluetooth_service))
			.setContentTitle(getText(R.string.title_notification))
			.setContentText(getText(R.string.message_notification))
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setContentIntent(pendingIntent)
			.build()
		startForeground(NOTIFICATION_NUMBER, notification)
	}

	@SuppressLint("MissingPermission")
	private fun updateBindedA2dpDevices() {
		val list = mutableListOf<BluetoothSpeaker>()
		bluetoothManager.adapter.bondedDevices.forEach {
			if (it.uuids != null && it.uuids.contains(A2DP_UUID)) {
				Log.d("Bluetooth Bonded Devices", it.toString())
				list.add(BluetoothSpeaker(it, SCAN_INTERVAL, this))
			}
		}
		listOfBindedBluetoothSpeakers.postValue(list)
	}

	override fun onDestroy() {
		Log.d(this.javaClass.name,"Service stopped")
		stopForeground(true)
		listOfBindedBluetoothSpeakers.postValue(emptyList())
		listOfSelectedBluetoothSpeakers.postValue(emptyList())
		super.onDestroy()
	}
}