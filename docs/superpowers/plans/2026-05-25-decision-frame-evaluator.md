# Decision Frame Evaluator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a high-confidence, rules-based Mahjong Soul review pipeline that converts `FinalPaipu` event streams into evaluated `MahjongTurn` records without depending on an AI engine.

**Architecture:** Add a small evaluator pipeline between `FinalPaipu` and the existing `MahjongRoundEvaluator`. The pipeline creates decision frames for the view player, computes deterministic hand-efficiency and safety features, filters to high-confidence review points, and keeps uncertain strategy calls out of the report.

**Tech Stack:** Kotlin, Android app module, JUnit4 unit tests, existing `FinalPaipu`, `MahjongRound`, and `MahjongRoundEvaluator` models.

---

## File Structure

- Create `app/src/main/java/com/example/mahjongcoach/evaluator/Tile.kt`
  - Parses tile strings such as `1m`, `0p`, `7z`.
  - Normalizes red fives to logical five while preserving display text.
  - Provides tile ordering and validation helpers.
- Create `app/src/main/java/com/example/mahjongcoach/evaluator/ShantenCalculator.kt`
  - Computes minimum shanten across normal hands, seven pairs, and thirteen orphans.
  - Exposes `calculate(hand: List<String>): Int`.
- Create `app/src/main/java/com/example/mahjongcoach/evaluator/UkeireCalculator.kt`
  - Enumerates candidate discards.
  - Counts live tiles that improve shanten after each discard.
- Create `app/src/main/java/com/example/mahjongcoach/evaluator/DangerEstimator.kt`
  - Scores discard danger with simple explainable rules: genbutsu, honors, dora, riichi pressure, suji, and late-round pressure.
- Create `app/src/main/java/com/example/mahjongcoach/evaluator/DecisionFrame.kt`
  - Defines state snapshots for one view-player discard decision.
  - Defines computed feature rows for candidate discards.
- Create `app/src/main/java/com/example/mahjongcoach/evaluator/DecisionFrameBuilder.kt`
  - Replays `FinalPaipu` events and emits one `DecisionFrame` per view-player discard.
- Create `app/src/main/java/com/example/mahjongcoach/evaluator/FinalPaipuAnalyzer.kt`
  - Converts a full `FinalPaipu` into a `MahjongRound` compatible with the existing `MahjongRoundEvaluator`.
- Modify `app/src/main/java/com/example/mahjongcoach/data/FinalPaipuModels.kt`
  - Add optional `viewSeat` and `viewPlayer` to `PaipuHead` if not already present in the current branch.
- Test `app/src/test/java/com/example/mahjongcoach/evaluator/ShantenCalculatorTest.kt`
- Test `app/src/test/java/com/example/mahjongcoach/evaluator/UkeireCalculatorTest.kt`
- Test `app/src/test/java/com/example/mahjongcoach/evaluator/DangerEstimatorTest.kt`
- Test `app/src/test/java/com/example/mahjongcoach/evaluator/DecisionFrameBuilderTest.kt`
- Test `app/src/test/java/com/example/mahjongcoach/evaluator/FinalPaipuAnalyzerTest.kt`

---

### Task 1: Tile Parsing And Counts

**Files:**
- Create: `app/src/main/java/com/example/mahjongcoach/evaluator/Tile.kt`
- Test: `app/src/test/java/com/example/mahjongcoach/evaluator/TileTest.kt`

- [ ] **Step 1: Write the failing tile parser tests**

```kotlin
package com.example.mahjongcoach.evaluator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TileTest {
    @Test
    fun parse_normalizesRedFiveButKeepsDisplay() {
        val tile = Tile.parse("0p")

        assertEquals(TileSuit.Pin, tile.suit)
        assertEquals(5, tile.rank)
        assertEquals("0p", tile.display)
        assertTrue(tile.isRedFive)
    }

    @Test
    fun countVisibleTiles_countsRedFiveAsFive() {
        val counts = TileCounts.from(listOf("5m", "0m", "1z"))

        assertEquals(2, counts.count(Tile.parse("5m")))
        assertEquals(1, counts.count(Tile.parse("1z")))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.TileTest
```

Expected: FAIL with unresolved references for `Tile`, `TileSuit`, or `TileCounts`.

- [ ] **Step 3: Implement tile parsing**

```kotlin
package com.example.mahjongcoach.evaluator

enum class TileSuit(val code: Char) {
    Man('m'),
    Pin('p'),
    Sou('s'),
    Honor('z'),
}

data class Tile(
    val suit: TileSuit,
    val rank: Int,
    val display: String,
    val isRedFive: Boolean,
) {
    val normalizedKey: String = "$rank${suit.code}"

    companion object {
        fun parse(raw: String): Tile {
            require(raw.length == 2) { "Tile must use two-character notation: $raw" }
            val suit = when (raw[1]) {
                'm' -> TileSuit.Man
                'p' -> TileSuit.Pin
                's' -> TileSuit.Sou
                'z' -> TileSuit.Honor
                else -> error("Unsupported tile suit: $raw")
            }
            val rawRank = raw[0].digitToIntOrNull() ?: error("Unsupported tile rank: $raw")
            val rank = if (rawRank == 0 && suit != TileSuit.Honor) 5 else rawRank
            require(rank in 1..9 || suit == TileSuit.Honor && rank in 1..7) {
                "Invalid tile rank: $raw"
            }
            return Tile(suit = suit, rank = rank, display = raw, isRedFive = rawRank == 0)
        }
    }
}

class TileCounts private constructor(
    private val counts: Map<String, Int>,
) {
    fun count(tile: Tile): Int = counts[tile.normalizedKey].orEmpty()

    companion object {
        fun from(rawTiles: List<String>): TileCounts {
            val counts = rawTiles
                .map(Tile::parse)
                .groupingBy { it.normalizedKey }
                .eachCount()
            return TileCounts(counts)
        }
    }
}

private fun Int?.orEmpty(): Int = this ?: 0
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.TileTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mahjongcoach/evaluator/Tile.kt app/src/test/java/com/example/mahjongcoach/evaluator/TileTest.kt
git commit -m "feat: add mahjong tile parsing"
```

---

### Task 2: Shanten Calculator

**Files:**
- Create: `app/src/main/java/com/example/mahjongcoach/evaluator/ShantenCalculator.kt`
- Test: `app/src/test/java/com/example/mahjongcoach/evaluator/ShantenCalculatorTest.kt`

- [ ] **Step 1: Write failing shanten tests**

```kotlin
package com.example.mahjongcoach.evaluator

import org.junit.Assert.assertEquals
import org.junit.Test

class ShantenCalculatorTest {
    private val calculator = ShantenCalculator()

    @Test
    fun calculate_returnsZeroForTenpaiNormalHand() {
        val hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m")

        assertEquals(0, calculator.calculate(hand))
    }

    @Test
    fun calculate_returnsMinusOneForCompleteNormalHand() {
        val hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m", "5m")

        assertEquals(-1, calculator.calculate(hand))
    }

    @Test
    fun calculate_handlesSevenPairsTenpai() {
        val hand = listOf("1m", "1m", "2m", "2m", "3p", "3p", "4p", "4p", "5s", "5s", "6s", "6s", "7z")

        assertEquals(0, calculator.calculate(hand))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.ShantenCalculatorTest
```

Expected: FAIL with unresolved reference `ShantenCalculator`.

- [ ] **Step 3: Implement shanten calculator**

```kotlin
package com.example.mahjongcoach.evaluator

class ShantenCalculator {
    fun calculate(hand: List<String>): Int {
        val counts = to34Counts(hand)
        return minOf(
            normalShanten(counts),
            sevenPairsShanten(counts),
            thirteenOrphansShanten(counts),
        )
    }

    private fun normalShanten(counts: IntArray): Int {
        var best = 8

        fun removeMelds(index: Int, mutable: IntArray, melds: Int, taatsu: Int, pair: Int) {
            var i = index
            while (i < 34 && mutable[i] == 0) i++
            if (i >= 34) {
                val cappedTaatsu = minOf(taatsu, 4 - melds)
                val shanten = 8 - melds * 2 - cappedTaatsu - pair
                best = minOf(best, shanten)
                return
            }

            if (mutable[i] >= 3) {
                mutable[i] -= 3
                removeMelds(i, mutable, melds + 1, taatsu, pair)
                mutable[i] += 3
            }
            if (i < 27 && i % 9 <= 6 && mutable[i + 1] > 0 && mutable[i + 2] > 0) {
                mutable[i]--
                mutable[i + 1]--
                mutable[i + 2]--
                removeMelds(i, mutable, melds + 1, taatsu, pair)
                mutable[i]++
                mutable[i + 1]++
                mutable[i + 2]++
            }
            if (pair == 0 && mutable[i] >= 2) {
                mutable[i] -= 2
                removeMelds(i, mutable, melds, taatsu, 1)
                mutable[i] += 2
            }
            if (mutable[i] >= 2) {
                mutable[i] -= 2
                removeMelds(i, mutable, melds, taatsu + 1, pair)
                mutable[i] += 2
            }
            if (i < 27 && i % 9 <= 7 && mutable[i + 1] > 0) {
                mutable[i]--
                mutable[i + 1]--
                removeMelds(i, mutable, melds, taatsu + 1, pair)
                mutable[i]++
                mutable[i + 1]++
            }
            if (i < 27 && i % 9 <= 6 && mutable[i + 2] > 0) {
                mutable[i]--
                mutable[i + 2]--
                removeMelds(i, mutable, melds, taatsu + 1, pair)
                mutable[i]++
                mutable[i + 2]++
            }

            mutable[i]--
            removeMelds(i, mutable, melds, taatsu, pair)
            mutable[i]++
        }

        removeMelds(0, counts.copyOf(), 0, 0, 0)
        return best
    }

    private fun sevenPairsShanten(counts: IntArray): Int {
        val pairs = counts.count { it >= 2 }
        val unique = counts.count { it > 0 }
        return 6 - pairs + maxOf(0, 7 - unique)
    }

    private fun thirteenOrphansShanten(counts: IntArray): Int {
        val terminals = intArrayOf(0, 8, 9, 17, 18, 26, 27, 28, 29, 30, 31, 32, 33)
        val unique = terminals.count { counts[it] > 0 }
        val hasPair = terminals.any { counts[it] >= 2 }
        return 13 - unique - if (hasPair) 1 else 0
    }
}

fun to34Counts(hand: List<String>): IntArray {
    val counts = IntArray(34)
    hand.map(Tile::parse).forEach { tile ->
        val index = when (tile.suit) {
            TileSuit.Man -> tile.rank - 1
            TileSuit.Pin -> 9 + tile.rank - 1
            TileSuit.Sou -> 18 + tile.rank - 1
            TileSuit.Honor -> 27 + tile.rank - 1
        }
        counts[index]++
    }
    return counts
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.ShantenCalculatorTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mahjongcoach/evaluator/ShantenCalculator.kt app/src/test/java/com/example/mahjongcoach/evaluator/ShantenCalculatorTest.kt
git commit -m "feat: add shanten calculator"
```

---

### Task 3: Ukeire Calculator

**Files:**
- Create: `app/src/main/java/com/example/mahjongcoach/evaluator/UkeireCalculator.kt`
- Test: `app/src/test/java/com/example/mahjongcoach/evaluator/UkeireCalculatorTest.kt`

- [ ] **Step 1: Write failing ukeire tests**

```kotlin
package com.example.mahjongcoach.evaluator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UkeireCalculatorTest {
    @Test
    fun evaluateCandidates_keepsOnlyUniqueDiscardTiles() {
        val result = UkeireCalculator().evaluateCandidates(
            hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m", "9p"),
            visibleTiles = emptyList(),
        )

        assertEquals(result.map { it.discard }.distinct().size, result.size)
    }

    @Test
    fun evaluateCandidates_countsLiveImprovingTiles() {
        val result = UkeireCalculator().evaluateCandidates(
            hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m", "9p"),
            visibleTiles = listOf("5m", "5m"),
        )

        val best = result.maxBy { it.ukeire }

        assertTrue(best.ukeire > 0)
        assertTrue(result.all { it.shantenAfter >= -1 })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.UkeireCalculatorTest
```

Expected: FAIL with unresolved reference `UkeireCalculator`.

- [ ] **Step 3: Implement ukeire calculator**

```kotlin
package com.example.mahjongcoach.evaluator

data class UkeireCandidate(
    val discard: String,
    val shantenAfter: Int,
    val ukeire: Int,
    val improvingTiles: List<String>,
)

class UkeireCalculator(
    private val shantenCalculator: ShantenCalculator = ShantenCalculator(),
) {
    fun evaluateCandidates(hand: List<String>, visibleTiles: List<String>): List<UkeireCandidate> {
        return hand
            .distinctBy { Tile.parse(it).normalizedKey }
            .map { discard ->
                val afterDiscard = removeOne(hand, discard)
                val shanten = shantenCalculator.calculate(afterDiscard)
                val improving = allTileTypes().filter { draw ->
                    liveCount(draw, visibleTiles + afterDiscard) > 0 &&
                        shantenCalculator.calculate(afterDiscard + draw) < shanten
                }
                UkeireCandidate(
                    discard = discard,
                    shantenAfter = shanten,
                    ukeire = improving.sumOf { liveCount(it, visibleTiles + afterDiscard) },
                    improvingTiles = improving,
                )
            }
    }

    private fun removeOne(hand: List<String>, discard: String): List<String> {
        val discardKey = Tile.parse(discard).normalizedKey
        var removed = false
        return hand.filter { tile ->
            if (!removed && Tile.parse(tile).normalizedKey == discardKey) {
                removed = true
                false
            } else {
                true
            }
        }
    }

    private fun liveCount(tile: String, visibleTiles: List<String>): Int {
        val key = Tile.parse(tile).normalizedKey
        val visible = visibleTiles.count { Tile.parse(it).normalizedKey == key }
        return (4 - visible).coerceAtLeast(0)
    }

    private fun allTileTypes(): List<String> {
        return buildList {
            for (suit in listOf("m", "p", "s")) {
                for (rank in 1..9) add("$rank$suit")
            }
            for (rank in 1..7) add("${rank}z")
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.UkeireCalculatorTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mahjongcoach/evaluator/UkeireCalculator.kt app/src/test/java/com/example/mahjongcoach/evaluator/UkeireCalculatorTest.kt
git commit -m "feat: add ukeire candidate evaluation"
```

---

### Task 4: Danger Estimator

**Files:**
- Create: `app/src/main/java/com/example/mahjongcoach/evaluator/DangerEstimator.kt`
- Test: `app/src/test/java/com/example/mahjongcoach/evaluator/DangerEstimatorTest.kt`

- [ ] **Step 1: Write failing danger tests**

```kotlin
package com.example.mahjongcoach.evaluator

import org.junit.Assert.assertTrue
import org.junit.Test

class DangerEstimatorTest {
    @Test
    fun estimate_scoresGenbutsuLowerThanUnknownTileAgainstRiichi() {
        val estimator = DangerEstimator()
        val frame = dangerFrame(riichiSeats = setOf(1), visibleDiscards = mapOf(1 to listOf("9s")))

        val genbutsu = estimator.estimate("9s", frame)
        val unknown = estimator.estimate("5m", frame)

        assertTrue(genbutsu.score < unknown.score)
        assertTrue(genbutsu.reasons.any { it.contains("现物") })
    }

    @Test
    fun estimate_scoresDoraHigherUnderPressure() {
        val estimator = DangerEstimator()
        val frame = dangerFrame(riichiSeats = setOf(1), doraIndicators = listOf("4m"))

        val dora = estimator.estimate("5m", frame)
        val terminal = estimator.estimate("1m", frame)

        assertTrue(dora.score > terminal.score)
    }

    private fun dangerFrame(
        riichiSeats: Set<Int>,
        visibleDiscards: Map<Int, List<String>> = emptyMap(),
        doraIndicators: List<String> = emptyList(),
    ): DecisionFrame {
        return DecisionFrame(
            roundIndex = 0,
            turn = 8,
            viewSeat = 0,
            hand = listOf("1m", "2m", "3m", "4p", "5p", "6p", "7s", "8s", "9s", "2z", "2z", "5m", "5m", "9p"),
            drawnTile = "9p",
            chosenDiscard = "9p",
            visibleDiscards = visibleDiscards,
            calls = emptyMap(),
            doraIndicators = doraIndicators,
            scores = listOf(25000, 25000, 25000, 25000),
            riichiSeats = riichiSeats,
            visibleTiles = visibleDiscards.values.flatten(),
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.DangerEstimatorTest
```

Expected: FAIL with unresolved references for `DangerEstimator` and `DecisionFrame`.

- [ ] **Step 3: Add `DecisionFrame` and implement danger estimator**

```kotlin
package com.example.mahjongcoach.evaluator

data class DecisionFrame(
    val roundIndex: Int,
    val turn: Int,
    val viewSeat: Int,
    val hand: List<String>,
    val drawnTile: String?,
    val chosenDiscard: String,
    val visibleDiscards: Map<Int, List<String>>,
    val calls: Map<Int, List<List<String>>>,
    val doraIndicators: List<String>,
    val scores: List<Int>,
    val riichiSeats: Set<Int>,
    val visibleTiles: List<String>,
)

data class DangerEstimate(
    val score: Double,
    val reasons: List<String>,
)

class DangerEstimator {
    fun estimate(discard: String, frame: DecisionFrame): DangerEstimate {
        val tile = Tile.parse(discard)
        val reasons = mutableListOf<String>()
        var score = 0.20

        if (frame.riichiSeats.isNotEmpty()) {
            score += 0.25
            reasons += "对手立直后基础危险度上升"
        }

        val isGenbutsu = frame.riichiSeats.any { seat ->
            frame.visibleDiscards[seat].orEmpty().any { Tile.parse(it).normalizedKey == tile.normalizedKey }
        }
        if (isGenbutsu) {
            score -= 0.40
            reasons += "现物"
        }

        if (tile.suit == TileSuit.Honor) {
            score += if (frame.visibleTiles.count { Tile.parse(it).normalizedKey == tile.normalizedKey } >= 2) -0.10 else 0.10
            reasons += "字牌按已见枚数调整"
        }

        if (tile.rank == 5 && tile.suit != TileSuit.Honor) {
            score += 0.10
            reasons += "中张牌危险度较高"
        }

        if (isDora(discard, frame.doraIndicators)) {
            score += 0.15
            reasons += "宝牌危险度和损失更高"
        }

        if (frame.turn >= 12 && frame.riichiSeats.isNotEmpty()) {
            score += 0.10
            reasons += "后巡面对立直压力更高"
        }

        return DangerEstimate(score = score.coerceIn(0.0, 1.0), reasons = reasons)
    }

    private fun isDora(tile: String, indicators: List<String>): Boolean {
        return indicators.any { nextDora(Tile.parse(it)).normalizedKey == Tile.parse(tile).normalizedKey }
    }

    private fun nextDora(indicator: Tile): Tile {
        val nextRank = when (indicator.suit) {
            TileSuit.Man, TileSuit.Pin, TileSuit.Sou -> if (indicator.rank == 9) 1 else indicator.rank + 1
            TileSuit.Honor -> when (indicator.rank) {
                4 -> 1
                7 -> 5
                else -> indicator.rank + 1
            }
        }
        return indicator.copy(rank = nextRank, display = "$nextRank${indicator.suit.code}", isRedFive = false)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.DangerEstimatorTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mahjongcoach/evaluator/DecisionFrame.kt app/src/main/java/com/example/mahjongcoach/evaluator/DangerEstimator.kt app/src/test/java/com/example/mahjongcoach/evaluator/DangerEstimatorTest.kt
git commit -m "feat: add explainable danger estimator"
```

---

### Task 5: Decision Frame Builder

**Files:**
- Create: `app/src/main/java/com/example/mahjongcoach/evaluator/DecisionFrameBuilder.kt`
- Test: `app/src/test/java/com/example/mahjongcoach/evaluator/DecisionFrameBuilderTest.kt`
- Modify if needed: `app/src/main/java/com/example/mahjongcoach/data/FinalPaipuModels.kt`

- [ ] **Step 1: Write failing frame builder test**

```kotlin
package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEvent
import com.example.mahjongcoach.data.PaipuEventType
import com.example.mahjongcoach.data.PaipuHead
import com.example.mahjongcoach.data.PaipuPlayer
import com.example.mahjongcoach.data.PaipuRound
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionFrameBuilderTest {
    @Test
    fun build_emitsFrameForViewPlayerDiscard() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "16",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 25000), PaipuPlayer(2, "opponent", 1, 25000)),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 0,
                    events = listOf(
                        PaipuEvent(0, PaipuEventType.DealTile, 0, "1m"),
                        PaipuEvent(1, PaipuEventType.DealTile, 0, "2m"),
                        PaipuEvent(2, PaipuEventType.DealTile, 0, "3m"),
                        PaipuEvent(3, PaipuEventType.DealTile, 0, "9p"),
                        PaipuEvent(4, PaipuEventType.DiscardTile, 0, "9p"),
                        PaipuEvent(5, PaipuEventType.DiscardTile, 1, "5m"),
                    ),
                )
            ),
        )

        val frames = DecisionFrameBuilder().build(paipu)

        assertEquals(1, frames.size)
        assertEquals(0, frames.first().viewSeat)
        assertEquals("9p", frames.first().chosenDiscard)
        assertEquals(4, frames.first().hand.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.DecisionFrameBuilderTest
```

Expected: FAIL with unresolved reference `DecisionFrameBuilder` or missing `viewSeat` property.

- [ ] **Step 3: Add view-seat fields if missing**

If `PaipuHead` does not have `viewSeat`, update it to:

```kotlin
data class PaipuHead(
    val modeId: String?,
    val startTime: Long?,
    val endTime: Long?,
    val players: List<PaipuPlayer>,
    val viewSeat: Int? = null,
) {
    val viewPlayer: PaipuPlayer?
        get() = viewSeat?.let { seat -> players.firstOrNull { it.seat == seat } }
}
```

- [ ] **Step 4: Implement decision frame builder**

```kotlin
package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEventType

class DecisionFrameBuilder {
    fun build(paipu: FinalPaipu): List<DecisionFrame> {
        val viewSeat = paipu.head.viewSeat
            ?: paipu.head.players.firstOrNull()?.seat
            ?: return emptyList()

        return paipu.rounds.flatMap { round ->
            val hands = mutableMapOf<Int, MutableList<String>>()
            val discards = mutableMapOf<Int, MutableList<String>>()
            val calls = mutableMapOf<Int, MutableList<List<String>>>()
            val riichiSeats = mutableSetOf<Int>()
            val doraIndicators = mutableListOf<String>()
            val frames = mutableListOf<DecisionFrame>()
            var viewTurn = 0
            var lastDraw: String? = null

            round.events.forEach { event ->
                val seat = event.actorSeat
                when (event.type) {
                    PaipuEventType.NewRound -> {
                        event.payload["dora"]?.let { doraIndicators += it }
                    }
                    PaipuEventType.DealTile -> {
                        if (seat != null && event.tile != null) {
                            hands.getOrPut(seat) { mutableListOf() }.add(event.tile)
                            if (seat == viewSeat) lastDraw = event.tile
                        }
                    }
                    PaipuEventType.DiscardTile -> {
                        if (seat != null && event.tile != null) {
                            if (seat == viewSeat) {
                                viewTurn++
                                val handBeforeDiscard = hands.getOrPut(seat) { mutableListOf() }.toList()
                                frames += DecisionFrame(
                                    roundIndex = round.roundIndex,
                                    turn = viewTurn,
                                    viewSeat = viewSeat,
                                    hand = handBeforeDiscard,
                                    drawnTile = lastDraw,
                                    chosenDiscard = event.tile,
                                    visibleDiscards = discards.mapValues { it.value.toList() },
                                    calls = calls.mapValues { it.value.toList() },
                                    doraIndicators = doraIndicators.toList(),
                                    scores = paipu.head.players.sortedBy { it.seat }.map { it.score ?: 25000 },
                                    riichiSeats = riichiSeats.toSet(),
                                    visibleTiles = discards.values.flatten() + calls.values.flatten().flatten() + doraIndicators,
                                )
                                removeOne(hands.getOrPut(seat) { mutableListOf() }, event.tile)
                                lastDraw = null
                            }
                            discards.getOrPut(seat) { mutableListOf() }.add(event.tile)
                        }
                    }
                    PaipuEventType.ChiPengGang -> {
                        if (seat != null) {
                            val tiles = event.payload["tiles"]?.split(",").orEmpty()
                            calls.getOrPut(seat) { mutableListOf() }.add(tiles)
                            tiles.forEach { removeOne(hands.getOrPut(seat) { mutableListOf() }, it) }
                        }
                    }
                    PaipuEventType.Riichi -> {
                        if (seat != null) riichiSeats += seat
                    }
                    PaipuEventType.Hule, PaipuEventType.Liuju, PaipuEventType.Unknown -> Unit
                }
            }
            frames
        }
    }

    private fun removeOne(hand: MutableList<String>, tile: String) {
        val key = Tile.parse(tile).normalizedKey
        val index = hand.indexOfFirst { Tile.parse(it).normalizedKey == key }
        if (index >= 0) hand.removeAt(index)
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.DecisionFrameBuilderTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/mahjongcoach/data/FinalPaipuModels.kt app/src/main/java/com/example/mahjongcoach/evaluator/DecisionFrameBuilder.kt app/src/test/java/com/example/mahjongcoach/evaluator/DecisionFrameBuilderTest.kt
git commit -m "feat: build decision frames from paipu events"
```

---

### Task 6: Final Paipu Analyzer

**Files:**
- Create: `app/src/main/java/com/example/mahjongcoach/evaluator/FinalPaipuAnalyzer.kt`
- Test: `app/src/test/java/com/example/mahjongcoach/evaluator/FinalPaipuAnalyzerTest.kt`

- [ ] **Step 1: Write failing analyzer test**

```kotlin
package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.PaipuEvent
import com.example.mahjongcoach.data.PaipuEventType
import com.example.mahjongcoach.data.PaipuHead
import com.example.mahjongcoach.data.PaipuPlayer
import com.example.mahjongcoach.data.PaipuRound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FinalPaipuAnalyzerTest {
    @Test
    fun analyze_returnsMahjongRoundWithCandidateFeatures() {
        val paipu = FinalPaipu(
            uuid = "game-1",
            officialUrl = "https://example.test",
            head = PaipuHead(
                modeId = "16",
                startTime = 1,
                endTime = 2,
                players = listOf(PaipuPlayer(1, "me", 0, 25000), PaipuPlayer(2, "opponent", 1, 25000)),
                viewSeat = 0,
            ),
            rounds = listOf(
                PaipuRound(
                    roundIndex = 0,
                    events = listOf(
                        PaipuEvent(0, PaipuEventType.DealTile, 0, "1m"),
                        PaipuEvent(1, PaipuEventType.DealTile, 0, "2m"),
                        PaipuEvent(2, PaipuEventType.DealTile, 0, "3m"),
                        PaipuEvent(3, PaipuEventType.DealTile, 0, "4p"),
                        PaipuEvent(4, PaipuEventType.DealTile, 0, "5p"),
                        PaipuEvent(5, PaipuEventType.DealTile, 0, "6p"),
                        PaipuEvent(6, PaipuEventType.DealTile, 0, "7s"),
                        PaipuEvent(7, PaipuEventType.DealTile, 0, "8s"),
                        PaipuEvent(8, PaipuEventType.DealTile, 0, "9s"),
                        PaipuEvent(9, PaipuEventType.DealTile, 0, "2z"),
                        PaipuEvent(10, PaipuEventType.DealTile, 0, "2z"),
                        PaipuEvent(11, PaipuEventType.DealTile, 0, "5m"),
                        PaipuEvent(12, PaipuEventType.DealTile, 0, "5m"),
                        PaipuEvent(13, PaipuEventType.DealTile, 0, "9p"),
                        PaipuEvent(14, PaipuEventType.DiscardTile, 0, "9p"),
                    ),
                )
            ),
        )

        val round = FinalPaipuAnalyzer().analyze(paipu)

        assertEquals("game-1", round.id)
        assertEquals(1, round.turns.size)
        assertTrue(round.turns.first().ukeireBest >= round.turns.first().ukeireChosen)
        assertTrue(round.turns.first().chosenDanger in 0.0..1.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.FinalPaipuAnalyzerTest
```

Expected: FAIL with unresolved reference `FinalPaipuAnalyzer`.

- [ ] **Step 3: Implement analyzer**

```kotlin
package com.example.mahjongcoach.evaluator

import com.example.mahjongcoach.data.FinalPaipu
import com.example.mahjongcoach.data.MahjongRound
import com.example.mahjongcoach.data.MahjongTurn

class FinalPaipuAnalyzer(
    private val frameBuilder: DecisionFrameBuilder = DecisionFrameBuilder(),
    private val ukeireCalculator: UkeireCalculator = UkeireCalculator(),
    private val dangerEstimator: DangerEstimator = DangerEstimator(),
) {
    fun analyze(paipu: FinalPaipu): MahjongRound {
        val frames = frameBuilder.build(paipu)
        val turns = frames.mapNotNull(::toTurn)
        val playerName = paipu.head.viewPlayer?.nickname ?: "复盘玩家"
        return MahjongRound(
            id = paipu.uuid,
            title = "$playerName 的雀魂复盘",
            source = paipu.officialUrl,
            description = "从完整牌谱事件流生成的规则型复盘输入。",
            focus = "高置信牌效率与攻守判断",
            turns = turns,
        )
    }

    private fun toTurn(frame: DecisionFrame): MahjongTurn? {
        val candidates = ukeireCalculator.evaluateCandidates(frame.hand, frame.visibleTiles)
        if (candidates.isEmpty()) return null

        val chosen = candidates.firstOrNull { sameTile(it.discard, frame.chosenDiscard) }
            ?: candidates.first()
        val bestEfficiency = candidates.maxWith(compareBy<UkeireCandidate> { it.ukeire }.thenBy { -it.shantenAfter })
        val dangerByDiscard = candidates.associate { candidate ->
            candidate.discard to dangerEstimator.estimate(candidate.discard, frame).score
        }
        val safest = dangerByDiscard.minBy { it.value }

        val opponentPressure = when {
            frame.riichiSeats.isNotEmpty() && frame.turn >= 12 -> 0.95
            frame.riichiSeats.isNotEmpty() -> 0.80
            frame.calls.any { it.key != frame.viewSeat && it.value.isNotEmpty() } -> 0.45
            else -> 0.20
        }

        return MahjongTurn(
            turn = frame.turn,
            chosenDiscard = frame.chosenDiscard,
            bestDiscard = bestEfficiency.discard,
            safestDiscard = safest.key,
            ukeireChosen = chosen.ukeire.toDouble(),
            ukeireBest = bestEfficiency.ukeire.toDouble(),
            chosenDanger = dangerEstimator.estimate(frame.chosenDiscard, frame).score,
            bestDanger = dangerByDiscard[bestEfficiency.discard] ?: 0.0,
            opponentPressure = opponentPressure,
            shantenAfter = chosen.shantenAfter.toDouble(),
        )
    }

    private fun sameTile(left: String, right: String): Boolean {
        return Tile.parse(left).normalizedKey == Tile.parse(right).normalizedKey
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.FinalPaipuAnalyzerTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mahjongcoach/evaluator/FinalPaipuAnalyzer.kt app/src/test/java/com/example/mahjongcoach/evaluator/FinalPaipuAnalyzerTest.kt
git commit -m "feat: analyze final paipu into review turns"
```

---

### Task 7: High-Confidence Report Gate

**Files:**
- Modify: `app/src/main/java/com/example/mahjongcoach/evaluator/MahjongRoundEvaluator.kt`
- Test: `app/src/test/java/com/example/mahjongcoach/evaluator/MahjongRoundEvaluatorTest.kt`

- [ ] **Step 1: Add failing test for ignoring low-confidence frames**

Append this test to `MahjongRoundEvaluatorTest`:

```kotlin
@Test
fun evaluate_doesNotInventDecisionPointsForLowSignalRound() {
    val report = MahjongRoundEvaluator().evaluate(
        MahjongRound(
            id = "low-signal",
            title = "低置信局面",
            source = "unit-test",
            description = "all choices are close",
            focus = "置信度过滤",
            turns = listOf(
                MahjongTurn(3, "4m", "5m", "4m", 18.0, 19.0, 0.22, 0.20, 0.20, 1.0),
                MahjongTurn(4, "6p", "6p", "6p", 20.0, 20.0, 0.18, 0.18, 0.20, 1.0),
            ),
        )
    )

    assertTrue(report.decisionPoints.isEmpty())
    assertTrue(report.trainingFocus.theme.contains("保持"))
}
```

- [ ] **Step 2: Run test to verify current behavior**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.MahjongRoundEvaluatorTest
```

Expected: PASS if current thresholds already suppress low-signal decisions. If it fails, continue with Step 3.

- [ ] **Step 3: Tighten thresholds only if the test fails**

In `MahjongRoundEvaluator.evaluate`, keep or update thresholds to:

```kotlin
if (efficiencyLoss >= 4.0) { ... }
if (pushRisk >= 0.45 && turn.shantenAfter >= 1.0) { ... }
if (dangerGap >= 0.35 && efficiencyLoss <= 2.0) { ... }
```

- [ ] **Step 4: Run evaluator tests**

Run:

```bash
./gradlew testDebugUnitTest --tests com.example.mahjongcoach.evaluator.MahjongRoundEvaluatorTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mahjongcoach/evaluator/MahjongRoundEvaluator.kt app/src/test/java/com/example/mahjongcoach/evaluator/MahjongRoundEvaluatorTest.kt
git commit -m "test: lock high-confidence review filtering"
```

---

### Task 8: Integration Verification

**Files:**
- Test all evaluator and data parser tests.
- Modify docs only if implementation behavior differs from this plan.

- [ ] **Step 1: Run all unit tests**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Fix compile fallout from constructor changes**

If `PaipuHead` constructor changes break existing tests, update affected test constructors by adding named argument `viewSeat = null` only when Kotlin cannot use the default. Prefer named arguments for clarity:

```kotlin
PaipuHead(
    modeId = "16",
    startTime = 1,
    endTime = 2,
    players = listOf(PaipuPlayer(1, "玩家A", 0, 25000)),
    viewSeat = null,
)
```

- [ ] **Step 3: Run all unit tests again**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit integration fixes**

```bash
git add app/src/main/java/com/example/mahjongcoach app/src/test/java/com/example/mahjongcoach docs/superpowers/plans/2026-05-25-decision-frame-evaluator.md
git commit -m "test: verify decision frame evaluator pipeline"
```

---

## Self-Review

- Spec coverage: The plan covers deterministic feature calculation, high-confidence filtering, no-AI constraints, and compatibility with the existing `MahjongRoundEvaluator`.
- Placeholder scan: No unresolved marker words or unspecified implementation steps remain.
- Type consistency: `DecisionFrame`, `UkeireCandidate`, `DangerEstimate`, and `FinalPaipuAnalyzer` signatures are introduced before use in later tasks.
- Scope check: The plan intentionally excludes advanced EV, NAGA/Mortal comparison, full yaku value estimation, and UI integration. Those should be separate plans after this rules-based core is verified.
