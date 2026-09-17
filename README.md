# M/Monit Android Status

Aplicativo Android que consulta periodicamente um servidor M/Monit e exibe o pior estado encontrado em uma tela e em uma notificação persistente.

## Escopo do MVP

- Configuração somente de endereço, usuário e senha.
- Login por formulário com os campos padrão do M/Monit.
- Consulta de `/api/2/status/hosts/summary`.
- Polling periódico com `WorkManager`, respeitando o intervalo mínimo de 15 minutos do Android.
- Solicitação de exclusão da otimização de bateria para reduzir suspensões do polling quando a tela está desligada.
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

O workflow de release gera `app-release.apk` assinado com uma chave de produção armazenada nos GitHub Actions secrets. O workflow `Build` continua gerando um APK debug apenas para testes. A chave privada não é versionada no repositório.

## Instalação com Obtainium

O [Obtainium](https://github.com/ImranR98/Obtainium) pode acompanhar os releases deste repositório e instalar novas versões diretamente do GitHub.

1. Instale o Obtainium pelo [GitHub](https://github.com/ImranR98/Obtainium/releases), F-Droid ou IzzyOnDroid.
2. Abra o Obtainium e toque em `Add App`.
3. Cole a URL do repositório: `https://github.com/girino/mmonit-android-notification`.
4. Confirme a fonte GitHub e adicione o aplicativo.
5. Para testar a versão alpha atual, habilite o acompanhamento de pre-releases nas opções da fonte e selecione `v0.1.0-alpha.2`.
6. Para acompanhar somente versões estáveis, deixe pre-releases desabilitado. O Obtainium poderá verificar e instalar os próximos releases automaticamente.

Também é possível abrir diretamente a tela de adição no Obtainium:

```text
obtainium://add?url=https%3A%2F%2Fgithub.com%2Fgirino%2Fmmonit-android-notification
```

Na primeira instalação, o Android pode pedir autorização para que o Obtainium instale aplicativos de fontes desconhecidas. A release `v0.1.0-alpha.1` foi publicada antes da configuração da chave de produção e é um APK debug; a `v0.1.0-alpha.2` é a primeira release assinada com a chave de produção. Se a alpha.1 já estiver instalada, será necessário desinstalá-la antes de instalar a alpha.2.

Estas instruções também são incluídas nas notas das GitHub Releases, que o Obtainium exibe como changelog do aplicativo.

## Observações

O Android não permite garantir um ícone colorido arbitrário na barra de status. O app usa um ícone monocromático obrigatório, a cor do canal/notificação e um círculo colorido como ícone expandido, além do indicador colorido na tela principal.

## Execução em segundo plano

Ao abrir o app, enquanto a isenção não estiver concedida, ele solicita a exclusão da otimização de bateria para tentar manter o polling funcionando durante períodos longos com a tela desligada. Essa autorização não garante execução contínua: alguns fabricantes também exigem que o app seja configurado como `Sem restrições` ou `Não otimizado` nas configurações de bateria e que os dados em segundo plano estejam liberados.

## Licença

O aplicativo é distribuído sob a [Girino Anarchist's License (GAL)](https://license.girino.org). A cópia da licença está no arquivo `LICENSE`, e o app também exibe essas informações na tela `Sobre`.
