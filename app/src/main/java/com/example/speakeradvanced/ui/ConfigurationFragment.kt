package com.example.speakeradvanced.ui

import android.Manifest.permission.*
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.speakeradvanced.R
import com.example.speakeradvanced.utils.BuildUtils
import com.example.speakeradvanced.viewmodel.ConfigurationViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task

class ConfigurationFragment : Fragment() {

    companion object {
        const val REQUEST_CODE_CHECK_SETTINGS = 12
    }

    private lateinit var viewModel: ConfigurationViewModel

    private val bluetoothManager by lazy { context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val locationManager by lazy { context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    //region Receivers
    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (viewModel.allPermissionsGiven())
                checkServices()
        }
    }

    private val bluetoothRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){}

    private val requestBackgroundPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.changeStateOfPermission(ACCESS_BACKGROUND_LOCATION, true)
            } else {
                if (shouldShowRequestPermissionRationale(ACCESS_BACKGROUND_LOCATION)) {
                    askPermissions()
                } else {
                    showPersistentRationale(ACCESS_BACKGROUND_LOCATION)
                }
            }

            if (viewModel.allPermissionsGiven()) {
                checkServices()
            }
        }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            val permission =
                if (BuildUtils.version() > 9) ACCESS_FINE_LOCATION else ACCESS_COARSE_LOCATION
            if (isGranted) {
                viewModel.changeStateOfPermission(permission, true)
            } else {
                if (shouldShowRequestPermissionRationale(permission)) {
                    askPermissions()
                } else {
                    showPersistentRationale(permission)
                }
            }

            if (viewModel.allPermissionsGiven()) {
                checkServices()
            }
        }

    private val requestBluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.changeStateOfPermission(BLUETOOTH, true)
            }

            if (viewModel.allPermissionsGiven()) {
                checkServices()
            }
        }

    private val requestBluetoothScanPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.changeStateOfPermission(BLUETOOTH_SCAN, true)
        }

        if (viewModel.allPermissionsGiven()) {
            checkServices()
        }
    }

    private val requestBluetoothAdminPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if(BuildUtils.isVersion(12)) {
                    viewModel.changeStateOfPermission(BLUETOOTH_CONNECT, true)
                } else {
                    viewModel.changeStateOfPermission(BLUETOOTH_ADMIN, true)
                }
            }

            if (viewModel.allPermissionsGiven()) {
                checkServices()
            }
        }

    //endregion

    //region lifecycle methods
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_configuration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[ConfigurationViewModel::class.java]
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        activity?.registerReceiver(serviceBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        activity?.registerReceiver(serviceBroadcastReceiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))
        viewModel.initializePermissionValues(requireContext())

        if (viewModel.allPermissionsGiven()) {
            checkServices()
        } else {
            showRationale()
        }
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(serviceBroadcastReceiver)
    }

    //endregion

    private fun checkServices() {
        if (!bluetoothManager.adapter.isEnabled)
            askForBluetooth()
        else if (!LocationManagerCompat.isLocationEnabled(locationManager))
            askForLocation()
        else
            findNavController().navigate(R.id.selectionFragment)
    }

    private fun showRationale() {
        val message = SpannableStringBuilder(
            getString(R.string.message_explanation) +
                    "\n\n" + getString(R.string.message_location) +
                    "\n" + getString(R.string.message_bluetooth)
        ).toString()
        AlertDialog.Builder(context)
            .setTitle(R.string.title_explanation)
            .setMessage(message)
            .setPositiveButton(R.string.ok_text) { _, _ -> askPermissions() }
            .setCancelable(false)
            .show()
    }

    private fun askForBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothRequest.launch(enableBtIntent)
    }

    private fun askForLocation() {
        val request = LocationSettingsRequest.Builder().setNeedBle(true).build()
        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(request)
        task.addOnSuccessListener { locationSettingsResponse ->
            locationSettingsResponse.locationSettingsStates?.let {
                if(it.isBleUsable)
                    findNavController().navigate(R.id.selectionFragment)
                else
                    showRationale()
            }
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult().
                    exception.startResolutionForResult(requireActivity(), REQUEST_CODE_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun askPermissions() {
        for (permission in viewModel.getAllUngivenPermissions()) {
            when (permission) {
                BLUETOOTH -> {
                    requestBluetoothPermission.launch(permission)
                }

                BLUETOOTH_SCAN -> {
                    requestBluetoothScanPermission.launch(permission)
                }

                BLUETOOTH_ADMIN, BLUETOOTH_CONNECT -> {
                    requestBluetoothAdminPermission.launch(permission)
                    break
                }

                ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION -> {
                    requestLocationPermission.launch(permission)
                    break
                }

                ACCESS_BACKGROUND_LOCATION -> {
                    if (!viewModel.getAllUngivenPermissions().contains(ACCESS_COARSE_LOCATION) && !viewModel.getAllUngivenPermissions().contains(ACCESS_FINE_LOCATION)
                    ) {
                        requestBackgroundPermission.launch(permission)
                        break
                    }
                }
            }
        }
    }

    private fun showPersistentRationale(permission: String) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(R.string.runtime_request)
            .setPositiveButton(R.string.ok_text) { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                } catch (e: Exception) {
                    Log.d("Open settings", "Can't open app specific settings, trying general.")
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        when (permission) {
            ACCESS_COARSE_LOCATION -> {
                alertDialog.setMessage(R.string.location_denied)
            }
            BLUETOOTH_ADMIN -> {
                alertDialog.setMessage(R.string.bluetooth_denied)
            }
        }
        alertDialog.show()
    }
}