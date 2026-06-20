$env:JAVA_HOME="C:\Program Files\Java\jdk-24"

Write-Host "Iniciando Eureka Server (esperando 25s)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\eureka-server; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal
Start-Sleep -Seconds 25

Write-Host "Iniciando Config Service (esperando 25s)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\config-service; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal
Start-Sleep -Seconds 25

Write-Host "Iniciando Companies Service (esperando 25s)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\companies-service; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal
Start-Sleep -Seconds 25

Write-Host "Iniciando API Gateway..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\api-gateway; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando AuthAccess Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\authaccess-service; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando Beneficiaries Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\beneficiaries-service; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando Shrinkage Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\shrinkage-service; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando Donations Logistics Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\donations-logistics-service; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "Iniciando Subscription Service..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit -Command `"cd d:\ProyectosVS\Fluxus\microservices\subscription-service; `$env:JAVA_HOME='C:\Program Files\Java\jdk-24'; .\gradlew bootRun`"" -WindowStyle Normal

Write-Host "¡Todos los servicios de backend han sido lanzados en ventanas separadas usando Java 24!" -ForegroundColor Green
