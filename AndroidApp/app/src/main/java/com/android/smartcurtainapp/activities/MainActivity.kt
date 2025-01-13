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
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.smartcurtainapp.adapters.HouseAdapter
import com.android.smartcurtainapp.models.House
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.android.smartcurtainapp.R



class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewHouseList: RecyclerView
    private lateinit var houseList: MutableList<House>
    private lateinit var houseAdapter: HouseAdapter
    private lateinit var addHouseButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var logoutButton: ImageView

    private var selectedHouse: House? = null

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        logoutButton = findViewById(R.id.btn_logout)

        // Sự kiện click cho nút đăng xuất
        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        addHouseButton = findViewById(R.id.btn_add_house)

        // Recycler view
        recyclerViewHouseList = findViewById(R.id.recyclerView_houseList)
        recyclerViewHouseList.layoutManager = GridLayoutManager(this, 2)

        // house adapter
        houseList = mutableListOf()
        houseAdapter = HouseAdapter(houseList,
            onClick = { house ->
                val intent = Intent(this, HouseDetailActivity::class.java)
                intent.putExtra("house_id", house.house_id)
                intent.putExtra("house_name", house.house_name)
                startActivity(intent)
            },
            onLongClick = { house, view ->
                selectedHouse = house
                registerForContextMenu(view)
                view.showContextMenu()
            }
        )

        recyclerViewHouseList.adapter = houseAdapter

        fetchHouses()
        Log.d("Main", "fetchHouse")

        addHouseButton.setOnClickListener { showAddHouseDialog() }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Bạn có chắc chắn muốn đăng xuất không?")
            .setPositiveButton("Đồng ý") { dialog, which ->
                auth.signOut()
                Toast.makeText(this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun fetchHouses() {
        database.child("houses").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                houseList.clear()
                for (houseSnapshot in snapshot.children) {
                    val house_id = houseSnapshot.key ?: ""
                    val house_name = houseSnapshot.child("name").getValue(String::class.java) ?: ""
                    val created_at = houseSnapshot.child("created_at").getValue(String::class.java) ?: ""

                    val house = House(house_id = house_id, house_name = house_name, created_at = created_at)
                    houseList.add(house)
                    Log.d("MainActivity", "Fetched House ID: ${house.house_id} ${house.house_name} created at $created_at")
                }
                houseAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Error fetching houses", error.toException())
            }
        })
    }

    private fun showAddHouseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_add_house, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etHouseName = dialogView.findViewById<EditText>(R.id.etAddHouseName)
        val btnSaveHouse = dialogView.findViewById<Button>(R.id.btnSaveHouse)

        btnSaveHouse.setOnClickListener {
            val houseName = etHouseName.text.toString().trim()

            if (houseName.isNotEmpty()) {
                val newHouseRef = database.child("houses").push()
                val houseData = mapOf(
                    "name" to houseName,
                    "created_at" to System.currentTimeMillis().toString()
                )

                newHouseRef.setValue(houseData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Thêm mới $houseName thành công!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Đã xảy ra lỗi!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                etHouseName.error = "Tên nhà không được để trống"
            }
        }
        dialog.show()
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.house_context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.house_action_rename -> {
                selectedHouse?.let { showRenameHouseDialog(it) }
                true
            }
            R.id.house_action_delete -> {
                selectedHouse?.let { confirmDeleteHouse(it) }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun showRenameHouseDialog(house: House) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rename_house, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etNewName = dialogView.findViewById<EditText>(R.id.etNewHouseName)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveNewName)

        etNewName.setText(house.house_name)

        btnSave.setOnClickListener {
            val newName = etNewName.text.toString().trim()
            if (newName.isNotEmpty()) {
                database.child("houses").child(house.house_id).child("name")
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

    private fun confirmDeleteHouse(house: House) {
        AlertDialog.Builder(this)
            .setTitle("Xóa nhà")
            .setMessage("Bạn có chắc chắn muốn xóa nhà \"${house.house_name}\" không?")
            .setPositiveButton("Xóa") { _, _ ->
                database.child("houses").child(house.house_id)
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
}
