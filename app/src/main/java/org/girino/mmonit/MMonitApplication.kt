package org.girino.mmonit

import android.app.Application
import org.girino.mmonit.notification.StatusNotification

class MMonitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StatusNotification.createChannel(this)
    }
}
