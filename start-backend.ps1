$env:JAVA_HOME="C:\Program Files\Java\jdk-24"

Write-Host "Iniciando Eureka Server (esperando 25s)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\eureka-server`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal
Start-Sleep -Seconds 25

Write-Host "Iniciando Config Service (esperando 25s)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\config-service`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal
Start-Sleep -Seconds 25

Write-Host "Iniciando Companies Service (esperando 25s)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\companies-service`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal
Start-Sleep -Seconds 25

Write-Host "Iniciando API Gateway..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\api-gateway`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando AuthAccess Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\authaccess-service`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando Beneficiaries Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\beneficiaries-service`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando Shrinkage Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\shrinkage-service`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando Donations Logistics Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\donations-logistics-service`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando Subscription Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd `"$PSScriptRoot\subscription-service`"; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "¡Todos los servicios de backend han sido lanzados en ventanas separadas usando Java 24!" -ForegroundColor Green
