from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class Metric:
    name: str
    value: float
    explanation: str


@dataclass(frozen=True)
class DecisionPoint:
    label: str
    severity: int
    current_choice: str
    recommended_choice: str
    reason: str
    training_focus: str

    def severity_label(self) -> str:
        if self.severity >= 8:
            return "high"
        if self.severity >= 5:
            return "medium"
        return "low"


@dataclass(frozen=True)
class Situation:
    game: str
    title: str
    raw: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class EvaluationReport:
    situation: Situation
    metrics: list[Metric]
    decisions: list[DecisionPoint]
    summary: str

    def to_markdown(self) -> str:
        lines = [
            f"# {self.situation.title}",
            "",
            f"Game: {self.situation.game}",
            "",
            "## Summary",
            "",
            self.summary,
            "",
            "## Metrics",
            "",
        ]
        for metric in self.metrics:
            lines.append(f"- **{metric.name}**: {metric.value:.2f} - {metric.explanation}")
        lines.extend(["", "## Key Decision Points", ""])
        for index, decision in enumerate(self.decisions, start=1):
            lines.extend(
                [
                    f"### {index}. {decision.label} ({decision.severity_label()})",
                    "",
                    f"- Current: {decision.current_choice}",
                    f"- Recommended: {decision.recommended_choice}",
                    f"- Reason: {decision.reason}",
                    f"- Training focus: {decision.training_focus}",
                    "",
                ]
            )
        return "\n".join(lines).strip() + "\n"


def top_decisions(decisions: list[DecisionPoint], limit: int = 5) -> list[DecisionPoint]:
    return sorted(decisions, key=lambda item: item.severity, reverse=True)[:limit]
