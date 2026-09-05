<# 
.SYNOPSIS
    Install Android SDK Command-Line Tools to E:\Android
.DESCRIPTION
    Downloads latest cmdline-tools, extracts, installs platforms;android-36,
    build-tools;36.0.0, platform-tools, emulator. No Android Studio IDE required.
.NOTES
    Run as Administrator for best results (PATH persistence).
    Requires: Windows 10/11, PowerShell 5.1+, Internet.
#>

param(
    [string]$InstallRoot = "E:\Android",
    [string]$ApiLevel    = "36",
    [string]$BuildTools  = "36.0.0",
    [switch] $AddToUserPath
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

Write-Host "=== Android SDK Command-Line Tools Installer ===" -ForegroundColor Cyan
Write-Host "Target: $InstallRoot" -ForegroundColor Gray
Write-Host "API Level: $ApiLevel | Build-Tools: $BuildTools" -ForegroundColor Gray

# --- 1. Prerequisites ---
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java tidak ditemukan. Install JDK 17 dulu (sudah ada di C:\Program Files\Eclipse Adoptium\jdk-17...)."
    exit 1
}
java -version 2>&1 | Select-Object -First 1 | Write-Host

# --- 2. Folder structure ---
$toolsDir      = Join-Path $InstallRoot "cmdline-tools"
$latestDir     = Join-Path $toolsDir "latest"
$binDir        = Join-Path $latestDir "bin"
$platformTools = Join-Path $InstallRoot "platform-tools"
$zipPath       = Join-Path $env:TEMP "cmdline-tools-latest.zip"
$url           = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
New-Item -ItemType Directory -Force -Path $platformTools | Out-Null

# --- 3. Download ---
Write-Host "`n[1/5] Downloading cmdline-tools..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
    Write-Host "    Done: $([math]::Round((Get-Item $zipPath).Length/1MB,1)) MB" -ForegroundColor Green
}
catch {
    Write-Error "Download gagal: $_"
    exit 1
}

# --- 4. Extract ---
Write-Host "[2/5] Extracting..." -ForegroundColor Yellow
$tmpExtract = Join-Path $toolsDir "tmp_extract"
if (Test-Path $tmpExtract) { Remove-Item $tmpExtract -Recurse -Force -ErrorAction SilentlyContinue }

try {
    Expand-Archive -LiteralPath $zipPath -DestinationPath $tmpExtract -Force
    # Struktur zip: cmdline-tools/{bin,lib,NOTICE.txt,source.properties}
    $inner = Get-ChildItem $tmpExtract -Directory | Select-Object -First 1
    if ($null -eq $inner) { throw "Zip structure unexpected" }
    if (Test-Path $latestDir) { Remove-Item $latestDir -Recurse -Force -ErrorAction SilentlyContinue }
    Move-Item $inner.FullName $latestDir -Force
    Remove-Item $tmpExtract -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "    Extracted to $latestDir" -ForegroundColor Green
}
catch {
    Write-Error "Extract gagal: $_"
    exit 1
}

# --- 5. Verify sdkmanager ---
$sdkmanager = Join-Path $binDir "sdkmanager.bat"
if (-not (Test-Path $sdkmanager)) {
    Write-Error "sdkmanager.bat tidak ditemukan di $binDir"
    exit 1
}
Write-Host "[3/5] sdkmanager found" -ForegroundColor Green

# --- 6. Install packages ---
Write-Host "[4/5] Installing SDK packages (this may take 5-15 min)..." -ForegroundColor Yellow
$packages = @(
    "platforms;android-$ApiLevel",
    "build-tools;$BuildTools",
    "platform-tools",
    "emulator"
)

# Accept licenses non-interactively
$licensesDir = Join-Path $InstallRoot "licenses"
New-Item -ItemType Directory -Force -Path $licensesDir | Out-Null
# Android SDK License IDs (as of 2024)
@("24333f8a63b6825ea9c5514f83c2829b004d1fee", "d56f5187479451eabf01fb78af6dfcb131a6481e") |
    ForEach-Object { Set-Content -Path (Join-Path $licensesDir "android-sdk-license") -Value $_ -Force }

$pkgList = $packages -join " "
Write-Host "    Packages: $pkgList" -ForegroundColor Gray
& $sdkmanager --sdk_root=$InstallRoot --install $pkgList 2>&1 | ForEach-Object { Write-Host "    $_" }
if ($LASTEXITCODE -ne 0) {
    Write-Warning "sdkmanager exit code $LASTEXITCODE (mungkin sudah ada paket, lanjut...)"
}

# --- 7. Verify key tools ---
Write-Host "[5/5] Verifying installation..." -ForegroundColor Yellow
$checks = @(
    @{Name="adb"; Path=Join-Path $platformTools "adb.exe"},
    @{Name="aapt2"; Path=(Get-ChildItem -Recurse (Join-Path $InstallRoot "build-tools") -Filter "aapt2.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName},
    @{Name="emulator"; Path=(Get-ChildItem -Recurse (Join-Path $InstallRoot "emulator") -Filter "emulator.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName},
    @{Name="avdmanager"; Path=Join-Path $binDir "avdmanager.bat"}
)

foreach ($c in $checks) {
    if ($c.Path -and (Test-Path $c.Path)) {
        Write-Host "    [$c.Name] OK -> $c.Path" -ForegroundColor Green
    } else {
        Write-Host "    [$c.Name] MISSING" -ForegroundColor Red
    }
}

# --- 8. Environment vars ---
Write-Host "`n=== Environment Variables ===" -ForegroundColor Cyan
Write-Host "Add these to your shell profile (PowerShell/Bash) or Windows Env Vars:" -ForegroundColor Gray
Write-Host "`$env:ANDROID_HOME = `"$InstallRoot`""
Write-Host "`$env:PATH += `";$InstallRoot\cmdline-tools\latest\bin;$InstallRoot\platform-tools`""
Write-Host "`$env:JAVA_HOME = `"`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`" (sudah ada)"

# Optional: persist to user PATH
if ($AddToUserPath) {
    Write-Host "`n[+] Adding to User PATH..." -ForegroundColor Yellow
    $userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
    $add1 = Join-Path $InstallRoot "cmdline-tools\latest\bin"
    $add2 = Join-Path $InstallRoot "platform-tools"
    foreach ($p in @($add1, $add2)) {
        if ($userPath -notlike "*$p*") {
            $userPath += ";$p"
            Write-Host "    Added: $p"
        }
    }
    [Environment]::SetEnvironmentVariable("PATH", $userPath, "User")
    [Environment]::SetEnvironmentVariable("ANDROID_HOME", $InstallRoot, "User")
    Write-Host "    Done. Restart terminal/PowerShell to take effect." -ForegroundColor Green
}

Write-Host "`n=== NEXT STEPS ===" -ForegroundColor Cyan
Write-Host "1. Restart terminal / buka PowerShell baru"
Write-Host "2. Jalankan: `adb version` → harus keluar Android Debug Bridge version"
Write-Host "3. Buat AVD (emulator): `avdmanager create avd -n test -k 'system-images;android-$ApiLevel;default;arm64'`"
Write-Host "4. Jalankan emulator: `emulator -avd test`"
Write-Host "5. Build APK: `cd Apk-Laporan-Keuangan; ./gradlew assembleDebug` (butuh Git Bash/WSL untuk ./gradlew)"
Write-Host "`nDone!" -ForegroundColor Green