package org.girino.mmonit.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import org.girino.mmonit.data.ConfigStore
import org.girino.mmonit.data.MMonitClient
import org.girino.mmonit.data.MMonitException
import org.girino.mmonit.data.StatusStore
import org.girino.mmonit.domain.MMonitStatusSnapshot
import org.girino.mmonit.notification.StatusNotification

class MMonitPollWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val config = ConfigStore(applicationContext).load() ?: return Result.success()
        val status = try {
            MMonitClient().poll(config)
        } catch (exception: MMonitException) {
            MMonitStatusSnapshot.unavailable(exception.message ?: "Falha ao consultar o M/Monit.")
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            MMonitStatusSnapshot.unavailable("Falha ao consultar o M/Monit.")
        }

        StatusStore(applicationContext).save(status)
        StatusNotification.show(applicationContext, status)
        return Result.success()
    }
}
