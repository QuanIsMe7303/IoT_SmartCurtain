package com.android.smartcurtainapp.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.smartcurtainapp.R
import com.android.smartcurtainapp.adapters.HouseAdapter
import com.android.smartcurtainapp.models.House
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewHouseList: RecyclerView
    private lateinit var houseList: MutableList<House>
    private lateinit var houseAdapter: HouseAdapter

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        // Recycler view
        recyclerViewHouseList = findViewById(R.id.recyclerView_houseList)
        recyclerViewHouseList.layoutManager = GridLayoutManager(this, 2)

        // house adapter
        houseList = mutableListOf()
        houseAdapter = HouseAdapter(houseList) { house ->
            val intent = Intent(this, HouseDetailActivity::class.java)
            intent.putExtra("house_id", house.house_id)
            intent.putExtra("house_name", house.house_name)

            startActivity(intent)
        }
        recyclerViewHouseList.adapter = houseAdapter

        // fetch data
        fetchHouses()
    }

    private fun fetchHouses() {
        database.child("houses").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                houseList.clear()
                for (houseSnapshot in snapshot.children) {
                    val house_id = houseSnapshot.key ?: ""
                    val house_name = houseSnapshot.child("name").getValue(String::class.java) ?: ""

                    val house = House(house_id = house_id, house_name = house_name)
                    if (house != null) {
                        houseList.add(house)
                        Log.d("MainActivity", "Fetched House ID: ${house.house_id} ${house.house_name}")
                    }
                }
                houseAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Error fetching houses", error.toException())
            }
        })
    }
}
