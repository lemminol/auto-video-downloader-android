@echo off
setlocal
where java >nul 2>nul || (echo [ERROR] java.exe not found in PATH & exit /b 1)
where javac >nul 2>nul || (echo [ERROR] javac.exe not found in PATH. Install a full JDK 17 and select it as Gradle JDK in Android Studio. & exit /b 2)
echo === java ===
java -version
echo.
echo === javac ===
javac -version
echo.
echo JDK compiler is available.
