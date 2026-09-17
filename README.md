# Android 1.1.14 / versionCode 15

GitHub Actions가 매번 다른 디버그 키를 생성해 업데이트 설치가 거부되던 문제를 수정했습니다. 워크플로는 GitHub Secrets에 보관한 고정 릴리스 키로 `assembleRelease`를 실행하고 APK 서명과 버전을 검사합니다.

필요한 GitHub Actions Secrets:

- `AVD_KEYSTORE_BASE64`
- `AVD_KEYSTORE_PASSWORD`
- `AVD_KEY_ALIAS`
- `AVD_KEY_PASSWORD`

기존 임시 디버그 키로 설치한 앱에서 고정 릴리스 키로 전환할 때는 서명이 달라 한 번 삭제 후 설치해야 합니다. 이후에는 이 키를 바꾸거나 잃어버리지 않는 한 높은 `versionCode`의 APK로 계속 덮어쓰기 업데이트할 수 있습니다.

상단 URL 숨김, 새로고침·설정 아이콘, WebView, 뒤로가기·종료 확인 기능은 유지됩니다. SDK 경로인 `local.properties`와 실제 키 파일은 포함하지 않습니다.
