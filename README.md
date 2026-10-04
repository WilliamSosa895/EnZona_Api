# EnZona API

API REST de **EnZona**, plataforma multiplataforma (web y Android) para descubrir y gestionar eventos locales con boletos electrónicos firmados y validados por código QR. Área de estudio: Orizaba, Veracruz.

La API es el único lugar donde viven las reglas de negocio (Anexo E, RNF-10): la aplicación web (React + Vite) y la app Android (Kotlin + Jetpack Compose) consumen los mismos endpoints y la misma base de datos.

## Propósito

- Mostrar eventos cercanos según la ubicación del usuario (RF-19) o por ciudad, con búsqueda y filtros.
- Permitir a los organizadores publicar y gestionar eventos gratuitos o de pago, con asientos numerados cuando aplica.
- Vender o confirmar boletos sin sobreventa, emitir boletos firmados de un solo uso y enviarlos por correo.
- Validar el acceso en la puerta, también sin conexión, y mostrar al organizador sus ventas y accesos.

## Tecnologías

| Pieza | Tecnología |
|---|---|
| Lenguaje y framework | Java 21, Spring Boot 3.3 |
| Persistencia | PostgreSQL 15 (Aiven), Spring Data JPA, Flyway |
| Seguridad | Spring Security, JWT HS256, BCrypt, AES-256-GCM (CURP), HMAC-SHA256 (boletos) |
| Documentación | springdoc-openapi (Swagger UI) |
| Correo y QR | Spring Mail, ZXing |
| Pruebas | JUnit 5, Mockito, AssertJ, Testcontainers, JaCoCo |

## Requisitos

- JDK 21
- Maven 3.9 o superior
- PostgreSQL 15 (local, en Docker o en Aiven)
- Docker, solo para las pruebas de integración y para `docker compose`

## Instalación

```bash
git clone https://github.com/WilliamSosa895/EnZona_Api.git
cd EnZona_Api
cp .env.example .env
```

Llena `.env`. Para cada clave secreta genera un valor aleatorio:

```bash
openssl rand -base64 32
```

Las variables se explican en [docs/VARIABLES_DE_ENTORNO.md](docs/VARIABLES_DE_ENTORNO.md).

## Ejecución

### Opción A: Docker Compose (API + PostgreSQL local)

```bash
docker compose --env-file .env up --build
```

### Opción B: Maven contra una base existente (por ejemplo, Aiven)

En `.env`, usa la URL JDBC de Aiven con SSL:

```
DB_URL=jdbc:postgresql://<host>:<puerto>/defaultdb?sslmode=require
```

Carga las variables y ejecuta:

```bash
set -a; source .env; set +a          # Linux / macOS / Git Bash
mvn spring-boot:run
```

En Windows (PowerShell) define las variables con `$env:NOMBRE="valor"` antes de `mvn spring-boot:run`.

Flyway crea o actualiza el esquema al arrancar. Si la base ya se había creado con `esquema_enzona_v2.sql`, Flyway la toma como versión 1 y solo aplica las migraciones V2 a V5.

### Verificar

| Recurso | URL |
|---|---|
| Salud | http://localhost:8080/actuator/health |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Especificación OpenAPI | http://localhost:8080/v3/api-docs (también en [docs/openapi.yaml](docs/openapi.yaml)) |

Con `NOTIFICATIONS_MODE=log`, el código de verificación del registro aparece en la consola. Con `PAYMENT_PROVIDER=sandbox`, los pagos se simulan con los tokens `tok_aprobado`, `tok_rechazado` y `tok_pendiente`.

## Pruebas

```bash
mvn test      # pruebas unitarias (no requieren base de datos)
mvn verify    # unitarias + integración con Testcontainers (requiere Docker) + reporte de cobertura
```

El reporte de cobertura queda en `target/site/jacoco/index.html`. GitHub Actions ejecuta `mvn verify` en cada push y pull request.

Las pruebas de integración cubren, entre otros, CP-01, CP-02, CP-04, CP-05, CP-10, CP-11 (aforo con compras concurrentes), CP-12, CP-13, CP-21, CP-40, CP-MULTI-05 y CP-MULTI-07.

## Arquitectura

Monolito modular por capas (Anexo E, DA-01). Cada paquete es un módulo de dominio:

| Módulo | Responsabilidad | Requisitos |
|---|---|---|
| `identidad` | Registro con CURP, verificación por código, sesión JWT, perfil y roles | RF-01 a RF-04 |
| `eventos` | Catálogo, búsqueda, filtros, cercanía, gestión, asientos, validadores | RF-05 a RF-09, RF-13, RF-19 |
| `boletaje` | Órdenes, reserva todo o nada, pagos, emisión y firma de boletos, reembolsos | RF-10 a RF-12, RF-16 |
| `validacion` | Validación del QR, manifiesto y sincronización sin conexión | RF-15, RNF-08 |
| `cancelacion` | Política y cancelación de eventos | RF-14 |
| `reportes` | Panel del organizador | RF-17 |
| `administracion` | Gestión de usuarios y supervisión de eventos | RF-18 |
| `notificaciones` | Correos con QR y avisos | RF-12, RF-14 |
| `comun` | Seguridad, errores, configuración y utilidades | Transversal |

Decisiones clave:

- **Sin sobreventa (RNF-05).** El inventario se descuenta con una actualización condicional en la base de datos (`... WHERE cantidad_disponible >= N`), no leyendo y escribiendo desde Java.
- **Boletos de un solo uso (RNF-02).** El paso VALIDO → USADO también es una actualización condicional: solo el primer escaneo lo consigue.
- **Cobro sin bloqueos.** La pasarela de pago se llama fuera de cualquier transacción.
- **Pasarela intercambiable.** La pasarela está detrás de la interfaz `PasarelaPago`; para integrar un proveedor real basta con una nueva implementación.

## Documentación

| Documento | Contenido |
|---|---|
| [docs/ENDPOINTS.md](docs/ENDPOINTS.md) | Método, URL, permisos, parámetros, cuerpo, respuesta y códigos de cada endpoint |
| [docs/EJEMPLOS.md](docs/EJEMPLOS.md) | Solicitudes y respuestas JSON de los flujos principales |
| [docs/AUTENTICACION.md](docs/AUTENTICACION.md) | Cómo obtener y enviar el token, roles y permisos |
| [docs/VARIABLES_DE_ENTORNO.md](docs/VARIABLES_DE_ENTORNO.md) | Variables que necesita la API |
| [docs/MODELO_DE_DATOS.md](docs/MODELO_DE_DATOS.md) | Entidades, relaciones y migraciones |
| [docs/CODIGOS_DE_ERROR.md](docs/CODIGOS_DE_ERROR.md) | Formato de error y significado de cada código |
| [docs/openapi.yaml](docs/openapi.yaml) | Especificación OpenAPI 3 |
| [docs/COHERENCIA_APP_MOVIL.md](docs/COHERENCIA_APP_MOVIL.md) | Alineación con la app Android y pendientes |
| [CHANGELOG.md](CHANGELOG.md) | Historial de versiones |

## Flujo de trabajo

- **Ramas (git flow):** `main` contiene versiones liberadas y etiquetadas (`vX.Y.Z`). `develop` es la integración. El trabajo se hace en `feature/*`, `bugfix/*` y `docs/*`, y se publica con `release/*`. Las correcciones urgentes van en `hotfix/*`.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/) (`feat`, `fix`, `docs`, `test`, `build`, `ci`, `chore`, `refactor`), con alcance por módulo, por ejemplo `feat(boletaje): ...`.
- **Versiones:** [SemVer](https://semver.org/lang/es/).

## Equipo

Proyecto de las experiencias educativas Taller de Pruebas de Software y Desarrollo de Empresas Tecnológicas, Facultad de Negocios y Tecnologías, Universidad Veracruzana.
