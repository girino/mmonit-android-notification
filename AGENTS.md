# Instruções do projeto

## Toolchain local obrigatória

Este projeto mantém o Android SDK e o JDK usados no build dentro de `.tools/`, que é ignorado pelo Git. Não usar automaticamente SDKs, JDKs ou Gradle instalados globalmente quando a tarefa exigir uma dessas ferramentas.

Paths esperados no Windows:

- Android SDK: `.tools/android-sdk`
- JDK: `.tools/jdk-17`
- Gradle: `.tools/gradle`
- Configuração local do Android Studio/Gradle: `local.properties` com `sdk.dir` apontando para `.tools/android-sdk`

Antes de compilar, configurar a sessão do PowerShell:

```powershell
$env:JAVA_HOME = "$PWD\.tools\jdk-17"
$env:ANDROID_HOME = "$PWD\.tools\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"
```

Os pacotes mínimos do Android SDK são `platform-tools`, `platforms;android-35` e `build-tools;35.0.0`. Se `.tools/` não estiver instalado, baixar uma distribuição JDK 17 e as Android SDK Command-line Tools para dentro dessa pasta; não gravar SDKs ou JDKs no repositório versionado. Verifique `java -version`, `sdkmanager --version` e `adb version` antes do build.

Use o Gradle Wrapper do projeto ou uma distribuição Gradle dentro de `.tools/`. Não adicione arquivos de `.tools/` ao Git e não coloque credenciais em `local.properties`.

### Instalação inicial no Windows

Executar os comandos PowerShell visivelmente, a partir da raiz do projeto, somente quando os diretórios locais ainda não existirem:

```powershell
New-Item -ItemType Directory -Path .tools -Force
curl.exe -L --fail --output .tools\temurin-17.zip "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
Expand-Archive .tools\temurin-17.zip -DestinationPath .tools
Rename-Item .tools\jdk-* jdk-17

New-Item -ItemType Directory -Path .tools\android-sdk\cmdline-tools -Force
curl.exe -L --fail --output .tools\commandlinetools.zip "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
Expand-Archive .tools\commandlinetools.zip -DestinationPath .tools\cmdline-extract
Move-Item .tools\cmdline-extract\cmdline-tools .tools\android-sdk\cmdline-tools\latest

curl.exe -L --fail --output .tools\gradle.zip "https://services.gradle.org/distributions/gradle-8.14.3-bin.zip"
Expand-Archive .tools\gradle.zip -DestinationPath .tools
Rename-Item .tools\gradle-8.14.3 gradle

$env:JAVA_HOME = "$PWD\.tools\jdk-17"
$env:ANDROID_HOME = "$PWD\.tools\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"

$yes = ("y`n" * 100)
$yes | sdkmanager.bat --sdk_root="$env:ANDROID_HOME" --licenses
sdkmanager.bat --sdk_root="$env:ANDROID_HOME" "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

O arquivo local `local.properties` deve conter somente `sdk.dir` apontando para `.tools/android-sdk`; ele é ignorado pelo Git. O projeto atual já possui essa configuração e o toolchain local instalado.

## Convenções

- Código Android em Kotlin, com Compose para a UI.
- Consultas de produção usam somente leitura da API do M/Monit.
- Nunca registrar ou imprimir usuário, senha, cookies de sessão ou URLs com credenciais.
- Executar `test` antes de `assembleDebug` quando o toolchain local estiver disponível.
