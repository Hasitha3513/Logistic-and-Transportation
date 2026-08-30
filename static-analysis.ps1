# Transport & Logistics - PowerShell Static Analysis
$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Transport & Logistics - Static Analysis" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Auto-detect JDK 21
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $candidates = @(
        "C:\Program Files\Java\jdk-21*",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Microsoft\jdk-21*",
        "C:\Program Files\Amazon Corretto\jdk21*",
        "C:\Program Files\Zulu\zulu-21*",
        "C:\Program Files\BellSoft\LibericaJDK-21*"
    )
    foreach ($pattern in $candidates) {
        $found = Get-Item $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found -and (Test-Path "$($found.FullName)\bin\java.exe")) {
            $env:JAVA_HOME = $found.FullName
            $env:PATH = "$($found.FullName)\bin;$env:PATH"
            break
        }
    }
}

# Resolve Maven command
$mvnCmd = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } elseif (Test-Path ".\mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }

Write-Host "[1/5] Compile + Tests..." -ForegroundColor Yellow
& $mvnCmd clean verify

Write-Host "[2/5] SpotBugs..." -ForegroundColor Yellow
& $mvnCmd spotbugs:check

Write-Host "[3/5] PMD..." -ForegroundColor Yellow
& $mvnCmd pmd:check

Write-Host "[4/5] Checkstyle..." -ForegroundColor Yellow
& $mvnCmd checkstyle:check

Write-Host "[5/5] OWASP Dependency Check..." -ForegroundColor Yellow
& $mvnCmd org.owasp:dependency-check-maven:check

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host " ALL STATIC ANALYSIS CHECKS PASSED" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
