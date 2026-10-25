# Mystic Arts

A Fabric mod for Minecraft 26.3 with a sorcerer's relics: a Sling Ring that opens portals to anywhere, the Cloak
of Levitation, the Eye of Agamotto that turns back time, and mandala shields. Everything else sees it all too.

**Sling Ring:** sneak and use it to attune it to where you stand (its tooltip shows where). Use it anywhere else
and you trace a circle with your ring hand as a portal sparks open in front of you, a spinning rim of fire. Its other
end opens on the attuned spot, in any dimension. Walk through, or throw things through, and you come out the other
end, turned the way you faced when you attuned it and still moving. The far end works the other way, so you can come
back. Use the ring again to close both ends; they close on their own after a minute.

**Cloak of Levitation:** worn in the chest slot. You wear the sorcerer's blue tunic under a long red cloak with a high
collar, and:
- you can fly (double-tap jump, like creative flight). Flying, you levitate: upright, legs hanging, the cloak
  billowing out behind you;
- you take no fall damage, and once a fall gets long the cloak slows it to a drift;
- it counts as armor: 4 armor, 1 toughness;
- you can conjure shields (below).

Taking it off in mid-air gives 5 seconds of slow falling. Creative and spectator players keep their own flight.

**Mandala shield:** hold **Conjure Shield** (default `X`) while wearing the cloak. An orange mandala of runes spins up
in front of you, palms out. It stops anything that comes at you from the front: blows (and throws the attacker back),
arrows and other projectiles (which bounce back the way they came, now yours) and explosions. Hits from behind or the
side still land.

**Eye of Agamotto:** hold right-click to turn back time around you. Green rune circles wind around your arm and a
great clock face spreads across the ground, 40 blocks across. Every block changed within 20 blocks in the last minute
runs backwards, newest first, faster the longer you hold: broken blocks come back, placed ones go, craters fill in,
and chests come back with what was in them. What spilled out of broken blocks in that time goes back too. Let go to
stop; the overlay shows how far back you've gone. Items someone already picked up stay picked up, so rewinding a
block whose drop you're holding gives you both.

Crafting (shaped):

```
Sling Ring:          Blaze Powder  Gold Ingot        Blaze Powder
                     Gold Ingot    ·                 Gold Ingot
                     Gold Ingot    Eye of Ender      Gold Ingot

Cloak of Levitation: Red Wool      Gold Ingot        Red Wool
                     Red Wool      Phantom Membrane  Red Wool
                     Feather       Red Wool          Feather

Eye of Agamotto:     Gold Ingot    String            Gold Ingot
                     Clock         Block of Emerald  Clock
                     Gold Ingot    Nether Star       Gold Ingot
```

It's needed on both the server and every player's client, and needs nothing but Fabric API. It works alongside
[Rings](https://github.com/ssan9876/minecraft-rings).

## Develop

Needs JDK 25.

| Command | What it does |
|---|---|
| `./gradlew build` | Builds `build/libs/mystic-<version>.jar` |
| `./gradlew runClient` | Starts Minecraft with the mod, in singleplayer |
| `./gradlew runClientGameTest` | Opens a real client and plays through every relic (`src/gametest`). Screenshots land in `build/run/clientGameTest/screenshots` |
| `./release.sh` | Publishes the version in `gradle.properties` as a GitHub release and points the players' mods list at it |
| `./deploy.sh` | Builds and installs the mod on the server, then restarts it |
| `python tools/gen_textures.py` | Redraws every texture: items, tunic, cloak, and the spell sigils |

## Releasing

Players get mods through the join scripts in
[minecraft-cloudflare-tunnel](https://github.com/ssan9876/minecraft-cloudflare-tunnel), which read the `mods.txt` in
the Rings repo. To ship a change:

1. Bump `version` in `gradle.properties` and commit.
2. `./release.sh` publishes the release and updates this mod's line in the Rings checkout's `mods.txt`
   (`../mc-mod/mods.txt`). Commit and push that there.
3. `./deploy.sh` installs it on the server.

Minecraft 26.x isn't obfuscated, so the code uses Mojang's own names, and `./gradlew genSources` gives readable
sources to look things up in.

## How it works

- **Portals:** a portal is an entity (`SlingPortal`), never saved, linked to its other end by id. Each tick it notes
  which side of its disc every nearby entity is on; anything that changed sides inside the rim is teleported to the
  same spot relative to the other disc, turned by the difference between their facings. Chunk tickets keep both ends
  loaded. `PortalRenderer` draws the rim as spinning textured sigils.
- **Cloak:** an equippable chest item whose equipment asset is the tunic. `CloakLayer` draws `CloakModel` (cloak,
  mantles, collar) on the player model, swung by vanilla's cape physics. `Cloak` grants flight and slows long falls;
  fall damage is cancelled in Fabric's `ALLOW_DAMAGE` event.
- **Shield:** the key sends a packet when it goes down and up, and the server sets an attachment that syncs to every
  client. `MandalaShield` bounces projectiles each tick and cancels damage coming from the front.
  `MysticEffects` draws it.
- **Time:** `LevelMixin` wraps every block change on the server and remembers the old state, with its block entity's
  data saved just before (`TimeRewind`). The drop mixins remember items spilled from blocks. Holding the eye walks
  back through the changes within reach, putting each back without updates or drops.
- **Poses:** `PlayerModelMixin` poses the arms for each spell and the legs for levitating, after vanilla's pose.
