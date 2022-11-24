package com.example.speakeradvanced.data


import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.example.speakeradvanced.R
import com.example.speakeradvanced.recycler.AdapterA2DP
import java.io.Closeable

@SuppressLint("MissingPermission")
class BluetoothSpeaker(
    private val bluetoothDevice: BluetoothDevice,
    gattDevice: BluetoothDevice,
    private var interval: Long,
    context: Context
) : AdapterA2DP.BluetoothDisplayInfo, Closeable {

    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothScanRunnable = object : Runnable {
        override fun run() {
            bluetoothGatt.readRemoteRssi()
            Log.d("BluetoothGatt", "readRemoteRssi() is called.")
            handler.postDelayed(this, interval)
        }
    }
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val nextAverage = nextMovingAverageRssi(rssi)
                Log.d("BluetoothGattCall", gatt!!.device.address.toString() + ": " + nextAverage)
                setCurrentSignal(nextAverage)
                liveDataRssi.postValue(nextAverage)
            }
            super.onReadRemoteRssi(gatt, rssi, status)
        }

        private fun nextMovingAverageRssi(rssi : Int): Int {
            if (liveDataRssi.value == null) return rssi
            return (liveDataRssi.value!!.plus(rssi).div(2))
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            connectionState = newState
            if(BluetoothGatt.GATT_SUCCESS == status && BluetoothGatt.STATE_CONNECTED == connectionState){
                handler.post(bluetoothScanRunnable)
            }
            Log.d("BluetoothGattCall", "${gatt.toString()} -> Success: ${BluetoothGatt.GATT_SUCCESS == status}, Connected : ${BluetoothGatt.STATE_CONNECTED == newState}")
        }
    }

    private var connectionState = BluetoothGatt.STATE_DISCONNECTED
    private var bluetoothGatt: BluetoothGatt = gattDevice.connectGatt(context, true, bluetoothGattCallback)
    private val liveDataRssi = MutableLiveData<Int>()
    private val liveSignalImageId = MutableLiveData(R.drawable.weak_signal)

    fun isActive() : Boolean {
        return connectionState == BluetoothGatt.STATE_CONNECTED
    }

    fun getRssi(): LiveData<Int> {
        return liveDataRssi.distinctUntilChanged()
    }

    override fun getDeviceName(): String {
        return bluetoothDevice.name
    }

    override fun getDeviceAddress(): String {
        return bluetoothDevice.address
    }

    private fun setCurrentSignal(rssi: Int) {
        liveSignalImageId.postValue(when(rssi) {
            in -10..0 -> R.drawable.great_signal
            in -30..-10 -> R.drawable.good_signal
            in -60..-30 -> R.drawable.fair_signal

            else -> {R.drawable.weak_signal}
        })
    }

    override fun getSignalImageId(): LiveData<Int> {
        return liveSignalImageId.distinctUntilChanged()
    }

    fun setIntervalForGatt(interval : Long) { this.interval = interval }

    override fun close() {
        handler.removeCallbacks(bluetoothScanRunnable)
        bluetoothGatt.disconnect()
    }
}
