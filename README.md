# dbeaver-ili2pg-plugin

## todo
- Falls error -> gesamtes Logfile in "show details"

## develop

Das Projekt ist ein Eclipse-Projekt, d.h. kein Maven- oder Gradle-Projekt (auch wenn Gradle später für eine Aufgabe verwendet wird). Zum Entwicklen wird am besten "Eclipse IDE for RCP and RAP Developers" verwendet. 

Damit man für die Codeänderungen möglichst rasch ein Feedback bekommt, kann man via Eclipse eine dbeaver-Instanz starten und das Plugin dorthin deployen. Dafür müssen einige Einstellungen in Eclipse vorgenommen werden. Als erstes muss die Target Platform definiert werden `Settings` - `Plug-in Development` - `Target Platform` - `Add` ... Hier muss das `plugin`-Directory der dbeaver-Installation ausgewählt werden. Die neu erstellte Target Platform muss anschliessend explizit ausgewählt werden.

Ausführen muss man das Plugin-Projekt als `Eclipse Application`. Für den Workspace wählt man den Pfad zum Workspace dbeaver-Installation, z.B. unter macOS: `/Users/stefan/Library/DBeaverData/workspace6`. Dies ist optional aber notwendig, wenn man nicht mit einem leeren Workspace, d.h. ohne vordefinierte Datenbanken in dbeaver starten will. Unter `Run an Application` ist `org.jkiss.dbeaver.ui.app.standalone.standalone` auszuwählen. Jetzt wird bei jedem Ausführen des Plugins dbeaver gestartet und das Plugin deployed.

Benötigt das Plugin 3rd party libraries, muss (resp. kann neben anderen Varianten) man diese in das OSGi-Bundle packen. Im vorliegenden Fall übernimmt ein Gradle-Task `downloadAndExtractIli2pg` diese Aufgabe. Er lädt die die gezippte ili2pg-Datei herunter, entzippt sie und kopiert die Libraries in ein lib-Verzeichnis. Den Konsolenoutput des Tasks kann man auch für die MANIFEST.MF-Datei und für build.properties verwenden. Beiden müssen die Libraries bekannt gemacht werden. Falls notwendig: in Eclipse den Classpath updaten `Plug-in Tools` - `Update Classpath`.

## releasing / update site

Für die Update Site wird ein weiteres Eclipse-Projekt benötigt, das Feature-Projekt https://github.com/edigonzales/dbeaver-ilitools-feature. Ein Feature könnte mehrere Plugins haben (soweit ich es verstanden habe). Im Feature-Projekt muss man das dbeaver-Plugin hinzufügen. Sinnvollerweise erhalten beide Projekte die gleiche Versionsnummer. Jeder Release sollte eine neue Versionsnummer erhalten.

Das Feature-Projekt muss man exportieren: `Export` - `Deployable Features` - Directory wählen (siehe unten die Eclipse-Befehle). Anschliessend müssen die erstellten Dateien mit weiteren Metainformationen angereichert werden. Dazu kann man folgende Befehle mit Eclipse auf der Konsole ausführen. Die Pfade sind im Prinzip willkürlich.


Eclipse 2025-6:
```
/Users/stefan/apps/eclipse/rcp-2025-06/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository "file:/Users/stefan/tmp/updatesite/" \
  -artifactRepository "file:/Users/stefan/tmp/updatesite/" \
  -source "/Users/stefan/sources/dbeaver-ili2pg-plugin/build/update-input" \
  -compress \
  -publishArtifacts
```

(Achtung: falscher categoryDefinition-Pfad. Ist Zufall, dass es funktioniert)
```
/Users/stefan/apps/eclipse/rcp-2025-06/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.CategoryPublisher \
  -metadataRepository "file:/Users/stefan/tmp/updatesite/" \
  -categoryDefinition "file:/Users/stefan/Documents/eclipse-workspace-2025-06/ch.so.agi.dbeaver.ili2pg.feature/category.xml" \
  -categoryQualifier "interlis"
```

Eclipse 2025-9:
```
/Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
  -metadataRepository "file:/Users/stefan/tmp/updatesite/" \
  -artifactRepository "file:/Users/stefan/tmp/updatesite/" \
  -source "/Users/stefan/sources/dbeaver-ili2pg-plugin/build/update-input" \
  -compress \
  -publishArtifacts
```

```
/Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse \
  -nosplash -consolelog \
  -application org.eclipse.equinox.p2.publisher.CategoryPublisher \
  -metadataRepository "file:/Users/stefan/tmp/updatesite/" \
  -categoryDefinition "file:/Users/stefan/sources/dbeaver-ilitools-feature/category.xml" \
  -categoryQualifier "interlis"
```