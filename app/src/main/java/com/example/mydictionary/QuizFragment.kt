package com.example.mydictionary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.example.mydictionary.databinding.FragmentQuizBinding
import com.google.android.material.card.MaterialCardView

class QuizFragment : Fragment() {
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private var score = 0
    private var currentQuestionIndex = 0
    private var selectedAnswerCard: MaterialCardView? = null

    // Örnek sorular (gerçek uygulamada veritabanından gelecek)
    private val questions = listOf(
        QuizQuestion(
            imageResId = android.R.drawable.ic_menu_gallery, // Geçici olarak varsayılan bir ikon kullanıyoruz
            correctAnswer = "Elma",
            options = listOf("Elma", "Armut", "Muz", "Portakal")
        ),
        QuizQuestion(
            imageResId = android.R.drawable.ic_menu_camera,
            correctAnswer = "Armut",
            options = listOf("Elma", "Armut", "Muz", "Portakal")
        ),
        QuizQuestion(
            imageResId = android.R.drawable.ic_menu_edit,
            correctAnswer = "Muz",
            options = listOf("Elma", "Armut", "Muz", "Portakal")
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupQuestion()
        setupAnswerCardListeners()
    }

    private fun setupQuestion() {
        val currentQuestion = questions[currentQuestionIndex]
        binding.quizImage.setImageResource(currentQuestion.imageResId)
        
        // Cevapları karıştır
        val shuffledOptions = currentQuestion.options.shuffled()
        binding.answer1.text = shuffledOptions[0]
        binding.answer2.text = shuffledOptions[1]
        binding.answer3.text = shuffledOptions[2]
        binding.answer4.text = shuffledOptions[3]

        // Skor güncelle
        binding.scoreText.text = "Puan: $score"
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
        // Önceki seçimi temizle
        selectedAnswerCard?.isChecked = false
        
        // Yeni seçimi işaretle
        selectedCard.isChecked = true
        selectedAnswerCard = selectedCard

        // Cevabı kontrol et
        val selectedAnswer = when (selectedCard) {
            binding.answer1Card -> binding.answer1.text.toString()
            binding.answer2Card -> binding.answer2.text.toString()
            binding.answer3Card -> binding.answer3.text.toString()
            binding.answer4Card -> binding.answer4.text.toString()
            else -> ""
        }

        val currentQuestion = questions[currentQuestionIndex]
        if (selectedAnswer == currentQuestion.correctAnswer) {
            // Doğru cevap
            score += 10
            val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
            selectedCard.startAnimation(bounceAnimation)
            
            // Kısa bir gecikme sonra sonraki soruya geç
            selectedCard.postDelayed({
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    setupQuestion()
                } else {
                    // Quiz bitti, sonuç ekranına git
                    showResults()
                }
            }, 1000)
        } else {
            // Yanlış cevap
            val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)
            selectedCard.startAnimation(shakeAnimation)
        }
    }

    private fun showResults() {
        // Sonuç ekranına git
        // TODO: Implement results screen
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class QuizQuestion(
    val imageResId: Int,
    val correctAnswer: String,
    val options: List<String>
) 