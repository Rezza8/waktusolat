package com.example.waktusolat.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.waktusolat.R

data class AyahItem(
    val number: Int,
    val arabic: String,
    val rumi: String,
    val malay: String
)

class AyahAdapter : ListAdapter<AyahItem, AyahAdapter.ViewHolder>(DiffCallback()) {

    var primaryTextColor: Int = Color.WHITE
    var rumiTextColor: Int = 0xFF81C784.toInt()
    var malayTextColor: Int = 0xFFE8F5E9.toInt()
    var badgeColor: Int = 0xFF43A047.toInt()
    var itemBackgroundColor: Int = 0

    private val itemShape = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 16f
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ayah, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            getItem(position),
            primaryTextColor,
            rumiTextColor,
            malayTextColor,
            badgeColor,
            itemBackgroundColor,
            itemShape
        )
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_ayah_number)
        private val tvArabic: TextView = itemView.findViewById(R.id.tv_ayah_arabic)
        private val tvRumi: TextView = itemView.findViewById(R.id.tv_ayah_rumi)
        private val tvMalay: TextView = itemView.findViewById(R.id.tv_ayah_malay)

        fun bind(
            ayah: AyahItem,
            primaryColor: Int,
            rumiColor: Int,
            malayColor: Int,
            badgeColor: Int,
            bgColor: Int,
            shape: GradientDrawable
        ) {
            if (bgColor != 0) {
                val clone = shape.mutate() as GradientDrawable
                clone.setColor(bgColor)
                itemView.background = clone
            } else {
                itemView.background = null
            }

            tvNumber.text = ayah.number.toString()
            tvNumber.setTextColor(Color.WHITE)
            val bg = tvNumber.background?.mutate() as? GradientDrawable
            bg?.setColor(badgeColor)
            tvArabic.text = ayah.arabic
            tvArabic.setTextColor(primaryColor)
            tvRumi.text = ayah.rumi
            tvRumi.setTextColor(rumiColor)
            tvMalay.text = ayah.malay
            tvMalay.setTextColor(malayColor)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AyahItem>() {
        override fun areItemsTheSame(oldItem: AyahItem, newItem: AyahItem): Boolean =
            oldItem.number == newItem.number

        override fun areContentsTheSame(oldItem: AyahItem, newItem: AyahItem): Boolean =
            oldItem == newItem
    }
}
