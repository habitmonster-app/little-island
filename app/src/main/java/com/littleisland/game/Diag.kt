package com.littleisland.game

import android.content.Context

/** 배경화면이 홈 화면에서 받은 신호를 기록해 앱 화면에서 볼 수 있게 한다 (테스트용). */
object Diag {
    private const val P = "diag"
    fun save(c: Context, xOff: Float, xStep: Float, paging: Boolean) {
        val sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        sp.edit()
            .putFloat("xOff", xOff).putFloat("xStep", xStep)
            .putBoolean("paging", paging)
            .putInt("offCount", sp.getInt("offCount", 0) + 1)
            .apply()
    }
    fun tap(c: Context) {
        val sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        sp.edit().putInt("taps", sp.getInt("taps", 0) + 1).apply()
    }
    fun text(c: Context): String {
        val sp = c.getSharedPreferences(P, Context.MODE_PRIVATE)
        val mode = if (sp.getBoolean("paging", false)) "페이지 넘김 연동" else "빈 곳 톡 → 이동"
        return "점검: " + mode + " · 넘김신호 " + sp.getInt("offCount", 0) + "회 (위치 " +
            "%.2f".format(sp.getFloat("xOff", -1f)) + ", 간격 " + "%.2f".format(sp.getFloat("xStep", -1f)) +
            ") · 톡 " + sp.getInt("taps", 0) + "회"
    }
}
