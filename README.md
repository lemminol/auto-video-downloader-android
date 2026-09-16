# Auto Video Downloader Android v1.1.7

## 실행 즉시 종료 수정
v1.1.6에서 MainActivity.buildUi가 setContentView 전에 Window.getInsetsController를 호출하여 일부 기기에서 DecorView가 null인 상태로 접근했습니다. avd-crash.txt의 PhoneWindow.getInsetsController → MainActivity.buildUi 충돌에 해당합니다.

v1.1.7은 해당 호출을 제거하고 화면에 연결된 View의 WindowInsets 콜백에서 View.getWindowInsetsController를 조회합니다. null이면 색상 변경을 건너뜁니다. versionCode는 8입니다.

## 적용
- Android Studio: settings.gradle이 있는 이 폴더를 열고 Gradle 동기화 후 assembleDebug / Generate APKs로 빌드합니다.
- GitHub: 압축 안 파일·폴더를 저장소 루트에 덮어쓰기 업로드하고 main에 커밋합니다. .github도 포함하세요. 기존 저장소의 빌드 환경은 유지됩니다.
- 최소 변경 파일은 app/src/main/java/com/lemminol/avd/MainActivity.java 및 app/build.gradle 두 개입니다. 두 파일을 한 커밋으로 반영하세요.
- 출력 파일은 app/build/outputs/apk/debug/AVDv1.1.7.apk입니다.
- GitHub 워크플로는 KST 시간 표시 및 Releases에 APK 직접 첨부 설정을 포함합니다.
- 기존 설치 앱과 같은 서명으로 빌드해야 업데이트 설치가 가능합니다. 서명이 다르면 삭제하기 전에 앱의 서버 주소 설정을 기록하세요.
- NAS 서버 v61 및 서버의 작업 기록·저장 파일은 수정하지 않습니다.

## 확인 범위
제공된 충돌 로그와 기존 소스의 호출 위치를 대조했고, 시작 시 Window.getInsetsController 호출이 제거된 것을 확인했습니다. YAML 및 워크플로 셸 문법 검사를 수행했습니다. 이 환경에는 Android SDK/Gradle/기기가 없어 APK 컴파일·실제 실행 검증은 수행하지 못했습니다.
