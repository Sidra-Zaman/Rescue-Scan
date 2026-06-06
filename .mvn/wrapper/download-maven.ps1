param(
    [string]$MavenVersion,
    [string]$MavenHome
)

$ErrorActionPreference = "Stop"

$downloadUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVersion/apache-maven-$MavenVersion-bin.zip"
$zipFile = "$env:TEMP\apache-maven-$MavenVersion-bin.zip"

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

try {
    Write-Host "Downloading Maven $MavenVersion..."
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFile

    if (-not (Test-Path $MavenHome)) {
        New-Item -ItemType Directory -Force -Path $MavenHome | Out-Null
    }

    Write-Host "Extracting Maven..."
    Expand-Archive -Path $zipFile -DestinationPath $MavenHome -Force

    Remove-Item -Force $zipFile

    $mvnCmd = "$MavenHome\apache-maven-$MavenVersion\bin\mvn.cmd"
    if (-not (Test-Path $mvnCmd)) {
        Write-Host "ERROR: Maven binary not found after extraction."
        exit 1
    }

    Write-Host "Maven $MavenVersion installed successfully."
    exit 0
}
catch {
    Write-Host "ERROR: Failed to download or extract Maven."
    Write-Host $_.Exception.Message
    exit 1
}
