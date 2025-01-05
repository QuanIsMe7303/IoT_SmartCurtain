package com.android.smartcurtainapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.smartcurtainapp.R
import com.android.smartcurtainapp.models.Curtain

class CurtainAdapter(
    private val curtainList: List<Curtain>,
    private val onCurtainAction: (Curtain, String) -> Unit
) : RecyclerView.Adapter<CurtainAdapter.CurtainViewHolder>() {

    class CurtainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val curtainName: TextView = itemView.findViewById(R.id.curtainName)
        val curtainStatus: TextView = itemView.findViewById(R.id.curtainStatus)
        val buttonOpen: Button = itemView.findViewById(R.id.button_openCurtain)
        val buttonClose: Button = itemView.findViewById(R.id.button_closeCurtain)
        val switchMode: Switch = itemView.findViewById(R.id.switchMode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurtainViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_curtain, parent, false)
        return CurtainViewHolder(view)
    }

    override fun onBindViewHolder(holder: CurtainViewHolder, position: Int) {
        val curtain = curtainList[position]
        holder.curtainName.text = "Rèm: ${curtain.name}"
        holder.curtainStatus.text = "Trạng thái: ${if (curtain.status) "Mở" else "Đóng"}"
        holder.switchMode.isChecked = curtain.control?.auto_mode ?: false

        holder.buttonOpen.setOnClickListener { onCurtainAction(curtain, "open") }
        holder.buttonClose.setOnClickListener { onCurtainAction(curtain, "close") }
        holder.switchMode.setOnCheckedChangeListener { _, _ -> onCurtainAction(curtain, "toggleMode") }

        // Lắng nghe sự kiện ấn giữ
        holder.itemView.setOnLongClickListener {
            onCurtainAction(curtain, "options")
            true
        }
    }


    override fun getItemCount(): Int = curtainList.size
}
