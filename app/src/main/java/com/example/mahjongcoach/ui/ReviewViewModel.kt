package com.example.mahjongcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.mahjongcoach.data.PaipuLinkParser
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
        showLinkEntry()
    }

    fun showLinkEntry() {
        mutableState.value = ReviewUiState.LinkEntry(repository.listSamples())
    }

    fun updateLinkInput(input: String) {
        val current = mutableState.value
        mutableState.value = ReviewUiState.LinkEntry(
            samples = repository.listSamples(),
            input = input,
            parsedLink = (current as? ReviewUiState.LinkEntry)?.parsedLink,
        )
    }

    fun parseCurrentLink() {
        val current = mutableState.value as? ReviewUiState.LinkEntry ?: return
        mutableState.value = current.copy(parsedLink = PaipuLinkParser.parse(current.input))
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
