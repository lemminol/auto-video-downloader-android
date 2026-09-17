# Android v1.1.9 — 뒤로가기 수정

Android 13 이상은 OnBackInvokedDispatcher로, Android 12 이하는 onBackPressed로 동일하게 WebView 이전 페이지로 이동합니다. 이전 페이지가 없으면 앱을 종료하지 않고 안내 메시지를 표시합니다. 연결 설정 화면의 뒤로가기는 기존대로 메인 화면으로 돌아갑니다.

.github/workflows를 포함한 프로젝트 전체를 GitHub 저장소 루트에 반영하고 기존 Build Android APK 워크플로를 실행하세요. 또는 Android Studio에서 빌드할 수 있습니다. 생성 APK는 AVDv1.1.9.apk입니다. 기존 설치와 동일한 서명 키를 사용해야 덮어쓰기 설치가 가능합니다.

검증: 소스 변경 및 Manifest XML 확인. 현재 환경에 Android SDK가 없어 APK 빌드 및 기기 동작 검증은 수행하지 않았습니다.

# Auto Video Downloader Android v1.1.8

## 외부 네트워크 HTTP / HTTPS 선택
연결 → 외부 네트워크 → 엔드포인트 추가에서 https:// 또는 http://를 선택하고 서버 주소/IP 및 포트를 입력하세요.
전체 URL을 붙여넣으면 프로토콜이 자동 선택됩니다. 붙여넣은 뒤 선택 버튼을 변경하면 마지막 선택을 적용합니다. 주소를 추가한 후 저장하고 연결을 눌러야 영구 저장됩니다.
같은 서버의 HTTP/HTTPS 주소를 각각 등록할 수 있습니다. 기존 주소 우선순위와 자동 전환 방식은 유지됩니다.
외부 HTTP는 사용자가 명시적으로 등록할 때 허용합니다. HTTPS 연결 실패 시 외부 주소를 임의로 HTTP로 낮추지 않습니다. HTTP 연결은 암호화되지 않습니다.

## 설치와 빌드
Android Studio에서는 settings.gradle이 있는 폴더를 열어 빌드하세요. 출력은 app/build/outputs/apk/debug/AVDv1.1.8.apk입니다.
GitHub에서는 압축 안 내용을 저장소 루트에 덮어쓰고 main에 커밋하세요. .github의 기존 KST / Releases APK 자동 첨부 설정을 포함합니다.
변경 파일: app/build.gradle, app/src/main/java/com/lemminol/avd/ConnectionActivity.java, app/src/main/java/com/lemminol/avd/EndpointManager.java.
앱 시작 충돌 수정은 v1.1.7에서 유지했습니다. NAS 서버 재설치는 필요 없습니다.
기존 앱과 같은 서명으로 빌드해야 덮어쓰기 설치할 수 있습니다.

## 검증 범위
주소 등록·정규화·저장·연결 검사 경로의 HTTP 허용 조건을 검토했습니다. XML과 워크플로 문법 검사를 완료했습니다. 이 환경에 Android SDK/Gradle이 없어 APK 컴파일 및 실제 휴대폰 연결은 검증하지 못했습니다.
