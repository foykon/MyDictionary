package com.example.mydictionary.data

data class QuizUnit(
    val id: Int,
    val name: String,
    val quizzes: List<QuizType>,
    var isCompleted: Boolean = false,
    var isUnlocked: Boolean = false
)

enum class QuizType {
    IMAGE_TO_WORD,
    WORD_TO_IMAGE,
    SOUND_QUIZ,
    SPEECH_QUIZ;

    private val completedUnits = mutableSetOf<Int>()

    fun isCompleted(unitId: Int): Boolean {
        return completedUnits.contains(unitId)
    }

    fun markAsCompleted(unitId: Int) {
        completedUnits.add(unitId)
    }

    fun reset(unitId: Int) {
        completedUnits.remove(unitId)
    }

    fun resetAll() {
        completedUnits.clear()
    }
} 