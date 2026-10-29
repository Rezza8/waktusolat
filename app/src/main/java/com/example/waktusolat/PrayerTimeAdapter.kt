package com.example.waktusolat

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

data class PrayerRowItem(
    val name: String,
    val time: String,
    val iconRes: Int,
    val isNext: Boolean,
    val notifEnabled: Boolean,
    val isMuted: Boolean = false
)

class PrayerTimeAdapter(
    private val onBellClick: (String) -> Unit
) : ListAdapter<PrayerRowItem, PrayerTimeAdapter.ViewHolder>(DiffCallback()) {

    var themeIndex: Int = 0

    private val prayerIconColors = mapOf(
        "Imsak" to 0xFF607D8B.toInt(),
        "Subuh" to 0xFF4CAF50.toInt(),
        "Syuruk" to 0xFFFFC107.toInt(),
        "Zuhur" to 0xFFFF9800.toInt(),
        "Asar" to 0xFFFF5722.toInt(),
        "Maghrib" to 0xFFE91E63.toInt(),
        "Isyak" to 0xFF9C27B0.toInt()
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.iv_prayer_icon)
        val name: TextView = view.findViewById(R.id.tv_prayer_name)
        val time: TextView = view.findViewById(R.id.tv_prayer_time)
        val bell: ImageView = view.findViewById(R.id.iv_bell)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prayer_time, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val primaryColor = ThemeHelper.getPrimaryTextColor(themeIndex)
        val accentColor = ThemeHelper.getAccentColor(themeIndex)

        holder.icon.setImageResource(item.iconRes)
        val iconColor = prayerIconColors[item.name] ?: accentColor
        holder.icon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        holder.name.text = item.name
        holder.name.setTextColor(primaryColor)
        holder.time.text = item.time
        holder.time.setTextColor(primaryColor)
        
        when {
            !item.notifEnabled -> {
                holder.bell.setImageResource(R.drawable.ic_bell_off)
                holder.bell.setColorFilter(Color.argb(120, 255, 255, 255), PorterDuff.Mode.SRC_IN)
            }
            item.isMuted -> {
                holder.bell.setImageResource(R.drawable.ic_bell_muted)
                holder.bell.setColorFilter(Color.argb(180, 255, 255, 255), PorterDuff.Mode.SRC_IN)
            }
            else -> {
                holder.bell.setImageResource(R.drawable.ic_bell_on)
                holder.bell.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
            }
        }

        val bgRes = if (item.isNext) R.drawable.bg_prayer_current else R.drawable.bg_prayer_default
        holder.itemView.background = ContextCompat.getDrawable(holder.itemView.context, bgRes)

        holder.bell.setOnClickListener { onBellClick(item.name) }
    }

    class DiffCallback : DiffUtil.ItemCallback<PrayerRowItem>() {
        override fun areItemsTheSame(oldItem: PrayerRowItem, newItem: PrayerRowItem) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: PrayerRowItem, newItem: PrayerRowItem) = oldItem == newItem
    }
}
