# Kingdoms Addon — Jails

An addon for [KingdomsX](https://github.com/CryptoMorin/KingdomsX), inspired by Towny's jail plots.

Every kingdom has **one jail**. Members holding the **JAIL** kingdom permission lock players in it,
with an optional **bail** (money, never resource points) and a **sentence**. The server can also
throw the invader of a **failed invasion** in the defender's jail, with a bail it sets itself.

A prisoner gets out by:

| Way out | How |
|---|---|
| Serving the sentence | automatic, online **or offline** (configurable) |
| Paying the bail | `/k jail paybail` — through Vault |
| Leaving the kingdom | only for a member of the jailing kingdom |
| Escaping | reaching the wilderness: the kingdom is responsible for building a proper jail |
| Being released | `/k jail release <player>` by the kingdom, `/k admin jail release <player>` by an admin |

Released prisoners are optionally sent to their kingdom home. While in jail, a prisoner can't
teleport (nor be teleported), use items (ender pearls and chorus fruits first), fight, build,
talk in the global chat or use some commands — **every one of these rules can be switched off**.

Jailing never touches the prisoner's inventory.

---

## Installation

1. Build: `mvn package` → `target/Kingdoms-Addon-Jails-1.0.0.jar`, or download it: every push to
   `main` and every pull request is built by GitHub Actions (*Actions* tab → *Build* → the run →
   *Artifacts* → `Kingdoms-Addon-Jails`). CI builds against KingdomsX **1.17.18.1-BETA**, the
   latest on Maven Central; the `KINGDOMS_VERSION` repository variable changes it, provided that
   version can be downloaded. For a build against your server's exact version, build locally.
2. Drop the jar into the server's `plugins/` folder, next to KingdomsX.
3. Restart. The addon installs:
   - `plugins/Kingdoms/jails.yml` — the configuration;
   - `plugins/Kingdoms/jails/languages/<code>.yml` — one message file per language.
4. **Give the JAIL permission to the ranks that should manage the jail**, in KingdomsX's
   `ranks.yml`. KingdomsX only gives it to the king by default.

Requirements: Minecraft **1.16+**, KingdomsX **1.17.27.3**, Vault + an economy plugin for the bails.
Folia is supported.

**Rebuild on every KingdomsX version bump, before deploying.** KingdomsX signatures do move between
releases, and compiling is the fastest way to find out. Maven Central stops at 1.17.18.1-BETA, so
install your own server's jar — which is also the more faithful thing to build against:

```bash
mvn install:install-file -Dfile=<server>/plugins/KingdomsX-1.17.27.3.jar -DgroupId=com.github.cryptomorin -DartifactId=kingdoms -Dversion=1.17.27.3 -Dpackaging=jar
```

Without it, the build also works against the version published on Maven Central:
`mvn package -Dkingdoms.version=1.17.18.1-BETA`.

## Commands

| Command | Who | What it does |
|---|---|---|
| `/k jail location` | JAIL permission | Sets the kingdom's jail where you stand (in your own land) |
| `/k jail location remove` | JAIL permission | Removes the jail |
| `/k jail member <player> [bail] [duration] [reason]` | JAIL permission | Jails a player |
| `/k jail status [player]` | anyone | Sentence, time served and left, bail, reason |
| `/k jail paybail [player]` | anyone | Pays your bail — or someone else's |
| `/k jail release <player>` | JAIL permission | Releases a prisoner of your kingdom |
| `/k jail list` | members | The prisoners of your kingdom |
| `/k admin jail member <kingdom> <player> [bail] [duration] [reason]` | admin | Jails, no requirement checked |
| `/k admin jail release <player>` | admin | Releases, whatever the kingdom |
| `/k admin jail resetcooldown <player>` | admin | Clears the jailing cooldown |

Aliases: `/k jails`, `/k prison`.

`[bail] [duration] [reason]` are all optional, in that order: a number is the bail, a duration
carries its unit, the rest is the reason. `-` or `none` means no bail, `permanent` a sentence
without time limit (if allowed).

```
/k jail member Steve                         default sentence and bail
/k jail member Steve 500                     $500 bail
/k jail member Steve 500 2h Griefing         $500 bail, 2 hours, with a reason
/k jail member Steve - 30m                   no bail, 30 minutes
/k jail member Steve Spawn killing           default sentence, with a reason
```

Durations everywhere — commands and `jails.yml` — read `30s`, `10m`, `2h`, `1d`, `1w`, or combined:
`1h30m`. A bare number is a number of seconds.

## Who can jail whom

- Only members holding the jail permission (`jail.permission`, `JAIL` by default). Admin mode
  (`/k admin`) counts as having it.
- **Members holding the jail permission can never jail one another.** With
  `respect-rank-hierarchy`, a member can only jail members of a lower rank.
- The target must stand in the kingdom's land and be online, unless configured otherwise.
- Members of other kingdoms can be jailed depending on the relation between the two kingdoms
  (`jailing.targets.relations`, `NEUTRAL, ENEMY, TRUCE` by default).
- A player can't be jailed again for **10 minutes** (`jailing.cooldown`) after being jailed — by
  any kingdom, or by the same kingdom with `cooldown-per-kingdom`.
- Players holding `kingdoms.jails.exempt` can never be jailed.

## Failed invasions

Disabled by default:

```yaml
invasions:
  auto-jail:
    enabled: true
    results: [ ATTACKER_SURRENDERED, DIED, TIMES_UP, ATTACKER_DEATH_LIMIT, LOGOUT ]
    bail: '1000'                 # set by the server; a math expression is fine
    bail-receiver: NONE          # or KINGDOM_BANK: the defender's bank
    duration: 1h
    reason: 'Failed invasion'
```

Only the invader is jailed, unless `attackers-in-invaded-land` also takes the attacking kingdom's
members standing in the invaded land. An invader who logged out or died is jailed all the same:
they are sent to the jail on their next login or respawn.

## Restrictions

All under `restrictions` in `jails.yml`, each one can be switched off:

| Rule | Default |
|---|---|
| Teleporting, by cause (`COMMAND`, `PLUGIN`, `ENDER_PEARL`, `CHORUS_FRUIT`, portals…) | blocked |
| Items: `ALL` blocks everything except an allow list (some food), `LIST` blocks a block list | `ALL` |
| Attacking players | blocked |
| Being hit, for a prisoner who is a member of the jailing kingdom | protected |
| Being hit, for any other prisoner | allowed (optionally only by the jailing kingdom) |
| Breaking, placing, right-clicking blocks | blocked (whitelist available) |
| Right-clicking entities | blocked |
| Talking in the KingdomsX channels listed in `blocked-channels` | `GLOBAL` blocked |
| Commands, blacklist or whitelist | `home`, `spawn`, `tpa`, `k home`… blocked |

`/k jail …` and `/k leave` stay allowed whatever the list says (`always-allowed`). Players holding
`kingdoms.jails.bypass` ignore every restriction.

## Languages

Messages live in **one file per language**, `plugins/Kingdoms/jails/languages/<code>.yml`, with
exactly the same layout as the KingdomsX language files (the `command:` and `jails:` trees). A
missing key falls back to the English text built into the addon, so a partial translation breaks
nothing.

A file is created on startup for every language installed on the server: French ships translated,
the others are generated from the English texts, ready to be translated.

KingdomsX has no French locale (it ships `en, de, es, it, pt, pl, ru, cs, hu, tr, uk, vi, zh`), so a
French server runs in English. Point `EN` at `fr.yml` to get French messages:

```yaml
# jails.yml
messages:
  language-files:
    EN: fr        # the EN language reads fr.yml
```

## Configuration — `jails.yml`

The file is commented option by option; the main sections:

| Section | What it holds |
|---|---|
| `jail` | permission, location rules, max prisoners, respawn in jail, exempt/bypass permissions, disabled worlds |
| `jailing` | cooldown, sentence (default/min/max/custom/permanent), bail (default/min/max), reason, who can be jailed |
| `notifications` | who is told about jailings and releases, reminder interval |
| `release` | time (offline time counted or not), bail (receiver, paying for others), leaving the kingdom, escape mode, release teleport destinations |
| `invasions.auto-jail` | jailing after a failed invasion |
| `restrictions` | everything a prisoner can't do |
| `status`, `list`, `timer` | who sees what, how often sentences are checked |

Release destinations are tried in order, the first available wins: `OWN_KINGDOM_HOME`,
`JAILER_KINGDOM_HOME`, `PREVIOUS_LOCATION`, `BED_SPAWN`, `WORLD_SPAWN`, `MAIN_WORLD_SPAWN`, and
`NONE` to stop there. Which releases teleport at all is a list of reasons (an escape doesn't, by
default).

## Technical notes

- **Storage** — everything is kingdom or player metadata, saved with the KingdomsX data, whatever
  the database: `Jails:LOCATION`, `Jails:PRISONERS` and `Jails:COOLDOWNS` on the kingdom,
  `Jails:PENDING_RELEASE` on the player (a release that happened while they were offline). Each
  one goes through the plainest call of the KingdomsX data API, `setString` / `asString`, with
  URL-encoded records: the map and section setters change between versions, these two don't.
- **Prisoners live on the jailing kingdom** — kingdoms are always in memory, players are not, and
  a jail that disappears with its kingdom frees its prisoners by construction. An index from
  prisoner to kingdom answers "is this player jailed?" — asked on every move, chat line and
  command — without walking every kingdom; it is rebuilt every minute as a safety net.
- **Built-in jail of KingdomsX** — KingdomsX ships unfinished jail classes (`JailStructure`,
  `CommandJail`) that are not registered anywhere; only its `JAIL` permission is used here, so
  `/k jail` is free. If a future KingdomsX version registers its own `/k jail`, the two will
  conflict: rename this one in the language files (`command.jail.name`).
- **Invasions** — `KingdomInvadeEndEvent` fires with the result already set, but KingdomsX still
  sends the invader back afterwards. Jailing waits `invasions.auto-jail.delay` so the two
  teleports don't fight.
- **Teleport restriction** — the addon's own teleports (to and out of the jail) are let through,
  everything else is checked against the blocked causes. `PlayerPortalEvent` has its own handler
  list and is listened to separately.
- **Chat** — the channel is the player's KingdomsX channel. A message of the `ranged` channel
  starting with its range bypass prefix (`!`) reaches everyone, so it counts as global: blocking
  `GLOBAL` still lets prisoners talk to whoever stands near them, nothing more. If the chat API
  moved, every message is treated as global.
- **Folia** — tasks go through the KingdomsX scheduler (global region / entity schedulers), and
  teleports use Paper's `teleportAsync` when it exists, looked up by reflection since the addon
  compiles against the Spigot API.
- **Leaving the kingdom** — the release runs a tick after `KingdomLeaveEvent`, once the player is
  really out: the release teleport would otherwise send them to the home of the kingdom they are
  leaving.

## API

```java
JailSession session = KingdomJails.getSession(playerId);     // null if free
boolean jailed = KingdomJails.isJailed(playerId);
List<JailSession> prisoners = KingdomJails.getPrisoners(kingdom);
Location jail = KingdomJails.getJailLocation(kingdom);

JailService.jail(kingdom, offlinePlayer, issuerId, JailType.MANUAL, durationMillis, bail, reason);
JailService.release(session, ReleaseReason.PLUGIN);
```

Two Bukkit events: `PlayerJailEvent` and `PlayerUnjailEvent`, both cancellable (a release caused
by a disband or by leaving the kingdom can't be cancelled).

## Project layout

```
src/main/java/org/kingdoms/jails/
├── JailsAddon.java              the addon's entry point
├── commands/                    /k jail … and /k admin jail …
├── config/                      jails.yml options, messages
├── data/                        sessions, metadata handlers, KingdomJails API
├── events/                      PlayerJailEvent, PlayerUnjailEvent
├── locale/                      per-language message files
├── managers/                    jailing/releasing, timer, restrictions, escape, invasions
└── util/                        durations, storage codec, teleports, scheduling
src/main/resources/
├── jails.yml
└── languages/fr.yml
src/test/java/                   unit tests (parsing, storage, timers, matching)
```

## Tests

`mvn package` runs the unit tests: argument parsing, durations, the storage round trip of a
session, the online-time clock, command and material matching. Everything that needs a running
server — events, teleports, KingdomsX data — has to be checked in game.
