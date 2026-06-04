# Authaccess Service

Standalone Spring Boot auth/access microservice extracted from the Fluxus monolith.

## Runtime Stack

- Java 24
- Spring Boot 4.0.6
- Spring Cloud 2025.1.1 (Oakwood, compatible with Spring Boot 4.0.x)
- Gradle wrapper configured for Gradle 8.14.3
- MySQL by default

## Services

Start services in this order:

1. `config-service` on port `8081`
2. `authaccess-service` on port `8094`

From `C:\Users\sackx\Downloads\microservices\config-service`:

```powershell
.\gradlew.bat bootRun
```

From `C:\Users\sackx\Downloads\microservices\authaccess-service`:

```powershell
.\gradlew.bat bootRun
```

## Configuration

`authaccess-service` uses Spring Cloud Config. Local values live in:

```text
C:\Users\sackx\Downloads\microservices\config-data\authaccess-service.yml
```

The default datasource is:

```text
jdbc:mysql://localhost:3306/fluxus_authaccess?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
username: root
password: password
```

## External References

Authaccess owns only its user, role, permission, and auth tables. It validates external ownership references through Feign clients:

- Retail company service: `services.retail-company.base-url`, default `http://localhost:8101`
- Beneficiary service: `services.beneficiary.base-url`, default `http://localhost:8102`

These URLs are placeholders until the retail-company and beneficiary-institution services are extracted.

## Main Endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/profile`
- `PUT /api/auth/profile`
- `PUT /api/auth/change-password`
- `GET /api/auth/users`
- `GET /api/auth/users/{userId}`
- `PUT /api/auth/users/{userId}/role`
- `GET /api/auth/roles`
- `POST /api/auth/roles`
- `GET /api/auth/retail-users`
- `POST /api/auth/retail-users`

Swagger UI is available at `http://localhost:8094/swagger-ui.html` after startup.

## Verification

Run builds after Java 24 is available on PATH or `JAVA_HOME` is set:

```powershell
cd C:\Users\sackx\Downloads\microservices\config-service
.\gradlew.bat test
cd C:\Users\sackx\Downloads\microservices\authaccess-service
.\gradlew.bat test
```
