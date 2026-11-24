# Plan de mejoras de navidroid

El plan está completo y todo está en `main`. Cada ola entró como un PR propio, verificado en un
emulador contra un Navidrome real antes del merge. Abajo está qué hay, dónde y lo que se decidió no hacer.

## Cómo se prueba

- Servidor: Navidrome 0.64 local con una biblioteca generada con `ffmpeg` (50 canciones, 10 álbumes,
  5 géneros, colaboraciones "feat."), playlists con comentarios `nb1:` de NaviBeat para tener mixes
  y radios, una smart playlist `.nsp` y una segunda usuaria (`ana`).
- Emulador: un AVD propio (`navidroid_wave`, puerto 5560), la app en `githubDebug` conectada a
  `http://10.0.2.2:4533`. La build `githubRelease` (R8) se prueba igual antes de cerrar.
- Antes de publicar: instalar y abrir la build release, no solo compilarla, y pasar
  `scripts/check-release-ctors.sh` sobre el APK.
- Cada PR: tests unitarios de la lógica pura (`./gradlew testGithubDebugUnitTest`, 84 tests) y un
  verificador que recorre los escenarios en el emulador con capturas, sin leer el código.

## Hecho

| PR | Qué cambia para ti |
|---|---|
| [#2](https://github.com/rene-roid/navidroid/pull/2) | Me gusta con lo más nuevo arriba. "Escuchando ahora" en Inicio. Biblioteca con chips combinables, Radios separadas, orden, cuadrícula y ocultar mixes. Radio al tocar una canción. Buscar nuevo. Ajustes y login rediseñados. Widget Now Playing |
| [#3](https://github.com/rene-roid/navidroid/pull/3) | Estantes de descubrimiento en Inicio, "Fans also like", décadas, radio de género y década, canciones nuevas primero en la radio, smart playlists |
| [#4](https://github.com/rene-roid/navidroid/pull/4) | Editar playlists (reordenar, quitar, renombrar, pública, borrar), seleccionar todo, deslizar para añadir a la cola, avisos al añadir, portadas de playlist |
| [#5](https://github.com/rene-roid/navidroid/pull/5) | Varias cuentas en servidores distintos y cambiar entre ellas; apariencia y calidad compartidas |
| [#6](https://github.com/rene-roid/navidroid/pull/6) | Jams: el móvil que suena comparte un enlace; los invitados eligen música desde el navegador o la app, con permisos configurables. Funciona en la red local y por Tailscale |
| [#7](https://github.com/rene-roid/navidroid/pull/7) | Tus estadísticas, enlaces para compartir, descargas automáticas de Me gusta y de las mixes diarias |
| [#8](https://github.com/rene-roid/navidroid/pull/8) | Accesos directos del icono, widget de playlist, volver a la misma pantalla tras cerrar el proceso |
| [#9](https://github.com/rene-roid/navidroid/pull/9) | Color del fondo de pantalla, ordenar y ocultar secciones de Inicio, densidad de listas, reproductor Vinilo y Letras primero, controles grandes, vista previa del widget |
| [#10](https://github.com/rene-roid/navidroid/pull/10) | Cola arrastrable con historial, temporizador, velocidad, ReplayGain, "me gusta" en la notificación, shuffle sin repetir artista, Android Auto |
| [#11](https://github.com/rene-roid/navidroid/pull/11) | Las builds release (R8) vuelven a arrancar y los widgets cargan; `scripts/check-release-ctors.sh` lo comprueba en cada APK |

## Sin verificar en un dispositivo

- El árbol de Android Auto: tiene tests y el servicio está declarado, pero en el emulador no hay
  ningún cliente que lo recorra. Se prueba con el Desktop Head Unit o activando "fuentes
  desconocidas" en Android Auto.
- La pantalla trasera del Xiaomi con el nuevo `MediaLibraryService`, el móvil con letra grande y
  navegación de 3 botones, las vistas de tablet y los widgets en Android 9 a 11.
- "Because you listened to" y "Fans also like" se ocultan en el servidor de prueba porque no tiene
  datos de artistas similares (Last.fm); en un servidor con esos datos deberían aparecer.

## Decidido no hacer

- Tema claro: la identidad de la app es el cielo nocturno con aurora, y cambiaría cada pantalla.
  En su lugar está el acento del fondo de pantalla.
- Crossfade: ExoPlayer no lo trae; haría falta un segundo reproductor y sincronizarlos.
- Traducir letras y modo karaoke: lo primero manda tus letras a un servicio externo; lo segundo
  necesita tiempos por palabra que Subsonic no da.
- Escuchar a la vez en varios móviles dentro de un Jam: el Jam actual es "misma habitación" (suena
  el anfitrión y los demás eligen). Sincronizar el audio entre móviles necesita un relé y corrección
  de reloj; es el siguiente paso natural si hace falta.

## Ideas para después

- Portadas en Android Auto sin conexión (un ContentProvider sobre la caché de portadas).
- "Me gusta" en el widget grande.
- Reanudar la reproducción tras reiniciar el móvil (`onPlaybackResumption`).
- Integrar la rama `t3/local-folder-music-library` (música de carpetas del móvil) sobre las cuentas.
