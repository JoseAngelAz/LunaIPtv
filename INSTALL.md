# LunaIPtv — Guía de Instalación y Despliegue

## Requisitos Previos

### Para Desarrollo
- **Android Studio** Ladybug (2024.2) o posterior
- **JDK 17** (viene incluido con Android Studio)
- **Android SDK 36** (instalar via SDK Manager)
- **Dispositivo Android TV** o emulador con API 26+

### Para Instalación en Dispositivo (Usuario Final)
- **Android TV** con Android 8.0 (API 26) o superior
- **Archivo APK** compilado (ver paso 3)

---

## Paso 1: Clonar el Repositorio

```bash
git clone https://github.com/ahXN00/LunaIPtv.git
cd LunaIPtv
```

> **Nota:** Este es un fork de LunaIPtv rebrandado como LunaIPtv para fines educativos sobre IPTV.

---

## Paso 2: Abrir en Android Studio

1. Abre **Android Studio**
2. Selecciona **File → Open** (o **Open** en la pantalla de bienvenida)
3. Navega hasta la carpeta del proyecto y selecciona `build.gradle.kts` (la raíz)
4. Espera a que **Gradle Sync** termine (puede tardar 2-5 minutos la primera vez)
5. Si aparecen errores de SDK, ve a **File → Settings → Languages & Frameworks → Android SDK** y asegúrate de que Android 14 (API 34) o superior esté instalado

---

## Paso 3: Compilar el APK

### Opción A: Desde Android Studio
1. Ve a **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Espera a que termine la compilación
3. El APK se generará en: `app/build/outputs/apk/debug/app-debug.apk`

### Opción B: Desde Terminal
```bash
# En la raíz del proyecto
./gradlew assembleDebug
```

El APK estará en: `app/build/outputs/apk/debug/app-debug.apk`

---

## Paso 4: Instalar en Dispositivo Android TV

### Método 1: USB (Recomendado para desarrollo)
1. Conecta tu Android TV por **USB** al computador
2. Activa **Debugging USB** en el TV:
   - Ve a **Ajustes → Información → Versión**
   - Presiona **"Número de compilación"** 7 veces para activar opciones de desarrollador
   - Ve a **Ajustes → Opciones de desarrollador**
   - Activa **Debugging USB**
3. Ejecuta desde terminal:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Método 2: Por Red (WiFi)
1. Activa **Debugging inalámbrico** en el TV:
   - Ve a **Ajustes → Opciones de desarrollador → Depuración inalámbrica**
2. Empareja el dispositivo:
   ```bash
   adb pair <IP_DEL_TV>:<PUERTO>
   adb connect <IP_DEL_TV>:5555
   ```
3. Instala:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Método 3: USB Drive (Para usuarios finales)
1. Copia el APK a un **USB drive**
2. Conecta el USB al Android TV
3. Usa un explorador de archivos (como **File Commander**) para navegar al APK
4. Selecciona el archivo y selecciona **Instalar**
5. Si aparece "Instalación bloqueada", ve a **Ajustes → Seguridad** y activa **Orígenes desconocidos**

### Método 4: Enviar por Bluetooth
1. En tu teléfono/computador, envía el APK por **Bluetooth** al Android TV
2. Acepta la transferencia en el TV
3. Abre el archivo recibido para instalarlo

---

## Paso 5: Ejecutar la App

Una vez instalada, busca **LunaIPtv** en la pantalla principal de tu Android TV o en la sección de aplicaciones.

---

## Configuración Inicial

Al abrir LunaIPtv por primera vez:

1. **Crear perfil** — Ingresa tu nombre de usuario
2. **Agregar fuente** — Necesitas una de estas opciones:
   - **URL de servidor Xtream** (proveedor IPTV)
   - **URL de playlist M3U** (archivo `.m3u` o `.m3u8`)
3. **Explorar contenido** — Navega por:
   - **Inicio** — Contenido destacado y reciente
   - **TV en Vivo** — Canales en tiempo real
   - **Películas** — Catálogo de películas
   - **Series** — Catálogo de series
   - **Guía TV** — Programación (EPG)
   - **Descargas** — Contenido guardado offline
   - **Ajustes** — Configuración de la app

---

## Estructura del Proyecto

```
LunaIPtv/
├── app/
│   ├── src/main/
│   │   ├── java/com/lunaiptv/
│   │   │   ├── core/           # Base de datos, sincronización, actualizaciones
│   │   │   ├── features/       # Pantallas por función (home, live, movies, series, settings)
│   │   │   ├── player/         # Motor de reproducción (libmpv + ExoPlayer)
│   │   │   ├── ui/             # Tema, colores, componentes reutilizables
│   │   │   └── MainActivity.kt # Punto de entrada
│   │   └── res/
│   │       ├── drawable/       # Iconos, imágenes de splash
│   │       ├── values/         # Strings (inglés), colores, temas
│   │       └── values-es/      # Strings (español)
│   └── build.gradle.kts        # Dependencias y configuración
├── gradle/
│   └── libs.versions.toml      # Catálogo de versiones de dependencias
└── README.md
```

---

## Tecnologías Utilizadas

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Kotlin 2.3.10 |
| UI | Jetpack Compose for TV |
| Navegación | Compose Navigation |
| Inyección de dependencias | Koin |
| Base de datos | Room + KSP |
| Red | OkHttp |
| Imágenes | Coil |
| Reproducción | libmpv (FFmpeg) + ExoPlayer (Media3) |
| Preferencias | DataStore |
| Trabajo en segundo plano | WorkManager |

---

## Solución de Problemas

### "Error de SDK"
Ve a **File → Settings → Android SDK** y instala la API 34 o superior.

### "Error de Gradle"
```bash
./gradlew clean
./gradlew assembleDebug
```

### La app no aparece en el TV
Asegúrate de que el TV tenga Android 8.0 (API 26) o superior.

### No hay contenido
LunaIPtv es un **reproductor**, no provee canales. Necesitas agregar tu propia fuente IPTV (Xtream o M3U).

---

## Licencia

Este proyecto está bajo la licencia **GNU General Public License v3.0 (GPLv3)**.

Puedes:
- Usar, copiar, modificar y distribuir el software
- Vender o regalar el software (bajo los mismos términos de licencia)
- Crear trabajos derivados

Ver el archivo [LICENSE](LICENSE) para más detalles.

---

## Créditos

- **Proyecto original:** [LunaIPtv](https://github.com/ahXN00/LunaIPtv) por ahXN00
- **Fork y rebrand:** Jose Angel Azucena Mendez — para aprendizaje personal sobre IPTV
- **Built with AI** — Asistido por inteligencia artificial
