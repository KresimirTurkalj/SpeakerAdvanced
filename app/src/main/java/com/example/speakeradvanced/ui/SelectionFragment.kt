package com.example.speakeradvanced.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.speakeradvanced.R
import com.example.speakeradvanced.databinding.FragmentSelectionBinding
import com.example.speakeradvanced.service.BluetoothFGService
import com.example.speakeradvanced.viewmodel.SelectionViewModel
import com.example.speakeradvanced.recycler.AdapterA2DP

class SelectionFragment : Fragment() {

    private lateinit var binding: FragmentSelectionBinding

    private var mBound: Boolean = false
    private lateinit var viewModel: SelectionViewModel
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            viewModel.setBluetoothService(service as BluetoothFGService.BluetoothBinder)

            binding.deviceList.adapter = AdapterA2DP(
                viewModel.getLiveDataSpeakers(),
                this@SelectionFragment,
                requireActivity()
            ) { position -> viewModel.updateSelectedSpeakers(position) }

            binding.selectButton.isEnabled = true
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    //region lifecycle methods
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[SelectionViewModel::class.java]
        binding.deviceList.layoutManager = LinearLayoutManager(requireContext())

        binding.selectButton.setOnClickListener {
            if(mBound) {
                viewModel.provideSelectedSpeakers()
                findNavController().navigate(R.id.servingFragment)
            } else {
                Toast.makeText(requireContext(), "Service is down.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStart() {
        Toast.makeText(requireContext(), "Selection is started.", Toast.LENGTH_LONG).show()
        super.onStart()
        Intent(requireContext(), BluetoothFGService::class.java).also { intent : Intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
    //endregion
}