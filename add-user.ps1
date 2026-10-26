# Add (or reset) a login user for share mode.   .\add-user.ps1 kim      Remove:  .\add-user.ps1 kim -Remove
# The password is typed at the prompt and stored only as a salted PBKDF2 hash in %USERPROFILE%\.ebs-planner\users.txt
param(
    [Parameter(Mandatory = $true)][string]$Id,
    [switch]$Remove,
    [string]$Java = (Join-Path $env:USERPROFILE 'tools\jdk-17\bin\java.exe')
)
$ErrorActionPreference = 'Stop'
$jar = Join-Path $PSScriptRoot 'target\ebs-planner-0.1.0.jar'
if (-not (Test-Path $jar)) { throw "Jar not found: $jar  (build with: mvn -DskipTests package)" }
if (-not (Test-Path $Java)) { $Java = 'java' }
& $Java -Dfile.encoding=UTF-8 -jar $jar $(if ($Remove) { 'remove-user' } else { 'add-user' }) $Id
# users file: this Windows user only
$dir = Join-Path $env:USERPROFILE '.ebs-planner'
if (Test-Path $dir) { icacls $dir /inheritance:r /grant:r "${env:USERNAME}:(OI)(CI)F" | Out-Null }
