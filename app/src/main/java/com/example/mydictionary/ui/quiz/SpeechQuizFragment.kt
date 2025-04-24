package com.example.mydictionary.ui.quiz

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.mydictionary.databinding.FragmentSpeechQuizBinding
import com.example.mydictionary.data.Word
import com.example.mydictionary.data.QuizType
import com.example.mydictionary.ui.dictionary.DictionaryViewModel
import java.util.Locale

class SpeechQuizFragment : Fragment() {

    private var _binding: FragmentSpeechQuizBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DictionaryViewModel
    private lateinit var speechLauncher: ActivityResultLauncher<Intent>
    private var currentWord: Word? = null
    private var currentQuestionIndex = 0
    private var score = 0
    private val totalQuestions = 5
    private val REQUEST_MICROPHONE_PERMISSION = 1
    private var currentUnitId: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeechQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]
        currentUnitId = arguments?.getInt("unitId", 1) ?: 1

        speechLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                val matches = result.data!!
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val heard = matches?.firstOrNull().orEmpty()
                binding.tvRecognized.text = heard

                checkAnswer(heard)
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE_PERMISSION)
        }

        setupQuiz()

        binding.btnSpeak.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Please say something")
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Device does not support speech recognition", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupQuiz() {
        viewModel.words.observe(viewLifecycleOwner) { words ->
            if (words.isNotEmpty()) {
                currentWord = words.shuffled().first()
                Glide.with(this)
                    .load(currentWord?.imageUrl)
                    .into(binding.speechQuizImage)
            } else {
                Toast.makeText(context, "Please add at least one word first.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkAnswer(recognizedText: String) {
        val currentWord = currentWord ?: return
        val isCorrect = recognizedText.equals(currentWord.word, ignoreCase = true)
        
        if (isCorrect) {
            score += 10
            binding.tvScore.text = "Score: $score"
            binding.tvResult.text = "Correct!"
            binding.tvResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            
            // Move to next question after delay
            view?.postDelayed({
                if (currentQuestionIndex < totalQuestions - 1) {
                    currentQuestionIndex++
                    setupQuiz()
                } else {
                    // Quiz complete
                    markQuizAsCompleted()
                    showFinalResult()
                }
            }, 1000)
        } else {
            binding.tvResult.text = "Try again!"
            binding.tvResult.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        }
    }

    private fun markQuizAsCompleted() {
        QuizType.SPEECH_QUIZ.markAsCompleted(currentUnitId)
        
        // Save completion status with unit ID
        val prefs = requireContext().getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val completedQuizzes = prefs.getStringSet("completed_quizzes", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        completedQuizzes.add("${currentUnitId}_${QuizType.SPEECH_QUIZ.name}")
        prefs.edit().putStringSet("completed_quizzes", completedQuizzes).apply()

        // Check if all quizzes in the unit are completed
        val allQuizzesCompleted = QuizType.values().all { quizType ->
            completedQuizzes.contains("${currentUnitId}_${quizType.name}")
        }

        if (allQuizzesCompleted) {
            // Mark unit as completed
            val completedUnits = prefs.getStringSet("completed_units", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            completedUnits.add(currentUnitId.toString())
            prefs.edit().putStringSet("completed_units", completedUnits).apply()
        }
    }

    private fun showFinalResult() {
        // Toplam puanı kaydet
        val prefs = requireContext().getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val prevScore = prefs.getInt("total_score", 0)
        prefs.edit().putInt("total_score", prevScore + score).apply()

        // Sonuç ekranı
        AlertDialog.Builder(requireContext())
            .setTitle("Quiz Completed!")
            .setMessage("Your total score: $score")
            .setPositiveButton("Back to Home") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MICROPHONE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // İzin verildi, konuşma tanıma başlatılabilir
            } else {
                Toast.makeText(context, "Mikrofon izni verilmedi", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 
