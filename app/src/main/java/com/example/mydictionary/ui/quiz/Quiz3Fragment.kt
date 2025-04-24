package com.example.mydictionary.ui.quiz

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.mydictionary.R
import com.example.mydictionary.data.QuizType
import com.example.mydictionary.data.Word
import com.example.mydictionary.databinding.FragmentQuiz3Binding
import com.example.mydictionary.ui.dictionary.DictionaryViewModel
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class Quiz3Fragment : Fragment(), TextToSpeech.OnInitListener {
    private var _binding: FragmentQuiz3Binding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DictionaryViewModel
    private var currentQuestionIndex = 0
    private var score = 0
    private lateinit var questions: List<Quiz3Question>
    private var selectedAnswerCard: MaterialCardView? = null
    private lateinit var textToSpeech: TextToSpeech
    private var currentUnitId: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuiz3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]
        currentUnitId = arguments?.getInt("unitId", 1) ?: 1
        textToSpeech = TextToSpeech(requireContext(), this)
        setupQuiz()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "Türkçe dil desteği bulunamadı!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "TextToSpeech başlatılamadı!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupQuiz() {
        viewModel.words.observe(viewLifecycleOwner) { words ->
            if (words.size >= 5) {
                // Rastgele 5 kelime seç
                val randomWords = words.shuffled().take(5)
                questions = randomWords.map { word ->
                    // Her soru için 3 yanlış resim seç
                    val wrongImages = (words - word).shuffled().take(3)
                    val allImages = (wrongImages + word).shuffled()
                    Quiz3Question(word, allImages)
                }
                showQuestion(0)
            } else {
                Toast.makeText(context, "Lütfen en az 5 kelime ekleyin!", Toast.LENGTH_LONG).show()
            }
        }

        setupAnswerCardListeners()
        setupPlaySoundButton()
    }

    private fun setupPlaySoundButton() {
        binding.playSoundCard.setOnClickListener {
            playCurrentWordSound()
        }
    }

    private fun playCurrentWordSound() {
        val currentQuestion = questions[currentQuestionIndex]
        val word = currentQuestion.correctWord.word
        textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun showQuestion(index: Int) {
        val question = questions[index]
        binding.scoreText.text = "Puan: $score"
        
        // Kartları sıfırla
        resetAllCards()

        // Resimleri göster
        val answerImages = listOf(
            binding.answer1,
            binding.answer2,
            binding.answer3,
            binding.answer4
        )

        // Resimleri ve kelimeleri birlikte karıştır
        val shuffledPairs = question.allImages.mapIndexed { index, word -> 
            Pair(word, answerImages[index])
        }.shuffled()
        
        // Tüm resimleri göster ve tag'leri ayarla
        shuffledPairs.forEach { (word, imageView) ->
            imageView.tag = word.imageUrl
            Glide.with(this)
                .load(word.imageUrl)
                .into(imageView)
        }
    }

    private fun resetAllCards() {
        val answerCards = listOf(
            binding.answer1Card to "#FFE066",
            binding.answer2Card to "#98FB98",
            binding.answer3Card to "#87CEEB",
            binding.answer4Card to "#DDA0DD"
        )

        answerCards.forEach { (card, color) ->
            card.isEnabled = true
            card.alpha = 1.0f
            card.strokeWidth = 0
            card.strokeColor = 0
            card.setCardBackgroundColor(android.graphics.Color.parseColor(color))
        }
        selectedAnswerCard = null
    }

    private fun setupAnswerCardListeners() {
        val answerCards = listOf(
            binding.answer1Card,
            binding.answer2Card,
            binding.answer3Card,
            binding.answer4Card
        )

        answerCards.forEach { card ->
            card.setOnClickListener {
                handleAnswerSelection(card)
            }
        }
    }

    private fun handleAnswerSelection(selectedCard: MaterialCardView) {
        // Clear previous selection
        selectedAnswerCard?.isChecked = false
        
        // Mark new selection
        selectedCard.isChecked = true
        selectedAnswerCard = selectedCard

        // Check answer
        val selectedImageUrl = when (selectedCard) {
            binding.answer1Card -> binding.answer1.tag
            binding.answer2Card -> binding.answer2.tag
            binding.answer3Card -> binding.answer3.tag
            binding.answer4Card -> binding.answer4.tag
            else -> null
        }

        val currentQuestion = questions[currentQuestionIndex]
        if (selectedImageUrl == currentQuestion.correctWord.imageUrl) {
            // Correct answer
            score += 10
            val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
            selectedCard.startAnimation(bounceAnimation)
            
            // Move to next question after delay
            selectedCard.postDelayed({
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    showQuestion(currentQuestionIndex)
                } else {
                    // Quiz complete
                    markQuizAsCompleted()
                    showFinalResult()
                }
            }, 1000)
        } else {
            // Wrong answer
            val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)
            selectedCard.startAnimation(shakeAnimation)
        }
    }

    private fun disableAllCards() {
        val answerCards = listOf(
            binding.answer1Card,
            binding.answer2Card,
            binding.answer3Card,
            binding.answer4Card
        )

        answerCards.forEach { card ->
            card.isEnabled = false
            card.alpha = 0.7f
        }
    }

    private fun showFinalResult() {
        // Birikmiş toplam puanı güncelle
        val prefs = requireContext().getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val prev = prefs.getInt("total_score", 0)
        prefs.edit().putInt("total_score", prev + score).apply()
        
        val message = when (score) {
            50 -> "Awesome!"
            in 40..49 -> "Well done!"
            in 30..39 -> "Good work! Keep practicing!"
            in 20..29 -> "Nice try! You can do better!"
            else -> "Keep learning! Practice makes perfect!"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Quiz Complete!")
            .setMessage("Your score: $score/50\n\n$message")
            .setPositiveButton("Tekrar Dene") { _, _ ->
                // Quiz'i sıfırla
                score = 0
                currentQuestionIndex = 0
                setupQuiz()
            }
            .setNegativeButton("Ana Sayfaya Dön") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun markQuizAsCompleted() {
        QuizType.SOUND_QUIZ.markAsCompleted(currentUnitId)
        
        // Save completion status with unit ID
        val prefs = requireContext().getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val completedQuizzes = prefs.getStringSet("completed_quizzes", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        completedQuizzes.add("${currentUnitId}_${QuizType.SOUND_QUIZ.name}")
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

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech.stop()
        textToSpeech.shutdown()
        _binding = null
    }
}

data class Quiz3Question(
    val correctWord: Word,
    val allImages: List<Word>
) 