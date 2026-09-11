# Ethical Trading — first public beta

Server-side Fabric mod for Minecraft Java 26.2. Villagers trade according to actual sleep and sustained stress. Compact trading halls are allowed: an accessible bed per villager is sufficient; there are no floor-area or building requirements.

- Reversible quantity limits: one missed night without penalties, restrictions from two night-equivalents, strike at six by default.
- Real sleep restores fatigue-related trading ability. Assigned but inaccessible beds do not count.
- Brief scares have a grace period; sustained panic reduces quantities and calm recovers stress.
- Per-villager persistence; unloaded time adds no penalties; player-skipped nights are handled separately.
- Fatigue/fear reasons in server-supplied trading text; protection against taking stale trade results.

**Requires:** Java 25, Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API 0.159.0+26.2. Install on the server; clients do not need Ethical Trading. Back up your world before installing this beta.

**Verification:** ten Java tests and nine mod-specific Minecraft GameTests plus Fabric's baseline, including six real nights and a two-JVM disk restart. Manual graphical-client play and a large trading-hall load benchmark have not been performed. Custom dimensions and the integrated singleplayer server are not separately verified. This is why the release is marked beta.

Food/social bonuses, supply containers, player-specific retaliation, curing changes, NeoForge and older Minecraft versions are not included.
