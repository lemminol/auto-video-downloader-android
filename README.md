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

## 기본 APK 빌드 경로

APK는 이 저장소의 [GitHub Actions](https://github.com/lemminol/auto-video-downloader-android/actions/workflows/build-apk.yml)에서 빌드합니다. `main`에 변경을 올리면 자동 실행되며, Actions → Build Android APK → Run workflow로 수동 실행할 수도 있습니다. 성공한 실행의 Artifacts에서 `AVDv<버전>` ZIP을 내려받아 APK를 꺼내세요. 버전과 APK 이름은 `app/build.gradle`을 기준으로 자동 반영됩니다.

## v1.1.6

- 화면 시스템 여백이 WebView에 중복 적용되는 문제를 수정했습니다.
- 키보드와 화면 잘림 영역을 반영하고 시스템 영역 배경색을 맞췄습니다.
- 로그인 유지와 파일·폴더 선택/용량/하단 도구막대 수정은 서버 v61을 함께 적용해야 합니다. 서버 로그인 화면의 '로그인 유지 (30일)'를 체크해 다시 로그인하세요.
- GitHub Actions는 Debug APK를 생성합니다. 기존 설치 앱과 서명이 같아야 덮어쓰기 업데이트가 가능합니다.

## 로컬 빌드 (선택)

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

초기 서버 주소는 비어 있습니다. 앱의 `연결` 화면에서 사용자가 직접 서버 주소/IP를 입력합니다. `http://` 또는 `https://`를 생략하면 HTTPS를 먼저 시도하고, 사설망 주소에서는 HTTP도 자동으로 확인합니다.

## GitHub Actions로 APK 만들기

프로젝트를 GitHub 저장소에 올리면 `.github/workflows/build-apk.yml`이 Android SDK 36을 설치하고 Debug APK를 빌드합니다. Actions의 `Build Android APK` 결과에서 `AVDv<버전>` 아티팩트를 받으면 됩니다.


## v1.1.0 fixes

- Android 15/16 edge-to-edge system bar insets: the connection bar and settings screen no longer render under the status/navigation bars.
- WebView login session hardening: cookies are flushed after navigation and when the app is paused/stopped.
- Automatic network callbacks keep the currently connected endpoint while it remains healthy, preventing a post-login switch between an external hostname and an internal/LAN address from appearing as an unexpected logout.
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


## v1.1.5 connection UI

- External endpoint rows prioritize the URL width.
- Endpoint URLs stay on one line and use middle ellipsis when space is insufficient.
- Up/down controls are compact 30dp icon buttons.
- Delete is reduced to a compact 44dp button.
- The connection status dot is smaller so long Japanese/Korean/ASCII host names get more horizontal space.


## APK 파일명

Android Studio에서 `Generate APKs`를 실행하면 기본 `app-debug.apk`와 함께 `app/build/outputs/apk/debug/AVDv1.1.6.apk`가 자동 생성됩니다. AGP 9.4.0에서 제거된 `applicationVariants` API는 사용하지 않습니다.


## v1.1.5 build fix
- Fixed Groovy syntax error in APK rename task.
- Debug build creates both `app-debug.apk` and `AVDv1.1.6.apk`.

