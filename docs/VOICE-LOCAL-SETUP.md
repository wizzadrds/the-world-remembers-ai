# Voz local de The World Remembers

Este documento explica cómo activar las voces locales de los aldeanos sin depender de un servicio de voz remoto.

## 1. Qué hace el sistema

El cliente del mod puede:

- detectar micrófonos y salidas de audio Java disponibles;
- seleccionar el micrófono y los auriculares/altavoces desde **Voice & AI**;
- probar el micrófono y reproducir un tono de prueba;
- usar un proceso local para STT (voz -> texto);
- usar un proceso local para TTS (texto -> voz);
- asignar voces distintas de forma estable a los aldeanos;
- variar ritmo, tono y expresividad según el aldeano/profesión;
- mantener las voces opcionales: si el TTS local falla, el juego continúa.

La configuración se guarda en:

`config/the_world_remembers_voice.json`

No hace falta editar este archivo para la configuración normal: la pantalla **Voice & AI** es la vía recomendada.

## 2. Requisitos

- Minecraft/Fabric con el mod instalado.
- Java compatible con la versión del mod.
- Para TTS local: un ejecutable de TTS que acepte los argumentos configurados.
- Para STT local: un ejecutable que reciba un archivo PCM mono de 16 kHz y devuelva la transcripción por stdout.
- El proceso de TTS debe generar un WAV en la ruta indicada por `{output}`.

## 3. Instalar Piper para voces locales

Se recomienda Piper como backend TTS local. El proyecto activo es **OHF-Voice/piper1-gpl**; el repositorio antiguo de rhasspy/piper está archivado.

Descarga Piper y al menos una voz compatible con tu idioma. Para español, usa una voz española disponible en tu distribución de Piper.

Primero prueba Piper fuera de Minecraft. El objetivo es conseguir un comando que pueda generar un WAV correctamente.

Ejemplo conceptual:

```text
piper ... texto ... salida.wav
```

La integración del mod permite plantillas, por ejemplo:

```text
mi-comando {text} {output} {language} {voice} {rate} {pitch} {expressiveness}
```

Si tu wrapper de Piper necesita otro orden de argumentos, conserva ese orden en **TTS command**.

### Importante sobre `{voice}`

Puedes introducir varias voces separadas por comas en **TTS voice**:

```text
voz_1, voz_2, voz_3
```

El mod elige una de forma estable por UUID del aldeano. Así, el mismo aldeano mantiene su voz entre sesiones.

## 4. Instalar faster-whisper para STT

Para reconocimiento de voz local, una opción recomendada es faster-whisper.

Puedes instalarlo en un entorno Python separado para no mezclar dependencias con Minecraft:

```text
python -m venv .venv
```

Activa el entorno y prueba tu instalación de faster-whisper antes de conectarla al mod.

Necesitas un pequeño comando/wrapper que reciba el archivo PCM suministrado por el mod y escriba solamente la transcripción en stdout.

La plantilla puede usar:

```text
{pcm}
```

Ejemplo conceptual:

```text
python stt_wrapper.py {pcm}
```

Si no aparece `{pcm}` en el comando, el mod añade automáticamente el archivo PCM como último argumento.

## 5. Configurar los dispositivos de audio

Abre:

**Voice & AI -> Audio**

El mod vuelve a enumerar los dispositivos de Java y muestra:

- **Mic**: micrófonos disponibles;
- **Output**: auriculares/altavoces disponibles;
- **Test microphone**: comprueba captura y muestra el nivel;
- **Test output**: reproduce un tono por la salida seleccionada;
- **Rescan devices**: vuelve a detectar dispositivos conectados después de cambiar un USB/Bluetooth.

Si un dispositivo seleccionado deja de existir, el mod no cambia silenciosamente a otro dispositivo. Mostrará el fallo para que puedas volver a seleccionar uno válido.

Para Bluetooth, conecta primero los auriculares y después usa **Rescan devices**.

## 6. Configurar las voces de los aldeanos

En **NPC voices**:

1. activa **Villager voices**;
2. selecciona un **Temperament**;
3. escribe las instrucciones de voz;
4. configura **TTS model** y **TTS voice**;
5. pulsa **Test villager voice**.

El texto recomendado para conservar el estilo de aldeano es:

```text
Speak like a Minecraft villager: warm, conversational, slightly rustic,
short phrases, natural pauses. Avoid announcer, robotic or radio-presenter delivery.
```

El sistema también aplica pequeñas variaciones estables de tono y velocidad por aldeano y temperamento por profesión. Por ejemplo:

- granjero/pescador: más alegre;
- bibliotecario/cleric: más calmado;
- butcher/weaponsmith/toolsmith: más firme;
- nitwit: más tímido.

No se intenta imitar una voz concreta de un actor: se busca una voz humana, breve y claramente propia del mundo de Minecraft.

## 7. Configuración de comandos

En **AI / TTS** puedes configurar:

- **STT command**
- **TTS command**
- **STT model**
- **TTS model**
- **TTS voice**
- idioma.

Los comandos se dividen respetando comillas, por lo que las rutas con espacios deben escribirse entre comillas.

Para STT, `{pcm}` se sustituye por el archivo temporal de audio.

Para TTS están disponibles:

- `{text}`
- `{output}`
- `{language}`
- `{model}`
- `{voice}`
- `{rate}`
- `{pitch}`
- `{expressiveness}`
- `{temperament}`
- `{instructions}`

Si el comando TTS no contiene ninguna plantilla, el mod añade los parámetros de texto, salida, idioma, modelo, ritmo, tono y expresividad al final.

## 8. Solución de problemas

### No aparece mi micrófono

1. Comprueba que el sistema operativo lo reconoce.
2. Conecta el dispositivo antes de abrir Minecraft.
3. Pulsa **Rescan devices**.
4. Comprueba permisos de micrófono del sistema.
5. Selecciónalo explícitamente en **Mic**.

### No se oye el tono

1. Comprueba el volumen del sistema.
2. Pulsa **Rescan devices**.
3. Selecciona los auriculares/altavoces correctos.
4. Pulsa **Test output**.
5. Si el dispositivo desapareció, vuelve a seleccionarlo.

### TTS dice que no produce audio

Comprueba que el ejecutable realmente crea el archivo solicitado por `{output}` y que termina con código 0.

El mod elimina archivos WAV antiguos antes de una síntesis y elimina archivos parciales si el proceso falla, para que una síntesis antigua nunca se confunda con una nueva.

### STT se queda colgado

El proceso local tiene un tiempo máximo. Si supera el límite, se termina y la conversación actual se cancela. También existe un límite de tamaño para el audio enviado al proceso local.

### El archivo de configuración está corrupto

El mod no lo sobreescribe silenciosamente. Lo mueve a:

```text
the_world_remembers_voice.json.broken
the_world_remembers_voice.json.broken.2
...
```

y genera valores por defecto.

## 9. Rendimiento

Las voces locales se ejecutan fuera del hilo principal del juego. Además:

- la captura tiene un límite de memoria;
- STT/TTS tienen timeout;
- la salida de diagnóstico de los procesos está limitada;
- las tareas de conversación antiguas se cancelan al empezar una nueva;
- las voces de aldeanos tienen una cola limitada;
- el reproductor de voz limita frames pendientes;
- la simulación de aldeanos utiliza presupuestos por tick y rotación.

Esto evita que un TTS/STT defectuoso bloquee el servidor o que una cola de voces crezca sin límite.

## 10. Seguridad y privacidad

Con STT/TTS local, el audio y el texto de voz pueden permanecer en tu equipo. El mod no necesita enviar la captura a un proveedor remoto para usar los backends locales.

Si configuras un proveedor remoto para la IA, sus propias condiciones de privacidad son aplicables a las solicitudes que se envíen.

## 11. Configuración recomendada para empezar

```text
Microphone: Default o tu micrófono USB
Output: tus auriculares
Villager voices: ON
Push-to-talk: ON
Language: es-ES
STT model: faster-whisper
TTS model: piper
Voice: una voz española de Piper
Voice distance: 32
```

Primero prueba **Test output**, después **Test microphone**, después **Test villager voice** y finalmente prueba una conversación dentro del mundo.
