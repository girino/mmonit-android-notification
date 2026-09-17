# M/Monit Android Status

Aplicativo Android que consulta periodicamente um servidor M/Monit e exibe o pior estado encontrado em uma tela e em uma notificação persistente.

## Escopo do MVP

- Configuração somente de endereço, usuário e senha.
- Login por formulário com os campos padrão do M/Monit.
- Consulta de `/api/2/status/hosts/summary`.
- Polling periódico com `WorkManager`, respeitando o intervalo mínimo de 15 minutos do Android.
- Notificação persistente com círculo colorido na área expandida e estado textual.
- Toque no círculo de status para abrir o endereço configurado no navegador padrão.
- Credenciais cifradas com AES-GCM usando uma chave do Android Keystore.
- Endereços `https://` são recomendados; `http://` funciona somente quando o servidor foi configurado explicitamente dessa forma.

## Estados

O parser aplica a prioridade abaixo, do pior para o menos grave:

1. `failed` -> vermelho
2. `services failed` -> amarelo
3. `some services failed/unmonitored` -> laranja
4. `inactive` ou `ignored` -> cinza
5. `ok` -> verde
6. Falha de rede, login ou resposta -> preto

## Requisitos

- Android SDK 35
- JDK 17 local em `.tools/jdk-17`
- Android SDK local em `.tools/android-sdk`
- Gradle local em `.tools/gradle` ou Gradle Wrapper

## Build e testes

```text
gradlew.bat test
gradlew.bat assembleDebug
```

Antes dos comandos, configure a sessão do PowerShell conforme o `AGENTS.md`. O projeto mantém o JDK 17, Android SDK 35, Platform Tools e Gradle 8.14.3 dentro de `.tools/`; essa pasta é ignorada pelo Git.

## GitHub Actions e releases

- `Test` executa os testes unitários em pushes para `main` e pull requests.
- `Build` gera e publica o APK debug como artefato em pushes para `main` e pull requests.
- `Release` é executado somente por tags iniciadas com `v`.
- Tags `v1.2.3` geram releases normais.
- Tags `v1.2.3-alpha1`, `v1.2.3-beta`, `v1.2.3-rc1` e outros sufixos semver geram pre-releases.

O workflow de release usa o APK debug assinado automaticamente pelo Android para permitir instalação sem armazenar uma chave privada no repositório. Uma chave de assinatura de produção deverá ser configurada antes de distribuir uma versão final para a Play Store.

## Observações

O Android não permite garantir um ícone colorido arbitrário na barra de status. O app usa um ícone monocromático obrigatório, a cor do canal/notificação e um círculo colorido como ícone expandido, além do indicador colorido na tela principal.
