# dbeaver-ili2pg-plugin

## todo
- Falls error -> gesamtes Logfile im show details


## develop

Das Projekt ist ein Eclipse-Projekt, d.h. kein Maven- oder Gradle-Projekt (auch wenn Gradle später für eine Aufgabe verwendet wird). Zum Entwicklen wird am besten "Eclipse IDE for RCP and RAP Developers" verwendet. 

Damit man für die Codeänderungen möglichst rasch ein Feedback bekommt, kann man via Eclipse eine dbeaver-Instanz starten und das Plugin dorthin deployen. Dafür müssen einige Einstellungen in Eclipse vorgenommen werden. Als erstes muss die Target Platform definiert werden `Settings` - `Plug-in Development` - `Target Platform` - `Add` ... Hier muss das `plugin`-Directory der dbeaver-Installation ausgewählt werden. Die neu erstellte Target Platform muss anschliessend explizit ausgewählt werden.

Ausführen muss man das Plugin als `Eclipse Application`. Für den Workspace wählt man den Pfad zum Workspace dbeaver-Installation, z.B. unter macOS: `/Users/stefan/Library/DBeaverData/workspace6`. Dies ist optional aber notwendig, wenn man nicht mit einem leeren Workspace, d.h. ohne vordefinierte Datenbanken in dbeaver starten will.

Workspace:
```
/Users/stefan/Library/DBeaverData/workspace6
```

Run as application:
```
org.jkiss.dbeaver.ui.app.standalone.standalone
```

Target Platform (vorgängig definieren):

- Settings - Plug-in Development - Target Platform - Add ...
- plugin-Directory von dbeaver auswählen

Third party libs:

- Mit build.gradle herunterladen
- Konsolenoutput verwenden für MANIFEST.MF und build.properties
- Plug-in Tools - Update Classpath...

## update site

Leider noch manuell:

1. Feature-Projekt: Export -> Deployable Features -> Directory wählen 
2. Plugin-Projekt: Export -> Deployable plug-ins and fragments -> Directory wählen

Frage: Ist (2) überhaupt notwendig? (1) scheint auch gleich Plugin zu exportieren.

Beide sollten die gleiche Versionsnummer aufweisen.

FIXME:

```
/Users/stefan/apps/eclipse/rcp-2025-06/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository "file:/Users/stefan/tmp/updatesite/" \
  -artifactRepository "file:/Users/stefan/tmp/updatesite/" \
  -source "/Users/stefan/sources/dbeaver-ili2pg-plugin/build/update-input" \
  -compress \
  -publishArtifacts


/Users/stefan/apps/eclipse/rcp-2025-06/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.CategoryPublisher \
  -metadataRepository "file:/Users/stefan/tmp/updatesite/" \
  -categoryDefinition "file:/Users/stefan/Documents/eclipse-workspace-2025-06/ch.so.agi.dbeaver.ili2pg.feature/category.xml" \
  -categoryQualifier "interlis"
```

```
/Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository "file:/Users/stefan/tmp/updatesite/" \
  -artifactRepository "file:/Users/stefan/tmp/updatesite/" \
  -source "/Users/stefan/sources/dbeaver-ili2pg-plugin/build/update-input" \
  -compress \
  -publishArtifacts


/Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.CategoryPublisher \
  -metadataRepository "file:/Users/stefan/tmp/updatesite/" \
  -categoryDefinition "file:/Users/stefan/sources/dbeaver-ilitools-feature/category.xml" \
  -categoryQualifier "interlis"
```