# Releasing / Update Site Guide

This guide describes the manual generation of the shared p2 update site for:
- `ch.so.agi.dbeaver.ili2pg.feature`
- `ch.so.agi.dbeaver.ai.feature`

The finished artifacts (`content.jar`, `artifacts.jar`, `features/`, `plugins/`) are then manually uploaded to `https://dbeaver.sogeo.services/updates/`.

---

## One-Time Setup

1. **Feature Project exists:**
   - Path: `/Users/stefan/sources/dbeaver-ilitools-feature`
   - Feature-ID: `ch.so.agi.dbeaver.ili2pg.feature`

2. **Category definition:**
   - File: `/Users/stefan/sources/dbeaver-ilitools-feature/category.xml`
   - Categories:
     - `interlis` for `ili2pg`
     - `ai` for `AI`

---

## Release Steps

1. **Set versions:**
   - Plugin `META-INF/MANIFEST.MF`: `Bundle-Version: x.y.z.qualifier`
   - Feature `feature.xml`: `version="x.y.z.qualifier"`
   - Increment version for each release

2. **Use empty staging directory:**
   - `/Users/stefan/sources/dbeaver-release-staging/update-input`

3. **In Eclipse, export features to the same staging directory:**
   - `File → Export → Deployable Features`
   - Select `ch.so.agi.dbeaver.ili2pg.feature`
   - Select destination: staging directory
   - Repeat for `ch.so.agi.dbeaver.ai.feature` (if releasing AI plugin as well)

4. **Check staging content:**
   - `features/ch.so.agi.dbeaver.ili2pg.feature_.jar`
   - `plugins/ch.so.agi.dbeaver.ili2pg_.jar`
   - (AI plugin artifacts if applicable)

5. **Generate p2 repository:**

   **Eclipse 2025-09:**
   ```bash
   /Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse \
     -nosplash -consolelog \
     -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher \
     -metadataRepository "file:/tmp/dbeaver-updatesite/" \
     -artifactRepository "file:/tmp/dbeaver-updatesite/" \
     -source "/Users/stefan/sources/dbeaver-release-staging/update-input" \
     -compress \
     -publishArtifacts
   ```

6. **Apply categories:**

   **Note:** Ensure correct categoryDefinition path.
   ```bash
   /Users/stefan/apps/eclipse/rcp-2025-09/Eclipse.app/Contents/MacOS/eclipse \
     -nosplash -consolelog \
     -application org.eclipse.equinox.p2.publisher.CategoryPublisher \
     -metadataRepository "file:/tmp/dbeaver-updatesite/" \
     -categoryDefinition "file:/Users/stefan/sources/dbeaver-ilitools-feature/category.xml" \
     -categoryQualifier "interlis"
   ```

7. **Manually upload result from `/tmp/dbeaver-updatesite/` to `https://dbeaver.sogeo.services/updates/`**

---

## Optional: Process via Helper Script

Instead of the two publisher commands, you can use a helper script:

```bash
/Users/stefan/sources/dbeaver-ili2pg-plugin/scripts/build-updatesite.sh
```

**Defaults:**
- Staging: `/Users/stefan/sources/dbeaver-release-staging/update-input`
- Output: `/tmp/dbeaver-updatesite`
- Category file: `/Users/stefan/sources/dbeaver-ilitools-feature/category.xml`

**Alternative paths:**
```bash
/Users/stefan/sources/dbeaver-ili2pg-plugin/scripts/build-updatesite.sh \
  /Users/stefan/sources/dbeaver-release-staging/update-input \
  /tmp/dbeaver-updatesite \
  /Users/stefan/sources/dbeaver-ilitools-feature/category.xml
```

---

## Acceptance Checks

1. **`content.jar` contains:**
   - `ch.so.agi.dbeaver.ili2pg`
   - `ch.so.agi.dbeaver.ili2pg.feature.feature.group`
   - (AI plugin entries if applicable)

2. **`artifacts.jar` contains core artifacts:**
   - Plugin JARs
   - Feature JARs

3. **In DBeaver, the site shows:**
   - Category `INTERLIS Tools` (or similar)
   - Plugin `ch.so.agi.dbeaver.ili2pg`

4. **Test installation:**
   - Fresh DBeaver installation
   - Add update site: `https://dbeaver.sogeo.services/updates/`
   - Install `ch.so.agi.dbeaver.ili2pg`
   - Verify `ili2pg` menu appears in context menu

---

## Notes

- **Category Publisher Warning:** The categoryDefinition path must be correct. If the path is wrong, the publisher may fail silently or categories won't be applied.
- **Version Consistency:** Plugin and Feature should have matching versions for clarity.
- **Qualifier:** The `.qualifier` suffix is typically replaced by a timestamp during automated builds.
