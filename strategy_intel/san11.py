from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from .core import DecisionPoint, EvaluationReport, Metric, Situation, top_decisions


def load_san11_state(path: str | Path) -> dict[str, Any]:
    with Path(path).open("r", encoding="utf-8") as file:
        return json.load(file)


def evaluate_san11_state(state: dict[str, Any]) -> EvaluationReport:
    decisions: list[DecisionPoint] = []
    forces = state.get("forces", [])

    for force in forces:
        name = force.get("name", "Unknown force")
        target = _choose_target(force)
        assembly_gap = _number(force, "available_troops") - _number(force, "frontier_troops")
        supply_ratio = _ratio(_number(force, "food"), max(_number(force, "available_troops"), 1))
        threat = _number(force, "neighbor_threat")
        cohesion = _number(force, "target_cohesion")

        if not force.get("strategic_goal"):
            decisions.append(
                DecisionPoint(
                    label=f"{name}: missing strategic goal",
                    severity=8,
                    current_choice="No stable expansion goal",
                    recommended_choice=f"Set primary target to {target}",
                    reason="A force without a stable target is easy for players to exploit through tempo and baiting.",
                    training_focus="Assign each force a phase goal: consolidate, expand, punish weak neighbor, or contain leader.",
                )
            )

        if assembly_gap > 25000 and threat < 0.65:
            decisions.append(
                DecisionPoint(
                    label=f"{name}: under-assembled frontier",
                    severity=7,
                    current_choice="Troops stay away from the active front",
                    recommended_choice=f"Move reserves toward {target} before declaring attack",
                    reason="The force has enough troops overall but lacks a coherent staging point.",
                    training_focus="Prefer staged attacks over scattered one-city sorties.",
                )
            )

        if supply_ratio < 1.8:
            decisions.append(
                DecisionPoint(
                    label=f"{name}: campaign supply risk",
                    severity=6,
                    current_choice="Prepare attack with thin food reserves",
                    recommended_choice="Delay offensive and run a supply consolidation phase",
                    reason="Low food-to-troop ratio makes the AI collapse after a promising opening.",
                    training_focus="Gate offensive behavior behind minimum campaign supplies.",
                )
            )

        if cohesion < 0.45 and len(force.get("candidate_targets", [])) > 1:
            decisions.append(
                DecisionPoint(
                    label=f"{name}: target switching risk",
                    severity=7,
                    current_choice="Multiple comparable targets",
                    recommended_choice=f"Lock target preference to {target} for several turns",
                    reason="Frequent target switching creates the classic AI pattern of marching without strategic intent.",
                    training_focus="Add hysteresis: changing targets should require a strong new reason.",
                )
            )

    metrics = [
        Metric("Average strategic cohesion", _average(forces, "target_cohesion"), "Higher means forces keep stable goals."),
        Metric("Average neighbor threat", _average(forces, "neighbor_threat"), "Pressure from adjacent enemies."),
        Metric("Average supply ratio", _average_supply_ratio(forces), "Food divided by available troops."),
    ]
    selected = top_decisions(decisions, limit=5)
    summary = (
        "This demo converts force status into Mod-tuning priorities: stable targets, staged attacks, "
        f"and fewer exploitable AI openings. It found {len(selected)} high-value tuning point(s)."
    )
    situation = Situation(
        game="San11",
        title=state.get("title", "San11 AI Mod Demo"),
        raw=state,
    )
    return EvaluationReport(situation=situation, metrics=metrics, decisions=selected, summary=summary)


def _choose_target(force: dict[str, Any]) -> str:
    candidates = force.get("candidate_targets", [])
    if not candidates:
        return "nearest weak neighbor"
    return max(candidates, key=lambda item: item.get("score", 0)).get("city", "highest-score target")


def _number(data: dict[str, Any], key: str) -> float:
    value = data.get(key, 0)
    return float(value if value is not None else 0)


def _ratio(numerator: float, denominator: float) -> float:
    if denominator == 0:
        return 0.0
    return numerator / denominator


def _average(items: list[dict[str, Any]], key: str) -> float:
    if not items:
        return 0.0
    return sum(_number(item, key) for item in items) / len(items)


def _average_supply_ratio(forces: list[dict[str, Any]]) -> float:
    if not forces:
        return 0.0
    return sum(_ratio(_number(force, "food"), max(_number(force, "available_troops"), 1)) for force in forces) / len(forces)
