package com.example.speakeradvanced.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.speakeradvanced.utils.BuildUtils

class ConfigurationViewModel : ViewModel() {

    private val allPermissions by lazy {        //Should move to Provider
        mutableMapOf<String, Boolean>().let {
            when (BuildUtils.version()) {
                in 6..9 -> {
                    it[Manifest.permission.ACCESS_COARSE_LOCATION] = false
                    it[Manifest.permission.BLUETOOTH_ADMIN] = false
                }
                in 10..11 -> {
                    it[Manifest.permission.BLUETOOTH] = false
                    it[Manifest.permission.BLUETOOTH_ADMIN] = false
                    it[Manifest.permission.ACCESS_FINE_LOCATION] = false
                    it[Manifest.permission.ACCESS_BACKGROUND_LOCATION] = false
                }
                12 -> {
                    it[Manifest.permission.ACCESS_FINE_LOCATION] = false
                    it[Manifest.permission.ACCESS_BACKGROUND_LOCATION] = false
                    it[Manifest.permission.BLUETOOTH_CONNECT] = false
                }
            }
            it
        }
    }

    fun getAllPermissions() : Set<String> = allPermissions.keys
    fun allPermissionsGiven(): Boolean = !allPermissions.containsValue(false)

    fun changeStateOfPermission(permission : String, value : Boolean) {
        allPermissions[permission] = value
    }

    fun getAllUngivenPermissions(): ArrayList<String> {
        val array = arrayListOf<String>()
        for (permissionPair in allPermissions) {
            permissionPair.takeIf { !(it.value) }?.let { array.add(it.key) }
        }
        return array
    }

    fun initializePermissionValues(context: Context) {
        getAllPermissions().forEach { permission ->
            val permissionAllowed = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            changeStateOfPermission(permission, permissionAllowed)
        }
    }
}