# MonitorTV (SonicGuard)

Native Android app for intelligent TV loudness normalization and advertisement volume control.

## Phase 1A — TV discovery foundation

This initial repository milestone provides:
- Kotlin + Jetpack Compose Android application
- mDNS discovery for Android TV Remote v2 service: `_androidtvremote2._tcp.`
- Device list with service name, IP address and port
- Android 17 local-network permission declaration and request
- Remote-control interface boundary for the pairing/command implementation

**Not yet implemented:** secure pairing, TLS remote session, protobuf messages, heartbeat, and actual volume commands. Discovery does not prove a TV can be paired or controlled.

## Build
1. Open this repository in Android Studio.
2. Install JDK 17 and Android SDK Platform 37.
3. Sync Gradle and run the `app` configuration on an Android phone.
4. Put phone and TV on the same non-isolated Wi-Fi network.
5. Tap **Scan for TVs** and grant local-network permission.

## Compatibility notes
The Android TV Remote v2 protocol is an unofficial device service rather than a stable public SDK. Some manufacturers/firmware may disable or alter it. mDNS multicast can also be blocked by guest Wi-Fi or router client isolation. Manual IP fallback will be added.

## Next: Phase 1B
Implement certificate generation/storage, six-character pairing-code flow over TLS port 6467, authenticated remote TLS session on port 6466, varint-delimited protobuf messages, TV ping handling, and volume/mute commands.
