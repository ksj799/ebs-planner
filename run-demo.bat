@echo off
rem EBS Planner - local demo (H2 sample data). Double-click to start, close this window to stop.
chcp 65001 >nul
cd /d "%~dp0"
rem Java 17: %USERPROFILE%\tools\jdk-17 -> JAVA_HOME -> PATH order
set "JAVA=%USERPROFILE%\tools\jdk-17\bin\java.exe"
if not exist "%JAVA%" if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"
if not exist "target\ebs-planner-0.1.0.jar" (
  echo [ERROR] target\ebs-planner-0.1.0.jar not found. Build first: mvn -DskipTests package
  pause
  exit /b 1
)
echo Starting demo server ... open http://localhost:8080 when "Started PlannerApplication" appears.
start "" /b cmd /c "timeout /t 12 >nul & start http://localhost:8080"
"%JAVA%" -Dfile.encoding=UTF-8 -jar target\ebs-planner-0.1.0.jar --spring.profiles.active=demo
pause
