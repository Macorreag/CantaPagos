# CantaPagos

**Tu celular canta los pagos. Tú sigues atendiendo.**

CantaPagos convierte un Android en un altavoz de cobros: lee las notificaciones de
Nequi, Daviplata y Bancolombia, y anuncia el monto en voz alta con una campanita de
caja registradora. Funciona **100% offline**.

[![APK](https://github.com/Macorreag/CantaPagos/actions/workflows/apk.yml/badge.svg)](https://github.com/Macorreag/CantaPagos/actions/workflows/apk.yml)

### [⬇ Descargar el APK](https://github.com/Macorreag/CantaPagos/releases/latest/download/CantaPagos.apk)

Sitio del proyecto: <https://macorreag.github.io/CantaPagos/>

---

## Qué hace

- **Canta el pago**: campanita + voz en español de Colombia (*"¡Nequi! Recibiste veinticinco mil pesos"*).
- **Filtra publicidad**: descarta préstamos, promociones y "preaprobado"; solo pasa lo que es un abono real.
- **Cuadre del día**: total del turno y desglose por billetera.
- **Historial**: hora, monto y billetera de cada pago cantado. Los nombres se anonimizan.
- **Simulador y prueba de sonido** para dejar el volumen listo antes de abrir.
- **Guía de batería** para MIUI/HyperOS, One UI y Motorola.

## Privacidad

El `AndroidManifest.xml` **no declara `android.permission.INTERNET`**: el sistema
operativo le impide a la app abrir cualquier conexión. Los pagos se guardan en una
base de datos Room local y la voz se sintetiza en el dispositivo.

## Instalar

1. Descarga `CantaPagos.apk` desde [Releases](https://github.com/Macorreag/CantaPagos/releases/latest).
2. Permite instalar apps de origen desconocido (Android lo pregunta una vez).
3. Abre la app → **Habilitar en Ajustes** → activa *Acceso a notificaciones* para CantaPagos.
4. Pulsa **Probar sonido ($10.000)** y sube el volumen de multimedia.

Requiere Android 7.0 (API 24) o superior.

## Stack

Kotlin · Jetpack Compose (Material 3) · Room · KSP · TextToSpeech · SoundPool ·
Kotlin Coroutines · Moshi · AGP 9.1.1 · Gradle 9.3.1 · compileSdk 36.

Decisiones de producto y arquitectura: [`docs/design.md`](docs/design.md).

## Desarrollo

El repositorio se generó en Google AI Studio y **no incluye el Gradle wrapper**.
Usa un Gradle instalado (9.3.1) y un JDK 21:

```bash
gradle :app:assembleDebug
```

La firma se toma del entorno, tal como la espera `app/build.gradle.kts`:

| Variable | Uso |
|---|---|
| `KEYSTORE_PATH` | Ruta al `.jks` (por defecto `my-upload-key.jks` en la raíz) |
| `STORE_PASSWORD` | Contraseña del keystore |
| `KEY_PASSWORD` | Contraseña de la llave (alias `upload`) |

## Automatización

| Workflow | Qué hace |
|---|---|
| [`apk.yml`](.github/workflows/apk.yml) | Compila y firma el APK en cada push a `main` y lo publica en Releases (`v1.0`). También compila al empujar un tag `v*`. |
| [`pages.yml`](.github/workflows/pages.yml) | Publica `docs/` (la landing) en GitHub Pages. |

Secrets necesarios: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`.

La landing vive en `docs/` y no toca el build de Android.
