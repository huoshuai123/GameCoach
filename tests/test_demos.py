import unittest

from strategy_intel.mahjong import evaluate_mahjong_round, load_mahjong_round
from strategy_intel.san11 import evaluate_san11_state, load_san11_state


class DemoTests(unittest.TestCase):
    def test_mahjong_demo_finds_review_points(self):
        report = evaluate_mahjong_round(load_mahjong_round("samples/mahjong_round.json"))

        self.assertEqual(report.situation.game, "Mahjong Soul")
        self.assertGreaterEqual(len(report.decisions), 3)
        self.assertIn("Turn 9", report.to_markdown())

    def test_san11_demo_finds_ai_tuning_points(self):
        report = evaluate_san11_state(load_san11_state("samples/san11_state.json"))

        self.assertEqual(report.situation.game, "San11")
        self.assertGreaterEqual(len(report.decisions), 3)
        self.assertIn("Liu Bei", report.to_markdown())


if __name__ == "__main__":
    unittest.main()
