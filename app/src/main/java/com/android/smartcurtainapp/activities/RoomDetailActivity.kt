package com.android.smartcurtainapp.activities

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_detail)

        database = FirebaseDatabase.getInstance().reference
        textView_temperature = findViewById(R.id.tvTemperature)
        textView_humidity = findViewById(R.id.tvHumidity)

        val houseId = intent.getStringExtra("house_id") ?: ""
        val houseName = intent.getStringExtra("house_name") ?: ""
        val roomId = intent.getStringExtra("room_id") ?: ""
        val roomName = intent.getStringExtra("room_name")

        recyclerViewCurtainList = findViewById(R.id.recyclerViewCurtains)
        recyclerViewCurtainList.layoutManager = LinearLayoutManager(this)

        curtainList = mutableListOf()
        adapter = CurtainAdapter(curtainList) { curtain, action ->
            when (action) {
                "open" -> updateCurtainState(this, curtain.name, curtain.id!!, true)
                "close" -> updateCurtainState(this, curtain.name, curtain.id!!, false)
                "toggleMode" -> toggleCurtainMode(this, curtain.name, curtain.id!!)
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


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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

}
