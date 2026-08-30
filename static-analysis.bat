@echo off
setlocal enabledelayedexpansion

echo =========================================
echo  Transport ^& Logistics - Static Analysis
echo =========================================
echo.

:: Auto-detect JDK 21
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" goto JAVA_FOUND
)

for %%D in (
    "C:\Program Files\Java\jdk-21*"
    "C:\Program Files\Eclipse Adoptium\jdk-21*"
    "C:\Program Files\Microsoft\jdk-21*"
    "C:\Program Files\Amazon Corretto\jdk21*"
    "C:\Program Files\Zulu\zulu-21*"
    "C:\Program Files\BellSoft\LibericaJDK-21*"
) do (
    if exist "%%~fD\bin\java.exe" (
        set "JAVA_HOME=%%~fD"
        set "PATH=%%~fD\bin;!PATH!"
        goto JAVA_FOUND
    )
)

:JAVA_FOUND
:: Resolve Maven command
where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "MVN_CMD=mvn"
) else (
    if exist "mvnw.cmd" (
        set "MVN_CMD=mvnw.cmd"
    ) else (
        echo [!] Maven not found. Please install Maven or use mvnw.cmd.
        exit /b 1
    )
)

echo [1/5] Compile + Tests
call %MVN_CMD% clean verify
if %ERRORLEVEL% neq 0 goto ERROR

echo [2/5] SpotBugs
call %MVN_CMD% spotbugs:check
if %ERRORLEVEL% neq 0 goto ERROR

echo [3/5] PMD
call %MVN_CMD% pmd:check
if %ERRORLEVEL% neq 0 goto ERROR

echo [4/5] Checkstyle
call %MVN_CMD% checkstyle:check
if %ERRORLEVEL% neq 0 goto ERROR

echo [5/5] OWASP Dependency Check
call %MVN_CMD% org.owasp:dependency-check-maven:check
if %ERRORLEVEL% neq 0 goto ERROR

echo.
echo =========================================
echo  ALL STATIC ANALYSIS CHECKS PASSED
echo =========================================
exit /b 0

:ERROR
echo.
echo [ERROR] Static analysis failed. See above output for details.
exit /b 1
