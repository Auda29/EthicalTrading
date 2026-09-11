# Ethical Trading — Fabric 26.2

Serverseitige Minecraft-Java-Mod, Version **0.1.0+mc26.2**. Villager handeln abhängig von tatsächlichem Schlaf und anhaltender Panik. Keine Mindestfläche, kein Gebäudecheck, kein Futterzwang: Ein eigenes erreichbares Bett pro Villager genügt. Arbeitsplatz und die normalen Vanilla-Voraussetzungen für Nachfüllen bleiben nötig.

## Installation

1. Welt sichern; zum ersten Ausprobieren eine Kopie verwenden.
2. **Minecraft Java 26.2**, **Java 25**, **Fabric Loader 0.19.5 oder neuer** installieren.
3. **Fabric API 0.159.0+26.2** und `ethical-trading-0.1.0+mc26.2.jar` in den `mods`-Ordner des Servers kopieren.
4. Server starten. `config/ethical-trading.json` wird angelegt.
5. Clients brauchen weder diese Mod noch ein Ressourcenpaket. Texte werden vom Server geliefert. Für Einzelspieler die Mod und Fabric API lokal installieren; die Logik läuft auf dem integrierten Server. Diese Betriebsart ist noch nicht separat getestet.

Keine Unterstützung für Bedrock, NeoForge oder Minecraft 1.21.11 in dieser Datei. Nicht die `-sources.jar` installieren.

## Verhalten

- Nur echte geladene Villager-Ticks zählen, nicht Echtzeit oder vergangene Welt-Tage. Die Mod liest den wirklichen Schlafzustand, nicht die HOME-Bettzuweisung.
- Standardmäßig entsprechen **12.000 geladene wache Ruhezeit-Ticks** einer versäumten Nacht. Teils geladene Nächte zählen anteilig. Die 26.2-Vanilla-Ruheaktivität dauert ungefähr eine halbe Minecraft-Tageslänge; ihre ersten Morgen-Ticks zählen ebenfalls mit.
- Bei weniger als zwei solchen Nächten kein Mengenabzug; bei zwei 80 %, drei 60 %, vier 40 %, fünf 20 %, ab sechs Streik.
- **200 zusammenhängende echte Schlaf-Ticks** beseitigen Schlafmangel vollständig. Stress muss separat in Ruhe abklingen. Unterbrochene Schlafhäppchen werden nicht addiert.
- Bei echtem Überspringen der Nacht durch Spieler genügen **20 vorher beobachtete zusammenhängende Schlaf-Ticks**, sofern der Villager beim Sprung noch schläft. Ein wacher Villager bekommt keine erfundene Erholung. Die übersprungene Zeit erzeugt keinerlei zusätzliche Schuld; die tatsächlich wach verbrachten Ticks davor zählen weiterhin.
- Kurze Panik bleibt bis einschließlich 600 aufgesammelter Stress-Ticks folgenlos. Anhaltende oder wiederholte Panik erhöht Stress; ruhige Ticks bauen ihn wieder ab. Fünf Mengenstufen bis maximal 50 % Abzug.
- Schlaf- und Stressabzug werden **nicht multipliziert**: Es gilt nur die stärkere Einschränkung. Auf ganze Handelsvorgänge wird abgerundet; mindestens einer bleibt möglich, bis ein Streik erreicht ist.
- Mengen gelten **pro Angebot und Vanilla-Nachfüllzyklus**, nicht pro Öffnen oder Spieler. Bereits verbrauchte Nutzungen bleiben verbraucht. Erholung erhöht wieder die erlaubte Menge, füllt aber keine normal ausverkauften Angebote magisch auf.
- „Übermüdet“ / „Verängstigt“ erscheint im Fenstertitel; bei Handelsversuchen gibt es einen Hinweis. Bei Streik öffnet sich kein Handelsfenster. Ändert sich die Mengenstufe während eines Handels, schließt die Mod das Fenster sicher; erneut ansprechen. Vanilla gibt die eingesetzten Gegenstände dabei zurück.

Schlafmangel entsteht nur in Dimensionen, in denen Vanilla das Überschlafen von Nächten erlaubt. Babys sammeln keine neuen Nachteile. Keine Umgebungssuche und kein künstliches Laden von Chunks.

## Konfiguration und Tests

- [CONFIGURATION.md](CONFIGURATION.md): alle Einstellungen und Grenzen.
- [TESTING.md](TESTING.md): tatsächlich ausgeführte Tests und offene Abdeckung.
- [PLAN.md](PLAN.md): Umsetzungsplan und Architektur.
- [PUBLISHING.md](PUBLISHING.md): GitHub-, Modrinth- und CurseForge-Veröffentlichung.

## Selbst bauen

Voraussetzung: JDK 25, Internet für den ersten Dependency-Download. Gradle Wrapper ist enthalten.

```sh
./gradlew build
```

Windows: `gradlew.bat build`. Ausgabe in `build/libs/`. `build` enthält Unit- und einen GameTest-Serverdurchlauf. Mit dem Test-Setup wird die Minecraft-EULA für die Testinstanz akzeptiert (`eula = true` in `build.gradle`); vor der Ausführung prüfen.

Für den zusätzlichen echten Neustarttest (zwei getrennte Server-JVMs, Python 3):

```sh
./gradlew test assemble
python scripts/run-gametests.py --jar build/libs/ethical-trading-0.1.0+mc26.2.jar
```

Der sechs Nächte lange Abnahmetest lässt echte Server- und Weltzeit-Ticks durchlaufen; je nach Rechner kann die Suite mehrere Minuten benötigen. Der Runner hält Gradle und Minecraft nicht gleichzeitig im Speicher. Er verwendet ausschließlich `build/run/gameTest/`, erzeugt dort eine Testwelt und verändert keine reguläre Spielwelt. Die Kotlin/Java-Entwicklungsquellen von Minecraft werden nicht mitgeliefert. Plattform-Uploads erfolgen ausschließlich aus geprüften GitHub-Release-Assets; Einrichtung und Freigabestatus stehen in `PUBLISHING.md`.

## Nicht enthalten

Nahrungs-/Kontaktboni, Versorgungsbehälter, generelles nächtliches Handelsverbot, spielerbezogene Strafe nach Schlägen und zusätzliche Änderungen an Zombifizierung/Heilung. Diese optionalen Mechaniken und Ports auf NeoForge oder ältere Versionen sind spätere Arbeit. Die normale Vanilla-Heilungsmechanik bleibt unverändert.

## Entfernen

Server stoppen und nur die Ethical-Trading-JAR entfernen. Vanilla-Angebote behalten ihre ursprünglichen Maximalmengen, Preise und Nutzungszähler. Ohne die Mod können beim nächsten Speichern die zusätzlichen Welfare-NBT-Daten verloren gehen; vorübergehendes Deinstallieren ist kein unterstützter Weg, um den Zustand zu konservieren.

MIT-Lizenz. Minecraft ist ein Produkt von Mojang/Microsoft; dieses Projekt ist nicht offiziell.
