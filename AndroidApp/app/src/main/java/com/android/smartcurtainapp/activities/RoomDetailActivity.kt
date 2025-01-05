package com.android.smartcurtainapp.activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.smartcurtainapp.R
import com.android.smartcurtainapp.adapters.CurtainAdapter
import com.android.smartcurtainapp.models.Control
import com.android.smartcurtainapp.models.Curtain
import com.google.firebase.database.*

class RoomDetailActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var recyclerViewCurtainList: RecyclerView
    private lateinit var curtainList: MutableList<Curtain>
    private lateinit var adapter: CurtainAdapter
    private lateinit var textView_temperature: TextView
    private lateinit var textView_humidity: TextView
    private var roomId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        database = FirebaseDatabase.getInstance().reference
        textView_temperature = findViewById(R.id.tvTemperature)
        textView_humidity = findViewById(R.id.tvHumidity)

        val houseId = intent.getStringExtra("house_id") ?: ""
        val houseName = intent.getStringExtra("house_name") ?: ""
        roomId = intent.getStringExtra("room_id") ?: ""
        val roomName = intent.getStringExtra("room_name")

        recyclerViewCurtainList = findViewById(R.id.recyclerViewCurtains)
        recyclerViewCurtainList.layoutManager = LinearLayoutManager(this)

        curtainList = mutableListOf()
        adapter = CurtainAdapter(curtainList) { curtain, action ->
            when (action) {
                "open" -> updateCurtainState(this, curtain.name, curtain.id!!, true)
                "close" -> updateCurtainState(this, curtain.name, curtain.id!!, false)
                "toggleMode" -> toggleCurtainMode(this, curtain.name, curtain.id!!)
                "options" -> showCurtainOptionsDialog(curtain)
            }
        }


        recyclerViewCurtainList.adapter = adapter

        supportActionBar?.apply {
            title = roomName
            setDisplayHomeAsUpEnabled(true)
        }

        if (houseId.isNotEmpty() && roomId.isNotEmpty()) {
            fetchCurtainsAndSensors(roomId)
        }

        adapter.notifyDataSetChanged()
    }

    private fun fetchCurtainsAndSensors(roomId: String) {
        database.child("curtains").orderByChild("room_id").equalTo(roomId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(curtainSnapshot: DataSnapshot) {
                    curtainList.clear()
                    for (curtain in curtainSnapshot.children) {
                        val curtain_id = curtain.key ?: ""
                        val curtain_name = curtain.child("name").getValue(String::class.java) ?: ""
                        val curtain_status = curtain.child("status").getValue(Boolean::class.java) ?: false

                        val auto_mode = curtain.child("control/auto_mode").getValue(Boolean::class.java) ?: false
                        val manual_state = curtain.child("control/manual_state").getValue(Boolean::class.java) ?: false

                        val curtain_control = Control(auto_mode = auto_mode, manual_state = manual_state)
                        val curtainData = Curtain(
                            id = curtain_id,
                            name = curtain_name,
                            status = curtain_status,
                            control = curtain_control
                        )
                        curtainData?.let { curtainList.add(it) }
                    }

                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RoomDetailActivity", "Error fetching curtains", error.toException())
                }
            })
        database.child("sensors").child(roomId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(sensorSnapshot: DataSnapshot) {
                Log.d("RoomDetail", sensorSnapshot.toString())
                val humidity = sensorSnapshot.child("humidity").child("value").getValue(Double::class.java) ?: 0.0
                val temperature = sensorSnapshot.child("temperature").child("value").getValue(Double::class.java) ?: 0.0

                Log.d("RoomDetail", "Temperature: $temperature, Humidity: $humidity")

                textView_temperature.text = "Nhiệt độ: ${temperature}°C"
                textView_humidity.text = "Độ ẩm: ${humidity}%"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RoomDetailActivity", "Error fetching sensor data", error.toException())
            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.room_option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_device -> {
                showDeviceConnectionDialog()
                true
            }

            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateCurtainState(
        context: Context,
        curtainName: String,
        curtainId: String,
        targetState: Boolean
    ) {
        val curtainRef = database.child("curtains").child(curtainId)

        curtainRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val control = snapshot.child("control")
                val isAutoMode = control.child("auto_mode").getValue(Boolean::class.java) ?: false

                if (isAutoMode) {
                    Toast.makeText(
                        context,
                        "Không thể thay đổi trạng thái rèm $curtainName khi đang ở chế độ tự động!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val currentStatus = snapshot.child("status").getValue(Boolean::class.java) ?: false

                if (currentStatus == targetState) {
                    Toast.makeText(
                        context,
                        "Rèm $curtainName đã ${if (targetState) "mở" else "đóng"}!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                curtainRef.child("status").setValue(targetState)
                    .addOnSuccessListener {
                        Toast.makeText(
                            context,
                            "Đã ${if (targetState) "mở" else "đóng"} rèm $curtainName thành công!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Lỗi khi cập nhật trạng thái rèm!", Toast.LENGTH_SHORT)
                            .show()
                    }

                curtainRef.child("control").child("manual_state").setValue(targetState)
                    .addOnSuccessListener {

                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Lỗi khi cập nhật trạng thái rèm!", Toast.LENGTH_SHORT)
                            .show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("updateCurtainState", "Error fetching curtain data", error.toException())
                Toast.makeText(context, "Lỗi kết nối cơ sở dữ liệu!", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun toggleCurtainMode(context: Context, curtainName: String, curtainId: String) {
        val curtainRef = database.child("curtains").child(curtainId).child("control").child("auto_mode")
        curtainRef.addListenerForSingleValueEvent(object: ValueEventListener {
           override fun onDataChange(snapshot: DataSnapshot) {
               val currentMode = snapshot.getValue(Boolean::class.java) ?: false
               val newMode = !currentMode
               val mode = if (newMode) "Tự động" else "Thủ công"

               curtainRef.setValue(newMode)
                   .addOnSuccessListener {
                       Toast.makeText(context, "$curtainName chuyển sang chế độ $mode", Toast.LENGTH_SHORT).show()
                   }.addOnFailureListener {
                       Toast.makeText(context, "Đã xảy ra lỗi!", Toast.LENGTH_SHORT).show()
                   }
           }

           override fun onCancelled(error: DatabaseError) {
               TODO("Not yet implemented")
           }

       })
    }

    private fun showDeviceConnectionDialog() {
        val loadingDialog = ProgressDialog(this).apply {
            setMessage("Đang chờ thiết bị kết nối...")
            setCancelable(false)
        }

        loadingDialog.show()

        val newDeviceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val unassignedDevices = snapshot.children.firstOrNull { curtainSnapshot ->
                    val roomId = curtainSnapshot.child("room_id").getValue(String::class.java)
                    roomId == "defaultRoom"
                }

                if (unassignedDevices != null) {
                    database.child("curtains").removeEventListener(this)
                    val curtainId = unassignedDevices.key
                    val curtainRef = database.child("curtains").child(curtainId!!)

                    // Dữ liệu mới cho rèm
                    val newCurtainData = mapOf(
                        "room_id" to roomId,
                        "created_at" to System.currentTimeMillis().toString()
                    )

                    // Cập nhật dữ liệu rèm
                    curtainRef.updateChildren(newCurtainData)
                        .addOnSuccessListener {
                            Log.d("TEST", "new curtain id: $curtainId assigned to room: $roomId")
                            loadingDialog.dismiss()

                            Toast.makeText(
                                this@RoomDetailActivity,
                                "Thiết bị mới đã được tìm thấy và gán thành công!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            loadingDialog.dismiss()
                            Toast.makeText(
                                this@RoomDetailActivity,
                                "Lỗi khi gán thiết bị vào phòng.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }


            override fun onCancelled(error: DatabaseError) {
                loadingDialog.dismiss()
                Toast.makeText(
                    this@RoomDetailActivity,
                    "Lỗi kết nối: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        database.child("curtains")
            .addValueEventListener(newDeviceListener)

        // Timeout sau 2 phút nếu không tìm thấy thiết bị
        Handler(Looper.getMainLooper()).postDelayed({
            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
                database.child("curtains").removeEventListener(newDeviceListener)

                Toast.makeText(
                    this,
                    "Hết thời gian chờ. Không tìm thấy thiết bị mới.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, 2 * 60 * 1000) // Timeout 2 phút
    }


    private fun showCurtainOptionsDialog(curtain: Curtain) {
        val options = arrayOf("Đổi tên", "Xóa rèm")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tùy chọn cho rèm ${curtain.name}")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showRenameCurtainDialog(curtain)
                1 -> deleteCurtain(curtain)
            }
        }
        builder.show()
    }

    private fun showRenameCurtainDialog(curtain: Curtain) {
        val input = EditText(this).apply {
            hint = "Nhập tên mới"
            setText(curtain.name)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Đổi tên rèm")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank()) {
                    database.child("curtains").child(curtain.id!!).child("name")
                        .setValue(newName)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Đổi tên thành công!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Lỗi khi đổi tên!", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .create()
        dialog.show()
    }

    private fun deleteCurtain(curtain: Curtain) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Xóa rèm")
            .setMessage("Bạn có chắc chắn muốn xóa rèm ${curtain.name}?")
            .setPositiveButton("Xóa") { _, _ ->
                database.child("curtains").child(curtain.id!!)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Xóa rèm thành công!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi khi xóa rèm!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .create()
        dialog.show()
    }


}
