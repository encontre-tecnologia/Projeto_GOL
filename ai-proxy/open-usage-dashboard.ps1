$ErrorActionPreference = "Stop"

$dashboardPath = Join-Path $PSScriptRoot "usage-dashboard.html"

if (-not (Test-Path -LiteralPath $dashboardPath)) {
    throw "Nao encontrei usage-dashboard.html em: $dashboardPath"
}

Start-Process -FilePath "explorer.exe" -ArgumentList $dashboardPath
Write-Host "Painel aberto:"
Write-Host $dashboardPath
