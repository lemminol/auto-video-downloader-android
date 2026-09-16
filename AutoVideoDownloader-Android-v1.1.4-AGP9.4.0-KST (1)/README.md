# Auto Video Downloader Android

Auto Video Downloader 서버용 Android 전용 클라이언트입니다.

## 주요 기능

- WebView로 서버의 `/jobs`, `/files`, `/logs`, `/recording`, `/channels`, `/accounts`를 그대로 사용
- 지정 Wi-Fi에서는 로컬 엔드포인트 우선
- 다른 네트워크에서는 등록한 외부 HTTPS 엔드포인트를 위에서부터 자동 확인
- `/api/health`를 검사해 Auto Video Downloader 서버인지 확인
- Wi-Fi/LTE/5G 전환 시 현재 경로/파일 URL을 유지하며 서버 주소만 자동 전환
- Android 공유 메뉴에서 URL을 받아 `/jobs?share=...`로 자동 등록
- `avd://share?url=<URL>` 사용자 딥링크 지원 (일반 HTTP/HTTPS 브라우저 링크를 가로채지 않음)
- 서버 로그인 세션은 WebView CookieManager에 유지
- 서버의 쿠키 TXT 업로드 기능을 위한 파일 선택기 지원
- 인증된 파일 다운로드를 Android DownloadManager로 전달
- `intent://` 외부 플레이어 버튼 지원 (VLC, MX Player 등)
- 외부 HTTP 엔드포인트 금지, 사설망 HTTP만 허용
- SSL 인증서 오류 시 우회하지 않고 연결 차단

## 빌드

Android Studio Quail 4 (2026.1.4) / AGP 9.4.0 / JDK 17 이상을 권장합니다.

호환성 설정: AGP 9.4.0 / Gradle 9.6.0 / compileSdk 36 / JDK 17

1. 이 폴더를 Android Studio에서 엽니다.
2. Android SDK 36 / Build Tools 36.0.0을 설치합니다.
3. `app`을 Debug 또는 Release로 빌드합니다.

CLI 예시:

```bash
gradle :app:assembleDebug
```

## 서버 요구사항

동봉된 Auto Video Downloader v60 이상을 권장합니다. v60은:

- `GET /api/health` 제공
- `/jobs?share=<URL>` 공유 URL 자동 등록

을 지원합니다. 구버전 서버도 `/healthz` fallback으로 연결 확인은 가능하지만 공유 메뉴 자동 등록은 v60이 필요합니다.

## 기본 로컬 주소

초기값은 `http://192.168.50.242:8792`입니다. 앱의 `연결` 화면에서 변경할 수 있습니다.

## GitHub Actions로 APK 만들기

프로젝트를 GitHub 저장소에 올리면 `.github/workflows/build-apk.yml`이 Android SDK 36을 설치하고 Debug APK를 빌드합니다. Actions의 `Build Android APK` 결과에서 `AutoVideoDownloader-debug-apk` 아티팩트를 받으면 됩니다.


## v1.1.0 fixes

- Android 15/16 edge-to-edge system bar insets: the connection bar and settings screen no longer render under the status/navigation bars.
- WebView login session hardening: cookies are flushed after navigation and when the app is paused/stopped.
- Automatic network callbacks keep the currently connected endpoint while it remains healthy, preventing a post-login switch between `auto.lemminol.xyz` and `192.168.50.242` from appearing as an unexpected logout.
- The manual **재연결** button and saving connection settings still force endpoint re-selection.

## Windows / Android Studio JDK requirement

If Gradle reports `does not provide the required capabilities: [JAVA_COMPILER]`, the selected Gradle JDK is a runtime-only JRE/JBR without `javac`.

In Android Studio open:

`File > Settings > Build, Execution, Deployment > Build Tools > Gradle`

Set **Gradle JDK** to a full **JDK 17** installation. If none is installed, choose **Download JDK...** and install a JDK 17 distribution (for example Eclipse Temurin 17), then select it.

Verify in a Windows terminal:

```bat
java -version
javac -version
```

Both commands must work. Do not use an Android Studio `jbr` directory that does not contain `bin\\javac.exe`.

This project also accepts `JAVA_HOME` or `JDK17_HOME` when Gradle auto-detects toolchains.


## v1.1.4 connection UI

- External endpoint rows prioritize the URL width.
- Endpoint URLs stay on one line and use middle ellipsis when space is insufficient.
- Up/down controls are compact 30dp icon buttons.
- Delete is reduced to a compact 44dp button.
- The connection status dot is smaller so long Japanese/Korean/ASCII host names get more horizontal space.


## APK 파일명

Android Studio에서 `Generate APKs`를 실행하면 기본 `app-debug.apk`와 함께 `app/build/outputs/apk/debug/AVDv1.1.4.apk`가 자동 생성됩니다. AGP 9.4.0에서 제거된 `applicationVariants` API는 사용하지 않습니다.


## v1.1.4 build fix
- Fixed Groovy syntax error in APK rename task.
- Debug build creates both `app-debug.apk` and `AVDv1.1.4.apk`.
