from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from .core import DecisionPoint, EvaluationReport, Metric, Situation, top_decisions


def load_mahjong_round(path: str | Path) -> dict[str, Any]:
    with Path(path).open("r", encoding="utf-8") as file:
        return json.load(file)


def evaluate_mahjong_round(round_data: dict[str, Any]) -> EvaluationReport:
    decisions: list[DecisionPoint] = []

    for turn in round_data.get("turns", []):
        efficiency_loss = _number(turn, "ukeire_best") - _number(turn, "ukeire_chosen")
        danger_gap = _number(turn, "chosen_danger") - _number(turn, "best_danger")
        push_risk = _number(turn, "chosen_danger") * _number(turn, "opponent_pressure")
        shanten = _number(turn, "shanten_after")

        if efficiency_loss >= 4:
            decisions.append(
                DecisionPoint(
                    label=f"Turn {turn.get('turn')}: tile efficiency loss",
                    severity=min(10, int(4 + efficiency_loss / 2 + danger_gap)),
                    current_choice=f"Discarded {turn.get('chosen_discard')}",
                    recommended_choice=f"Prefer {turn.get('best_discard')}",
                    reason=(
                        f"The chosen discard loses {efficiency_loss:.0f} effective tiles compared "
                        "with the best candidate, which slows the hand before tenpai."
                    ),
                    training_focus="Review shanten and ukeire before choosing a safe-looking discard.",
                )
            )

        if push_risk >= 0.45 and shanten >= 1:
            decisions.append(
                DecisionPoint(
                    label=f"Turn {turn.get('turn')}: over-push risk",
                    severity=min(10, int(5 + push_risk * 6 + shanten)),
                    current_choice=f"Discarded {turn.get('chosen_discard')}",
                    recommended_choice=f"Consider {turn.get('safest_discard', turn.get('best_discard'))}",
                    reason=(
                        "Opponent pressure is high while the hand is not ready. The chosen tile carries "
                        "too much deal-in risk for the current hand speed."
                    ),
                    training_focus="Practice push/fold thresholds when one shanten or worse under riichi pressure.",
                )
            )

        if danger_gap >= 0.35 and efficiency_loss <= 2:
            decisions.append(
                DecisionPoint(
                    label=f"Turn {turn.get('turn')}: unnecessary danger",
                    severity=min(10, int(5 + danger_gap * 8)),
                    current_choice=f"Discarded {turn.get('chosen_discard')}",
                    recommended_choice=f"Prefer safer {turn.get('best_discard')}",
                    reason=(
                        "The selected tile is much more dangerous without buying meaningful speed or value."
                    ),
                    training_focus="When candidates have similar efficiency, downgrade clearly dangerous tiles.",
                )
            )

    metrics = [
        Metric("Average ukeire loss", _average_ukeire_loss(round_data), "Lower is better."),
        Metric("Average chosen danger", _average(round_data, "chosen_danger"), "Estimated deal-in risk proxy."),
        Metric("Pressure exposure", _average_pressure_exposure(round_data), "Risk taken under opponent pressure."),
    ]
    selected = top_decisions(decisions, limit=5)
    summary = (
        "This demo report focuses on tile efficiency, danger, and push/fold discipline. "
        f"It found {len(selected)} review-worthy decision point(s) from the provided structured round."
    )
    situation = Situation(
        game="Mahjong Soul",
        title=round_data.get("title", "Mahjong Soul Review Demo"),
        raw=round_data,
    )
    return EvaluationReport(situation=situation, metrics=metrics, decisions=selected, summary=summary)


def _number(data: dict[str, Any], key: str) -> float:
    value = data.get(key, 0)
    return float(value if value is not None else 0)


def _average(round_data: dict[str, Any], key: str) -> float:
    turns = round_data.get("turns", [])
    if not turns:
        return 0.0
    return sum(_number(turn, key) for turn in turns) / len(turns)


def _average_ukeire_loss(round_data: dict[str, Any]) -> float:
    turns = round_data.get("turns", [])
    if not turns:
        return 0.0
    return sum(_number(turn, "ukeire_best") - _number(turn, "ukeire_chosen") for turn in turns) / len(turns)


def _average_pressure_exposure(round_data: dict[str, Any]) -> float:
    turns = round_data.get("turns", [])
    if not turns:
        return 0.0
    return sum(_number(turn, "chosen_danger") * _number(turn, "opponent_pressure") for turn in turns) / len(turns)
