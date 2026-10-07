# Guía de voz local — The World Remembers

Esta guía configura el sistema de voz **sin depender de una API de voz en la nube**. El mod captura el micrófono, puede enviar audio al servidor para la conversación del jugador y puede sintetizar localmente las voces de los aldeanos.

## 1. Qué necesitas

- Minecraft 26.2 + Fabric Loader compatible con el proyecto.
- Java 25 para esta versión del mod.
- Python 3.10+ recomendado.
- Un motor STT local: **faster-whisper**.
- Un motor TTS local: **Piper**.
- Un modelo de voz TTS en español.
- Un micrófono y unos auriculares/altavoces.

Fabric mantiene las guías de desarrollo para 26.2 y el proyecto usa Fabric API 0.158.0+26.2. Consulta la documentación de Fabric si estás preparando una instalación de desarrollo: https://docs.fabricmc.net/develop/

## 2. Instalar faster-whisper

Crea un entorno virtual fuera de la carpeta de Minecraft si quieres mantener las dependencias separadas:

### Windows

```text
py -m venv .venv
.venv\Scripts\activate
python -m pip install --upgrade pip
pip install faster-whisper
```

### Linux/macOS

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install faster-whisper
```

El proyecto oficial documenta `pip install faster-whisper`, y también describe configuraciones CPU/GPU. Para CPU, el helper incluido por el mod usa INT8 por defecto para reducir memoria. https://github.com/SYSTRAN/faster-whisper

## 3. Instalar Piper

Piper es el motor TTS local que recomendamos para los aldeanos. El proyecto original de Rhasspy fue archivado y el desarrollo se trasladó a OHF-Voice; usa una distribución actual de Piper que proporcione el comando `piper`.

Una opción sencilla es instalar el paquete Python:

```bash
pip install piper-tts
```

También puedes usar un ejecutable de Piper. El repositorio histórico de Piper documenta el uso con un modelo ONNX y archivo WAV de salida: https://github.com/rhasspy/piper

Para voces disponibles y sus licencias, revisa el catálogo de voces. Las voces de español incluyen variantes `es_ES` y `es_MX` en el catálogo histórico. https://github.com/rhasspy/piper/blob/master/VOICES.md

**Importante:** revisa la licencia concreta del modelo de voz que descargues.

## 4. Modelos y variables de entorno

Para TTS, define la ruta del modelo Piper:

### Windows PowerShell

```powershell
$env:TWR_PIPER_MODEL="C:\Voces\es_ES\tu-modelo.onnx"
$env:TWR_PIPER_BIN="piper"
```

### Windows permanente

```powershell
setx TWR_PIPER_MODEL "C:\Voces\es_ES\tu-modelo.onnx"
setx TWR_PIPER_BIN "piper"
```

Cierra y vuelve a abrir el launcher después de usar `setx`.

### Linux/macOS

```bash
export TWR_PIPER_MODEL="$HOME/voces/es_ES/tu-modelo.onnx"
export TWR_PIPER_BIN="piper"
```

Para STT puedes seleccionar el modelo y dispositivo:

```bash
export TWR_STT_MODEL="small"
export TWR_STT_DEVICE="cpu"
export TWR_STT_COMPUTE="int8"
export TWR_STT_LANGUAGE="es"
```

En una GPU NVIDIA compatible puedes usar:

```bash
export TWR_STT_DEVICE="cuda"
export TWR_STT_COMPUTE="float16"
```

## 5. Configuración dentro de Minecraft

Abre **The World Remembers → Voice Settings** con la tecla `K`.

### Panel Audio

- **Mic**: lista los dispositivos de entrada que Java Sound detecta.
- **Output**: lista los auriculares/altavoces detectados.
- **Input volume**: ganancia del micrófono.
- **Output volume**: volumen de las voces.
- **Voice distance**: distancia máxima de la voz espacial.
- **Push-to-talk**: activa/desactiva el modo de pulsar `V`.
- **Villager voices**: activa/desactiva las voces de los aldeanos.
- **Rescan devices**: vuelve a detectar micrófonos y salidas si conectas un dispositivo después de abrir el juego.

No hace falta reiniciar Minecraft para cambiar el dispositivo de salida: al guardar, el reproductor cambia al dispositivo seleccionado.

## 6. Configuración AI / TTS

### STT command

Pon:

```text
python tools/voice/stt_faster_whisper.py {pcm}
```

Si ejecutas el mod desde otra carpeta, usa la ruta absoluta al script.

`{pcm}` es audio PCM **16 kHz, mono, 16-bit little-endian** generado por el mod.

### TTS command

Pon:

```text
python tools/voice/tts_piper.py {text} {output} {language} {model} {rate} {pitch} {expressiveness}
```

Los placeholders admitidos son:

- `{text}`
- `{output}`
- `{language}`
- `{model}`
- `{voice}`
- `{rate}`
- `{pitch}`
- `{expressiveness}`
- `{temperament}`

También puedes usar un comando antiguo sin placeholders: el mod mantiene el formato de argumentos posicionales para compatibilidad.

### TTS model / voice

Si dejas **TTS model = piper**, el helper usa `TWR_PIPER_MODEL`.

Si rellenas **TTS voice**, el valor se utiliza como identificador/modelo para el perfil del aldeano. Para configuraciones Piper complejas es preferible controlar el modelo mediante `TWR_PIPER_MODEL`.

## 7. Cómo hacer que los aldeanos suenen como aldeanos

El mod no usa la voz del jugador para los aldeanos.

Cada línea de conversación de aldeanos se envía como un evento de voz espacial con:

- posición del aldeano;
- distancia máxima;
- velocidad;
- tono;
- expresividad;
- prioridad;
- temperamento configurable.

El cliente sintetiza la línea **localmente**, de modo que el servidor no necesita generar audio.

En **NPC voices** puedes seleccionar:

- TIMID
- CALM
- WARM
- CHEERFUL
- ASSERTIVE
- NERVOUS
- IRRITABLE
- TIRED
- SERIOUS
- EXCITED

Además, el perfil de voz modifica la velocidad, tono y expresividad según la personalidad del aldeano. La intención es que no todos los aldeanos parezcan la misma voz robótica.

Para un resultado natural recomendamos:

- una voz española de Piper;
- velocidad cercana a 1.0;
- modelos de calidad media/alta;
- auriculares seleccionados explícitamente;
- evitar voces de narrador/locutor.

## 8. Voces y AI son sistemas independientes

No necesitas una API de OpenAI para que los aldeanos tengan voz.

El flujo local es:

```text
Simulación del aldeano
        ↓
memoria / relaciones / rumores
        ↓
línea de diálogo fundamentada
        ↓
VoicePacket
        ↓
cliente
        ↓
Piper local
        ↓
WAV
        ↓
auriculares seleccionados
```

La API de AI se utiliza para la conversación de voz del jugador cuando está configurada. Las voces de aldeanos pueden funcionar con TTS local.

## 9. Prueba rápida

1. Arranca Minecraft después de configurar las variables de entorno.
2. Abre `K → Voice Settings`.
3. Selecciona tu micrófono.
4. Selecciona tus auriculares.
5. Pulsa **Rescan devices**.
6. Activa **Villager voices**.
7. Elige `WARM`.
8. Configura STT y TTS.
9. Guarda.
10. Acércate a una aldea.
11. Pulsa `V` para probar STT/TTS del jugador.
12. Deja que dos aldeanos se aproximen para que el sistema social pueda producir una línea fundamentada.

## 10. Si no se escucha nada

Comprueba en este orden:

1. El dispositivo aparece en **Output**.
2. Minecraft tiene permiso para acceder al audio.
3. `piper --help` funciona desde la misma cuenta que ejecuta Minecraft.
4. `TWR_PIPER_MODEL` apunta a un modelo existente.
5. Ejecuta manualmente:

```bash
piper --model /ruta/modelo.onnx --output_file prueba.wav
```

y escribe una frase por stdin.

6. Ejecuta el helper TTS directamente:

```bash
python tools/voice/tts_piper.py "Hola, soy un aldeano." prueba.wav es-ES piper 1.0 1.0 0.6
```

7. Si falla STT, ejecuta el helper con un PCM real y comprueba que devuelve texto.
8. Revisa la consola de Minecraft para errores del adaptador local.

La ausencia o fallo de un adaptador local **no debe bloquear el servidor ni la simulación**.

## 11. Rendimiento

La voz se ejecuta fuera del hilo principal de Minecraft. La síntesis local no debe bloquear el tick del juego.

La simulación también usa presupuestos y rotaciones para evitar recalcular todos los aldeanos y todas las aldeas en cada tick.

Cuando haya muchos aldeanos:

- evita ejecutar TTS para cada pequeño evento;
- las líneas tienen enfriamiento;
- el audio se mezcla y se limita por hablante;
- los paquetes de voz están limitados;
- los escaneos caros están presupuestados.

Si detectas lag, activa el perfilado del servidor y comprueba TPS antes de desactivar sistemas de memoria, familia, inventario o relaciones. Esos sistemas son parte del núcleo del mod y no deben eliminarse para solucionar rendimiento.

## 12. Seguridad

El mod ejecuta comandos locales de STT/TTS que tú configuras.

No pegues comandos de terceros que no entiendas.

La API key, si se usa, se guarda en la configuración local del juego. No la compartas en logs, capturas de pantalla ni repositorios.

## 13. Referencias

- Fabric 26.2: https://docs.fabricmc.net/develop/
- faster-whisper: https://github.com/SYSTRAN/faster-whisper
- Piper: https://github.com/OHF-Voice/piper1-gpl
- Catálogo histórico de voces Piper: https://github.com/rhasspy/piper/blob/master/VOICES.md
