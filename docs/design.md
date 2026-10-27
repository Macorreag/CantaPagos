# CantaPagos: Altavoz de Cobros Offline

Convierte cualquier teléfono Android en un altavoz inteligente de pagos ("soundbox") para tenderos y comerciantes colombianos. **CantaPagos** detecta, filtra y "canta" en voz alta y en tiempo real las transferencias entrantes de billeteras móviles (Nequi, Daviplata, Bancolombia A la mano) y mensajes de texto SMS bancarios, con sonido de caja registradora ("clin-ck") y voz natural colombiana, protegiendo al comerciante contra estafas de capturas de pantalla falsas y funcionando 100% offline y sin internet.

### Decisiones Críticas y Nombre Seleccionado

> [!IMPORTANT]
> **Nombre del Producto: CantaPagos**
> - Expresión 100% colombiana, popular y cotidiana entre comerciantes ("la app que le canta los pagos").
> - Corto, memorable, fácil de pronunciar y encontrar en el teléfono (`app_name`: **CantaPagos**).
> - Libre de restricciones de marca registrada o patentes de hardware comerciales ("Soundbox").

- **Política Estricta 100% Offline**: El archivo `AndroidManifest.xml` no incluye `android.permission.INTERNET`. Todos los datos, filtros de regex, base de datos Room y síntesis de voz se procesan exclusivamente en el dispositivo del tendero.
- **Voz y Tono de Alerta**: Campanita clásica de caja registradora ("clin-ck") que suena inmediatamente antes de cantar la billetera y el monto (ej. *"¡Nequi! Recibiste quince mil pesos"*).
- **Canal de Audio Forzado**: Envío a través de `AudioAttributes.USAGE_ALARM` para que el volumen sea audible en el mostrador incluso si el celular del negocio está en modo vibración o silencio.
- **Fuentes Monitoreadas**: Notificaciones push de Nequi (`com.nequi.MobileApp`), Daviplata (`com.davivienda.daviplataapp`), Bancolombia (`com.todo1.mobile`), y notificaciones de SMS bancarios con palabras de abono.

---

### 1. Visión General y Concepto Central

- **Propósito**: Darle a los tenderos de barrio, panaderías, ferreterías, puestos de comida y pequeños negocios una herramienta confiable que reemplace las costosas cajas de sonido de suscripción mensual con su propio celular Android.
- **Problema que Resuelve**: El fraude por "pantallazos falsos" o aplicaciones clonadas de Nequi/Daviplata que muestran comprobantes editados en la pantalla del cliente mientras el tendero está ocupado atendiendo.
- **Valor Agregado**: Verificación auditiva inmediata sin tener que soltar lo que está haciendo, tocar la pantalla o desbloquear el celular.

---

### 2. Experiencia de Usuario y Diseño Visual

#### Flujos de Usuario Principales
1. **Activación y Permiso del Sistema**:
   - Al abrir CantaPagos, un indicador visual muestra si el servicio de escucha está activo o si requiere activar el *Acceso a Notificaciones* en Android.
   - Botón directo de 1-toque que abre la pantalla de ajustes de Android (`ACTION_NOTIFICATION_LISTENER_SETTINGS`).
   - Guía visual rápida para desactivar el ahorro de batería agresivo (MIUI/HyperOS, Samsung OneUI, Motorola).
2. **Mostrador en Espera Activa (Turno de Cobros)**:
   - Tarjeta principal de alto contraste con el **Total Recaudado Hoy ($ COP)** y el número de transferencias recibidas.
   - Interruptor principal para pausar o activar la lectura en cualquier momento.
   - Botón destacado de **"Probar Sonido ($10.000 COP)"** para verificar que el altavoz y el volumen estén listos en el mostrador.
3. **Historial de Cobros Cantados**:
   - Lista en tiempo real de pagos registrados con insignias de color distintivas de cada billetera:
     - Nequi (Morado característico `#7A1CAC`)
     - Daviplata (Rojo rubí `#DC2626`)
     - Bancolombia / A la mano (Amarillo `#EAB308`)
     - SMS Bancario (Azul `#0284C7`)
   - Hora exacta y valor formateado en pesos colombianos.
4. **Cierre de Caja Diario ("Cuadre del Día")**:
   - Botón "Cerrar Caja / Nuevo Turno" que calcula el total recaudado por billetera y reinicia el contador para el siguiente turno o día de trabajo.

#### Identidad Visual y Estilo
- **Concepto**: Herramienta de trabajo robusta, limpia y de máxima legibilidad en ambientes de tienda (iluminación alta, mostrador a distancia).
- **Paleta de Colores**:
  - `Fondo / Superficie`: Negro obsidiana suave `#0F172A` y tarjetas en gris pizarra `#1E293B`.
  - `Éxito / Aprobado`: Verde esmeralda `#10B981`.
  - `Nequi`: Morado `#7A1CAC`.
  - `Daviplata`: Rojo `#DC2626`.
  - `Bancolombia`: Dorado `#F59E0B`.
  - `Texto`: Blanco brillante `#F8FAFC` y texto secundario `#94A3B8`.
- **Tipografía y Escala**:
  - Cifras gigantes de 36sp en negrita para el total del día.
  - Formato legible de moneda colombiana (ej. `$ 25.000`).

---

### 3. Decisiones Técnicas y Reglas de Negocio

- **Motor de Expresiones Regulares (Regex)**:
  - *Filtro Negativo*: Descarta palabras publicitarias como *"préstamo"*, *"tarjeta"*, *"preaprobado"*, *"aprovecha"*, *"descarga"*, *"crédito"*.
  - *Filtro Positivo*: Busca verbos de abono confirmados (*"recibiste"*, *"te envió"*, *"te transfirió"*, *"te llegó"*, *"abono"*, *"transferencia exitosa"*, *"pago recibido"*) seguidos por la captura del monto numérico.
  - *Limpieza Fonética para TTS*: Se retiran decimales `.00`, comas y signos de peso para que el sintetizador pronuncie cantidades fluidas en español de Colombia (ej. *"veinticinco mil pesos"* en lugar de *"signo pesos veinticinco punto cero cero"*).
- **Sonido de Campanita ("Clin-ck")**:
  - Síntesis de sonido de campana de caja registradora generado limpiamente en memoria y reproducido a través de `SoundPool`, con un retardo de 300ms antes de comenzar la locución TTS.
- **Base de Datos Local (Room)**:
  - Entidad `PaymentTransaction`: `id`, `wallet` (Nequi, Daviplata, Bancolombia, SMS), `amount` (Long en COP), `timestamp` (Long), `rawMessage` (String).
  - Consultas reactivas con `Flow<List<PaymentTransaction>>` para actualizar la interfaz al instante cuando cae un pago.

---

### 4. Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       SISTEMA DE NOTIFICACIONES ANDROID                     │
│    Nequi (com.nequi.MobileApp) | Daviplata (com.davivienda.daviplataapp)    │
│    Bancolombia (com.todo1.mobile) | Notificaciones de SMS Bancarios         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Notificación entrante
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 SoundboxListenerService (100% Offline)                      │
│                                                                             │
│  ┌───────────────────────────┐      ┌────────────────────────────────────┐  │
│  │ WalletNotificationParser  │      │ AudioNotificationEngine            │  │
│  │ • Filtro antipublicidad   │ ───► │ • Campanita SoundPool (Clin-ck!)   │  │
│  │ • Detección de abonos     │      │ • TextToSpeech en es-CO            │  │
│  │ • Extractor de montos COP │      │ • AudioAttributes.USAGE_ALARM      │  │
│  └─────────────┬─────────────┘      └────────────────────────────────────┘  │
│                │                                                            │
│                │ Transacción válida                                         │
│                ▼                                                            │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ PaymentRepository -> Base de Datos Room (AppDatabase)                 │  │
│  │ Guarda: id, billetera, monto en COP, fecha/hora, texto recibido       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Flow reactivo
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                  INTERFAZ DE USUARIO (JETPACK COMPOSE)                      │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Cabecera: Estado del Lector + Switch Activar / Pausar                 │  │
│  ├───────────────────────────────────────────────────────────────────────┤  │
│  │ Tarjeta Resumen Cuadre de Caja: Total Hoy ($ COP) + Conteo de Pagos   │  │
│  │ Chips con desglose por billetera (Nequi, Daviplata, Bancolombia, SMS) │  │
│  ├───────────────────────────────────────────────────────────────────────┤  │
│  │ Botones de Acción: [ Probar Sonido ]  [ Cerrar Caja / Reiniciar Turno ]│  │
│  ├───────────────────────────────────────────────────────────────────────┤  │
│  │ Alerta de Permisos de Notificación (si no está concedido en Android)  │  │
│  ├───────────────────────────────────────────────────────────────────────┤  │
│  │ Historial de Pagos Cantados del Día (LazyColumn en vivo)              │  │
│  ├───────────────────────────────────────────────────────────────────────┤  │
│  │ Diálogo informativo: "Cómo evitar que el sistema cierre la app"      │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```
