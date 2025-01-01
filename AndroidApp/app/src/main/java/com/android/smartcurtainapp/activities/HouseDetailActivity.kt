package com.android.smartcurtainapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.smartcurtainapp.R
import com.android.smartcurtainapp.adapters.RoomAdapter
import com.android.smartcurtainapp.models.House
import com.android.smartcurtainapp.models.Room
import com.google.firebase.database.*

class HouseDetailActivity : AppCompatActivity() {
    private lateinit var recyclerViewRoomList: RecyclerView
    private lateinit var roomList: MutableList<Room>
    private lateinit var roomAdapter: RoomAdapter
    private var selectedRoom: Room? = null
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
        roomAdapter = RoomAdapter(
            roomList,
            onRoomClick = { room ->
                val intent = Intent(this, RoomDetailActivity::class.java)
                intent.putExtra("house_id", houseId)
                intent.putExtra("house_name", houseName)
                intent.putExtra("room_id", room.room_id)
                intent.putExtra("room_name", room.name)
                startActivity(intent)
            },
            onLongClick = { room, view ->
                selectedRoom = room // Lưu thông tin Room được chọn
                registerForContextMenu(view)
                view.showContextMenu()
            }
        )


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

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.room_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.room_action_rename -> {
                selectedRoom?.let { showRenameRoomDialog(it) }
                true
            }
            R.id.room_action_delete -> {
                selectedRoom?.let { confirmDeleteRoom(it) }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun showRenameRoomDialog(room: Room) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename_room, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etNewName = dialogView.findViewById<EditText>(R.id.etNewRoomName)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveNewName)

        etNewName.setText(room.name)

        btnSave.setOnClickListener {
            val newName = etNewName.text.toString().trim()
            if (newName.isNotEmpty()) {
                database.child("rooms").child(room.room_id).child("name")
                    .setValue(newName)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đổi tên thành công!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                etNewName.error = "Tên không được để trống"
            }
        }

        dialog.show()
    }


    private fun confirmDeleteRoom(room: Room) {
        AlertDialog.Builder(this)
            .setTitle("Xóa phòng")
            .setMessage("Bạn có chắc chắn muốn xóa phòng \"${room.name}\" không?")
            .setPositiveButton("Xóa") { _, _ ->
                database.child("rooms").child(room.room_id)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Xóa thành công!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.house_detail_option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_room -> {
                showAddRoomDialog()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddRoomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_room, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Thêm phòng mới")
            .create()

        val etRoomName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val btnSaveRoom = dialogView.findViewById<Button>(R.id.btnSaveRoom)

        btnSaveRoom.setOnClickListener {
            val roomName = etRoomName.text.toString().trim()
            if (roomName.isNotEmpty()) {
                val houseId = intent.getStringExtra("house_id") ?: return@setOnClickListener
                val roomId = FirebaseDatabase.getInstance().reference.child("rooms").push().key
                val roomData = mapOf(
                    "house_id" to houseId,
                    "name" to roomName,
                    "created_at" to System.currentTimeMillis().toString()
                )

                FirebaseDatabase.getInstance().reference.child("rooms").child(roomId!!)
                    .setValue(roomData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Thêm phòng thành công!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi khi thêm phòng!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                etRoomName.error = "Tên phòng không được để trống"
            }
        }

        dialog.show()
    }





}
