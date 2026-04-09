# Windows Task Scheduler에 데일리 브리핑 등록
# 실행: PowerShell을 관리자 권한으로 열고 아래 명령 실행
#   powershell -ExecutionPolicy Bypass -File "scripts\register-task.ps1"

$TaskName = "Claude Daily Briefing"
$ScriptPath = (Resolve-Path "$PSScriptRoot\daily-briefing.py").Path
$py3 = Get-Command python3 -ErrorAction SilentlyContinue
$py  = Get-Command python  -ErrorAction SilentlyContinue
if ($py3) { $Python = $py3.Source }
elseif ($py) { $Python = $py.Source }
else {
    Write-Error "Python을 찾을 수 없습니다. Python 3를 설치해 주세요."
    exit 1
}

Write-Host "Python: $Python"
Write-Host "스크립트: $ScriptPath"

# 기존 태스크 제거 (있을 경우)
Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue

# 태스크 설정
$Action = New-ScheduledTaskAction `
    -Execute $Python `
    -Argument "`"$ScriptPath`""

$Trigger = New-ScheduledTaskTrigger `
    -Daily `
    -At "08:03AM"

$Settings = New-ScheduledTaskSettingsSet `
    -ExecutionTimeLimit (New-TimeSpan -Minutes 5) `
    -StartWhenAvailable `
    -RunOnlyIfNetworkAvailable

# 현재 사용자 계정으로 등록 (로그인 여부 무관하게 실행)
Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $Action `
    -Trigger $Trigger `
    -Settings $Settings `
    -Force | Out-Null

Write-Host "등록 완료: '$TaskName' (매일 08:03)"
Write-Host "확인: Get-ScheduledTask -TaskName '$TaskName'"
Write-Host "수동 실행: Start-ScheduledTask -TaskName '$TaskName'"
