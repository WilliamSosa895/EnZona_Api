# Despliegue en Render (plan gratuito)

La API corre en Render como *Web Service* con Docker, a partir del `Dockerfile` del repositorio.
La base de datos sigue en Aiven; Render solo ejecuta la API.

## Crear el servicio

1. **New → Web Service → Public Git Repository**: `https://github.com/WilliamSosa895/EnZona_Api`,
   rama `develop` (o la rama que se quiera probar).
2. **Language:** Docker · **Region:** Ohio (la más cercana a México de las gratuitas) ·
   **Instance:** Free (512 MB, 0.1 CPU).
3. **Health Check Path:** `/actuator/health`.
4. **Environment Variables → Add from .env:** pegar el `.env` de producción con estos ajustes:

   | Variable | Valor en Render | Por qué |
   |---|---|---|
   | `PORT` | *(no definir)* | Render la fija sola; la API lee `${PORT:8080}` |
   | `IMAGE_LOCAL_DIR` | *(no definir)* | sin disco persistente; usar `IMAGE_PROVIDER=cloudinary` |
   | `MAIL_PORT` | `2525` | el plan gratuito bloquea la salida por 25, 465 y 587; Brevo también escucha en 2525 |
   | `JAVA_TOOL_OPTIONS` | `-Xmx288m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1` | la JVM debe caber en 512 MB |
   | `DB_POOL_MAX` | `4` | una instancia pequeña no necesita más conexiones |
   | `PUBLIC_URL` | `https://<servicio>.onrender.com` | activa el latido que evita que la instancia se duerma |

5. **Deploy web service.** El primer arranque tarda unos 3 minutos (Maven + Spring en 0.1 CPU).

## Mantenerla despierta

El plan gratuito apaga la instancia tras 15 minutos sin tráfico y tarda 2 a 3 minutos en volver.
Dos medidas se complementan:

- **Latido interno** (`PUBLIC_URL`): cada 10 minutos la API se pide a sí misma `/actuator/health`
  por su dirección pública; mientras esté arriba no se duerme.
- **Despertador externo:** algo fuera de Render debe hacer la primera petición después de un
  reinicio o de un despliegue. El repositorio de la app tiene un flujo programado de GitHub
  Actions (`mantener-api-despierta.yml`); los cron de GitHub se retrasan, así que un monitor
  gratuito tipo UptimeRobot o cron-job.org cada 5 minutos es más fiable.

La app móvil, por su parte, espera hasta 4 minutos a que el servidor despierte antes de dar por
perdida la conexión, mostrando que el servidor está arrancando.

## Actualizar

Render no despliega solo desde un repositorio público: tras fusionar a `develop`, **Manual Deploy →
Deploy latest commit** en el panel. Las migraciones de Flyway corren al arrancar.

## Límites conocidos

- 512 MB y 0.1 CPU: arranque lento y una sola instancia.
- 750 horas de instancia al mes por cuenta: suficiente para una API siempre despierta.
- Sin disco persistente: las portadas deben ir a Cloudinary.
