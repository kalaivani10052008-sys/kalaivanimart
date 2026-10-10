# Test Database Connectivity Script for KalaivaniMart
$env:JAVA_HOME = "C:\Users\sridh\tools\jdk-17.0.12+7"
$env:Path = "C:\Users\sridh\tools\jdk-17.0.12+7\bin;C:\Users\sridh\tools\apache-maven-3.9.6\bin;$env:Path"

Write-Host "Checking KalaivaniMart Database Health & Schema..." -ForegroundColor Cyan

mvn test "-Dtest=DatabaseConfigTest,ProductDaoTest,UserDaoTest"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n>>> Database & Catalog Validation: ALL TESTS PASSED SUCCESSFULLY!" -ForegroundColor Green
} else {
    Write-Host "`n>>> Database Validation FAILED. Please review the logs above." -ForegroundColor Red
}
