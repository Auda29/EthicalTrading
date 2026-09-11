# Umsetzungsplan

Ziel: Minecraft Java 26.2 / Fabric, zunächst nur der Kern.

1. Offizielle 26.2-Artefakte und Fabric-APIs prüfen; JDK 25 und gepinnte Build-Werkzeuge einrichten.
2. Loader-unabhängigen Zustandsautomaten mit Tests für Schlafschuld, Erholung, Stress, Mengenbegrenzung und persistente Snapshots entwickeln.
3. Serverseitige Villager-Ticks und Entity-NBT anbinden. Nacht-Sprung über den tatsächlichen Vanilla-Spieler-Schlafpfad erkennen.
4. Handelsmengen über berechnete Limits reduzieren, Originalangebote und gespeicherte Maximalmengen unverändert lassen. Veraltete Ergebnis-Slots absichern und Gründe anzeigen.
5. GameTests mit echter KI, Betten, Zombie, Händlerfenster, Chunk-Entladen und getrennten Serverprozessen ausführen. JAR und Dokumentation liefern.

## Architektur

- `core/`: reiner Java-Zustandsautomat und validierte Einstellungen; Ansatzpunkt für spätere Loader-Ports.
- `ConfigFile`: striktes JSON mit Default-Merge.
- `VillagerMixin`: ein O(1)-Schritt pro geladenem erwachsenem Villager-Tick; NBT, Handelshinweis, Fenstertitel.
- `ServerLevelMixin`: ein vorheriger Uhrwert pro Level-Tick; markiert den tatsächlichen Spieler-Nachtsprung ohne alle Villager zu durchsuchen.
- `AbstractVillagerMixin`: verbindet bestehende Angebote beim Abfragen mit dem Villager-Zustand.
- `MerchantOfferMixin`: berechnet wirksame Kapazität und Ausverkauft-Status; verändert weder `maxUses` noch Angebotsidentität. Vanillas Speicher-Codec schreibt die Originalfelder, ihr Netzwerk-Codec liest die wirksamen Getter.
- `MerchantResultSlotMixin` / `MerchantMenuMixin`: verhindern Entnahme bereits vorbereiteter, inzwischen gesperrter Ergebnisse.
- `src/gametest/`: separate Testmod, nicht Bestandteil der ausgelieferten Mod-JAR. Wiederholbarer Testursprung und Abgriff des serverseitigen Menütitels ausschließlich dort.

Kein weltweites Villager-Register, kein Bett-/Gebäude-Scanning, kein Pathfinding der Mod, keine periodischen Weltsuchen. Erreichbarkeit wird nicht geschätzt: Erst Vanillas tatsächlicher Schlafzustand zählt. Die Tests dürfen Chunks laden; die Produktionsmod tut das nicht.
