# Android 1.1.16 / versionCode 17

서버 v106과 함께 사용하세요. 서버의 최신 웹페이지를 그대로 표시하며, 작품 뷰어 전체화면에서 앱 상단 바를 숨깁니다. 뒤로가기는 전체화면을 먼저 닫고 그 다음 이전 페이지로 이동합니다. 웹의 영상 전체화면 콜백도 지원합니다.

설정 → 작품 뷰어 읽기 설정은 앱 WebView에도 저장됩니다. 일반 브라우저의 읽기 설정과 별도로 저장되므로 앱에서 처음 한 번 설정하세요.

이 패키지는 소스입니다. 로컬 APK 빌드는 수행하지 않았으며, 아래 기존 서명 Secrets를 유지한 GitHub Actions에서 빌드하세요.

GitHub Actions가 매번 다른 디버그 키를 생성해 업데이트 설치가 거부되던 문제를 수정했습니다. 워크플로는 GitHub Secrets에 보관한 고정 릴리스 키로 `assembleRelease`를 실행하고 APK 서명과 버전을 검사합니다.

필요한 GitHub Actions Secrets:

- `AVD_KEYSTORE_BASE64`
- `AVD_KEYSTORE_PASSWORD`
- `AVD_KEY_ALIAS`
- `AVD_KEY_PASSWORD`

기존 임시 디버그 키로 설치한 앱에서 고정 릴리스 키로 전환할 때는 서명이 달라 한 번 삭제 후 설치해야 합니다. 이후에는 이 키를 바꾸거나 잃어버리지 않는 한 높은 `versionCode`의 APK로 계속 덮어쓰기 업데이트할 수 있습니다.

상단 URL 숨김, 새로고침·설정 아이콘, WebView, 뒤로가기·종료 확인 기능은 유지됩니다. SDK 경로인 `local.properties`와 실제 키 파일은 포함하지 않습니다.
