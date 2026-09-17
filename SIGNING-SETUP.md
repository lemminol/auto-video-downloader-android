# GitHub APK 고정 서명키 설정

아래 작업은 최초 한 번만 합니다. 생성한 `avd-release.jks`와 암호를 잃어버리면 기존 앱을 업데이트할 수 없으므로 NAS 등 두 곳 이상에 백업하세요. 키 파일은 GitHub 저장소에 직접 올리지 않습니다.

## 1. Windows PowerShell에서 키 생성

JDK의 `keytool.exe`가 PATH에 등록된 터미널에서 실행합니다.

```powershell
keytool -genkeypair -v -keystore avd-release.jks -alias avd -keyalg RSA -keysize 4096 -validity 10000
```

## 2. 키 파일을 Base64 한 줄로 변환

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\avd-release.jks")) | Set-Content -NoNewline avd-release-base64.txt
```

## 3. GitHub 저장소 Secrets 등록

GitHub 저장소의 `Settings` → `Secrets and variables` → `Actions` → `New repository secret`에서 등록합니다.

| Secret | 값 |
|---|---|
| `AVD_KEYSTORE_BASE64` | `avd-release-base64.txt` 전체 내용 |
| `AVD_KEYSTORE_PASSWORD` | 키 저장소 생성 시 입력한 암호 |
| `AVD_KEY_ALIAS` | `avd` |
| `AVD_KEY_PASSWORD` | 키 암호 |

그 후 Actions에서 **Build Android APK**를 실행하면 서명된 `AVDv1.1.14.apk`가 Releases에 첨부됩니다.

## 업데이트 전환

이전 APK가 다른 키로 서명되어 있다면 기존 앱을 한 번 삭제하고 1.1.14를 설치해야 합니다. 이후 빌드에서는 같은 네 개의 Secrets를 유지하면 앱 데이터와 설정을 보존한 채 업데이트됩니다.
