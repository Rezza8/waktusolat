package com.example.waktusolat

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.fragment.app.DialogFragment
import com.example.waktusolat.databinding.BottomSheetPrayerNotifBinding

class DialogPrayerNotification(
    private val prayerName: String,
    private val onSaved: () -> Unit
) : DialogFragment() {

    private var _binding: BottomSheetPrayerNotifBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        _binding = BottomSheetPrayerNotifBinding.inflate(LayoutInflater.from(requireContext()))

        val prefs = requireContext().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
        val notifKey = "notif_${prayerName.lowercase()}"
        val azanKey = PrayerNotificationHelper.getAzanTypeKey(prayerName)
        val muteKey = PrayerNotificationHelper.getMuteKey(prayerName)

        binding.switchPrayerNotif.isChecked = prefs.getBoolean(notifKey, true)
        val currentType = prefs.getString(azanKey, "short") ?: "short"

        val isMuted = prefs.getBoolean(muteKey, false)
        binding.switchPrayerMute.isChecked = isMuted
        binding.ivMuteIcon.setImageResource(
            if (isMuted) android.R.drawable.ic_lock_silent_mode 
            else android.R.drawable.ic_lock_silent_mode_off
        )

        binding.switchPrayerMute.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(muteKey, isChecked).apply()
            binding.ivMuteIcon.setImageResource(
                if (isChecked) android.R.drawable.ic_lock_silent_mode 
                else android.R.drawable.ic_lock_silent_mode_off
            )
        }

        when (currentType) {
            "short" -> binding.rgAzanType.check(R.id.rb_short)
            "long" -> binding.rgAzanType.check(R.id.rb_long)
            "custom" -> binding.rgAzanType.check(R.id.rb_custom)
        }

        binding.btnPickSound.visibility = if (currentType == "custom") View.VISIBLE else View.GONE

        binding.rgAzanType.setOnCheckedChangeListener { _, checkedId ->
            binding.btnPickSound.visibility = if (checkedId == R.id.rb_custom) View.VISIBLE else View.GONE
        }

        binding.btnPickSound.setOnClickListener {
            val parent = parentFragment as? WaktuSolatFragment
            parent?.launchCustomPickerFor(prayerName)
            dismiss()
        }

        binding.switchPrayerNotif.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(notifKey, isChecked).apply()
        }

        binding.rgAzanType.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.rb_short -> "short"
                R.id.rb_long -> "long"
                R.id.rb_custom -> "custom"
                else -> "short"
            }
            prefs.edit().putString(azanKey, type).apply()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(prayerName)
            .setView(binding.root)
            .setPositiveButton("Tutup") { _, _ ->
                onSaved()
                dismiss()
            }
            .create().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
