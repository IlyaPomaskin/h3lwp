# Privacy Policy

_Last updated: 2026-10-24_

This privacy policy applies to the Android app **Heroes 3 Live Wallpaper** (package `com.homm3.livewallpaper`), distributed via Google Play and as source code at <https://github.com/IlyaPomaskin/h3lwp>.

## Summary

The app does **not** collect, transmit, or share any personal data. It runs entirely on your device.

## Data the app handles

The app reads files **you explicitly select** through the system file picker:

- `H3sprite.lod` — game sprites from your copy of Heroes of Might and Magic III, **or**
- the GOG offline installer (`setup_heroes_of_might_and_magic_3_complete_*.exe` and its `.bin` file) — the app extracts `H3sprite.lod` and the original maps from it.
- The Horn of the Abyss installer (`HotA_*_setup.exe`, optional) — the app extracts the HotA sprites and maps from it.
- `.h3m` map files — Heroes 3 maps you choose to display as wallpaper.

Installers are copied to the app's cache only for the time of extraction and deleted afterwards. The extracted files are kept in the app's private storage directory and used only to render the wallpaper on your device. They are never uploaded, transmitted, or shared with any server or third party.

The app also stores user preferences (scale, brightness, scroll behavior, map-change interval) locally using Android DataStore. These preferences never leave your device.

## Network access

The app does **not** request the `INTERNET` permission and does not make any network requests.

## Permissions

The app uses the standard system file picker (Storage Access Framework) to let you choose `.lod`, installer (`.exe`, `.bin`) and `.h3m` files. It does not request the legacy `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE` permissions.

## Third-party services

The app integrates no third-party SDKs for analytics, advertising, or crash reporting.

## Children

The app is suitable for all audiences and does not knowingly collect data from anyone, including children.

## Disclaimer

Heroes of Might and Magic is a trademark of Ubisoft Entertainment. This app is a fan project and is not affiliated with or endorsed by Ubisoft or the Horn of the Abyss team. The app includes no game content; you must provide files from your own copy of the game.

## Contact

Questions or issues: open an issue at <https://github.com/IlyaPomaskin/h3lwp/issues>.
