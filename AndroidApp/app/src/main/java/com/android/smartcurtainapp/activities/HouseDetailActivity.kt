package com.android.smartcurtainapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.smartcurtainapp.R
import com.android.smartcurtainapp.adapters.RoomAdapter
import com.android.smartcurtainapp.models.Room
import com.google.firebase.database.*

class HouseDetailActivity : AppCompatActivity() {
    private lateinit var recyclerViewRoomList: RecyclerView
    private lateinit var roomList: MutableList<Room>
    private lateinit var roomAdapter: RoomAdapter

    private var database: DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_house_detail)

        val houseId = intent.getStringExtra("house_id") ?: ""
        val houseName = intent.getStringExtra("house_name") ?: "House"

        // Setup RecyclerView
        recyclerViewRoomList = findViewById(R.id.recyclerViewRooms)
        recyclerViewRoomList.layoutManager = GridLayoutManager(this, 2)

        roomList = mutableListOf()
        roomAdapter = RoomAdapter(roomList) { room ->
            val intent = Intent(this, RoomDetailActivity::class.java)
            intent.putExtra("house_id", houseId)
            intent.putExtra("house_name", houseName)
            intent.putExtra("room_id", room.room_id)
            intent.putExtra("room_name", room.name)

            Log.d("HouseDetail", "${houseId}, ${room.room_id}, ${room.name}")
            startActivity(intent)
        }

        recyclerViewRoomList.adapter = roomAdapter

        // Fetch house name for title
        if (houseId.isNotEmpty()) {
            Log.d("HouseDetailActivity", "Received House ID: $houseId")
            fetchRooms(houseId)
        } else {
            Log.e("HouseDetailActivity", "HOUSE_ID is null or empty")
        }

        // Setup action bar
        supportActionBar?.apply {
            title = houseName
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun fetchRooms(houseId: String) {
        database.child("rooms").orderByChild("house_id").equalTo(houseId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                roomList.clear()

                for (roomSnapshot in snapshot.children) {
                    val roomId = roomSnapshot.key ?: ""
                    val name = roomSnapshot.child("name").getValue(String::class.java) ?: "Unknown Room"
                    val createdAt = roomSnapshot.child("created_at").getValue(String::class.java) ?: "Unknown Time"

                    val room = Room(
                        room_id = roomId,
                        name = name,
                        created_at = createdAt
                    )
                    roomList.add(room)
                    roomAdapter.notifyDataSetChanged()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("HouseDetailActivity", "Error fetching rooms", error.toException())
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}
