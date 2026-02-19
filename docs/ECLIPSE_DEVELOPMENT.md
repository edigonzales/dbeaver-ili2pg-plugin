# Eclipse Development Guide for `ch.so.agi.dbeaver.ili2pg`

## Goal
Develop, start, and test as quickly as possible:
1. Change code in Eclipse.
2. Start DBeaver as an `Eclipse Application` directly from Eclipse.
3. Test the plugin in the runtime workbench without manual deployment.

## Prerequisites
- Eclipse IDE for RCP and RAP Developers
- Java 21
- Local DBeaver installation (macOS default): `/Applications/DBeaver.app/Contents/Eclipse`

---

## 1. Import Project into Eclipse

**Recommended:**
1. `File → Import → Gradle → Existing Gradle Project`
2. Select project folder: `/Users/stefan/sources/dbeaver-ili2pg-plugin`

**Note:** The runtime model remains PDE/OSGi. Gradle is primarily used for `downloadAndExtractIli2pg`, `test`, and `syncBundleLibs`.

---

## 2. Set Up Target Platform (Important)

1. Go to `Settings/Preferences → Plug-in Development → Target Platform`
2. Click `Add... → Installation`
3. Select the DBeaver Eclipse installation as root: `/Applications/DBeaver.app/Contents/Eclipse`
4. Set the new Target Platform as active (`Set as Active`)

**Alternative:** If needed, select only the `plugins` directory: `/Applications/DBeaver.app/Contents/Eclipse/plugins`

---

## 3. Synchronize Third-Party Libraries

When ili2pg dependencies change or `lib/` is missing:

```bash
./gradlew downloadAndExtractIli2pg
```

Then in Eclipse:
1. Right-click on project
2. `PDE Tools → Update Classpath`

**Important for OSGi Runtime:**
- `META-INF/MANIFEST.MF` must include all runtime JARs in `Bundle-ClassPath`
- `build.properties` must include `lib/` in `bin.includes`

The current project is preconfigured; `downloadAndExtractIli2pg` updates the `lib/` content.

After running `downloadAndExtractIli2pg`, use the output to update:
1. `META-INF/MANIFEST.MF` - Bundle-ClassPath section
2. The task prints the correct entries for copy-paste

---

## 4. Launch Configuration for Quick Feedback

1. `Run → Run Configurations...`
2. Create a new `Eclipse Application`, e.g., `DBeaver ili2pg Runtime`
3. **Tab `Main`:**
   - `Run an application`: `org.jkiss.dbeaver.ui.app.standalone.standalone`
   - `Location` (Workspace/Data):
     - Safe for tests: own temp workspace, e.g., `/tmp/dbeaver-ili2pg-runtime-workspace`
     - Or persistent DBeaver workspace, e.g., `/Users/stefan/Library/DBeaverData/workspace6`
4. **Tab `Plug-ins`:**
   - `Launch with: all workspace and enabled target plug-ins`
5. **Tab `Arguments` (recommended):**
   - Program arguments: `-clean -consoleLog`

Then click `Run`.

---

## 5. Daily Development Loop

1. Change code.
2. If dependencies changed: `./gradlew downloadAndExtractIli2pg` + `PDE Tools → Update Classpath`.
3. Start the launch configuration.
4. Test in runtime DBeaver:
   - Right-click on database connection → `ili2pg` → `Create schema…`
   - Right-click on schema → `ili2pg` → `Import data…` / `Export schema…` / `Validate schema…`
   - Preferences → `ili2pg`

---

## 6. Troubleshooting

### Unresolved bundles / ClassNotFound / NoClassDefFoundError
- Is the Target Platform active?
- Did you run `./gradlew downloadAndExtractIli2pg`?
- Is `Bundle-ClassPath` in `META-INF/MANIFEST.MF` complete?
- Does `build.properties` contain `lib/`?
- Did you run `PDE Tools → Update Classpath`?

### Plugin starts, but Commands/View are missing
- Check `plugin.xml` IDs/Classes.
- Ensure classes are actually in the output/JAR.

### Runtime-Workspace locked
- If the normal DBeaver is already running, do not use the same workspace in parallel.
- Better to use a separate workspace for development runs.

### Dropins doesn't work
In the current DBeaver configuration, `dropins` is not automatically reconciled. This is irrelevant for development because the PDE runtime starts directly from Eclipse.

---

## 7. Note on Custom Download Tasks

The `downloadAndExtractIli2pg` task follows this pattern:
1. Download ili2pg distribution artifact
2. Extract JAR files
3. Copy required JARs to `lib/`
4. Print `Bundle-ClassPath` and `build.properties` entries for manual update

This approach ensures that all third-party dependencies are properly bundled with the plugin and available at runtime.
