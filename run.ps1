# Run Script for KalaivaniMart
$env:JAVA_HOME = "C:\Users\sridh\tools\jdk-17.0.12+7"
$env:Path = "C:\Users\sridh\tools\jdk-17.0.12+7\bin;C:\Users\sridh\tools\apache-maven-3.9.6\bin;$env:Path"

Write-Host "Starting KalaivaniMart..." -ForegroundColor Cyan
Write-Host "Application URL: http://localhost:8080/kalaivanimart/" -ForegroundColor Yellow
Write-Host "Health Check:    http://localhost:8080/kalaivanimart/health" -ForegroundColor Yellow

# Uses exec:exec to spawn a separate JVM (avoids classloader isolation issues with exec:java)
mvn exec:exec