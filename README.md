# Baritone 1.21.11 (Fabric)

A community fork of **[Baritone](https://github.com/cabaletta/baritone)** — the Minecraft pathfinding bot —
ported from **1.19.4** to **Minecraft 1.21.11** on **Fabric**.

> ⚠️ **Unofficial fork.** This is not made or endorsed by the original authors.
> All credit for Baritone goes to **[Leijurv](https://github.com/leijurv)** and the
> **[cabaletta/baritone](https://github.com/cabaletta/baritone)** contributors.
> The original project stays the source of truth — go there for stable, supported releases.

![Minecraft](https://img.shields.io/badge/MC-1.21.11-brightgreen.svg)
![Loader](https://img.shields.io/badge/loader-Fabric-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![License](https://img.shields.io/badge/license-LGPL--3.0-green.svg)

---

## What this fork is

The goal was simply to get Baritone running on Minecraft 1.21.11 with Fabric.

**Working:**
- Builds a Fabric jar (`fabric/build/libs/baritone-fabric-1.9.5.jar`).
- Verified in-game: `#goto`, `#explore`, `#mine` work and the path renders correctly.

**Known limitations** (see [`TODO.md`](TODO.md) for details):
- Path/goal is **not drawn through walls** yet (depth test is baked into the new render pipeline).
- The **goal beacon beam** is disabled — the goal is shown as a box instead.
- Movement input while a screen is open needs a different hook (`Screen.passEvents` was removed).
- ProGuard / obfuscated distribution build is untested.

This is a work-in-progress port, not a polished release.

---

## Build from source

Requires a **JDK 21**.

```bash
# set JAVA_HOME to your JDK 21, then:
./gradlew :fabric:remapJar          # Linux / macOS / Git Bash
gradlew.bat :fabric:remapJar        # Windows CMD / PowerShell
```

The finished jar lands in `fabric/build/libs/`. Drop it into your Fabric `mods` folder.

---

## Usage

Same chat commands as upstream Baritone:

- `#goto 1000 500` — walk to x=1000 z=500
- `#mine diamond_ore` — mine diamond ore
- `#stop` — stop

Full command reference and settings are in the original docs:
[USAGE.md](USAGE.md) · [FEATURES.md](FEATURES.md) · [SETUP.md](SETUP.md)

---

## Credits & License

- Original project: **[cabaletta/baritone](https://github.com/cabaletta/baritone)**
- Original author: **[Leijurv](https://github.com/leijurv)** and all Baritone contributors
- Original, based on [MineBot](https://github.com/leijurv/MineBot/)
- Community: [Baritone Discord](http://discord.gg/s6fRBAUpmr) — *for the original project; this fork is unofficial and not supported there.*

Licensed under **LGPL-3.0** (with the "anime exception"), the same license as the
original Baritone. See [LICENSE](LICENSE). If you use or redistribute this code, you
must keep the license and attribution intact.
