# build-package.ps1
# Automates building and packaging the Stock Management JavaFX application.

$ErrorActionPreference = "Stop"

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "  Building & Packaging: ESTF Stock Management" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# 1. Clean and build the Maven project
Write-Host "`n[1/4] Compiling and packaging Java project with Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests

# 2. Prepare the dependencies directory
Write-Host "`n[2/4] Copying runtime dependencies to target/libs..." -ForegroundColor Yellow
if (Test-Path "target/libs") {
    Remove-Item -Recurse -Force "target/libs"
}
New-Item -ItemType Directory -Path "target/libs" | Out-Null

mvn dependency:copy-dependencies -DoutputDirectory=target/libs

# Copy our application jar into target/libs as well
Copy-Item "target/estf-magasiner-1.0-SNAPSHOT.jar" "target/libs/"

# 3. Create the executable package using jpackage
Write-Host "`n[3/4] Packaging application with jpackage..." -ForegroundColor Yellow

$DestDir = "target/dist"
if (Test-Path $DestDir) {
    Remove-Item -Recurse -Force $DestDir
}
New-Item -ItemType Directory -Path $DestDir | Out-Null

# Define target modules for runtime. 
# We include jdk.compiler because JasperReports compiles .jrxml reports at runtime.
# We include java.scripting because JasperReports uses scripting bindings for report expressions.
$Modules = "java.base,java.sql,java.naming,java.xml,java.desktop,java.management,java.instrument,jdk.unsupported,jdk.compiler,java.scripting,java.logging"

jpackage --name "ESTF-Magasiner" `
         --input target/libs `
         --main-jar estf-magasiner-1.0-SNAPSHOT.jar `
         --main-class ma.estf.magasiner.Launcher `
         --type app-image `
         --dest $DestDir `
         --icon src/main/resources/ma/estf/magasiner/images/estf-icon.ico `
         --vendor "ESTF" `
         --app-version "1.0.0" `
         --add-modules $Modules

# 4. Copy current database and resources to distribution folder
Write-Host "`n[4/4] Finalizing portable application folder..." -ForegroundColor Yellow

$AppFolder = "target/dist/ESTF-Magasiner"

# Copy magasiner.db if it exists so the packaged app has the initial/existing data
if (Test-Path "magasiner.db") {
    Write-Host "Copying magasiner.db to $AppFolder..." -ForegroundColor Green
    Copy-Item "magasiner.db" $AppFolder/
    
    # Also copy WAL/SHM if they exist
    if (Test-Path "magasiner.db-wal") {
        Copy-Item "magasiner.db-wal" $AppFolder/
    }
    if (Test-Path "magasiner.db-shm") {
        Copy-Item "magasiner.db-shm" $AppFolder/
    }
} else {
    Write-Host "Warning: magasiner.db not found in project root. A new blank database will be created on first run." -ForegroundColor Cyan
}

# Copy BC.xlsx if it is needed by the app
if (Test-Path "BC.xlsx") {
    Write-Host "Copying BC.xlsx to $AppFolder..." -ForegroundColor Green
    Copy-Item "BC.xlsx" $AppFolder/
}

# Copy java.exe from JDK to the runtime/bin directory for debugging purposes on target machines
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Host "Bundling java.exe for target machine debugging..." -ForegroundColor Green
    Copy-Item "$env:JAVA_HOME\bin\java.exe" "$AppFolder/runtime/bin/"
}

# Create a debug runner batch file in the app directory
$DebugBatContent = @"
@echo off
title ESTF-Magasiner Debug Console
echo ====================================================
echo Starting ESTF-Magasiner in Debug Mode...
echo ====================================================
".\runtime\bin\java.exe" -cp ".\app\*" ma.estf.magasiner.Launcher
echo ====================================================
echo Application has stopped.
echo ====================================================
pause
"@

$DebugBatPath = "$AppFolder/debug-run.bat"
Set-Content -Path $DebugBatPath -Value $DebugBatContent
Write-Host "Created debug-run.bat in $AppFolder" -ForegroundColor Green

Write-Host "`n==============================================" -ForegroundColor Green
Write-Host "  Success! Portable app generated at:" -ForegroundColor Green
Write-Host "  $AppFolder" -ForegroundColor Green
Write-Host "  Run 'ESTF-Magasiner.exe' to launch." -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
