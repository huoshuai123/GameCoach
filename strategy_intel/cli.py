from __future__ import annotations

import argparse

from .mahjong import evaluate_mahjong_round, load_mahjong_round
from .san11 import evaluate_san11_state, load_san11_state


def main() -> None:
    parser = argparse.ArgumentParser(description="Strategy intelligence demo CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    mahjong = subparsers.add_parser("mahjong", help="Run Mahjong Soul review demo")
    mahjong.add_argument("--input", required=True, help="Path to structured mahjong round JSON")

    san11 = subparsers.add_parser("san11", help="Run San11 AI mod demo")
    san11.add_argument("--input", required=True, help="Path to San11 state JSON")

    args = parser.parse_args()
    if args.command == "mahjong":
        report = evaluate_mahjong_round(load_mahjong_round(args.input))
    elif args.command == "san11":
        report = evaluate_san11_state(load_san11_state(args.input))
    else:
        parser.error(f"Unknown command: {args.command}")
        return

    print(report.to_markdown())


if __name__ == "__main__":
    main()
