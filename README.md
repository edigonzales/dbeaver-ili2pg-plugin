# dbeaver-ili2pg-plugin

## todo
- Falls error -> gesamtes Logfile im show details


## develop

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