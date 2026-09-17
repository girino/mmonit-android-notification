package org.girino.mmonit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.girino.mmonit.R
import org.girino.mmonit.domain.MMonitStatusSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MMonitApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading -> LoadingScreen()
        state.showSettings -> ConfigurationScreen(state, viewModel)
        else -> StatusScreen(state, viewModel)
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StatusScreen(state: MainUiState, viewModel: MainViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("M/Monit Status") },
                actions = {
                    IconButton(onClick = viewModel::openSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Configurações",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusIndicator(state.status, onClick = viewModel::openServerUrl)
            Spacer(Modifier.size(18.dp))
            Text(
                text = state.status.level.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = state.status.detail,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.status.checkedAt > 0L) {
                Spacer(Modifier.size(12.dp))
                Text(
                    text = "Última consulta: ${formatTimestamp(state.status.checkedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(onClick = viewModel::refreshNow) {
                    Text("Atualizar agora")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = viewModel::openSettings) {
                    Text("Configurações")
                }
            }
            Spacer(Modifier.size(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Polling automático",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "O Android consulta o servidor quando houver rede disponível, respeitando o intervalo mínimo de 15 minutos do sistema.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            MessageText(state)
        }
    }
}

@Composable
private fun StatusIndicator(status: MMonitStatusSnapshot, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(112.dp)
            .background(Color(status.level.colorArgb), CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Abrir M/Monit no navegador"
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = status.level.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
private fun ConfigurationScreen(state: MainUiState, viewModel: MainViewModel) {
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val usernameAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.Username),
            boundingBox = Rect.Zero,
            onFill = viewModel::updateUsername,
        )
    }
    val passwordAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.Password),
            boundingBox = Rect.Zero,
            onFill = viewModel::updatePassword,
        )
    }
    DisposableEffect(autofillTree, usernameAutofillNode, passwordAutofillNode) {
        autofillTree += usernameAutofillNode
        autofillTree += passwordAutofillNode
        onDispose {
            autofillTree.children.remove(usernameAutofillNode.id)
            autofillTree.children.remove(passwordAutofillNode.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar M/Monit") },
                actions = {
                    if (state.configured) {
                        TextButton(onClick = viewModel::closeSettings) {
                            Text("Cancelar")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = "Informe somente os dados básicos. O app usa os caminhos e campos padrão do M/Monit.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::updateServerUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Endereço do site") },
                placeholder = { Text("https://mmonit.exemplo.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::updateUsername,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        usernameAutofillNode.boundingBox = coordinates.boundsInWindow()
                    },
                label = { Text("Usuário") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        passwordAutofillNode.boundingBox = coordinates.boundsInWindow()
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            autofill?.requestAutofillForNode(passwordAutofillNode)
                        }
                    },
                label = { Text("Senha") },
                supportingText = if (state.configured) {
                    { Text("Deixe em branco para manter a senha salva.") }
                } else {
                    null
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Ocultar" else "Mostrar")
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::testConnection,
                    modifier = Modifier.weight(1f),
                    enabled = !state.busy,
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Testar conexão")
                    }
                }
                Button(
                    onClick = viewModel::saveConfiguration,
                    modifier = Modifier.weight(1f),
                    enabled = !state.busy,
                ) {
                    Text("Salvar")
                }
            }
            if (state.configured) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TextButton(
                    onClick = viewModel::clearConfiguration,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Remover configuração", color = MaterialTheme.colorScheme.error)
                }
            }
            MessageText(state)
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun MessageText(state: MainUiState) {
    state.message?.let { message ->
        Text(
            text = message,
            modifier = Modifier.padding(top = 16.dp),
            color = if (state.messageIsError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatTimestamp(timestamp: Long): String = DateTimeFormatter
    .ofPattern("dd/MM/yyyy HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(timestamp))
