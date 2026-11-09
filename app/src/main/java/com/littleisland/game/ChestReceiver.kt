package com.littleisland.game

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/** 하루 세 번(9시·13시·20시) "보물 상자" 알림. 앱을 닫을 때 다음 알림을 하나 예약한다 */
class ChestReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val sp = ctx.getSharedPreferences("island", Context.MODE_PRIVATE)
        val last = sp.getLong("last", 0L)
        // 방금까지 앱을 보던 사람에게는 보내지 않는다
        if (System.currentTimeMillis() - last > 45 * 60 * 1000L) notify(ctx)
        schedule(ctx)
    }

    companion object {
        private val HOURS = intArrayOf(9, 13, 20)
        private val LINES = listOf(
            "보물 상자가 도착했어요! 열어 보세요",
            "배가 금화를 싣고 돌아왔어요",
            "선원들이 선장님을 기다려요",
            "해적이 나타났대요. 토벌하러 가요!")

        fun schedule(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = Calendar.getInstance()
            val t = Calendar.getInstance()
            var best: Long = Long.MAX_VALUE
            for (d in 0..1) for (h in HOURS) {
                t.timeInMillis = now.timeInMillis; t.add(Calendar.DAY_OF_YEAR, d)
                t.set(Calendar.HOUR_OF_DAY, h); t.set(Calendar.MINUTE, 0); t.set(Calendar.SECOND, 0)
                if (t.timeInMillis > now.timeInMillis + 60_000L && t.timeInMillis < best) best = t.timeInMillis
            }
            val pi = PendingIntent.getBroadcast(ctx, 7, Intent(ctx, ChestReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try { am.set(AlarmManager.RTC, best, pi) } catch (e: Exception) { }
        }

        fun notify(ctx: Context) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.createNotificationChannel(NotificationChannel("chest", "보물 상자", NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(ctx, 8, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val n = Notification.Builder(ctx, "chest")
                .setSmallIcon(ctx.applicationInfo.icon)
                .setContentTitle(ctx.applicationInfo.loadLabel(ctx.packageManager))
                .setContentText(LINES[(System.currentTimeMillis() / 3_600_000L % LINES.size).toInt()])
                .setContentIntent(open).setAutoCancel(true).build()
            try { nm.notify(1, n) } catch (e: SecurityException) { }
        }
    }
}
