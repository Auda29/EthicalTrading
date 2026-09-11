# Testbericht — Ethical Trading 0.1.0+mc26.2

## Umfang und klare Abgrenzung

Zielplattform: Minecraft Java **26.2**, Fabric Loader **0.19.5**, Fabric API **0.159.0+26.2**, JDK **25.0.4.1**, Loom **1.17.20**, Gradle **9.5.1**. Die Entwicklung und Tests laufen unter Linux. Die Tests verwenden Standardkonfiguration; ungültige und teilweise angegebene Konfigurationen werden separat geprüft.

**Manuelle Ingame-Tests mit grafischem Client: nicht durchgeführt.** Auch integrierter Einzelspielerserver, Installation unter Windows, umfangreiche Modpacks und ein Lastbenchmark großer Trading Halls wurden nicht geprüft. Serverseitiges Abgreifen eines Menütitels ist kein Nachweis seiner sichtbaren Darstellung im Client.

Die unten beschriebenen automatisierten Prüfungen sind echte Ausführungen, keine erfundenen Spielabläufe. Das vollständige Projekt-ZIP enthält die maßgeblichen Ausgaben unter `evidence/`. Die abschließende Ergebnisdatei `evidence/verification.json` bindet die Ergebnisse an den SHA-256 der ausgelieferten JAR.

## Reproduzieren

JDK 25 einstellen, beispielsweise über `JAVA_HOME`, und aus dem Projektverzeichnis ausführen:

```sh
./gradlew --no-daemon test assemble
python scripts/run-gametests.py --jar build/libs/ethical-trading-0.1.0+mc26.2.jar
```

Der zweite Befehl kompiliert die separate Testmod, beendet Gradle und startet **zwei getrennte Minecraft-Server-JVMs** mit derselben Testwelt. Die normalen Produktions-Klassen- und Ressourcenverzeichnisse werden aus dem Test-Classpath entfernt und durch die gebaute JAR ersetzt. Ihr Hash wird vor dem Start protokolliert. Fabric meldet die geladene Mod und ihre Version.

Das ist eine Prüfung der installierbaren Mod-JAR in einer Fabric-Entwicklungs-/GameTest-Umgebung, kein separat mit dem Fabric-Installer aufgesetzter Produktionsserver und kein grafischer Clientlauf. Die Testwelt liegt ausschließlich unter `build/run/gameTest/`. Die Konfiguration des Testservers akzeptiert die Minecraft-EULA; die Bedingungen vor eigener Ausführung prüfen.

## 1. Automatisierte Java-Logiktests

**10 Tests**, verteilt auf `WelfareStateTest` (8) und `ConfigTest` (2):

- Eine Nacht ohne Abzug, ab zwei Einschränkung, ab sechs Streik.
- Erholung nach ausreichendem tatsächlichem Schlaf; Bettzuweisung allein genügt nicht.
- Unterbrochene kurze Schlafphasen ergeben keine zusammenhängende Erholung.
- Kurze und wiederholte Schreckmomente, Stress-Schonzone und ruhiger Abbau.
- Kombination nimmt nur den stärkeren Abzug; kleine Angebote bleiben außerhalb eines Streiks mindestens einmal nutzbar.
- Snapshot-Roundtrip für Schuld, Stress und Teilschlaf ohne Offline-Nachberechnung.
- Kein Schlafkredit für wache Villager beim Überspringen; zu kurze Schlafphase zählt nicht.
- Tageszeit allein erzeugt keine Schlafschuld.
- Automatische Konfigurationsdatei, fehlende Schlüssel mit Defaults, strikte Fehler bei ungültigen Eingaben.

Belege: `evidence/final-build.log` und JUnit-XML-Dateien unter `evidence/unit/`. Ein `UP-TO-DATE` im Build bedeutet Wiederverwendung bereits erfolgreicher Tests unveränderter Eingaben, nicht einen neu ausgeführten Lauf; die XML-Dateien enthalten den tatsächlichen Testzeitpunkt.

## 2. Automatisierte Minecraft-Servertests

**9 eigene GameTests plus 1 Fabric-Basistest pro Durchlauf.** Kein Test behauptet manuelle Bedienung. Wo schnelle gezielte Grenzprüfungen die Zustandslogik vorbefüllen, ist das hier ausgewiesen.

| Eigener Test | Tatsächlich geprüfter Pfad / Einschränkung |
|---|---|
| `sixRealNightsProgressFromFreeTradingToStrike` | Frischer Villager ohne Schuld; echte Welt- und Serverzeit über sechs volle Tages-/Nachtzyklen. Keine direkten Core-Ticks, kein Vorbefüllen, kein Zeitsprung nach Testbeginn. NoAI isoliert den Nachtsensor von Wandern und Bedrohungen. |
| `reachableBedSleepsNaturallyButBlockedBedDoesNot` | Normale Vanilla-Villager-KI schläft im erreichbaren Bett und erholt sich. Zugewiesenes, durch Blöcke unerreichbares Bett bewirkt keine Erholung. Anfangszustand für beide gezielt auf Streik gesetzt. |
| `naturalZombiePanicHasGraceAndCalmRecovers` | Echte KI erkennt einen Zombie. Anfangs keine Handelsstrafe, anhaltende Panik reduziert Mengen; Entfernen des Zombies und ruhige Ticks beseitigen Stress. Bewegungsattribute verhindern unkontrolliertes Weglaufen. |
| `actualPlayerSkipCreditsShortRealSleepButNotAwakeVillager` | Ein serverseitiger Testspieler löst Vanillas tatsächlichen Schlafsprung aus. Vorher beobachteter kurzer Villager-Schlaf kann erholen, wacher Villager bleibt unverändert. Villager-Schlaf und Ausgangsschuld gezielt eingerichtet; kein grafischer Spieler. |
| `realTradeInteractionShowsReasonAndRejectsStrike` | Echte `mobInteract`-/Menüeröffnung, serverseitiger Titel mit Grund, Öffnungsverweigerung im Streik und Wiederöffnung nach Erholung. Mengenwechsel schließt ein bereits offenes Menü. Grenzen gezielt vorbefüllt. |
| `staleResultCannotBeTakenByClickOrShiftClick` | Bereits vorbereitetes Händler-Ergebnis nach Streik weder per normalem Klick noch Shift-Klick entnehmbar; Zahlung und Nutzungszähler unverändert. |
| `entityNbtRoundTripPreservesWelfareAndOriginalOffer` | Minecraft-Entity-NBT konserviert Schlafschuld, positiven Stress und Angebot. Nach Erholung originaler Maximalwert und tatsächliche Nutzungen erhalten. Dies ist ein NBT-Roundtrip, kein eigenständiger Neustarttest. |
| `villagerReceivesPersistentStateAndReversibleOfferLimits` | Echte Angebotsinstanz bleibt erhalten; wirksame Menge sinkt, reale Nutzungen bleiben, Erholung gibt verbleibende Nutzungen frei. |
| `realChunkUnloadReloadAndServerRestart` | Originalentity mit `UNLOADED_TO_CHUNK` entfernt; neuer Entity-Datensatz mit gleicher UUID aus Chunk geladen. 20 währenddessen übersprungene Clock-Tage ändern die Schlafschuld nicht. Zweite Server-JVM lädt dieselbe UUID aus derselben gespeicherten Welt. Der Disk-Test konserviert Schlafschuld und Angebotsnutzungen; positiver Stress wird im separaten NBT-Test geprüft. |

### Echte Nachtgrenzen

Die Standard-Vanilla-REST-Phase in 26.2 umfasst auch die ersten zehn Morgen-Ticks. Die Mod zählt diese ehrlich als beobachtete Ruhezeit; konfiguriert sind 12.000 wache Ruhezeit-Ticks pro Nachtäquivalent. Dadurch ergeben sich bis zum begrenzten Maximum folgende Messwerte für ein Angebot mit zehn ursprünglichen Nutzungen:

| Durchlaufene Nächte | Tatsächliche Server-Ticks | Schlafschuld-Ticks | Verfügbare Maximalmenge |
|---:|---:|---:|---:|
| 1 | 24000 | 12010 | 10 |
| 2 | 48000 | 24020 | 8 |
| 3 | 72000 | 36030 | 6 |
| 4 | 96000 | 48040 | 4 |
| 5 | 120000 | 60050 | 2 |
| 6 | 144000 | 72000 | 0 |

Die Rohzeilen heißen `[ETHICAL-NIGHTS]`. Dieser Test schließt die zuvor von der unabhängigen Prüfung benannte Lücke zwischen isolierter Logik und realen Nachtgrenzen. Er testet absichtlich die laufende Vanilla-Zeit und nicht sechs Aufrufe eines erfundenen „Nacht vergangen“-Events.

### Testaufbau und frühere Fehlversuche

- Die vollständige Quellcodegenerierung von Minecraft überschritt das Speicherbudget. Stattdessen wurden benötigte APIs gezielt geprüft; Build und reale Serverausführung sind anschließend gelungen.
- Nach einem direkten Uhrzeitwechsel im Test war die Tageshelligkeit noch zwischengespeichert. Das weckte den Testspieler vorzeitig. Der Test aktualisiert jetzt Environment-Cache und Helligkeit, bevor er den Spieler tickt.
- Ein Chunk kann aus der Entity-Sichtbarkeit verschwinden, bevor sein Datenträger-Unload abgeschlossen ist. Der Test wartet deshalb zusätzlich auf den tatsächlichen Removal-Grund, bevor er erneut lädt. Er ersetzt das Entladen nicht durch bloßes Serialisieren.
- Nach einem echten Serverneustart kann der Chunk bereits verfügbar sein, während die Entity-NBT-Daten noch asynchron geladen werden. Die frühere feste Prüfung bei Tick 40 scheiterte deshalb im Release-Lauf. Der Test wartet nun innerhalb der unveränderten Grenze von 1600 Ticks auf dieselbe gespeicherte UUID und prüft erst danach unverändert den Zustand und die Angebotsnutzungen. Eine separate Testmod-Injektion hält ausschließlich das echte Ladeergebnis des Fixture-Chunks bis Tick 120 zurück; sie erzeugt weder Ersatzentities noch Ersatzdaten. Mit dieser Verzögerung scheiterte die alte Prüfung reproduzierbar bei Tick 40, die korrigierte Prüfung bestand in zwei getrennten JVMs. Die Produktionsmod wurde dafür nicht geändert.
- Gradle weist auf künftig inkompatible veraltete Features hin. Getestet und gepinnt ist Gradle 9.5.1, nicht Gradle 10.

## 3. Offene manuelle Abnahme und Risiken

Noch **nicht durchgeführt**:

1. Vanilla-Client ohne Mod verbinden; Fenstertitel und Aktionsleisten-Hinweise visuell auf Lesbarkeit prüfen.
2. Im offenen Handelsfenster eine Mengenstufe wechseln lassen; sichtbare Schließung, Rückgabe der eingesetzten Gegenstände und anschließendes Wiederöffnen prüfen.
3. Wiederholte Interaktion bei Netzwerklatenz und mit mehreren echten Spielern.
4. Langzeitbetrieb einer großen Trading Hall messen. Die Produktionslogik ist O(1) pro geladenem Villager-Tick, ohne Bett-/Gebäudesuche und ohne zusätzliches Laden von Chunks; das ersetzt keinen TPS-Benchmark.
5. Einzelspieler sowie benutzerdefinierte Dimensionen mit anderen Clock-/Schlafmarkern. Der reguläre Overworld-Pfad ist geprüft; Sonderkonfigurationen sind nicht abgenommen.

Die unabhängige statische Prüfung meldete keinen konkreten Produktionsbug, aber die damals fehlenden realen Nachtgrenzen, manuelle GUI-Abnahme und den fehlenden Testbericht. Nachtgrenzen und Bericht wurden anschließend ergänzt. Die übrigen Lücken bleiben ausdrücklich offen; die ältere Prüfung ist keine zusätzliche Freigabe dieser späteren Änderungen.

## Ergebnisbindung

Maßgeblich sind `evidence/verification.json`, `evidence/final-packaged-tests.log` und die mitgelieferte `SHA256SUMS`. Die GameTest-Testmod ist **nicht** in der ausgelieferten Mod-JAR enthalten. Optionale Mechaniken und andere Loader/Versionen wurden weder implementiert noch als getestet ausgegeben.
