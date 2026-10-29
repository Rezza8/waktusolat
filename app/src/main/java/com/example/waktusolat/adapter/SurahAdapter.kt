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
import com.example.waktusolat.api.JuzAmmaSurah

class SurahAdapter(
    private val onItemClick: (JuzAmmaSurah) -> Unit
) : ListAdapter<JuzAmmaSurah, SurahAdapter.ViewHolder>(DiffCallback()) {

    var primaryTextColor: Int = Color.WHITE
    var secondaryTextColor: Int = 0xFFA5D6A7.toInt()
    var badgeColor: Int = 0xFF43A047.toInt()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_surah, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick, primaryTextColor, secondaryTextColor, badgeColor)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNumber: TextView = itemView.findViewById(R.id.tv_surah_number)
        private val tvArabic: TextView = itemView.findViewById(R.id.tv_surah_arabic)
        private val tvName: TextView = itemView.findViewById(R.id.tv_surah_name)
        private val tvMeaning: TextView = itemView.findViewById(R.id.tv_surah_meaning)
        private val tvVerses: TextView = itemView.findViewById(R.id.tv_surah_verses)

        fun bind(
            surah: JuzAmmaSurah,
            onItemClick: (JuzAmmaSurah) -> Unit,
            primaryColor: Int,
            secondaryColor: Int,
            badgeColor: Int
        ) {
            tvNumber.text = surah.number.toString()
            tvNumber.setTextColor(Color.WHITE)
            val bg = tvNumber.background?.mutate() as? GradientDrawable
            bg?.setColor(badgeColor)
            tvArabic.text = surah.nameArabic
            tvArabic.setTextColor(primaryColor)
            tvName.text = surah.nameMs
            tvName.setTextColor(primaryColor)
            tvMeaning.text = surah.meaning
            tvMeaning.setTextColor(secondaryColor)
            tvVerses.text = "${surah.verses} ayat"
            tvVerses.setTextColor(secondaryColor)
            itemView.setOnClickListener { onItemClick(surah) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<JuzAmmaSurah>() {
        override fun areItemsTheSame(oldItem: JuzAmmaSurah, newItem: JuzAmmaSurah): Boolean =
            oldItem.number == newItem.number

        override fun areContentsTheSame(oldItem: JuzAmmaSurah, newItem: JuzAmmaSurah): Boolean =
            oldItem == newItem
    }
}
