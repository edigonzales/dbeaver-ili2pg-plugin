# Architecture Document: `ch.so.agi.dbeaver.ili2pg`

## Overview

The plugin `ch.so.agi.dbeaver.ili2pg` is organized into 4 main packages:

1. `handlers` - Command handlers for menu actions
2. `jobs` - Background jobs for ili2pg execution
3. `log` - Logging utilities
4. `ui` - UI components (dialogs, preferences)

---

## Components

### Handlers (`ch.so.agi.dbeaver.ili2pg.handlers`)

- **`Ili2pgHandler`**
  - Main handler for ili2pg commands
  - Processes context menu selections (database, schema)
  - Opens appropriate dialogs based on command type
  - Commands handled:
    - `importSchema` - Create schema from INTERLIS models
    - `importData` - Import data into schema
    - `exportSchema` - Export schema to INTERLIS
    - `exportSchemaWithOptions` - Export with advanced options
    - `validateSchema` - Validate schema against models

- **`SampleHandler`**
  - Example/demonstration handler

### Jobs (`ch.so.agi.dbeaver.ili2pg.jobs`)

- **ili2pg Execution Jobs**
  - Run ili2pg commands in background threads
  - Progress monitoring via DBeaver progress service
  - Console output capture and display
  - Error handling and user notification

### Log (`ch.so.agi.dbeaver.ili2pg.log`)

- **Logging Utilities**
  - Capture ili2pg console output
  - Forward to DBeaver console/error log
  - Configurable log levels

### UI (`ch.so.agi.dbeaver.ili2pg.ui`)

- **`Ili2pgPreferencePage`**
  - Preference page for ili2pg settings
  - Configuration of ili2pg home, Java executable, log level

- **`Ili2pgPreferenceInitializer`**
  - Default preference values
  - Initialize settings on first startup

- **`Ili2pgImportDialog`**
  - Dialog for data import operations
  - File selection, option configuration

- **`Ili2pgImportSchemaDialog`**
  - Dialog for schema creation from INTERLIS models
  - Model selection, schema naming, option configuration

- **`Ili2pgExportDialog`**
  - Dialog for schema export operations
  - Target file selection, export options

---

## Extension Points Used

### `org.eclipse.ui.commands`
Defines commands for ili2pg operations:
- Create schema
- Import data
- Export schema
- Export schema with options
- Validate schema

### `org.eclipse.ui.handlers`
Binds command IDs to handler classes with enablement rules:
- Schema creation: enabled on database connection
- Import/Export/Validate: enabled on schema selection

### `org.eclipse.ui.menus`
Adds ili2pg submenu to database navigator context menu:
- Location: `popup:org.eclipse.ui.popup.any`
- Visible on: DataSource, DBPDataSourceContainer, DBSSchema

### `org.eclipse.ui.preferencePages`
Adds ili2pg preference page to DBeaver settings

### `org.eclipse.core.runtime.preferences`
Initializes default preference values

---

## Data Flow (Schema Import Example)

1. User right-clicks database connection → `ili2pg` → `Create schema…`
2. `Ili2pgHandler` receives event with selected datasource
3. Handler opens `Ili2pgImportSchemaDialog`
4. User configures:
   - INTERLIS model file(s)
   - Target schema name
   - ili2pg options
5. On OK, handler creates background job
6. Job executes ili2pg with configured parameters
7. Console output is captured and displayed
8. On success, schema is created in database
9. On error, user is notified with details

---

## Preferences

Preference Page ID: `ch.so.agi.dbeaver.ili2pg.prefs`

Key settings:
- ili2pg home directory
- Java executable path
- Default command-line options
- Log level

See `Ili2pgPreferencePage` and `Ili2pgPreferenceInitializer` for implementation details.

---

## Error Handling

1. **Validation Errors**: Invalid configuration is caught in dialogs
2. **Execution Errors**: ili2pg errors are captured from console output
3. **Display**: Errors shown in DBeaver Error Log and/or notification dialog
4. **Details**: Full log output available via "Show details" option

---

## Third-Party Dependencies

### ili2pg Libraries
Bundled in `lib/` directory:
- `ili2pg-*.jar` - Main ili2pg library
- `ili2db-*.jar` - ili2db core functionality
- `ili2c-*` - INTERLIS compiler
- `iox-ili-*.jar` - INTERLIS I/O
- PostgreSQL driver
- Additional dependencies (JAXB, Jackson, etc.)

### Bundle-ClassPath
All libraries are listed in `META-INF/MANIFEST.MF` Bundle-ClassPath.
Use `./gradlew downloadAndExtractIli2pg` to update libraries and generate entries.

---

## Extension Points

The plugin currently does not define custom extension points. It uses standard DBeaver and Eclipse extension points for integration.

---

## Future Enhancements

Potential areas for extension:
- Additional ili2pg command support
- Custom extension points for ili2pg providers
- Persistent job history
- Advanced option presets
- Batch processing support
