package com.example.speakeradvanced.ui

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.location.LocationManager
import android.location.LocationRequest
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.location.LocationManagerCompat
import androidx.navigation.fragment.findNavController
import com.example.speakeradvanced.R
import com.example.speakeradvanced.utils.BuildUtils
import com.example.speakeradvanced.viewmodel.ConfigurationViewModel

class ConfigurationFragment : Fragment() {

    companion object {
        const val REQUEST_CODE_CHECK_SETTINGS = 11
        const val LOCATION_UPDATE_INTERVAL = 1000L
        const val LOCATION_UPDATE_FASTEST_INTERVAL = 500L
    }

    private lateinit var viewModel: ConfigurationViewModel

    private val bluetoothManager by lazy { context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val locationManager by lazy { context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    //region Receivers
    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (areServicesEnabled())
                checkServices()
        }
    }

    private val requestBackgroundPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.changeStateOfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, true)
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    askPermissions()
                } else {
                    showPersistentRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }

            if (viewModel.allPermissionsGiven()) {
                checkServices()
            }
        }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            val permission = if (BuildUtils.version() > 9) Manifest.permission.ACCESS_FINE_LOCATION else Manifest.permission.ACCESS_COARSE_LOCATION
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

    private val requestBluetoothPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.changeStateOfPermission(Manifest.permission.BLUETOOTH_ADMIN, true)
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
        viewModel = ViewModelProvider(this)[ConfigurationViewModel::class.java] //TODO This is incorrect way of doing VM
        super.onViewCreated(view, savedInstanceState)
        viewModel.initializePermissionValues(requireContext())
    }

    override fun onStart() {
        super.onStart()
        activity?.registerReceiver(serviceBroadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        activity?.registerReceiver(serviceBroadcastReceiver, IntentFilter(LocationManager.MODE_CHANGED_ACTION))

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
        if (!areServicesEnabled()) {
            if (!bluetoothManager.adapter.isEnabled) {
                bluetoothManager.adapter.enable()
            }
            if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
                askForLocation()
            }
        } else {
            findNavController().navigate(R.id.selectionFragment)
        }
    }

    private fun askForLocation() {
        val locationRequest: LocationRequest = LocationRequest.Builder(LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(LOCATION_UPDATE_FASTEST_INTERVAL)
            .setPriority(PRIORITY_HIGH_ACCURACY)
            .build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            locationSettingsResponse.locationSettingsStates?.takeIf { it.isBlePresent }.apply {
                checkServices()
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

    private fun areServicesEnabled(): Boolean {
        return bluetoothManager.adapter.isEnabled && LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun askPermissions() {
        for (permission in viewModel.getAllUngivenPermissions()) {
            when (permission) {
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION -> {
                    requestLocationPermission.launch(permission)
                }

                Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                    if (!viewModel.getAllUngivenPermissions().contains(Manifest.permission.ACCESS_COARSE_LOCATION)
                        && !viewModel.getAllUngivenPermissions().contains(Manifest.permission.ACCESS_FINE_LOCATION)
                    ) {
                        requestBackgroundPermission.launch(permission)
                    }
                }

                Manifest.permission.BLUETOOTH_ADMIN -> {
                    requestBluetoothPermission.launch(permission)
                }
            }
        }
    }

    private fun showPersistentRationale(permission: String) {
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(R.string.runtime_request)
            .setPositiveButton(R.string.ok) { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
                } catch (e: Exception) {
                    Log.d("Open settings", "Can't open app specific settings, trying general.")
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        when (permission) {
            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                alertDialog.setMessage(R.string.location_denied)
            }
            Manifest.permission.BLUETOOTH_ADMIN -> {
                alertDialog.setMessage(R.string.bluetooth_denied)
            }
        }
        alertDialog.show()
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
            .setPositiveButton(R.string.ok) { _, _ -> askPermissions() }
            .setCancelable(false)
            .show()
    }
}