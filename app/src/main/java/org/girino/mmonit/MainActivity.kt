package org.girino.mmonit

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import org.girino.mmonit.ui.MMonitApp
import org.girino.mmonit.ui.MMonitTheme
import org.girino.mmonit.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            requestBatteryOptimizationExemptionIfNeeded()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationPermissionRequested = requestNotificationPermissionIfNeeded()
        setContent {
            MMonitTheme {
                MMonitApp(viewModel)
            }
        }
        if (!notificationPermissionRequested) {
            window.decorView.post(::requestBatteryOptimizationExemptionIfNeeded)
        }
    }

    private fun requestNotificationPermissionIfNeeded(): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return true
        }
        return false
    }

    private fun requestBatteryOptimizationExemptionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return

        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        } catch (_: ActivityNotFoundException) {
            openBatteryOptimizationSettings()
        } catch (_: SecurityException) {
            openBatteryOptimizationSettings()
        }
    }

    private fun openBatteryOptimizationSettings() {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

}
