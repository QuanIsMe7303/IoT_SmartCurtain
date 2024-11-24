package com.android.smartcurtainapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.smartcurtainapp.R
import com.android.smartcurtainapp.models.House

class HouseAdapter(
    private val houses: List<House>,
    private val onClick: (House) -> Unit
) : RecyclerView.Adapter<HouseAdapter.HouseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_house, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        val house = houses[position]
        holder.bind(house)
    }

    override fun getItemCount(): Int = houses.size

    inner class HouseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val houseNameTextView: TextView = itemView.findViewById(R.id.houseNameTextView)

        fun bind(house: House) {
            houseNameTextView.text = house.house_name
            itemView.setOnClickListener {
                onClick(house)
            }
        }
    }
}
