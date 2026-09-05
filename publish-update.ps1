<#
.SYNOPSIS
    Automates publishing a new DailyTrack Mobile update to GitHub Releases.

.DESCRIPTION
    1. Bumps versionCode and versionName in app/build.gradle.kts.
    2. Builds app-debug.apk using Android Studio's bundled JBR.
    3. Copies the built APK to releases/DailyTrack-v<version>.apk.
    4. Commits and pushes the version bump to Git.
    5. Creates a GitHub Release and uploads the APK automatically (if GITHUB_TOKEN is available)
       or opens the GitHub Releases page with the APK pre-staged for instant drag-and-drop.

.EXAMPLE
    .\publish-update.ps1 -Version "1.0.1" -Notes "Fixed dark mode balance colors and added auto-updater"
#>

param(
    [Parameter(Mandatory = $false)]
    [string]$Version,

    [Parameter(Mandatory = $false)]
    [string]$Notes,

    [Parameter(Mandatory = $false)]
    [string]$Title,

    [Parameter(Mandatory = $false)]
    [string]$GithubToken = $env:GITHUB_TOKEN
)

$ErrorActionPreference = "Stop"
$RepoOwner = "sabarishh14"
$RepoName = "DailyTrack-Mobile"
$RootPath = $PSScriptRoot

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "       DailyTrack App Update Publisher          " -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

# 1. Prompt for version if not provided
if (-not $Version) {
    $Version = Read-Host "Enter new version name (e.g. 1.0.1)"
}
$CleanVersion = $Version.Trim().TrimStart('v').TrimStart('V')
$Tag = "v$CleanVersion"

if (-not $Title) {
    $Title = "DailyTrack $Tag"
}

if (-not $Notes) {
    $Notes = Read-Host "Enter release notes / changelog (e.g. Bug fixes and performance improvements)"
    if (-not $Notes) {
        $Notes = "DailyTrack update $Tag with latest improvements and fixes."
    }
}

Write-Host "`n[1/5] Updating app/build.gradle.kts with version $CleanVersion..." -ForegroundColor Yellow

$GradleFile = Join-Path $RootPath "app\build.gradle.kts"
$GradleContent = Get-Content -Path $GradleFile -Raw

# Increment versionCode
if ($GradleContent -match 'versionCode\s*=\s*(\d+)') {
    $CurrentCode = [int]$matches[1]
    $NewCode = $CurrentCode + 1
    $GradleContent = $GradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $NewCode"
    Write-Host "  -> Incremented versionCode: $CurrentCode -> $NewCode" -ForegroundColor Green
} else {
    Write-Host "  -> Warning: Could not detect versionCode in build.gradle.kts" -ForegroundColor DarkYellow
}

# Update versionName
$GradleContent = $GradleContent -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$CleanVersion`""
Set-Content -Path $GradleFile -Value $GradleContent -NoNewline
Write-Host "  -> Set versionName = `"$CleanVersion`"" -ForegroundColor Green

# 2. Build APK
Write-Host "`n[2/5] Building debug APK with Gradle..." -ForegroundColor Yellow
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

cmd.exe /c "set ""JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"" && gradlew.bat assembleDebug"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed with exit code $LASTEXITCODE"
    exit 1
}

$ApkSource = Join-Path $RootPath "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $ApkSource)) {
    Write-Error "Could not find built APK at $ApkSource"
    exit 1
}

# Copy to releases/ folder
$ReleasesDir = Join-Path $RootPath "releases"
if (-not (Test-Path $ReleasesDir)) {
    New-Item -ItemType Directory -Path $ReleasesDir | Out-Null
}

$ApkDestName = "DailyTrack-$Tag.apk"
$ApkDest = Join-Path $ReleasesDir $ApkDestName
# Clean up any older release APKs
Get-ChildItem -Path $ReleasesDir -Filter "*.apk" -ErrorAction SilentlyContinue | Where-Object { $_.FullName -ne $ApkDest } | Remove-Item -Force
Copy-Item -Path $ApkSource -Destination $ApkDest -Force
$ApkSizeMb = [math]::Round(((Get-Item $ApkDest).Length / 1MB), 2)
Write-Host "  -> APK created at: $ApkDest ($ApkSizeMb MB)" -ForegroundColor Green

# 3. Git commit & push
Write-Host "`n[3/5] Committing and pushing version bump..." -ForegroundColor Yellow
git add app/build.gradle.kts
git commit -m "chore(release): bump version to $Tag"
git push origin main

# 4. Publish Release to GitHub
Write-Host "`n[4/5] Publishing Release to GitHub ($RepoOwner/$RepoName)..." -ForegroundColor Yellow

if (-not $GithubToken) {
    try {
        $credOutput = ('protocol=https' + "`n" + 'host=github.com' + "`n`n") | git credential fill 2>$null
        foreach ($line in ($credOutput -split "`n")) {
            if ($line.Trim().StartsWith("password=")) {
                $GithubToken = $line.Trim().Substring("password=".Length).Trim()
                Write-Host "  -> Auto-detected GitHub credentials from Git Credential Manager." -ForegroundColor Gray
                break
            }
        }
    } catch {}
}

if ($GithubToken) {
    try {
        $Headers = @{
            "Authorization" = "Bearer $GithubToken"
            "Accept" = "application/vnd.github.v3+json"
            "User-Agent" = "DailyTrack-Publisher"
        }

        $ReleaseBody = @{
            tag_name = $Tag
            target_commitish = "main"
            name = $Title
            body = $Notes
            draft = $false
            prerelease = $false
        } | ConvertTo-Json

        Write-Host "  -> Creating GitHub Release $Tag..." -ForegroundColor Gray
        $CreateUrl = "https://api.github.com/repos/$RepoOwner/$RepoName/releases"
        $Release = Invoke-RestMethod -Uri $CreateUrl -Method Post -Headers $Headers -Body $ReleaseBody -ContentType "application/json"
        
        $UploadUrl = $Release.upload_url -replace '\{.*\}', "?name=$ApkDestName"
        Write-Host "  -> Uploading $ApkDestName ($ApkSizeMb MB)..." -ForegroundColor Gray
        
        $Bytes = [System.IO.File]::ReadAllBytes($ApkDest)
        $UploadHeaders = @{
            "Authorization" = "Bearer $GithubToken"
            "Content-Type" = "application/vnd.android.package-archive"
            "User-Agent" = "DailyTrack-Publisher"
        }

        $UploadResponse = Invoke-RestMethod -Uri $UploadUrl -Method Post -Headers $UploadHeaders -Body $Bytes
        Write-Host "  -> Release published successfully!" -ForegroundColor Green
        Write-Host "  -> View online: $($Release.html_url)" -ForegroundColor Cyan
    } catch {
        Write-Host "  -> Automated upload failed: $_" -ForegroundColor Red
        Write-Host "  -> Falling back to manual browser upload..." -ForegroundColor Yellow
        $GithubToken = $null
    }
}

if (-not $GithubToken) {
    Write-Host "  -> No GITHUB_TOKEN provided (or upload fallback)." -ForegroundColor DarkYellow
    Write-Host "  -> Opening GitHub Release creation page in browser..." -ForegroundColor Cyan
    
    $EncodedNotes = [System.Uri]::EscapeDataString($Notes)
    $EncodedTitle = [System.Uri]::EscapeDataString($Title)
    $WebReleaseUrl = "https://github.com/$RepoOwner/$RepoName/releases/new?tag=$Tag&title=$EncodedTitle&body=$EncodedNotes"
    
    Start-Process $WebReleaseUrl
    
    Write-Host "`n================================================" -ForegroundColor Cyan
    Write-Host "                  NEXT STEP                     " -ForegroundColor Cyan
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "1. The browser is opened to create release $Tag."
    Write-Host "2. Drag and drop the APK binary located at:" -ForegroundColor Yellow
    Write-Host "   $ApkDest" -ForegroundColor Green
    Write-Host "3. Click 'Publish release'."
    Write-Host "4. That's it! Your app users can now open DailyTrack -> Settings -> App Updates and tap 'Update App'!" -ForegroundColor Cyan
}

Write-Host "`nDone!" -ForegroundColor Green
