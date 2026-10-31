# Tienda Desego: frontend Android

Esta etapa implementa una tienda de prueba en Compose. No cambia las tablas de
`bd_desego` ni envía pedidos a la API. El backend existente sigue disponible para
verificar la conexión MySQL; su integración comercial queda para la siguiente etapa.

## Un login, dos destinos

| Cuenta de prueba | Contraseña | Destino |
| --- | --- | --- |
| admin@desego.com | 123456 | Panel del personal existente |
| cliente@desego.com | 123456 | Tienda pública |

El registro crea cuentas de cliente locales, sin reemplazar las demás cuentas.
Las contraseñas registradas se guardan como hashes PBKDF2 con sal aleatoria.
Recordarme conserva la sesión; cerrar sesión conserva el carrito y favoritos.
Las cuentas y roles de esta versión son de demostración. La API deberá autenticar
y autorizar el rol real: el routing del frontend no concede permisos en MySQL.

## Flujos disponibles

- Inicio con fotografías, recomendaciones, cupón y acceso a reservas.
- Carta de seis productos: búsqueda, categorías, favoritos y detalle con cantidad.
- Carrito por cuenta, cantidades limitadas a 20 por producto, eliminación y totales
  en céntimos. Cupón de prueba `DESEGO10`: 10% sobre productos; delivery de ejemplo S/ 6.
- Delivery o recojo; direcciones guardadas, celular validado e indicaciones del pedido.
- Selector de contraentrega, tarjeta o Yape/Plin: no solicita datos bancarios ni cobra.
- Confirmación local, historial, cancelación y volver a pedir.
- Seguimiento visual: solo el primer estado corresponde al registro local;
  preparación, reparto y entrega requieren datos del servidor.
- Perfil, direcciones, reservas de prueba, novedades y formulario de ayuda.
- Persistencia local de carrito, favoritos, direcciones, pedidos y reservas, separada por cliente.

Las fotos, precios, ingredientes, tiempos y promociones son ejemplos. Las reservas
no bloquean mesas y el formulario de soporte no envía mensajes. Los siguientes
trabajos son conectar catálogo/stock, login y roles al servidor; validar cobertura,
reservas y estados reales; implementar mapas, notificaciones, comprobantes y,
al final según lo solicitado, la pasarela de pagos con confirmación del proveedor.

## Fotografías de prueba

Generadas con la herramienta integrada de imágenes (sin fallback), copiadas a
`app/src/main/res/drawable-nodpi`. Se comparten entre inicio, carta, carrito y detalle.

| Archivo | Descripción usada para generar |
| --- | --- |
| food_ceviche.png | Ceviche peruano de pescado, cebolla roja, cilantro, limón, camote y choclo en plato blanco. |
| food_pescado.png | Pescado entero frito peruano con yuca y ensalada fresca. |
| food_arroz.png | Arroz peruano con mariscos, langostinos y calamar. |
| food_chicha.png | Vaso de chicha morada con hielo y limón, maíz morado al lado. |
| food_chocolate.png | Porción de torta de chocolate oscuro. |

Dirección visual común: fotografía gastronómica realista, luz natural cálida,
mesa de lino crema, composición cuadrada, sin texto ni logotipos.

## Validación de esta etapa

- `assembleDebug` y `assembleDebugAndroidTest`: compilación correcta.
- Cuatro pruebas unitarias nuevas: céntimos, cupón, delivery, carrito vacío y entradas inválidas.
- Seis pruebas instrumentadas ejecutadas en el Xiaomi: rutas de administrador/cliente,
  registro de clientes, sesión recordada, contraseña incorrecta, persistencia aislada
  y checkout con recojo sin cobro. Resultado: `OK (6 tests)`.
- APK principal y APK de pruebas instalados con éxito mediante ADB.
- Inicio de tienda revisado visualmente: `review/desego-store-home.png`.

La primera ejecución mediante `connectedDebugAndroidTest` fue bloqueada por el
teléfono (`INSTALL_FAILED_USER_RESTRICTED`). Tras desbloquearlo, ambos APK se
instalaron manualmente y la ejecución directa de AndroidJUnitRunner pasó:

```powershell
adb -s xcyxsoivv87t5l85 shell am instrument -w -r -e class com.desego.StorefrontTest com.desego.debug.test/androidx.test.runner.AndroidJUnitRunner
```

APK para probar: `app/build/outputs/apk/debug/app-debug.apk`.
