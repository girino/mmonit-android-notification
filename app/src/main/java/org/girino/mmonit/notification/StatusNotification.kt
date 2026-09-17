package org.girino.mmonit.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.girino.mmonit.MainActivity
import org.girino.mmonit.R
import org.girino.mmonit.domain.MMonitStatusSnapshot
import kotlin.math.roundToInt

object StatusNotification {
    const val CHANNEL_ID = "mmonit_status"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun show(context: Context, status: MMonitStatusSnapshot) {
        createChannel(context)

        val notificationIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val color = android.graphics.Color.argb(
            255,
            ((status.level.colorArgb shr 16) and 0xFF).toInt(),
            ((status.level.colorArgb shr 8) and 0xFF).toInt(),
            (status.level.colorArgb and 0xFF).toInt(),
        )
        val shortDetail = status.detail.replace('\n', ' ').trim()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status)
            .setLargeIcon(createStatusBitmap(context, color))
            .setColor(color)
            .setContentTitle("${context.getString(R.string.notification_title)}: ${status.level.title}")
            .setContentText(shortDetail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(status.detail))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setShowWhen(status.checkedAt > 0L)
            .setWhen(status.checkedAt)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notifications can be disabled by the user on Android 13+.
        }
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createStatusBitmap(context: Context, color: Int): Bitmap {
        val size = (48 * context.resources.displayMetrics.density).roundToInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val center = size / 2f
        canvas.drawCircle(center, center, center * 0.82f, paint)
        return bitmap
    }
}
