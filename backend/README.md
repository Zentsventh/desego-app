# MySQL local: bd_desego

La API usa MySQL en `127.0.0.1:3306`, base `bd_desego`, usuario `root`
y contraseña vacía. La configuración está en `src/database.php`.
No hay dependencias de dominios ni de hosting.

## Iniciar la conexión para Android

1. Iniciar MySQL en XAMPP.
2. Ejecutar `start-local.ps1` desde PowerShell en esta carpeta.
   El script inicia la API PHP en `127.0.0.1:8000` y configura
   `adb reverse tcp:8000 tcp:8000` para los dispositivos conectados.
3. Mantener esa terminal abierta y el teléfono conectado por USB.
4. Ejecutar la variante debug de la app. En el login debe aparecer
   **Conexión disponible**; se puede volver a comprobar con el botón.

La redirección USB permite que `127.0.0.1:8000` desde el teléfono llegue
a esta computadora. No se necesita abrir MySQL ni un puerto de la API
a la red Wi-Fi. Tras desconectar/reconectar el dispositivo, ejecutar
de nuevo el comando `adb reverse tcp:8000 tcp:8000`.

## Comprobar MySQL sin Android

Ejecutar `php check-database.php`: muestra el nombre de la base y sus tablas.
Con el servidor activo, `http://127.0.0.1:8000/health.php` devuelve:

```json
{"service":"desego","database":"connected"}
```

El servidor necesita PHP 8 con `pdo_mysql`. Si no se encuentra PHP en PATH,
el script usa `C:\xampp\php\php.exe`. Las variables `DESEGO_DB_HOST`,
`DESEGO_DB_PORT`, `DESEGO_DB_NAME`, `DESEGO_DB_USER` y `DESEGO_DB_PASSWORD`
permiten ajustar los datos sin cambiar el código.

## Alcance

La tienda pública y los accesos locales por rol están documentados en
[FRONTEND.md](../FRONTEND.md). En esta etapa sus pedidos, reservas y pagos son
demostraciones locales; no escriben en MySQL.

La conexión ejecuta `SELECT 1` contra la base real; no modifica tablas.
Las credenciales de MySQL quedan en PHP, no en el APK.
El acceso HTTP a localhost se permite únicamente en la variante debug.
La variante release no tiene URL configurada para este servidor de desarrollo.

El login, registro y las pantallas de la demo todavía manejan datos locales.
Su integración con las tablas necesita endpoints de autenticación y datos;
el usuario `root` de MySQL no es un usuario de acceso a la app.

## Verificación realizada

- MySQL confirmó `bd_desego` y permitió consultar sus tablas.
- La API respondió HTTP 200 con `database: connected`.
- `assembleDebug` terminó con `BUILD SUCCESSFUL`.
- El APK se instaló y MainActivity abrió en el Xiaomi.
- La app consultó `/health.php` mediante la redirección USB y la API
  respondió HTTP 200.
