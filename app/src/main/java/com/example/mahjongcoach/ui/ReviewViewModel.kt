package com.example.mahjongcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.mahjongcoach.data.SampleRoundRepository
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.evaluator.MahjongRoundEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SampleRoundRepository(application)
    private val evaluator = MahjongRoundEvaluator()
    private val mutableState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)

    val state: StateFlow<ReviewUiState> = mutableState.asStateFlow()

    init {
        loadSampleRound()
    }

    fun loadSampleRound() {
        mutableState.value = try {
            val report = evaluator.evaluate(repository.loadSampleRound())
            ReviewUiState.Ready(report)
        } catch (error: Exception) {
            ReviewUiState.Error(error.message ?: "Unable to load sample round.")
        }
    }

    fun selectDecision(decision: DecisionPoint) {
        val current = mutableState.value
        if (current is ReviewUiState.Ready) {
            mutableState.value = current.copy(selectedDecision = decision)
        }
    }
}
