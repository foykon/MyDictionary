package com.example.mydictionary.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mydictionary.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val PREFS_NAME = "quiz_prefs"
    private val KEY_NAME = "user_name"
    private val KEY_SCORE = "total_score"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SharedPreferences yükle
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_NAME, "") ?: ""
        val totalScore = prefs.getInt(KEY_SCORE, 0)

        // Görünümü güncelle
        binding.etProfileName.setText(savedName)
        binding.tvTotalScore.text = totalScore.toString()

        // İsmi kaydet butonu
        binding.btnSaveName.setOnClickListener {
            val name = binding.etProfileName.text.toString().trim()
            prefs.edit().putString(KEY_NAME, name).apply()
            Toast.makeText(context, "Name saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 