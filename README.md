# DBeaver ILI2PG Plugin (`ch.so.agi.dbeaver.ili2pg`)

DBeaver-Plugin für INTERLIS-Datenbankoperationen mit ili2pg.

## Features

- **Create schema**: Generiert Datenbank-Schema aus INTERLIS-Modellen
- **Import data**: Importiert INTERLIS-Daten (XTF, XML) in PostgreSQL
- **Export schema**: Exportiert Datenbank-Schema als INTERLIS-Modell
- **Export schema with options**: Schema-Export mit erweiterten Optionen
- **Validate schema**: Validiert Datenbank gegen INTERLIS-Modelle

## Projektstruktur

- Java-Quellen: `./src/ch/so/agi/dbeaver/ili2pg`
- OSGi/Plugin-Metadaten:
  - `./plugin.xml`
  - `./META-INF/MANIFEST.MF`
  - `./build.properties`

## Build und Test

### Voraussetzungen

- Java 21
- Lokale DBeaver-Installation (Standardpfad auf macOS):
  - `/Applications/DBeaver.app/Contents/Eclipse/plugins`
- Optional anderer Plugin-Pfad:
  - Gradle Property: `-PdbeaverPluginsDir=/path/to/plugins`
  - oder Env-Var: `DBEAVER_PLUGINS_DIR=/path/to/plugins`

### Kommandos

```bash
./gradlew downloadAndExtractIli2pg
./gradlew syncBundleLibs
./gradlew printBundleClassPath
./gradlew clean test
```

## Entwicklung in Eclipse (PDE)

Detaillierte Schritt-für-Schritt-Anleitung für Target Platform, Launch Configuration, Runtime-Loop und Troubleshooting:
- [Eclipse Development Guide](docs/ECLIPSE_DEVELOPMENT.md)

Kurz:
1. In Eclipse (RCP/RAP) importieren.
2. DBeaver-Installation als aktive PDE Target Platform setzen.
3. Bei Lib-Änderungen `./gradlew downloadAndExtractIli2pg` und `./gradlew syncBundleLibs` ausführen und danach `PDE Tools -> Update Classpath`.
4. Als `Eclipse Application` mit `org.jkiss.dbeaver.ui.app.standalone.standalone` starten.

## Release / Update Site

Die Veröffentlichung auf der gemeinsamen Update-Site (`ili2pg` + `AI`) ist hier beschrieben:
- [Releasing / Update Site](docs/RELEASING_UPDATE_SITE.md)

## DBeaver Preferences

Preference Page ID: `ch.so.agi.dbeaver.ili2pg.prefs`

| UI Feld | Key | Default | Bedeutung / Verhalten |
|---------|-----|---------|----------------------|
| ili2pg Home | `ch.so.agi.dbeaver.ili2pg.home` | (auto) | Verzeichnis der ili2pg-Installation. Bei leerem Wert wird das im Plugin gebündelte ili2pg verwendet. |
| Java Executable | `ch.so.agi.dbeaver.ili2pg.java` | (system) | Pfad zur Java-Executable für ili2pg-Aufrufe. Bei leerem Wert wird `java` aus `PATH` verwendet. |
| Log Level | `ch.so.agi.dbeaver.ili2pg.logLevel` | `INFO` | Logging-Level für ili2pg-Ausgaben (`INFO`, `DEBUG`, `WARNING`, `ERROR`). |
| Create Schema Options | `ch.so.agi.dbeaver.ili2pg.createSchemaOptions` | `--createSchema` | Standard-Optionen für Schema-Erstellung. |
| Import Data Options | `ch.so.agi.dbeaver.ili2pg.importDataOptions` | `--import` | Standard-Optionen für Daten-Import. |
| Export Schema Options | `ch.so.agi.dbeaver.ili2pg.exportSchemaOptions` | `--export` | Standard-Optionen für Schema-Export. |

Hinweis: Die tatsächlichen Preference-Keys und Defaults können je nach Implementierung variieren. Siehe `Ili2pgPreferenceInitializer` für die definitive Liste.

## Nutzung

1. **Schema erstellen**:
   - Rechtsklick auf Datenbank-Connection → `ili2pg` → `Create schema…`
   - INTERLIS-Modell und Optionen auswählen
   - ili2pg erstellt das Schema in der Datenbank

2. **Daten importieren**:
   - Rechtsklick auf Schema → `ili2pg` → `Import data…`
   - XTF/XML-Datei auswählen
   - ili2pg importiert die Daten ins Schema

3. **Schema exportieren**:
   - Rechtsklick auf Schema → `ili2pg` → `Export schema…`
   - Zieldatei und Optionen auswählen
   - ili2pg exportiert das Schema als INTERLIS-Modell

4. **Schema validieren**:
   - Rechtsklick auf Schema → `ili2pg` → `Validate schema…`
   - Validierungsoptionen auswählen
   - ili2pg validiert das Schema gegen die INTERLIS-Modelle

## Hinweise

- **ili2pg Libraries**: Das Plugin enthält gebündelte ili2pg-Libraries im `lib/`-Verzeichnis. Diese werden via Gradle-Task `downloadAndExtractIli2pg` aktualisiert.
- **Bundle-ClassPath**: Bei Änderungen an `lib/` muss `META-INF/MANIFEST.MF` (Bundle-ClassPath) und `build.properties` aktualisiert werden. Der Task `printBundleClassPath` generiert die korrekten Einträge.
- **Error Log**: Detaillierte Fehlermeldungen sind im DBeaver `Error Log` (Window → Show View → Error Log) sichtbar.
- **PostgreSQL-Treiber**: Das Plugin enthält einen PostgreSQL-Treiber im `lib/`-Verzeichnis. Dieser wird für die ili2pg-Integration benötigt.

## Architektur

Details zur Plugin-Architektur:
- [Architecture](docs/ARCHITECTURE.md)

## About

Plugin für die Arbeit mit INTERLIS-Daten in DBeaver. Entwickelt vom Amt für Geoinformation des Kantons Solothurn.

## Resources

- [ili2pg Dokumentation](https://www.interlis.ch/ili2pg/)
- [INTERLIS](https://www.interlis.ch/)
- [DBeaver](https://dbeaver.io/)
