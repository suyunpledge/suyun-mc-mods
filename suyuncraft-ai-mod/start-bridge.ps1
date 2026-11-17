param([Parameter(Mandatory=$true)][string]$ConfigPath)
$ErrorActionPreference = 'Stop'
try {
    if (-not $env:SC_AI_BASE_URL) { $env:SC_AI_BASE_URL = Read-Host 'HTTPS API base URL (including /v1 or provider API path)' }
    if (-not $env:SC_AI_MODEL) { $env:SC_AI_MODEL = Read-Host 'Model ID (prefer a small non-thinking model)' }
    if (-not $env:SC_AI_API_KEY) {
        $secret = Read-Host 'API key (hidden; retained only in this process)' -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
        try { $env:SC_AI_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    }
    $pythonExe = $env:SC_AI_PYTHON
    if (-not $pythonExe) {
        $bundled = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
        if (Test-Path -LiteralPath $bundled) { $pythonExe = $bundled } else { $pythonExe = 'python' }
    }
    & $pythonExe (Join-Path $PSScriptRoot 'bridge\bridge.py') --config $ConfigPath
} finally {
    $env:SC_AI_API_KEY = $null
    Read-Host 'Bridge stopped. Press Enter to close' | Out-Null
}
