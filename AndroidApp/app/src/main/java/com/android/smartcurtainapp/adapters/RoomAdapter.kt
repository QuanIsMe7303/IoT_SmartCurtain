package com.android.smartcurtainapp.adapters

import Sensors
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.smartcurtainapp.R
import com.android.smartcurtainapp.models.House
import com.android.smartcurtainapp.models.Room

class RoomAdapter(
    private val roomList: List<Room>,
    private val onRoomClick: (Room) -> Unit,
    private val onLongClick: (Room, View) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = roomList[position]
        holder.bind(room)
    }

    override fun getItemCount(): Int = roomList.size

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val roomNameTextView: TextView = itemView.findViewById(R.id.roomName)

        fun bind(room: Room) {
            roomNameTextView.text = room.name

            itemView.setOnClickListener {
                onRoomClick(room)
            }

            // Xử lý sự kiện nhấn giữ
            itemView.setOnLongClickListener {
                onLongClick(room, itemView)
                true
            }
        }
    }
}

