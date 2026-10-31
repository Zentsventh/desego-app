# Diagnóstico del botón Run

## Causas comprobadas

1. Gradle falló al escribir su almacén binario en
   `C:\Users\nuevo_usuario\.gradle\.tmp`: el registro contiene
   `java.io.IOException: Espacio en disco insuficiente` y
   `com.esotericsoftware.kryo.KryoException`. El proyecto está en D:, pero
   Gradle y Android Studio también escriben en C:.
2. El registro de Android Studio contiene `PersistentEnumerator storage
   corrupted` en varios índices de Kotlin y de nombres de archivos, seguido
   de reconstrucciones repetidas del índice `Stubs`.
3. La instalación del APK en el Xiaomi devuelve
   `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: la app existente `com.desego` está
   firmada con otro certificado. La clave debug disponible se creó el
   12 de octubre de 2026.

Los registros consultados son `daemon-10456.out.log` en la carpeta de
Gradle y `idea.log` de `AndroidStudio2026.2.1`.

## Cambios

- La variante debug usa `com.desego.debug` y el nombre `Desego (Debug)`.
  Se puede instalar junto a la app anterior conservando sus datos. La
  variante release mantiene `com.desego`.
- El test de contexto comprueba el identificador de la variante debug.
- Se quitaron imports duplicados de `DashboardScreen.kt`.

## Recuperar Android Studio

1. Cancelar las ejecuciones pendientes.
2. Abrir **File > Invalidate Caches > Invalidate and Restart** para
   reconstruir los índices corruptos. No es necesario borrar el historial local.
3. Después del reinicio, sincronizar el proyecto con Gradle y esperar a que
   termine la indexación.
4. Seleccionar la configuración `app`, la variante `debug` y el Xiaomi;
   pulsar **Run**.

Referencia: https://www.jetbrains.com/help/idea/invalidate-caches.html

## Alcance de la revisión

Se revisaron la configuración Gradle, el manifiesto, MainActivity, navegación,
login, registro, dashboard, pantallas, recursos y configuración de ejecución.
El análisis Android Lint inicial terminó con 0 errores y 18 advertencias.
La app es una demo: autenticación y datos se manejan localmente; no hay un
cliente de API conectado. Esto no causa el fallo de compilación ni el bloqueo
de instalación.

## Verificación final

`assembleDebug`, `assembleDebugAndroidTest` y `lintDebug` terminaron con
`BUILD SUCCESSFUL`. Se generaron los APK de la app y de instrumentación.
El test de instrumentación se compiló, pero no se ejecutó en el dispositivo.

La primera instalación de `com.desego.debug` fue rechazada por el teléfono con
`INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`. Después de que
el usuario habilitara la instalación por USB, el segundo intento terminó
con `Success`. MainActivity abrió con `Status: ok` en 3236 ms y se confirmó
que su proceso seguía activo y su actividad estaba en primer plano en el Xiaomi.
No aparecieron cierres de `com.desego.debug` en el registro de crashes consultado.

La advertencia sobre SDK XML v3/v4 volvió a aparecer en esta verificación.
No bloqueó la compilación. Es distinta de los errores de disco, índices
corruptos y permisos de instalación descritos arriba.
