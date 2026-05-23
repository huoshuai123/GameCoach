package com.example.mahjongcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
                is ReviewUiState.Error -> ErrorScreen(current.message, viewModel::loadSampleRound)
                is ReviewUiState.Ready -> ReviewScreen(
                    report = current.report,
                    selectedDecision = current.selectedDecision,
                    onDecisionSelected = viewModel::selectDecision,
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
    report: EvaluationReport,
    selectedDecision: DecisionPoint?,
    onDecisionSelected: (DecisionPoint) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("雀魂复盘教练", style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold)
            Text(report.situation.title, color = Color(0xFF5A625F))
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
            Text(decision.label, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${decision.currentChoice} -> ${decision.recommendedChoice}")
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
        DetailLine("巡目", decision.turn.toString())
        DetailLine("玩家选择", decision.currentChoice)
        DetailLine("推荐选择", decision.recommendedChoice)
        DetailLine("原因", decision.reason)
        DetailLine("训练建议", decision.trainingTip)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.caption, color = Color(0xFF5A625F))
        Text(value)
    }
}
