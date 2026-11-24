# Plan de mejoras de navidroid

Plan por olas. Cada paso termina en algo que se puede comprobar en el emulador contra un Navidrome
real, y cada ola se puede publicar sola. La ola 1 ya está hecha en la rama `t3/app-wave-1`.

## Cómo se prueba

- Servidor: Navidrome 0.64 local con una biblioteca generada con `ffmpeg` (50 canciones, 10 álbumes,
  5 géneros, colaboraciones "feat."). Playlists con comentarios `nb1:` de NaviBeat para tener mixes
  y radios, y una segunda usuaria para "Escuchando ahora".
- Emulador: un AVD propio (`navidroid_wave`), la app en `githubDebug`, conectada a `http://10.0.2.2:4533`.
- Cada paso: tests unitarios de la lógica pura (`./gradlew testGithubDebugUnitTest`) y captura del
  emulador de la pantalla que cambia. La build `xiaomiRelease` (R8) tiene que compilar antes de publicar.

## Ola 1. Hecha

| Paso | Qué cambia para ti | Prueba |
|---|---|---|
| 1 | Me gusta muestra primero lo último que te gustó | `LikedTest`; Navidrome devuelve lo más nuevo primero y la app lo invertía |
| 2 | Inicio enseña quién más está escuchando en el servidor ("ana on Feishin"), y tus otros dispositivos | `ListeningTest`; captura con otra usuaria reproduciendo |
| 3 | Tu biblioteca: chips tipo Spotify (X para limpiar, subfiltros por tipo, combinables), Radios separadas de Mixes y de tus playlists, orden (guardados, A-Z, creador), lista o cuadrícula, interruptor para ocultar mixes y radios | `LibraryTest`; capturas de chips, radios y cuadrícula |
| 4 | Tocar una canción suelta arranca su radio: suena al instante y se añaden canciones parecidas, marcadas "Autoplay · songs like X" con botón Clear. "Start song radio" en el menú de cada canción. Autoplay activado por defecto. Cola con "Clear queue" y "Save as playlist" | `RadioTest`; captura de la cola |
| 5 | Buscar: búsquedas recientes, estante "Discover" de álbumes que nunca has puesto, "Shuffle your library", chips All/Songs/Artists/Albums/Genres, "Top result", error de red distinto de "sin resultados" | `SearchRankTest`; capturas |
| 6 | Ajustes rediseñados: tarjeta de cuenta con servidor, versión y latencia; categorías con icono y resumen en vivo; páginas propias de Reproducción, Descargas, Servidor, Almacenamiento y Acerca de | capturas |
| 7 | Login en dos pasos: la dirección se comprueba mientras escribes ("Navidrome 0.64.2 · not encrypted"), luego usuario y contraseña; direcciones extra opcionales. Añadir direcciones en Ajustes con la misma comprobación. Cerrar sesión pide confirmación y borra los datos de la cuenta | `ProbeTest`; capturas del login y del error "connection refused" |
| 8 | Widget "Now playing" redimensionable (pequeño, ancho, grande, alto) con fondo del color de la portada, oscuro o translúcido, y controles opcionales. Funciona con la app cerrada | `NowPlayingTest`; capturas, pausa y play con el proceso muerto |

## Ola 2. Varias cuentas y servidores

Ahora "servidores" significa varias direcciones del mismo servidor y la misma cuenta. Para tener
cuentas distintas (tu Navidrome, el de un amigo) hace falta separar los datos por cuenta.

1. Modelo: `Account(id, name, endpoints, username, token, salt)` en unas preferencias globales, más
   la cuenta activa. La cuenta que ya existe se queda con las rutas de hoy, así no se mueve nada.
2. Una función `App.dir(nombre)` da la carpeta de cada cuenta (`filesDir/a-<id>/...`); `Prefs`,
   `Offline` (descargas, portadas, snapshots), `queue.json` y la caché `meta/` pasan a usarla.
   Comprobación: test de que dos cuentas no comparten ni preferencias ni carpetas.
3. Cambiar de cuenta reinicia el proceso en la nueva (las cachés de Media3 no se pueden abrir dos
   veces). Comprobación: descargas de la cuenta A siguen ahí al volver de B.
4. UI: en la tarjeta de cuenta de Ajustes, "Cambiar de cuenta" con la lista y "Añadir cuenta", que
   reutiliza el login de dos pasos.
5. Antes de empezar: la rama `t3/local-folder-music-library` (sin commitear) toca las mismas clases.
   Hay que integrarla primero o rebasar esta ola encima.

## Ola 3. Jams (escuchar juntos)

Navidrome no tiene sesiones compartidas ni playlists colaborativas, así que hace falta un pequeño
relé. Diseño propuesto:

1. Relé: un servicio mínimo (WebSocket, unos cientos de líneas en Go o Node) que cualquiera puede
   alojar junto a su Navidrome, o un contenedor Docker. Guarda en memoria salas con `jamId`, anfitrión,
   cola y estado de reproducción (canción, posición, play/pausa, hora del servidor).
2. Enlace: `https://<relé>/j/<jamId>` y deep link `navidroid://jam/<jamId>`; un QR en pantalla.
3. Permisos configurables por el anfitrión: solo escuchar, añadir a la cola, votar para saltar, o
   control total. Límite de canciones por persona.
4. Reproducción: cada invitado reproduce desde su propio Navidrome si la canción existe en su
   biblioteca (mismo servidor: mismo id; otro servidor: buscar por título y artista o MusicBrainz id).
   Sincronía con el reloj del relé, corrigiendo la deriva con pequeños saltos.
5. Modo "misma habitación": solo el anfitrión suena y los demás eligen música desde su móvil.
6. Comprobación: dos emuladores en la misma sala, la cola y la posición coinciden en menos de 300 ms.

Esto es lo más caro del plan. Recomiendo empezar por el modo "misma habitación" en la red local
(descubrimiento por NSD, sin relé), que ya resuelve la fiesta en casa con una fracción del trabajo.

## Ola 4. Descubrir música

1. "Porque escuchaste X": estantes en Inicio a partir de los últimos álbumes, con `getSimilarSongs2`
   y `getArtistInfo2`.
2. "Nunca escuchado": álbumes y artistas con `playCount == 0`, con un botón de shuffle.
3. "Hoy hace X años": lo que escuchabas o lo que salió en esta fecha.
4. Radio por género y por década desde las tarjetas de Buscar.
5. Artistas similares en la página de artista (ya llega en `getArtistInfo2`).
6. Filtro "solo canciones que no he escuchado" en las radios.
7. Smart playlists de Navidrome (`.nsp`) mostradas como mixes propios.

## Ola 5. Usabilidad y control

1. Reordenar la cola arrastrando y ver lo que ya sonó.
2. Editar playlists propias: quitar, reordenar, renombrar, portada.
3. Seleccionar todo en listas y gestos de deslizar (deslizar para añadir a la cola).
4. Temporizador de apagado, velocidad, crossfade y ganancia de reproducción (ReplayGain).
5. Botón de "me gusta" en la notificación y en el widget grande.
6. Android Auto y Wear: pasar a `MediaLibraryService` con árbol de navegación.
7. Restaurar la pila de navegación tras matar el proceso.
8. Accesos directos del lanzador (Me gusta, Shuffle, última radio).
9. Un segundo widget: atajo a una playlist o mix elegida.

## Ola 6. Aspecto y personalización

1. Tema claro y "seguir al sistema", y Material You (color dinámico) como opción de acento.
2. Reordenar y ocultar secciones de Inicio.
3. Tamaño de portadas y densidad de listas.
4. Reproductor: estilos (vinilo, portada grande, letras a pantalla completa).
5. Página de configuración del widget con vista previa en vivo.

## Ideas sueltas

- Estadísticas personales (tu "Wrapped" con los scrobbles de Navidrome).
- Compartir canción o álbum con un enlace público (`createShare` de Subsonic).
- Letras: traducción y modo karaoke.
- Cola "inteligente": no repetir artista dos veces seguidas en shuffle.
- Descargas automáticas de Me gusta y de las mixes diarias en Wi-Fi.
- Modo coche: botones grandes en el reproductor.
