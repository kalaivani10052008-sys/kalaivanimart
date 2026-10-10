# Build Script for KalaivaniMart
$env:JAVA_HOME = "C:\Users\sridh\tools\jdk-17.0.12+7"
$env:Path = "C:\Users\sridh\tools\jdk-17.0.12+7\bin;C:\Users\sridh\tools\apache-maven-3.9.6\bin;$env:Path"

Write-Host "Building KalaivaniMart with Maven..." -ForegroundColor Cyan
mvn clean package
if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBUILD SUCCESSFUL! Target WAR generated at target/kalaivanimart.war" -ForegroundColor Green
} else {
    Write-Host "`nBUILD FAILED. Please review compiler logs." -ForegroundColor Red
}
