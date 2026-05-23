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
        showSampleList()
    }

    fun showSampleList() {
        mutableState.value = ReviewUiState.SampleList(repository.listSamples())
    }

    fun loadSampleRound(id: String = repository.listSamples().first().id) {
        mutableState.value = try {
            val sample = repository.listSamples().first { it.id == id }
            val report = evaluator.evaluate(repository.loadSampleRound(id))
            ReviewUiState.Ready(
                samples = repository.listSamples(),
                selectedSample = sample,
                report = report,
            )
        } catch (error: Exception) {
            ReviewUiState.Error(error.message ?: "无法读取样例牌谱。")
        }
    }

    fun selectDecision(decision: DecisionPoint) {
        val current = mutableState.value
        if (current is ReviewUiState.Ready) {
            mutableState.value = current.copy(selectedDecision = decision)
        }
    }
}
