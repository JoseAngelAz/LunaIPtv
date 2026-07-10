# Guía de Despliegue — LunaIPtv

## Compilar para Producción (Release)

### Requisitos
- **Keystore** de firmado (para release)
- **Android Studio** con Gradle configurado

### Paso 1: Generar Keystore (solo la primera vez)
```bash
keytool -genkey -v -keystore lunaipTV-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias lunaipTV
```

### Paso 2: Configurar Firmado
1. Coloca el keystore en la carpeta raíz del proyecto
2. Crea o edita `local.properties`:
   ```properties
   KEYSTORE_FILE=../lunaipTV-release.jks
   KEYSTORE_PASSWORD=tu_contraseña
   KEY_ALIAS=lunaipTV
   KEY_PASSWORD=tu_contraseña
   ```

### Paso 3: Compilar Release
```bash
./gradlew assembleRelease
```

El APK de release estará en: `app/build/outputs/apk/release/app-release.apk`

---

## Distribuir a Usuarios Finales

### Opción 1: Google Play Store (no disponible para fork)
### Opción 2: Sideloading directo
1. Sube el APK a un servicio de archivos (Google Drive, Dropbox, etc.)
2. Comparte el enlace con los usuarios
3. Los usuarios deben:
   - Descargar el APK en su Android TV
   - Activar "Orígenes desconocidos" en Ajustes → Seguridad
   - Instalar el APK

### Opción 3: Repositorio GitHub Releases
1. Crea un release en GitHub
2. Sube el APK como "Asset"
3. Los usuarios pueden descargar directamente desde GitHub

---

## Variables de Entorno (CI/CD)

Si usas GitHub Actions, estas variables se inyectan automáticamente:

| Variable | Descripción |
|----------|-------------|
| `VERSION_CODE` | Número de versión incremental |
| `VERSION_NAME` | Nombre de versión (ej: 1.0.0-beta) |
| `KEYSTORE_FILE` | Ruta al keystore |
| `KEYSTORE_PASSWORD` | Contraseña del keystore |
| `KEY_ALIAS` | Alias de la clave |
| `KEY_PASSWORD` | Contraseña de la clave |

---

## Configuración del Emulador Android TV

### Android Studio
1. Ve a **Tools → Device Manager**
2. Haz clic en **Create Device**
3. Selecciona **TV** (1080p o 4K)
4. Descarga la imagen del sistema (API 34 recomendada)
5. Inicia el emulador

### Sin Android Studio
```bash
# Descargar SDK
sdkmanager "system-images;android-34;google_apis;x86_64"

# Crear AVD
avdmanager create avd -n "AndroidTV" -k "system-images;android-34;google_apis;x86_64" -d "pixel_tv"

# Iniciar emulador
emulator -avd AndroidTV
```

---

## Debugging

### Ver logs en tiempo real
```bash
# Filtrar por tag de LunaIPtv
adb logcat -s OwnTVHome

# Ver todos los logs
adb logcat
```

### Capturar pantalla del TV
```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### Reiniciar la app
```bash
adb shell am force-stop tv.own.owntv
adb shell am start -n tv.own.owntv/.MainActivity
```

---

## Troubleshooting

### "Could not resolve..." durante Gradle Sync
- Verifica tu conexión a internet
- Limpia la caché: **File → Invalidate Caches → Invalidate and Restart**

### APK muy grande (~100MB)
Es normal: incluye libmpv (FFmpeg) con soporte completo de códecs.

### La app crashea al abrir
1. Verifica que el Android TV tenga Android 8.0+
2. Revisa los logs: `adb logcat | grep "FATAL"`
3. Desinstala y reinstala la app

### No se reproduce el video
- Verifica que la fuente IPTV sea accesible desde la red del TV
- Prueba con un reproductor externo (VLC) para descartar problemas de red
