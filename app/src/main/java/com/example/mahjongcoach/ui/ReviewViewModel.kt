package com.example.mahjongcoach.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahjongcoach.data.PaipuHistoryRepository
import com.example.mahjongcoach.data.PaipuImportPipeline
import com.example.mahjongcoach.data.SampleRound
import com.example.mahjongcoach.data.SampleRoundRepository
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.domain.EvaluationReport
import com.example.mahjongcoach.domain.withMjaiAssessments
import com.example.mahjongcoach.evaluator.FinalPaipuAnalyzer
import com.example.mahjongcoach.evaluator.MahjongRoundEvaluator
import com.example.mahjongcoach.mjai.MjaiReviewProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SampleRoundRepository(application)
    private val historyRepository = PaipuHistoryRepository(application)
    private val analyzer = FinalPaipuAnalyzer()
    private val evaluator = MahjongRoundEvaluator()
    private val importPipeline = PaipuImportPipeline()
    private val mjaiReviewProvider = MjaiReviewProvider()
    private val mutableState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)

    val state: StateFlow<ReviewUiState> = mutableState.asStateFlow()

    init {
        showLinkEntry()
    }

    fun showLinkEntry() {
        mutableState.value = ReviewUiState.LinkEntry(history = historyRepository.list())
    }

    fun updateLinkInput(input: String) {
        val current = mutableState.value
        mutableState.value = ReviewUiState.LinkEntry(
            input = input,
            parsedLink = (current as? ReviewUiState.LinkEntry)?.parsedLink,
            paipuDetail = null,
            finalPaipuDownload = null,
            history = (current as? ReviewUiState.LinkEntry)?.history ?: historyRepository.list(),
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
            ImportedReviewStateFactory.readyOrNull(result)?.let { ready ->
                withContext(Dispatchers.IO) {
                    result.finalPaipuDownload.paipu?.let { historyRepository.save(it) }
                }
                mutableState.value = ready
                result.finalPaipuDownload.paipu?.let { paipu ->
                    enhanceReportWithMjai(paipu, ready.report)
                }
                return@launch
            }
            val latest = mutableState.value as? ReviewUiState.LinkEntry ?: return@launch
            mutableState.value = latest.copy(
                isDownloading = false,
                parsedLink = result.parsedLink,
                paipuDetail = result.detail,
                finalPaipuDownload = result.finalPaipuDownload,
                history = historyRepository.list(),
            )
        }
    }

    fun openHistory(uuid: String) {
        viewModelScope.launch {
            mutableState.value = ReviewUiState.Loading
            val record = withContext(Dispatchers.IO) {
                historyRepository.load(uuid)
            }
            if (record == null) {
                mutableState.value = ReviewUiState.Error("未找到本地历史牌谱。")
                return@launch
            }

            mutableState.value = runCatching {
                withContext(Dispatchers.Default) {
                    val report = evaluator.evaluate(analyzer.analyze(record.paipu))
                    val sample = SampleRound(
                        id = "history-${record.entry.uuid}",
                        title = report.situation.title,
                        description = "从本地历史记录读取的复盘报告。",
                        focus = "历史牌谱分析",
                        assetName = "",
                    )
                    ReviewUiState.Ready(
                        samples = listOf(sample),
                        selectedSample = sample,
                        report = report,
                    )
                }
            }.getOrElse { error ->
                ReviewUiState.Error(error.message ?: "无法读取本地历史牌谱。")
            }
            (mutableState.value as? ReviewUiState.Ready)?.let { ready ->
                enhanceReportWithMjai(record.paipu, ready.report)
            }
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

    private fun enhanceReportWithMjai(
        paipu: com.example.mahjongcoach.data.FinalPaipu,
        ruleReport: EvaluationReport,
    ) {
        viewModelScope.launch {
            val enhanced = withContext(Dispatchers.IO) {
                val assessments = mjaiReviewProvider.assess(paipu, ruleReport)
                ruleReport.withMjaiAssessments(assessments)
            }
            val current = mutableState.value as? ReviewUiState.Ready ?: return@launch
            if (current.report.situation.title != ruleReport.situation.title) return@launch

            val selectedId = current.selectedDecision?.aiDecisionId
            mutableState.value = current.copy(
                report = enhanced,
                selectedDecision = enhanced.decisionPoints.firstOrNull { it.aiDecisionId == selectedId }
                    ?: enhanced.decisionPoints.firstOrNull(),
            )
        }
    }
}
