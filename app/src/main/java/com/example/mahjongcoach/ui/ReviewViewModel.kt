package com.example.mahjongcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahjongcoach.data.FinalPaipuDownloader
import com.example.mahjongcoach.data.PaipuDetailDownloader
import com.example.mahjongcoach.data.PaipuLinkParser
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
    private val detailDownloader = PaipuDetailDownloader()
    private val finalPaipuDownloader = FinalPaipuDownloader()
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

    fun parseCurrentLink() {
        val current = mutableState.value as? ReviewUiState.LinkEntry ?: return
        mutableState.value = current.copy(
            parsedLink = PaipuLinkParser.parse(current.input),
            paipuDetail = null,
            finalPaipuDownload = null,
            isDownloading = false,
        )
    }

    fun downloadPaipuDetail() {
        val current = mutableState.value as? ReviewUiState.LinkEntry ?: return
        val parsed = current.parsedLink ?: PaipuLinkParser.parse(current.input)
        mutableState.value = current.copy(parsedLink = parsed, isDownloading = true, paipuDetail = null)

        viewModelScope.launch {
            val detail = withContext(Dispatchers.IO) {
                detailDownloader.download(parsed)
            }
            val latest = mutableState.value as? ReviewUiState.LinkEntry ?: return@launch
            mutableState.value = latest.copy(isDownloading = false, paipuDetail = detail, finalPaipuDownload = null)
        }
    }

    fun prepareFinalPaipuDownload() {
        val current = mutableState.value as? ReviewUiState.LinkEntry ?: return
        val detail = current.paipuDetail ?: return
        mutableState.value = current.copy(finalPaipuDownload = finalPaipuDownloader.prepareDownload(detail))
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
