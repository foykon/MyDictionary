package com.example.mydictionary.ui.dictionary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.mydictionary.data.Word
import com.example.mydictionary.data.WordDatabase
import kotlinx.coroutines.launch

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WordDatabase.getDatabase(application)
    private val wordDao = database.wordDao()

    val words: LiveData<List<Word>> = wordDao.getAllWords().asLiveData()

    fun insertWord(word: Word) {
        viewModelScope.launch {
            wordDao.insertWord(word)
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            wordDao.deleteWord(word)
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            wordDao.updateWord(word)
        }
    }

    fun deleteAllWords() {
        viewModelScope.launch {
            wordDao.deleteAllWords()
        }
    }
} 