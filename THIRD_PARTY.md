# Drittanbieter

Der Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) stammt aus Gradle 9.5.1 / dem offiziellen Fabric-Beispielprojekt. Für Gradle gelten die eigene Apache-2.0-Lizenz und Hinweise in `licenses/gradle-LICENSE.txt` und `licenses/gradle-NOTICE.txt`; die MIT-Lizenz dieses Projekts ersetzt diese nicht. Die Wrapper-JAR enthält außerdem ihre eigene `META-INF/LICENSE`.

Minecraft, Fabric Loader, Fabric API, Fabric Loom, JUnit und Java sind Build-/Laufzeitabhängigkeiten, nicht Bestandteil der Mod-JAR oder des Projekt-ZIP. Ihre jeweiligen Lizenzen und Minecraft-Nutzungsbedingungen gelten unabhängig. Der Wrapper lädt die festgelegte Gradle-Version per HTTPS mit SHA-256-Prüfung nach.

Die leere NBT-Teststruktur wurde mit dem mitgelieferten Python-Skript für dieses Projekt erzeugt. Es werden keine dekompilierten Minecraft-Quellen, fremden Spiel-Assets, Testwelten oder Zugangsdaten ausgeliefert.
