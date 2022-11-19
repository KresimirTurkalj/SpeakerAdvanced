package com.example.speakeradvanced.recycler

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.speakeradvanced.R
import com.example.speakeradvanced.data.BluetoothSpeaker

class AdapterA2DP(
	liveDataOfSpeakers: LiveData<List<BluetoothSpeaker>>,
	private val lifecycleOwner: LifecycleOwner,
	private val fragmentActivity: FragmentActivity,
	private val itemClicked: (BluetoothSpeaker) -> Unit
) : RecyclerView.Adapter<HolderA2DP>() {

	private val listOfSpeakers = ArrayList<BluetoothSpeaker>()

	init {
		liveDataOfSpeakers.observe(lifecycleOwner) { updateList(it) }
	}
	interface BluetoothDisplayInfo {
		fun getDeviceName(): String
		fun getDeviceAddress(): String
		fun getSignalImageId(): LiveData<Int>
	}

	private fun updateList(list: List<BluetoothSpeaker>) {
		if(listOfSpeakers.containsAll(list) && list.containsAll(listOfSpeakers)) {
			Log.d("AdapterA2DP", "Lists are same.")
		}
		else if(listOfSpeakers.containsAll(list)) {
			Log.d("AdapterA2DP", "New list contains old and more.")
		}
		else if(list.containsAll(listOfSpeakers)) {
			Log.d("AdapterA2DP", "Old list contains new and more.")
		}
		else {
			Log.d("AdapterA2DP", "Neither list is a subset.")
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderA2DP {
		val holderA2DP = HolderA2DP(
			LayoutInflater.from(parent.context).inflate(
				R.layout.layout_a2dp_view_holder,
				parent,
				false
			)
		)

		holderA2DP.itemView.setOnClickListener {
			itemClicked(listOfSpeakers[holderA2DP.adapterPosition])
			toggleBackground(holderA2DP)
		}

		return holderA2DP
	}

	private fun toggleBackground(holderA2DP: HolderA2DP) {
		holderA2DP.lineBackground.let {
			it.visibility = if (it.visibility == View.VISIBLE) View.GONE else View.VISIBLE
		}
	}

	override fun onBindViewHolder(holder: HolderA2DP, position: Int) {
		listOfSpeakers.let {
			holder.textName.text = it[position].getDeviceName()
			holder.textAddress.text = it[position].getDeviceAddress()
			it[position].getSignalImageId().observe(lifecycleOwner) {
				fragmentActivity.runOnUiThread { holder.iconDrawable.setImageResource(it) }
			}
		}
	}

	override fun getItemCount(): Int {
		return listOfSpeakers.size
	}

}

class HolderA2DP(itemView: View) : RecyclerView.ViewHolder(itemView) {
	val lineBackground: View = itemView.findViewById(R.id.line_background)
	val iconDrawable: ImageView = itemView.findViewById(R.id.signal_quality)
	val textName: TextView = itemView.findViewById(R.id.device_name)
	val textAddress: TextView = itemView.findViewById(R.id.device_address)
}