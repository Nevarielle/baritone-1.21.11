# Port status — Baritone 1.19.4 → 1.21.11 (Fabric)

Branch: `update/1.21.11-fabric`. The 1.19.4 baseline is preserved in the first commit.

## Status

| Part | State |
|---|---|
| Toolchain (Gradle 8.12, unimined 1.4.1, JDK 21) | ✅ works |
| All source sets compile (`api`, `schematica_api`, `main`, `launch`/mixins) | ✅ |
| Fabric jar builds (`fabric/build/libs/baritone-fabric-1.9.5.jar`) | ✅ |
| Verified in-game: `#goto`, `#explore`, `#mine`, `#tunnel`; path renders | ✅ |

## Known limitations

- **Render through walls** — path/goal are not drawn through blocks yet (depth test is baked into the new render pipeline; needs a `NO_DEPTH_TEST` pipeline).
- **Goal beacon beam** disabled — the goal is shown as a box instead.
- **Input while a screen is open** needs a different hook (`Screen.passEvents` was removed).
- **ProGuard / obfuscated dist** build is untested.

## Build

Requires **JDK 21**. Set `JAVA_HOME` to it, then:

```bash
./gradlew :fabric:remapJar     # jar lands in fabric/build/libs/
```

---

*This port was vibe-coded with **Claude Opus** — ultracode, xhigh reasoning effort + workflows.*
