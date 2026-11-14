# ====================================================================
# SmartATS: Push to New GitHub Repository
# ====================================================================

param(
    [string]$RepoUrl
)

if (-not $RepoUrl) {
    Write-Host "`nCreate a new repository on https://github.com/new first (leave README, .gitignore unchecked)." -ForegroundColor Yellow
    $RepoUrl = Read-Host "Enter your GitHub Repository URL (e.g. https://github.com/your-username/smart-ats.git)"
}

if (-not $RepoUrl) {
    Write-Host "[ERROR] Repository URL is required." -ForegroundColor Red
    exit 1
}

Write-Host "`nSetting remote origin to $RepoUrl..." -ForegroundColor Cyan
& "C:\Program Files\Git\cmd\git.exe" remote remove origin 2>$null
& "C:\Program Files\Git\cmd\git.exe" remote add origin $RepoUrl

Write-Host "Pushing main branch to GitHub..." -ForegroundColor Green
& "C:\Program Files\Git\cmd\git.exe" branch -M main
& "C:\Program Files\Git\cmd\git.exe" push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n====================================================================" -ForegroundColor Green
    Write-Host "  [SUCCESS] Project pushed to GitHub successfully!" -ForegroundColor Green
    Write-Host "  You can now share this URL with your friends: $RepoUrl" -ForegroundColor Cyan
    Write-Host "====================================================================`n" -ForegroundColor Green
} else {
    Write-Host "`n[NOTICE] Push failed or was canceled. If prompted for GitHub login, please sign in via browser." -ForegroundColor Yellow
}
