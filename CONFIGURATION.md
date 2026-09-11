# Konfiguration

Datei: `config/ethical-trading.json`, beim ersten Start automatisch erzeugt. Nach Änderungen **Server neu starten**; kein Live-Reload. Fehlende Schlüssel übernehmen Standardwerte. Unbekannte Schlüssel, `null`, falsche Typen und ungültige Grenzen brechen den Start mit einer Fehlermeldung ab, statt die Datei unbemerkt zurückzusetzen.

```json
{
  "sleepEnabled": true,
  "stressEnabled": true,
  "nightTicks": 12000,
  "firstRestrictedNight": 2,
  "strikeNight": 6,
  "sleepRequiredTicks": 200,
  "skippedNightSleepTicks": 20,
  "stressGraceTicks": 600,
  "stressMaxTicks": 2400,
  "calmRecoveryPerTick": 1,
  "maximumStressPenalty": 0.5
}
```

| Schlüssel | Bedeutung / zulässiger Bereich |
|---|---|
| `sleepEnabled` | Schlafmangel zählen und anwenden; Boolean. Ausschalten hebt den Mengenabzug auf. Vorhandener Zustand bleibt gespeichert; echter Schlaf kann ihn abbauen. |
| `stressEnabled` | Stress zählen und anwenden; Boolean. Ausgeschaltet wird der vorhandene Stress eingefroren und nicht angewendet. |
| `nightTicks` | Wache, geladene Ruhezeit-Ticks pro Nachtäquivalent; 1–2.400.000. Das ist keine Wartezeit seit dem letzten Serverstart. |
| `firstRestrictedNight` | Erstes vollständiges Nachtäquivalent mit Mengenabzug; mindestens 1 und höchstens `strikeNight`. |
| `strikeNight` | Vollständiger Schlafmangel-Streik ab dieser Anzahl; mindestens `firstRestrictedNight`, höchstens 10.000. |
| `sleepRequiredTicks` | Zusammenhängende tatsächliche Schlaf-Ticks zur Erholung; 1–2.400.000. 200 Ticks sind bei 20 TPS zehn Sekunden. |
| `skippedNightSleepTicks` | Erforderliche bereits beobachtete Schlafphase beim Spieler-Nachtsprung; 1 bis `sleepRequiredTicks`. Kein Kredit für nur zugewiesene Betten. |
| `stressGraceTicks` | Bis einschließlich dieses Stresswerts kein Abzug; mindestens 0 und kleiner als `stressMaxTicks`. |
| `stressMaxTicks` | Maximale Stressansammlung; größer als `stressGraceTicks`, höchstens 2.400.000. Pro Panik-Tick kommt ein Stress-Tick dazu. |
| `calmRecoveryPerTick` | Stressabbau je ruhigem geladenen Tick; 1–2.400.000. Standardmäßig dauert Abbau von Maximum bis null 2.400 ruhige Ticks. |
| `maximumStressPenalty` | Maximaler Stress-Mengenabzug als Anteil, 0–1. `0.5` begrenzt ihn auf 50 %, `0` deaktiviert den Effekt, `1` erlaubt auch stressbedingten Streik. |

## Berechnung

`n = floor(missedSleepTicks / nightTicks)`. Unterhalb `firstRestrictedNight` bleibt der Schlaf-Faktor 1, ab `strikeNight` wird er 0. Dazwischen gilt `(strikeNight - n) / (strikeNight - firstRestrictedNight + 1)`.

Stress oberhalb der Schonzone wird auf fünf gleich große Stufen zwischen Schonzone und Maximum aufgerundet. Die maximale Stärke jeder Stufe hängt von `maximumStressPenalty` ab. Stress und Schlaf werden per Minimum der verbleibenden Mengenfaktoren kombiniert, nie per Multiplikation. Handelsmengen werden abgerundet, mit mindestens einer Nutzung pro Angebot außerhalb eines Streiks. Preise, Nachfrage, XP und freigeschaltete Angebote werden nicht umgewürfelt.

## Speicherdaten

Je Villager im zusätzlichen Entity-NBT-Compound `ethical_trading`: `version`, `missed_sleep_ticks`, `stress_ticks`, `sleep_streak`. Keine persistierten Wall-Clock-Zeitstempel und keine Offline-Nachberechnung. Beim Lesen werden negative oder übergroße Werte auf gültige Grenzen begrenzt. Ein Neustart konserviert auch die bereits beobachtete Teilschlafphase, wenn Vanilla den Villager weiterhin schlafend lädt; ein wacher Tick unterbricht sie.

`/time`-Änderungen erzeugen weder künstliche Schuld noch besondere Erholung. Die Sonderregel für kurzen Schlaf ist ausschließlich mit Vanillas Spieler-Schlafsprung verbunden. Eingefrorene Nachtzeit bei laufenden Entity-Ticks zählt dagegen weiterhin als wach verbrachte Ruhezeit. Server-Tick-Freeze und ungeladene Chunks liefern keine Entity-Ticks und daher keine neuen Nachteile.
