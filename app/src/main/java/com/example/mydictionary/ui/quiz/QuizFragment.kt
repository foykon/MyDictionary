package com.example.mydictionary.ui.quiz

import android.os.Bundle
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
import com.example.mydictionary.data.Word
import com.example.mydictionary.databinding.FragmentQuizBinding
import com.example.mydictionary.ui.dictionary.DictionaryViewModel
import com.google.android.material.card.MaterialCardView

class QuizFragment : Fragment() {
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DictionaryViewModel
    private var currentQuestionIndex = 0
    private var score = 0
    private lateinit var questions: List<QuizQuestion>
    private var selectedAnswerCard: MaterialCardView? = null

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
        viewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]
        setupQuiz()
    }

    private fun setupQuiz() {
        viewModel.words.observe(viewLifecycleOwner) { words ->
            if (words.size >= 5) {
                // Randomly select 5 words
                val randomWords = words.shuffled().take(5)
                questions = randomWords.map { word ->
                    // Select 3 wrong answers for each question
                    val wrongAnswers = (words - word).shuffled().take(3)
                    val answers = (wrongAnswers + word).shuffled()
                    QuizQuestion(word, answers)
                }
                showQuestion(0)
            } else {
                Toast.makeText(context, "Please add at least 5 words!", Toast.LENGTH_LONG).show()
            }
        }

        setupAnswerCardListeners()
    }

    private fun showQuestion(index: Int) {
        val question = questions[index]
        binding.scoreText.text = "Score: $score"
        
        // Kartları sıfırla
        resetAllCards()

        // Resmi göster
        Glide.with(this)
            .load(question.correctWord.imageUrl)
            .into(binding.quizImage)

        // Cevapları karıştır ve göster
        val shuffledOptions = question.answers.shuffled()
        binding.answer1.text = shuffledOptions[0].word
        binding.answer2.text = shuffledOptions[1].word
        binding.answer3.text = shuffledOptions[2].word
        binding.answer4.text = shuffledOptions[3].word
    }

    private fun resetAllCards() {
        val answerCards = listOf(
            binding.answer1Card to "#FFE066",
            binding.answer2Card to "#98FB98",
            binding.answer3Card to "#87CEEB",
            binding.answer4Card to "#DDA0DD"
        )

        answerCards.forEach { (card, color) ->
            // Sadece renkleri ve etkin durumunu sıfırla
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
        if (!selectedCard.isEnabled) return

        // Tüm kartları geçici olarak devre dışı bırak
        disableAllCards()
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
        if (selectedAnswer == currentQuestion.correctWord.word) {
            // Doğru cevap animasyonu ve geri bildirimi
            score += 10
            selectedCard.setCardBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
            selectedCard.strokeWidth = 4
            selectedCard.strokeColor = resources.getColor(android.R.color.holo_green_dark, null)
            val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
            selectedCard.startAnimation(bounceAnimation)
            
            Toast.makeText(context, "Doğru! +10 puan", Toast.LENGTH_SHORT).show()
            
            selectedCard.postDelayed({
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    showQuestion(currentQuestionIndex)
                } else {
                    showFinalResult()
                }
            }, 1000) // Süreyi 1 saniyeye düşürdüm
        } else {
            // Yanlış cevap animasyonu ve geri bildirimi
            selectedCard.setCardBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
            selectedCard.strokeWidth = 4
            selectedCard.strokeColor = resources.getColor(android.R.color.holo_red_dark, null)
            val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)
            selectedCard.startAnimation(shakeAnimation)
            
            // Doğru cevabı göster
            val correctCard = when (currentQuestion.correctWord.word) {
                binding.answer1.text -> binding.answer1Card
                binding.answer2.text -> binding.answer2Card
                binding.answer3.text -> binding.answer3Card
                binding.answer4.text -> binding.answer4Card
                else -> null
            }
            
            correctCard?.let {
                it.setCardBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
                it.strokeWidth = 4
                it.strokeColor = resources.getColor(android.R.color.holo_green_dark, null)
            }
            
            Toast.makeText(context, "Tekrar dene!", Toast.LENGTH_SHORT).show()
            
            selectedCard.postDelayed({
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    showQuestion(currentQuestionIndex)
                } else {
                    showFinalResult()
                }
            }, 1500) // Süreyi 1.5 saniyeye düşürdüm
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
        val message = when (score) {
            50 -> "Perfect! You're a vocabulary master!"
            in 40..49 -> "Great job! Almost perfect!"
            in 30..39 -> "Good work! Keep practicing!"
            in 20..29 -> "Nice try! You can do better!"
            else -> "Keep learning! Practice makes perfect!"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Quiz Complete!")
            .setMessage("Your score: $score/50\n\n$message")
            .setPositiveButton("Try Again") { _, _ ->
                // Reset quiz
                score = 0
                currentQuestionIndex = 0
                setupQuiz()
            }
            .setNegativeButton("Back to Home") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class QuizQuestion(
    val correctWord: Word,
    val answers: List<Word>
) 