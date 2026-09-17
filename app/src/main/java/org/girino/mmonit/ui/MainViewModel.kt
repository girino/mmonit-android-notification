package org.girino.mmonit.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.girino.mmonit.data.ConfigStore
import org.girino.mmonit.data.MMonitClient
import org.girino.mmonit.data.MMonitConfig
import org.girino.mmonit.data.MMonitException
import org.girino.mmonit.data.StatusStore
import org.girino.mmonit.domain.MMonitStatusSnapshot
import org.girino.mmonit.notification.StatusNotification
import org.girino.mmonit.worker.PollScheduler

data class MainUiState(
    val loading: Boolean = true,
    val configured: Boolean = false,
    val showSettings: Boolean = false,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val status: MMonitStatusSnapshot = MMonitStatusSnapshot.initial(),
    val busy: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val configStore = ConfigStore(appContext)
    private val statusStore = StatusStore(appContext)
    private val client = MMonitClient()
    private val _state = MutableStateFlow(MainUiState())
    private var savedConfig: MMonitConfig? = null

    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val config = withContext(Dispatchers.IO) { configStore.load() }
            savedConfig = config
            _state.update {
                it.copy(
                    loading = false,
                    configured = config != null,
                    showSettings = config == null,
                    serverUrl = config?.serverUrl.orEmpty(),
                    username = config?.username.orEmpty(),
                )
            }
            if (config != null) {
                PollScheduler.schedule(appContext)
            }
        }

        viewModelScope.launch {
            statusStore.observe().collectLatest { status ->
                if (status != null) {
                    _state.update { it.copy(status = status) }
                }
            }
        }
    }

    fun updateServerUrl(value: String) {
        _state.update { it.copy(serverUrl = value, message = null) }
    }

    fun updateUsername(value: String) {
        _state.update { it.copy(username = value, message = null) }
    }

    fun updatePassword(value: String) {
        _state.update { it.copy(password = value, message = null) }
    }

    fun openSettings() {
        _state.update { it.copy(showSettings = true, message = null, password = "") }
    }

    fun closeSettings() {
        if (_state.value.configured) {
            _state.update { it.copy(showSettings = false, message = null, password = "") }
        }
    }

    fun saveConfiguration() {
        val current = _state.value
        val password = current.password.ifEmpty { savedConfig?.password.orEmpty() }
        val config = try {
            MMonitConfig(
                serverUrl = current.serverUrl,
                username = current.username,
                password = password,
            ).normalized()
        } catch (exception: IllegalArgumentException) {
            showError(exception.message ?: "Configuração inválida.")
            return
        }

        _state.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { configStore.save(config) }
                savedConfig = config
                PollScheduler.schedule(appContext)
                PollScheduler.enqueueNow(appContext)
                _state.update {
                    it.copy(
                        loading = false,
                        configured = true,
                        showSettings = false,
                        password = "",
                        busy = false,
                        message = "Configuração salva. Consulta iniciada.",
                        messageIsError = false,
                    )
                }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        busy = false,
                        message = "Não foi possível salvar a configuração.",
                        messageIsError = true,
                    )
                }
            }
        }
    }

    fun testConnection() {
        val current = _state.value
        val password = current.password.ifEmpty { savedConfig?.password.orEmpty() }
        val config = try {
            MMonitConfig(
                serverUrl = current.serverUrl,
                username = current.username,
                password = password,
            ).normalized()
        } catch (exception: IllegalArgumentException) {
            showError(exception.message ?: "Configuração inválida.")
            return
        }

        _state.update { it.copy(busy = true, message = "Testando conexão...") }
        viewModelScope.launch {
            try {
                val status = withContext(Dispatchers.IO) { client.poll(config) }
                _state.update {
                    it.copy(
                        busy = false,
                        status = status,
                        message = "Conexão bem-sucedida.",
                        messageIsError = false,
                    )
                }
            } catch (exception: MMonitException) {
                val message = exception.message ?: "Falha ao consultar o M/Monit."
                _state.update {
                    it.copy(
                        busy = false,
                        status = MMonitStatusSnapshot.unavailable(message),
                        message = message,
                        messageIsError = true,
                    )
                }
            } catch (_: Exception) {
                showError("Falha ao consultar o M/Monit.")
            }
        }
    }

    fun refreshNow() {
        if (!_state.value.configured) return
        PollScheduler.enqueueNow(appContext)
        _state.update {
            it.copy(
                message = "Atualização solicitada.",
                messageIsError = false,
            )
        }
    }

    fun openServerUrl() {
        val url = _state.value.serverUrl.takeIf { it.isNotBlank() } ?: return
        try {
            appContext.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
            _state.update {
                it.copy(
                    message = "Nenhum navegador está disponível neste dispositivo.",
                    messageIsError = true,
                )
            }
        }
    }

    fun clearConfiguration() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                configStore.clear()
                statusStore.clear()
            }
            PollScheduler.cancel(appContext)
            StatusNotification.dismiss(appContext)
            savedConfig = null
            _state.value = MainUiState(showSettings = true, loading = false)
        }
    }

    private fun showError(message: String) {
        _state.update {
            it.copy(
                busy = false,
                message = message,
                messageIsError = true,
            )
        }
    }
}
