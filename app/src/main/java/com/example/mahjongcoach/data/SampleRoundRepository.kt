package com.example.mahjongcoach.data

import android.content.Context

class SampleRoundRepository(
    private val context: Context,
) {
    private val samples = listOf(
        SampleRound(
            id = "efficiency-east-2",
            title = "东二局：效率损失复盘",
            description = "中盘前的弃牌选择拉低进张，适合检查向听数和有效牌意识。",
            focus = "牌效率",
            assetName = "mahjong_round.json",
        ),
        SampleRound(
            id = "defense-south-1",
            title = "南一局：高压防守复盘",
            description = "对手立直后仍选择危险牌推进，适合训练一向听以下的弃和判断。",
            focus = "危险度",
            assetName = "mahjong_defense_round.json",
        ),
        SampleRound(
            id = "push-fold-east-4",
            title = "东四局：攻守切换复盘",
            description = "速度、分数和危险度同时变化，适合练习什么时候继续攻、什么时候收手。",
            focus = "攻守判断",
            assetName = "mahjong_push_fold_round.json",
        ),
    )

    fun listSamples(): List<SampleRound> = samples

    fun loadSampleRound(id: String): MahjongRound {
        val sample = samples.firstOrNull { it.id == id } ?: samples.first()
        val json = context.assets.open(sample.assetName)
            .bufferedReader()
            .use { it.readText() }
        return MahjongRoundParser.parse(json)
    }
}
