package com.example.mahjongcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mahjongcoach.data.FinalPaipuDownload
import com.example.mahjongcoach.data.FinalPaipuDownloadStatus
import com.example.mahjongcoach.data.PaipuDetail
import com.example.mahjongcoach.data.PaipuHistoryEntry
import com.example.mahjongcoach.data.ParsedPaipuLink
import com.example.mahjongcoach.data.SampleRound
import com.example.mahjongcoach.data.TurnCandidateSnapshot
import com.example.mahjongcoach.data.TurnContextSnapshot
import com.example.mahjongcoach.domain.DecisionPoint
import com.example.mahjongcoach.domain.EvaluationReport
import com.example.mahjongcoach.domain.Metric
import com.example.mahjongcoach.domain.Priority
import com.example.mahjongcoach.ui.ReviewUiState
import com.example.mahjongcoach.ui.ReviewViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MahjongCoachApp()
        }
    }
}

@Composable
fun MahjongCoachApp(viewModel: ReviewViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = Color(0xFF22665A),
            secondary = Color(0xFFC7772A),
            background = Color(0xFFF7F4EE),
            surface = Color(0xFFFFFCF7),
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            color = MaterialTheme.colors.background,
        ) {
            when (val current = state) {
                ReviewUiState.Loading -> LoadingScreen()
                is ReviewUiState.Error -> ErrorScreen(current.message) { viewModel.showLinkEntry() }
                is ReviewUiState.LinkEntry -> LinkEntryScreen(
                    input = current.input,
                    parsedLink = current.parsedLink,
                    paipuDetail = current.paipuDetail,
                    finalPaipuDownload = current.finalPaipuDownload,
                    history = current.history,
                    isDownloading = current.isDownloading,
                    onInputChanged = viewModel::updateLinkInput,
                    onImportClick = viewModel::importPublicPaipu,
                    onHistoryClick = viewModel::openHistory,
                )
                is ReviewUiState.Ready -> ReviewScreen(
                    selectedSample = current.selectedSample,
                    report = current.report,
                    selectedDecision = current.selectedDecision,
                    onDecisionSelected = viewModel::selectDecision,
                    onBackToLinkEntry = viewModel::showLinkEntry,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("正在读取样例牌谱")
    }
}

@Composable
private fun LinkEntryScreen(
    input: String,
    parsedLink: ParsedPaipuLink?,
    paipuDetail: PaipuDetail?,
    finalPaipuDownload: FinalPaipuDownload?,
    history: List<PaipuHistoryEntry>,
    isDownloading: Boolean,
    onInputChanged: (String) -> Unit,
    onImportClick: () -> Unit,
    onHistoryClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("雀魂复盘教练", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("粘贴公开雀魂牌谱链接，一次导入后进入复盘流程。", color = Color(0xFF5A625F))
        }

        item {
            LinkInputCard(
                input = input,
                parsedLink = parsedLink,
                paipuDetail = paipuDetail,
                finalPaipuDownload = finalPaipuDownload,
                isDownloading = isDownloading,
                onInputChanged = onInputChanged,
                onImportClick = onImportClick,
            )
        }

        if (history.isNotEmpty()) {
            item {
                HistoryPanel(
                    history = history,
                    onHistoryClick = onHistoryClick,
                )
            }
        }

        item {
            Text("历史记录保存在本机；打开历史牌谱会直接读取已下载内容。", color = Color(0xFF5A625F))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LinkInputCard(
    input: String,
    parsedLink: ParsedPaipuLink?,
    paipuDetail: PaipuDetail?,
    finalPaipuDownload: FinalPaipuDownload?,
    isDownloading: Boolean,
    onInputChanged: (String) -> Unit,
    onImportClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp,
        backgroundColor = Color(0xFFFFFCF7),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("导入牌谱", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            TextField(
                value = input,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(MahjongCoachTestTags.LinkInput),
                placeholder = { Text("粘贴雀魂公开牌谱链接") },
            )
            Button(
                onClick = onImportClick,
                enabled = input.isNotBlank() && !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(MahjongCoachTestTags.ImportButton),
            ) {
                Text(if (isDownloading) "正在导入" else "导入牌谱")
            }
            if (isDownloading) {
                ImportProgressPanel()
            }
            if (!isDownloading && (parsedLink != null || paipuDetail != null || finalPaipuDownload != null)) {
                ImportResultPanel(
                    parsedLink = parsedLink,
                    detail = paipuDetail,
                    download = finalPaipuDownload,
                )
            }
        }
    }
}

@Composable
private fun HistoryPanel(
    history: List<PaipuHistoryEntry>,
    onHistoryClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("历史记录", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
        history.forEach { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHistoryClick(entry.uuid) },
                shape = RoundedCornerShape(8.dp),
                elevation = 1.dp,
                backgroundColor = Color(0xFFFFFCF7),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.title, fontWeight = FontWeight.Bold)
                    if (entry.playerSummary.isNotBlank()) {
                        Text(entry.playerSummary, style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
                    }
                    Text(entry.uuid, style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
                }
            }
        }
    }
}

@Composable
private fun ImportProgressPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEAF3EF), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator()
            Text("正在准备复盘数据", fontWeight = FontWeight.Bold, color = Color(0xFF184D44))
        }
        Text("这通常只需要几秒钟。", color = Color(0xFF5A625F))
    }
}

@Composable
private fun ImportResultPanel(
    parsedLink: ParsedPaipuLink?,
    detail: PaipuDetail?,
    download: FinalPaipuDownload?,
) {
    val success = download?.status == FinalPaipuDownloadStatus.Fetched
    val background = if (success) Color(0xFFEAF3EF) else Color(0xFFFFF2DD)
    val titleColor = if (success) Color(0xFF184D44) else Color(0xFF7A4A12)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MahjongCoachTestTags.ImportResult)
            .background(background, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (success) "导入完成" else "导入结果", fontWeight = FontWeight.Bold, color = titleColor)
        parsedLink?.let {
            DetailLine("来源", it.source.label)
            it.viewAccountId?.let { account -> DetailLine("视角账号", account.toString()) }
        }
        detail?.uuid?.let { DetailLine("牌谱 UUID", it) }
        download?.request?.let {
            DetailLine("公开解析源", it.majGgUrl)
        }
        download?.paipu?.let {
            DetailLine("玩家数", it.head.players.size.toString())
            DetailLine("局数", it.rounds.size.toString())
        }
        Text(download?.message ?: detail?.message ?: parsedLink?.message.orEmpty(), color = Color(0xFF5A625F))
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("牌谱读取失败", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text("重新加载")
        }
    }
}

@Composable
private fun ReviewScreen(
    selectedSample: SampleRound,
    report: EvaluationReport,
    selectedDecision: DecisionPoint?,
    onDecisionSelected: (DecisionPoint) -> Unit,
    onBackToLinkEntry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(MahjongCoachTestTags.ReviewScreen)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("雀魂复盘教练", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            Text(selectedSample.title, color = Color(0xFF5A625F))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBackToLinkEntry) {
                    Text("返回链接")
                }
            }
        }

        item {
            SituationContextPanel(report.situation.context)
        }

        item {
            SectionTitle("本局结论")
            Text(report.summary, style = MaterialTheme.typography.body1)
        }

        item {
            SectionTitle("训练重点")
            HighlightPanel(
                title = report.trainingFocus.theme,
                body = report.trainingFocus.nextAction,
                footnote = report.trainingFocus.evidence,
            )
        }

        item {
            SectionTitle("局势指标")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                report.metrics.forEach { MetricRow(it) }
            }
        }

        item {
            SectionTitle("关键决策点")
        }

        items(report.decisionPoints) { decision ->
            DecisionCard(
                decision = decision,
                selected = decision == selectedDecision,
                onClick = { onDecisionSelected(decision) },
            )
        }

        selectedDecision?.let { decision ->
            item {
                SectionTitle("决策详情")
                DecisionDetail(decision)
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SituationContextPanel(context: Map<String, String>) {
    val rows = listOfNotNull(
        context["room_rank"]?.takeIf { it.isNotBlank() }?.let { "房间段位" to it },
        context["result"]?.takeIf { it.isNotBlank() }?.let { "结果" to it },
        context["round"]?.takeIf { it.isNotBlank() }?.let { "局" to it },
        context["honba"]?.takeIf { it.isNotBlank() }?.let { "本场" to "${it}本场" },
    )
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFCF7), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { (label, value) ->
            DetailLine(label, value)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun HighlightPanel(title: String, body: String, footnote: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEAF3EF), RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF184D44))
        Spacer(Modifier.height(6.dp))
        Text(body)
        Spacer(Modifier.height(8.dp))
        Text(footnote, style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
    }
}

@Composable
private fun MetricRow(metric: Metric) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFCF7), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(metric.name, fontWeight = FontWeight.Bold)
            Text(metric.explanation, style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
        }
        Spacer(Modifier.width(12.dp))
        Text(String.format("%.2f", metric.value), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DecisionCard(
    decision: DecisionPoint,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        elevation = if (selected) 4.dp else 1.dp,
        backgroundColor = if (selected) Color(0xFFEAF3EF) else Color(0xFFFFFCF7),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriorityChip(decision.priority)
                Spacer(Modifier.width(8.dp))
                Text(decision.problemType.label, style = MaterialTheme.typography.caption)
            }
            Spacer(Modifier.height(8.dp))
            Text(decision.roundContextText(), style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
            Spacer(Modifier.height(4.dp))
            Text(decision.label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${decision.currentChoice} -> ${decision.recommendedChoice}")
            decision.aiRecommendedChoice?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.caption, color = Color(0xFF22665A))
            }
            decision.contextSnapshot?.let { snapshot ->
                Spacer(Modifier.height(8.dp))
                DecisionSnapshotSummary(decision, snapshot)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PriorityChip(priority: Priority) {
    val color = when (priority) {
        Priority.High -> Color(0xFFB33A2B)
        Priority.Medium -> Color(0xFFC7772A)
        Priority.Low -> Color(0xFF22665A)
    }
    Chip(
        onClick = {},
        colors = ChipDefaults.chipColors(backgroundColor = color.copy(alpha = 0.12f)),
    ) {
        Text(priority.label, color = color, style = MaterialTheme.typography.caption)
    }
}

@Composable
private fun DecisionDetail(decision: DecisionPoint) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFCF7), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        decision.roundLabel?.let { DetailLine("局", it) }
        decision.honba?.let { DetailLine("本场", "${it}本场") }
        DetailLine("巡目", decision.turn.toString())
        DetailLine("玩家选择", decision.currentChoice)
        DetailLine("推荐选择", decision.recommendedChoice)
        decision.aiSource?.let { DetailLine("AI 来源", it) }
        decision.aiRecommendedChoice?.let { DetailLine("AI 推荐", it) }
        decision.aiConfidence?.let { DetailLine("AI 推荐强度", String.format("%.2f", it)) }
        decision.aiStatus?.let { DetailLine("AI 状态", it) }
        decision.contextSnapshot?.let { DecisionSnapshotDetail(decision, it) }
        DetailLine("原因", decision.reason)
        DetailLine("训练建议", decision.trainingTip)
    }
}

@Composable
private fun DecisionSnapshotSummary(decision: DecisionPoint, snapshot: TurnContextSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3EFE6), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("手牌", style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
        TileWall(
            tiles = snapshot.hand,
            drawnTile = snapshot.drawnTile,
            highlightedTiles = decision.choiceTiles(),
        )
        if (snapshot.doraIndicators.isNotEmpty()) {
            Text("宝牌指示", style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
            TileRows(snapshot.doraIndicators.sortedTiles())
        }
        val pressure = snapshot.pressureText()
        if (pressure.isNotBlank()) {
            DetailLine("场况", pressure)
        }
    }
}

@Composable
private fun DecisionSnapshotDetail(decision: DecisionPoint, snapshot: TurnContextSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("当时手牌", fontWeight = FontWeight.Bold)
        TileWall(
            tiles = snapshot.hand,
            drawnTile = snapshot.drawnTile,
            highlightedTiles = decision.choiceTiles(),
        )
        if (snapshot.scores.isNotEmpty()) {
            DetailLine("点数", snapshot.scores.mapIndexed { seat, score -> "${seat.seatLabel()} $score" }.joinToString(" / "))
        }
        if (snapshot.doraIndicators.isNotEmpty()) {
            Text("宝牌指示", fontWeight = FontWeight.Bold)
            TileRows(snapshot.doraIndicators.sortedTiles())
        }
        val pressure = snapshot.pressureText()
        if (pressure.isNotBlank()) {
            DetailLine("场况压力", pressure)
        }
        val discards = snapshot.visibleDiscards.formatSeatTiles()
        if (discards.isNotBlank()) {
            DetailLine("弃牌河", discards)
        }
        val calls = snapshot.calls.formatCalls()
        if (calls.isNotBlank()) {
            DetailLine("副露", calls)
        }
        if (snapshot.candidates.isNotEmpty()) {
            Text("候选弃牌", fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                snapshot.candidates.forEach { candidate ->
                    CandidateLine(decision, candidate)
                }
            }
        }
    }
}

@Composable
private fun CandidateLine(decision: DecisionPoint, candidate: TurnCandidateSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F4EE), RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("打", fontWeight = FontWeight.Bold)
            TileBox(
                tile = candidate.discard,
                highlighted = decision.choiceTiles().contains(candidate.discard.normalizedTileKey()),
            )
        }
        Text(
            "向听 ${candidate.shantenAfter} · 有效牌 ${candidate.ukeire} · 危险度 ${String.format("%.2f", candidate.danger)}",
            style = MaterialTheme.typography.caption,
            color = Color(0xFF5A625F),
        )
        if (candidate.improvingTiles.isNotEmpty()) {
            Text("改良", style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
            TileRows(candidate.improvingTiles.sortedTiles())
        }
    }
}

@Composable
private fun TileWall(
    tiles: List<String>,
    drawnTile: String?,
    highlightedTiles: Set<String>,
) {
    val allTiles = tiles + listOfNotNull(drawnTile?.takeIf { it.isNotBlank() })
    TileRows(allTiles.sortedTiles(), highlightedTiles)
}

@Composable
private fun TileRows(
    tiles: List<String>,
    highlightedTiles: Set<String> = emptySet(),
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 3.dp
        val tileWidth = when {
            maxWidth < 240.dp -> 26.dp
            maxWidth < 300.dp -> 30.dp
            else -> 34.dp
        }
        val tileHeight = when {
            maxWidth < 240.dp -> 34.dp
            maxWidth < 300.dp -> 38.dp
            else -> 42.dp
        }
        val columns = ((maxWidth.value + gap.value) / (tileWidth.value + gap.value))
            .toInt()
            .coerceAtLeast(1)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            tiles.chunked(columns).forEach { rowTiles ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowTiles.forEach { tile ->
                        TileBox(
                            tile = tile,
                            highlighted = highlightedTiles.contains(tile.normalizedTileKey()),
                            width = tileWidth,
                            height = tileHeight,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileBox(
    tile: String,
    highlighted: Boolean = false,
    width: androidx.compose.ui.unit.Dp = 34.dp,
    height: androidx.compose.ui.unit.Dp = 42.dp,
) {
    val borderColor = if (highlighted) Color(0xFFC7772A) else Color(0xFFB8B0A3)
    val background = if (highlighted) Color(0xFFFFE6BD) else Color(0xFFFFFCF7)
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(background, RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            tile,
            textAlign = TextAlign.Center,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = tileTextColor(tile),
            style = MaterialTheme.typography.caption,
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
        Text(value)
    }
}

private fun DecisionPoint.roundContextText(): String {
    return listOfNotNull(
        roundLabel,
        honba?.let { "${it}本场" },
        "第 ${turn} 巡",
    ).joinToString(" · ")
}

private fun TurnContextSnapshot.pressureText(): String {
    val parts = buildList {
        if (riichiSeats.isNotEmpty()) {
            add("立直 ${riichiSeats.sorted().joinToString("、") { it.seatLabel() }}")
        }
        val calledSeats = calls.filterValues { it.isNotEmpty() }.keys.sorted()
        if (calledSeats.isNotEmpty()) {
            add("副露 ${calledSeats.joinToString("、") { it.seatLabel() }}")
        }
    }
    return parts.joinToString("；")
}

private fun Map<Int, List<String>>.formatSeatTiles(): String {
    return entries
        .sortedBy { it.key }
        .filter { it.value.isNotEmpty() }
        .joinToString(" / ") { (seat, tiles) -> "${seat.seatLabel()} ${tiles.joinToString(" ")}" }
}

private fun Map<Int, List<List<String>>>.formatCalls(): String {
    return entries
        .sortedBy { it.key }
        .filter { it.value.isNotEmpty() }
        .joinToString(" / ") { (seat, melds) ->
            val meldText = melds.joinToString(" | ") { it.joinToString(" ") }
            "${seat.seatLabel()} $meldText"
        }
}

private fun DecisionPoint.choiceTiles(): Set<String> {
    return Regex("""[0-9][mpsz]""")
        .findAll("$currentChoice $recommendedChoice")
        .map { it.value.normalizedTileKey() }
        .toSet()
}

private fun List<String>.sortedTiles(): List<String> {
    return sortedWith(compareBy<String> { it.tileSuitOrder() }.thenBy { it.tileNumberOrder() }.thenBy { it })
}

private fun String.normalizedTileKey(): String {
    val match = Regex("""[0-9][mpsz]""").find(this) ?: return this
    return match.value
}

private fun String.tileSuitOrder(): Int {
    return when (normalizedTileKey().lastOrNull()) {
        'm' -> 0
        'p' -> 1
        's' -> 2
        'z' -> 3
        else -> 4
    }
}

private fun String.tileNumberOrder(): Int {
    val number = normalizedTileKey().firstOrNull()?.digitToIntOrNull() ?: 10
    return if (number == 0) 5 else number
}

private fun tileTextColor(tile: String): Color {
    return when (tile.normalizedTileKey().lastOrNull()) {
        'm' -> Color(0xFFB33A2B)
        'p' -> Color(0xFF22665A)
        's' -> Color(0xFF2B5DA8)
        'z' -> Color(0xFF4F4A45)
        else -> Color(0xFF2C2A27)
    }
}

private fun Int.seatLabel(): String {
    return when (this) {
        0 -> "东家"
        1 -> "南家"
        2 -> "西家"
        3 -> "北家"
        else -> "${this}号位"
    }
}
