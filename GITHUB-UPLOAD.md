# GitHub에서 APK 빌드하기

대상: https://github.com/lemminol/auto-video-downloader-android

1. ZIP을 압축 해제합니다.
2. 저장소 Code → Add file → Upload files에서 압축 해제된 내용(안쪽 파일/폴더)을 업로드합니다. ZIP 자체를 올리지 마세요.
3. .github/workflows/build-apk.yml도 반드시 교체해야 합니다. 숨김 폴더가 업로드되지 않으면 GitHub에서 해당 파일을 열고 Edit으로 압축 안 파일 내용을 붙여넣으세요.
4. main에 커밋하면 Build Android APK가 자동 실행됩니다.
5. Actions에서 성공한 실행을 열고 Artifacts의 AVDv1.1.6을 다운로드합니다. ZIP 안에 AVDv1.1.6.apk가 있습니다.

이번 요청에서는 GitHub 연결의 쓰기 권한이 403으로 거부되어 원격 변경/빌드를 실행하지 못했습니다. 빌드 설정과 소스는 준비되어 있지만 실제 빌드 성공은 아직 확인하지 못했습니다.

로그인 유지·파일 선택·용량 표시·하단 고정 수정은 서버 v61도 함께 적용해야 합니다.
