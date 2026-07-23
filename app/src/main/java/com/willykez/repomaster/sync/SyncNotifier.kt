package com.willykez.repomaster.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.willykez.repomaster.MainActivity
import com.willykez.repomaster.R

private const val CHANNEL_ID = "background_sync"
private const val NOTIFICATION_ID = 4201

/**
 * Posts exactly one notification per background sync pass — not one per repo — summarizing
 * however many repos actually had new commits. [SyncWorker] fetches every tracked repo every
 * time it runs; most passes find nothing new, and even a genuinely busy sync period touching
 * several repos should read as one thing to check, not a small flood of separate alerts.
 */
object SyncNotifier {

    /** Registers the notification channel. Safe to call every app start — creating a channel
     *  that already exists is a no-op — and channels only need to exist on API 26+. Must run
     *  before the first [notify] call, so this lives in [com.willykez.repomaster.App.onCreate]. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Background sync",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Lets you know when a background fetch finds new commits on a tracked repo"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * [reposWithNewCommits] is repo display names, one entry per repo that had at least one
     * tracking ref move during this sync pass — see [com.willykez.repomaster.git.GitEngine.fetchAndCountUpdates].
     * No-ops entirely if the list is empty (nothing new = no notification) or if the app
     * doesn't currently have notification permission — [SyncWorker] runs headless with no
     * Activity to request POST_NOTIFICATIONS from, so this only ever checks/uses whatever
     * permission state Settings already established, never prompts.
     */
    fun notify(context: Context, reposWithNewCommits: List<String>) {
        if (reposWithNewCommits.isEmpty()) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val title = if (reposWithNewCommits.size == 1) {
            "${reposWithNewCommits.first()} has new commits"
        } else {
            "${reposWithNewCommits.size} repos have new commits"
        }
        val body = reposWithNewCommits.joinToString(", ")

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the areNotificationsEnabled() check above and
            // this call (rare, but possible) — nothing useful to do from a headless worker
            // besides not crashing the sync job over a missed notification.
        }
    }
}
