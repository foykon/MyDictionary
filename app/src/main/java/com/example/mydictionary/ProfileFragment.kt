package com.example.mydictionary

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileFragment : Fragment() {

    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var etProfileName: TextInputEditText
    private lateinit var btnSaveName: MaterialButton
    private lateinit var tvTotalScore: TextView
    private lateinit var rvLeaderboard: RecyclerView
    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private lateinit var userRepository: UserRepository
    private var currentUserId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database
        val database = UserDatabase.getDatabase(requireContext())
        userRepository = UserRepository(database.userDao())

        // Initialize views
        nameInputLayout = view.findViewById(R.id.nameInputLayout)
        etProfileName = view.findViewById(R.id.etProfileName)
        btnSaveName = view.findViewById(R.id.btnSaveName)
        tvTotalScore = view.findViewById(R.id.tvTotalScore)
        rvLeaderboard = view.findViewById(R.id.rvLeaderboard)

        // Setup RecyclerView
        leaderboardAdapter = LeaderboardAdapter()
        rvLeaderboard.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = leaderboardAdapter
        }

        // Load or create user ID
        currentUserId = getUserId()

        // Load saved name and score
        lifecycleScope.launch {
            val user = userRepository.getUser(currentUserId)
            if (user != null) {
                etProfileName.setText(user.username)
                tvTotalScore.text = user.score.toString()
            }
        }

        // Setup save button
        btnSaveName.setOnClickListener {
            val name = etProfileName.text.toString()
            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    userRepository.insertUser(User(currentUserId, name, 0))
                    Toast.makeText(context, "İsim kaydedildi", Toast.LENGTH_SHORT).show()
                }
            } else {
                nameInputLayout.error = "Lütfen bir isim girin"
            }
        }

        // Load and observe leaderboard data
        loadLeaderboardData()
    }

    private fun loadLeaderboardData() {
        lifecycleScope.launch {
            userRepository.allUsers.collectLatest { users ->
                if (users.isNotEmpty()) {
                    val leaderboardItems = users.mapIndexed { index, user ->
                        LeaderboardItem(
                            rank = index + 1,
                            username = user.username,
                            score = user.score
                        )
                    }
                    leaderboardAdapter.updateItems(leaderboardItems)
                    Log.d("ProfileFragment", "Loaded ${users.size} users to leaderboard")
                } else {
                    Log.d("ProfileFragment", "No users found in database")
                }
            }
        }
    }

    private fun getUserId(): String {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        var userId = prefs.getString("user_id", null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
            prefs.edit().putString("user_id", userId).apply()
        }
        return userId
    }
} 