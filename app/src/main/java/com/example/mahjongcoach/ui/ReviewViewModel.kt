package com.example.mahjongcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahjongcoach.data.PaipuImportPipeline
import com.example.mahjongcoach.data.SampleRoundRepository
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.evaluator.MahjongRoundEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SampleRoundRepository(application)
    private val evaluator = MahjongRoundEvaluator()
    private val importPipeline = PaipuImportPipeline()
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
            paipuDetail = null,
            finalPaipuDownload = null,
        )
    }

    fun importPublicPaipu() {
        val current = mutableState.value as? ReviewUiState.LinkEntry ?: return
        mutableState.value = current.copy(
            isDownloading = true,
            paipuDetail = null,
            finalPaipuDownload = null,
        )

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                importPipeline.import(current.input)
            }
            ImportedReviewStateFactory.readyOrNull(repository.listSamples(), result)?.let { ready ->
                mutableState.value = ready
                return@launch
            }
            val latest = mutableState.value as? ReviewUiState.LinkEntry ?: return@launch
            mutableState.value = latest.copy(
                isDownloading = false,
                parsedLink = result.parsedLink,
                paipuDetail = result.detail,
                finalPaipuDownload = result.finalPaipuDownload,
            )
        }
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
