package com.example.mydictionary.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mydictionary.LeaderboardAdapter
import com.example.mydictionary.LeaderboardItem
import com.example.mydictionary.User
import com.example.mydictionary.UserDatabase
import com.example.mydictionary.UserRepository
import com.example.mydictionary.databinding.FragmentProfileBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val PREFS_NAME = "quiz_prefs"
    private val KEY_NAME = "user_name"
    private val KEY_SCORE = "total_score"
    private val KEY_USER_ID = "user_id"
    
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var userRepository: UserRepository
    private var currentUserId: String = ""
    private var scoreUpdateJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database and repository
        val database = UserDatabase.getDatabase(requireContext())
        userRepository = UserRepository(database.userDao())

        // Setup RecyclerView
        leaderboardAdapter = LeaderboardAdapter()
        binding.rvLeaderboard.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = leaderboardAdapter
        }

        // Load or create user ID
        currentUserId = getUserId()

        // Load saved name and score
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_NAME, "") ?: ""
        val totalScore = prefs.getInt(KEY_SCORE, 0)

        binding.etProfileName.setText(savedName)
        binding.tvTotalScore.text = totalScore.toString()

        // Load and observe leaderboard data
        loadLeaderboardData()

        // Add some example users if the database is empty
        lifecycleScope.launch {
            if (userRepository.getUserCount() == 0) {
                addExampleUsers()
            }
        }

        // Start random score updates
        startRandomScoreUpdates()

        // İsmi kaydet butonu
        binding.btnSaveName.setOnClickListener {
            val name = binding.etProfileName.text.toString().trim()
            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    userRepository.insertUser(User(currentUserId, name, totalScore))
                    prefs.edit().putString(KEY_NAME, name).apply()
                    Toast.makeText(context, "Name saved", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRandomScoreUpdates() {
        scoreUpdateJob = lifecycleScope.launch {
            while (isActive) {
                // Random delay between 5 and 15 seconds
                delay(Random.nextLong(5000, 15000))
                updateRandomUserScore()
            }
        }
    }

    private suspend fun updateRandomUserScore() {
        val users = userRepository.allUsers.first()
        if (users.isNotEmpty()) {
            // Filter out current user
            val otherUsers = users.filter { it.id != currentUserId }
            if (otherUsers.isNotEmpty()) {
                // Select random user
                val randomUser = otherUsers.random()
                // Add random score that is a multiple of 10 (10, 20, 30, 40, or 50)
                val scoreIncrease = Random.nextInt(1, 6) * 10
                val newScore = randomUser.score + scoreIncrease
                userRepository.updateUserScore(randomUser.id, newScore)
            }
        }
    }

    private fun loadLeaderboardData() {
        lifecycleScope.launch {
            userRepository.allUsers.collectLatest { users ->
                val leaderboardItems = users.mapIndexed { index, user ->
                    LeaderboardItem(
                        rank = index + 1,
                        username = user.username,
                        score = user.score
                    )
                }
                leaderboardAdapter.updateItems(leaderboardItems)
            }
        }
    }

    private suspend fun addExampleUsers() {
        val exampleUsers = listOf(
            User(UUID.randomUUID().toString(), "Furkan YILDIZ", 150),
            User(UUID.randomUUID().toString(), "Mert ÇALIŞKAN", 120),
            User(UUID.randomUUID().toString(), "Ali DEMİR", 100),
            User(UUID.randomUUID().toString(), "Yunus EMRE", 90),
            User(UUID.randomUUID().toString(), "Senan DEMİR", 80)
        )
        exampleUsers.forEach { user ->
            userRepository.insertUser(user)
        }
    }

    private fun getUserId(): String {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scoreUpdateJob?.cancel()
        _binding = null
    }
} 