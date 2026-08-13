@echo off
where mvn >nul 2>nul || (echo Maven is not installed. Install Maven 3.6.3+ or add the standard Maven Wrapper files. & exit /b 127)
mvn %*
