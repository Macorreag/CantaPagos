# Ficha de Google Play

Assets listos para subir:

| Archivo | Uso | Tamaño |
|---|---|---|
| `icon-512.png` | Ícono de la app | 512 × 512 |
| `feature-graphic-1024x500.png` | Gráfico destacado | 1024 × 500 |
| `src/*.html` | Fuentes para regenerarlos | — |

Falta (solo se puede hacer en un teléfono): **4 a 6 capturas reales** de la app.

## Nombre

CantaPagos

## Descripción corta (70/80)

Canta en voz alta los pagos de Nequi, Daviplata y Bancolombia. Gratis.

## Descripción completa

CantaPagos convierte tu celular Android en un altavoz de cobros.

Cuando entra un pago por Nequi, Daviplata o Bancolombia, la app lo dice en voz alta, con una campanita antes de la voz: «¡Nequi! Recibiste veinticinco mil pesos».

Así te enteras del pago sin soltar el mostrador y sin mirar la pantalla. Y si alguien te muestra un comprobante, el comprobante no canta.

QUÉ HACE

• Canta el monto de cada pago en voz alta, en español de Colombia.
• Filtra la publicidad: descarta préstamos y promociones, solo pasa lo que es un abono real.
• Lleva el cuadre del día con el total y el desglose por billetera.
• Guarda el historial del turno: hora, monto y billetera.
• Trae prueba de sonido y un simulador de pagos para dejar el volumen listo.
• Incluye una guía para que el teléfono no apague el lector con el ahorro de batería.

PRIVACIDAD

• No tiene permiso de internet: no puede conectarse a ningún servidor.
• No pide cuenta, correo ni teléfono.
• No tiene anuncios ni rastreadores.
• No pide ubicación, cámara, contactos ni almacenamiento.
• Los nombres que aparecen en las notificaciones se anonimizan antes de guardarse.

CÓMO EMPEZAR

1. Instala la app y ábrela.
2. Toca «Habilitar en Ajustes» y activa el acceso a notificaciones para CantaPagos.
3. Sube el volumen de multimedia y toca «Probar sonido».

El único permiso sensible es el acceso a notificaciones, y tiene un solo uso: leer el monto de un pago y cantarlo. Puedes revocarlo cuando quieras desde los ajustes de Android.

REQUISITOS

Android 7.0 o superior.

CantaPagos es gratis, sin anuncios y sin suscripción. Es un proyecto independiente y no está afiliado a Nequi, Daviplata ni Bancolombia.

## Formularios de la consola

| Sección | Respuesta |
|---|---|
| Categoría | Empresa (Business) |
| Anuncios | No contiene anuncios |
| Acceso a la app | Todo el contenido es accesible sin iniciar sesión |
| Público objetivo | 18 años o más |
| Data safety | No se recogen ni comparten datos |
| Política de privacidad | https://macorreag.github.io/CantaPagos/privacidad.html |
| Sitio web | https://macorreag.github.io/CantaPagos/ |
| Correo de contacto | macorreag@unal.edu.co |

## Notas para el revisor

CantaPagos usa `NotificationListenerService` porque leer la notificación del banco es su única función: extraer el monto y anunciarlo. No accede a internet (el manifiesto no declara `INTERNET`), no envía datos y no abre otras aplicaciones.

Para probar sin una cuenta bancaria real:

1. Instala y abre la app.
2. En «Ajustes de Android → Acceso a notificaciones», habilita CantaPagos.
3. Toca «Simular pago»: se puede elegir billetera y monto, y la app canta el pago como si fuera real.
4. «Probar sonido» reproduce la campanita y la voz.
