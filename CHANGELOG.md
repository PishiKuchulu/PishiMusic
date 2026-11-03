# Changelog

## [2026.10] - 2026-10-29

### Added
- **Library import:** installed over the app it replaces (same app ID and signing key), Tuned brings over liked songs, playlists, listening history, searches and the YouTube Music sign-in on first launch, then opens on Home.
- **Widgets:** four fixed sizes (Full cover, Small, Medium, Wide), coloured from the cover with a soft wash from the top.
- **Lyric story card:** share up to five lines as a 9:16 story with a frosted panel on a gradient from the cover's colours.
- **In-app updates:** Tuned checks GitHub for a new release, downloads the APK, checks it against the release's SHA-256 and hands it to Android to install. Find it under Settings, App.
- **AutoMix:** beat-matches songs whose tempos are close and swaps the bass lines halfway. When one song has no clear beat, they overlap at full level. Two clear beats that would clash still fade.
- **AutoMix on the cover:** the old cover melts into the new one during a blend.
- **Videos tab** in search.
- **Playlist Playground:** type an idea and get a playlist built from YouTube Music searches.
- **Sing:** lowers the vocals from the lyrics screen. It's a rough reducer, so centred instruments dip too.
- **Lyrics:** duets, background vocals, and one translate and pronounce button.
- **AutoPlay toggle** in the queue, and **Music Haptics**.
- **Yearly recap** on the You tab: minutes listened, then your top songs and artists ranked by time. Counting starts from your all-time plays, so the first year is approximate.
- **Pins:** pin songs, albums and artists from their menus; they sit at the top of Library with Liked Songs. Long-press to unpin.
- **Local albums:** songs on the phone group into albums by tag or folder, each with its own page, and folder chips filter the list when you have more than one folder.
- **YouTube Music links** open the album, playlist or artist in Tuned instead of playing straight away; a song link inside a playlist plays from that song.
- **Explicit tags** on songs and albums: in lists, after album and song titles, in the player, search and the queue.
- **Cast volume** from the volume keys, the system panel and the Sound sheet.
- **EQ and AutoEQ** are one tap away from the Sound sheet.
- **Playlist Playground** also opens from the Home header.

### Changed
- **Loading indicator:** one circular spinner everywhere, including pull-to-refresh.
- **Mini player:** restyled, same controls.
- **Logo:** the flower in notifications.
- **Text:** widget, Jam, queue and audio-speed labels can now be translated; sentence case throughout; no "·" separators; "Synced 1 playlist" reads correctly.
- **Code:** package `com.tuned.app`, with saved-data names kept in `data/StorageNames.kt` so nothing is lost on update.
- **Local songs page:** each folder is one row with its song count; Add folder and Rescan sit under them, and local albums drop the share button.
- **Music videos** keep playing while you open lyrics or the queue, and the page stays dimmed on every tab in video mode.
- **Streaming:** streams are checked before playing, more YouTube clients are tried, DNS falls back to DNS-over-HTTPS, media loads in ranges, and age-restricted songs play.
- **Hide the dock** now lets you swipe between tabs, and the Tuned font applies to all text.
- **You tab** play counts come from what you actually played.
- **Album tracks** only show artists who aren't already in the album's credit.
- **Other versions** uses YouTube Music's own list of editions.
- **App icon:** a light icon for light mode and a dark one for dark mode, each with a subtle gradient.
- **Player:** a long list of artists scrolls on one line like the title, instead of wrapping.
- **Settings:** bit-perfect, USB exclusive, internal direct output and USB volume have their own section, with a warning to change them only if you know what they do.
- **Widgets** fill the space the launcher gives them and can be resized.
- **USB exclusive** sets the volume in software by default and leaves the DAC's own volume alone; every session fades in.

### Fixed
- Closing the app now stops the music.
- Volume jumping to full on USB and bit-perfect output.
- Headphones going silent until replugged after exclusive USB output.
- Speed and pitch doing nothing on the main audio path.
- The sung lyric line sitting too low on songs with a long intro. It now stays centred.
- Album page crashing on scroll when an album appeared twice in a shelf.
- Liked songs showing as unliked after the app restarted.
- The About page had no way in; App language now sits in Appearance, and Share logs and the licences in Data and storage.
- Lossless add-on requests never being signed: the native check expected a package name the app didn't have.
- The lyrics source list showing a backend's name instead of "Apple Lyrics".
- Artist pages that search can't find, such as ¥$, opening empty.
- Music videos showing up in Quick picks, and songs playing their music video's audio.
- Other versions reopening the same album.
- Album tracks showing a single's cover.
- The paused cover glitching when the player opens.
- Player colours not updating between songs from the same album.
- A blank screen after pressing back, seeking in downloaded songs, a USB freeze and a search crash.
- The Taste strip ignoring its Home toggle.
- Volume jumping to full on bit-perfect output with EQ or effects on, when bit-perfect was switched on mid-song, or when a DAC was plugged in while playing.
- A burst at full volume at the end of songs on direct output, and fades starting loud on exclusive USB.
- USB earphones with a built-in DAC, such as the FiiO JD1, playing at full volume when bit-perfect was on.
- No sound after turning bit-perfect off while a song played.

## [0.1.0]

First Tuned build.
