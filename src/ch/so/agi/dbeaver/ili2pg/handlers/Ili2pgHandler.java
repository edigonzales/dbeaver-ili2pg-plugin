package ch.so.agi.dbeaver.ili2pg.handlers;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import ch.so.agi.dbeaver.ili2pg.log.Log;
import ch.so.agi.dbeaver.ili2pg.ui.Ili2pgExportDialog;
import ch.so.agi.dbeaver.ili2pg.ui.Ili2pgImportSchemaDialog;
import ch.so.agi.dbeaver.ili2pg.ui.Ili2pgPreferencePage;
import ch.so.agi.dbeaver.ili2pg.ui.Ili2pgImportDialog;
import ch.ehi.ili2db.gui.Config;
import ch.interlis.ili2c.Ili2cSettings;
import ch.so.agi.dbeaver.ili2pg.jobs.Ili2pgJob;

public class Ili2pgHandler extends AbstractHandler {
    private static final String CMD_SCHEMA_IMPORT = "ch.so.agi.dbeaver.ili2pg.commands.importSchema";
    private static final String CMD_DATA_IMPORT = "ch.so.agi.dbeaver.ili2pg.commands.importData";
    private static final String CMD_EXPORT = "ch.so.agi.dbeaver.ili2pg.commands.exportSchema";
    private static final String CMD_EXPORT_OPTS = "ch.so.agi.dbeaver.ili2pg.commands.exportSchemaWithOptions";
    private static final String CMD_VALIDATE = "ch.so.agi.dbeaver.ili2pg.commands.validateSchema";

    private enum Action {
        SCHEMA_IMPORT, IMPORT, EXPORT, EXPORT_WITH_OPTIONS, VALIDATE;

        static Action from(String id) {
            if (CMD_EXPORT.equals(id)) {
                return EXPORT;
            }
            if (CMD_EXPORT_OPTS.equals(id)) {
                return EXPORT_WITH_OPTIONS;
            }
            if (CMD_VALIDATE.equals(id)) {
                return VALIDATE;
            } if (CMD_SCHEMA_IMPORT.equals(id)) {
                return SCHEMA_IMPORT;
            }  
            if (CMD_DATA_IMPORT.equals(id)) {
                return IMPORT;
            }
            return EXPORT;
        }
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException { 
        Shell shell = HandlerUtil.getActiveShell(event);
        Action action = Action.from(event.getCommand().getId());
                
        ISelection sel = HandlerUtil.getCurrentSelection(event);
        if (!(sel instanceof IStructuredSelection) || ((IStructuredSelection) sel).isEmpty()) {
            return null;
        }
        Object first = ((IStructuredSelection) sel).getFirstElement();

        
        if (action == Action.SCHEMA_IMPORT) {
            DBSInstance database = extractDatabase(first);
            if (database == null) {
                IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
                MessageDialog.openInformation(
                        window.getShell(),
                        "",
                        "Not a PostgreSQL database."); 

                return null;
            }
            Log.info("Database: " + database);

            Ili2pgImportSchemaDialog dlg = new Ili2pgImportSchemaDialog(shell);
            if (dlg.open() == Window.OK) {
                String ini = dlg.getIniPath();
                String ilidata = dlg.getIliDataRef();
                String schema = dlg.getTargetSchema();
                
                Config settings = createConfig();
                if (ini != null) {
                    settings.setMetaConfigFile(ini);                    
                } else {
                    settings.setMetaConfigFile(ilidata);                    

                }
                settings.setDbschema(schema);
                
                new Ili2pgJob(shell, database, settings, Ili2pgJob.Mode.SCHEMA_IMPORT).schedule();
            }   
            return null;
        } else if (action == Action.IMPORT) {
            DBSSchema schema = extractSchema(first);
            if (schema == null) {
                IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
                MessageDialog.openInformation(
                        window.getShell(),
                        "",
                        "Not a PostgreSQL schema."); 

                return null;
            }
            Log.info("Schema: " + schema);
            if (!ensureActiveDatabaseMatches(schema, shell, action)) {
                return null;
            }

            List<String> modelNames;
            try {
                modelNames = loadModelNames(schema);
            } catch (Exception e) {
                MessageDialog.openError(shell, "ili2pg", formatLoadModelNamesError(e));
                return null;
            }
            Log.info("modelNames: " + modelNames);
            
            if (modelNames.isEmpty()) {
                MessageDialog.openInformation(shell, "ili2pg",
                        "No rows in " + schema.getName() + ".t_ili2db_model (or table missing).");
                return null;
            }
            
            Config settings = createConfig();

            Ili2pgImportDialog dlg = new Ili2pgImportDialog(shell, modelNames);
            if (dlg.open() != Window.OK) {
                return null;
            }

            String modelName = this.sanitizeModelName(dlg.getSelectedModel());
            settings.setModels(modelName);
            
            if (dlg.isDisableValidation()) {
                settings.setValidation(false);
            }
            if (dlg.getDataset() != null) {
                settings.setDatasetName(dlg.getDataset());
            }
            if (dlg.getBaskets() != null) {
                settings.setBaskets(dlg.getBaskets());
            }
            if (dlg.getTopics() != null) {
                settings.setTopics(dlg.getTopics());
            }
            if (dlg.getTransferFilePath() != null) {
                settings.setXtffile(dlg.getTransferFilePath());
            }
            if (dlg.getExternalTransferRef() != null) {
                settings.setXtffile(dlg.getExternalTransferRef());
            }

            new Ili2pgJob(shell, schema, settings, Ili2pgJob.Mode.IMPORT).schedule();
        } else {
            // Resolve the schema from the current selection
            DBSSchema schema = extractSchema(first);
            if (schema == null) {
                IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
                MessageDialog.openInformation(
                        window.getShell(),
                        "",
                        "Not a PostgreSQL schema."); 

                return null;
            }
            Log.info("Schema: " + schema);
            if (!ensureActiveDatabaseMatches(schema, shell, action)) {
                return null;
            }
            
            // Read model names from schema.t_ili2db_model
            List<String> modelNames;
            try {
                modelNames = loadModelNames(schema);
            } catch (Exception e) {
                MessageDialog.openError(shell, "ili2pg", formatLoadModelNamesError(e));
                return null;
            }
            Log.info("modelNames: " + modelNames);
            
            if (modelNames.isEmpty()) {
                MessageDialog.openInformation(shell, "ili2pg",
                        "No rows in " + schema.getName() + ".t_ili2db_model (or table missing).");
                return null;
            }
            
            Config settings = createConfig();
            switch (action) {
                case EXPORT, VALIDATE: {
                    String chosenModel = null;
                    if (modelNames.size() > 1) {
                        ElementListSelectionDialog dlg = new ElementListSelectionDialog(shell, new LabelProvider());
                        dlg.setTitle("Select ili2pg model");
                        dlg.setMessage("Choose a model from schema: " + schema.getName());
                        dlg.setMultipleSelection(false);
                        dlg.setElements(modelNames.toArray(new String[0]));
                        if (dlg.open() != Window.OK) {
                            return null;
                        }
                        chosenModel = (String) dlg.getFirstResult();
                    } else {
                        chosenModel = modelNames.get(0);
                    }
        
                    chosenModel = sanitizeModelName(chosenModel);
                    settings.setModels(chosenModel);
                    if (action == Action.EXPORT) {
                        new Ili2pgJob(shell, schema, settings, Ili2pgJob.Mode.EXPORT).schedule();
                    } else {
                        new Ili2pgJob(shell, schema, settings, Ili2pgJob.Mode.VALIDATE).schedule();                    
                    }
                    break;
                }
                case EXPORT_WITH_OPTIONS: {
                    Ili2pgExportDialog dlg = new Ili2pgExportDialog(shell, schema.getName(), modelNames);
                    if (dlg.open() != Window.OK) {
                        return null;
                    }

                    String modelName = this.sanitizeModelName(dlg.getSelectedModel());
                    settings.setModels(modelName);

                    if (dlg.isDisableValidation()) {
                        settings.setValidation(false);
                    }
                    if (dlg.getDatasets() != null) {
                        settings.setDatasetName(dlg.getDatasets());
                    }
                    if (dlg.getBaskets() != null) {
                        settings.setBaskets(dlg.getBaskets());
                    }
                    if (dlg.getTopics() != null) {
                        settings.setTopics(dlg.getTopics());
                    }
                    if (dlg.getExportDir() != null && !dlg.getExportDir().isEmpty()) {
                        java.nio.file.Path xtfPath = java.nio.file.Paths.get(dlg.getExportDir(), schema.getName() + ".xtf");
                        settings.setXtffile(xtfPath.toAbsolutePath().toString());
                    }
                    new Ili2pgJob(shell, schema, settings, Ili2pgJob.Mode.EXPORT).schedule();
                    break;
                }
            }  
        }
        return null;
    }
    
    private Config createConfig() {
        Config settings = new Config();
        new ch.ehi.ili2pg.PgMain().initConfig(settings);
        
        ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, Ili2pgPreferencePage.PLUGIN_ID);
        String ilidir = store.getString(Ili2pgPreferencePage.P_ILIDIR);
        if (!ilidir.isBlank()) {
            settings.setModeldir(ilidir);
        } else {
            settings.setModeldir(Ili2cSettings.DEFAULT_ILIDIRS);
        }
        
        return settings;
    }
    
    private String sanitizeModelName(String modelName) {
        if (modelName.contains("{")) {
            return modelName.substring(0, modelName.indexOf("{"));            
        }
        return modelName;
    }
    
    private DBSSchema extractSchema(Object element) {
        if (element instanceof DBNDatabaseItem) {
            DBSObject obj = ((DBNDatabaseItem) element).getObject();
            for (DBSObject p = obj; p != null; p = p.getParentObject()) {
                if (p instanceof org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema) {
                    return (DBSSchema) p;
                } 
            }
        }
        return null;
    }
    
    private DBSInstance extractDatabase(Object element) {
        if (element instanceof DBNDatabaseItem) {
            DBSObject obj = ((DBNDatabaseItem) element).getObject();
            for (DBSObject p = obj; p != null; p = p.getParentObject()) {
                if (p instanceof org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase) {
                    return (DBSInstance) p;
                } 
            }
        }
        return null;
    }

    private boolean ensureActiveDatabaseMatches(DBSSchema schema, Shell shell, Action action) {
        String schemaDatabaseName = normalizeDatabaseName(getSchemaDatabaseName(schema));
        String activeDatabaseName = normalizeDatabaseName(getActiveDatabaseName(schema));
        String configuredDatabaseName = normalizeDatabaseName(getConfiguredDatabaseName(schema));

        // Avoid false positives if DB names cannot be determined.
        if (schemaDatabaseName == null || activeDatabaseName == null) {
            return true;
        }
        if (schemaDatabaseName.equalsIgnoreCase(activeDatabaseName)) {
            return true;
        }

        Log.warn("Blocked " + action + " on schema '" + schema.getName() + "': schema DB='" + schemaDatabaseName
                + "', runtime active DB='" + activeDatabaseName + "', config DB='" + configuredDatabaseName + "'.");

        MessageDialog.openWarning(shell, "ili2pg",
                "The selected schema belongs to database \"" + schemaDatabaseName + "\", but the active database is \""
                        + activeDatabaseName + "\".\n\n"
                        + actionLabel(action) + " works only on the active/default database (bold in the navigator).\n"
                        + "Please set \"" + schemaDatabaseName
                        + "\" as default (bold) in the navigator or set it in Connection configuration, then retry.\n"
                        + "With \"Show all databases\", only the currently active/default (bold) database can be used for this action.");
        return false;
    }

    private String actionLabel(Action action) {
        switch (action) {
            case IMPORT:
                return "Import";
            case EXPORT:
            case EXPORT_WITH_OPTIONS:
                return "Export";
            case VALIDATE:
                return "Validate";
            default:
                return "This action";
        }
    }

    private String getSchemaDatabaseName(DBSSchema schema) {
        for (DBSObject p = schema; p != null; p = p.getParentObject()) {
            if (p instanceof org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase) {
                return p.getName();
            }
        }
        return null;
    }

    private String getActiveDatabaseName(DBSSchema schema) {
        DBPDataSource ds = schema.getDataSource();
        if (ds == null) {
            return null;
        }
        try {
            DBSInstance defaultInstance = ds.getDefaultInstance();
            String runtimeDefaultName = normalizeDatabaseName(defaultInstance == null ? null : defaultInstance.getName());
            if (runtimeDefaultName != null) {
                return runtimeDefaultName;
            }
        } catch (Exception e) {
            Log.warn("Could not determine runtime active database from default instance: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return getConfiguredDatabaseName(schema);
    }

    private String getConfiguredDatabaseName(DBSSchema schema) {
        DBPDataSource ds = schema.getDataSource();
        if (ds == null) {
            return null;
        }
        DBPDataSourceContainer container = ds.getContainer();
        if (container == null) {
            return null;
        }
        DBPConnectionConfiguration cc = container.getActualConnectionConfiguration();
        if (cc == null) {
            return null;
        }
        String dbName = normalizeDatabaseName(cc.getDatabaseName());
        if (dbName != null) {
            return dbName;
        }
        return normalizeDatabaseName(extractDatabaseNameFromJdbcUrl(cc.getUrl()));
    }

    private String extractDatabaseNameFromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank() || !jdbcUrl.startsWith("jdbc:")) {
            return null;
        }
        String jdbcBody = jdbcUrl.substring("jdbc:".length());
        try {
            URI uri = new URI(jdbcBody);
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return null;
            }
            String dbName = path.substring(path.lastIndexOf('/') + 1);
            return dbName.isBlank() ? null : dbName;
        } catch (URISyntaxException e) {
            Log.warn("Could not parse JDBC URL to determine database name: " + jdbcUrl);
            return null;
        }
    }

    private String normalizeDatabaseName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = unquoteIdentifier(name.trim());
        return normalized.isBlank() ? null : normalized;
    }

    private String unquoteIdentifier(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
    
    private String formatLoadModelNamesError(Exception e) {
        String base = "Failed to load model names";
        if (e instanceof ModelNamesLoadException failure) {
            String detail = firstErrorMessage(failure.getCause());
            if (failure.reconnectAttempted && failure.reconnectFailed) {
                return base + ". The database connection appears to have been lost and automatic reconnect failed "
                        + "(possible idle timeout or network drop)." + detail;
            }
            if (failure.reconnectAttempted) {
                return base + ". Automatic reconnect was attempted, but the query still failed." + detail;
            }
            return base + detail;
        }
        return base + firstErrorMessage(e);
    }

    private String firstErrorMessage(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            String msg = cur.getMessage();
            if (msg != null && !msg.isBlank()) {
                return ": " + msg;
            }
        }
        return "";
    }

    private boolean isConnectionLost(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (cur instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    return true;
                }
            }
            String msg = cur.getMessage();
            String className = cur.getClass().getName();
            String probe = ((msg == null ? "" : msg) + " " + className).toLowerCase(Locale.ROOT);
            if (probe.contains("i/o error occurred while sending to the backend")
                    || probe.contains("connection reset")
                    || probe.contains("broken pipe")
                    || probe.contains("eofexception")
                    || probe.contains("connection is closed")
                    || probe.contains("connection has been closed")) {
                return true;
            }
        }
        return false;
    }

    private boolean tryReconnect(DBSSchema schema, DBRProgressMonitor monitor) {
        DBPDataSource ds = schema.getDataSource();
        if (ds == null || ds.getContainer() == null) {
            Log.warn("Reconnect skipped while loading model names for schema '" + schema.getName()
                    + "': no data source container available.");
            return false;
        }
        DBPDataSourceContainer container = ds.getContainer();
        String containerName = container.getName();
        Log.warn("Connection lost while loading model names for schema '" + schema.getName()
                + "'. Trying reconnect for data source '" + containerName + "'.");
        try {
            boolean ok = container.reconnect(monitor);
            if (ok) {
                Log.info("Reconnect succeeded for data source '" + containerName + "'.");
            } else {
                Log.warn("Reconnect returned false for data source '" + containerName + "'.");
            }
            return ok;
        } catch (Exception e) {
            Log.warn("Reconnect failed for data source '" + containerName + "': "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    /** Query SELECT modelname FROM <schema>.t_ili2db_model ORDER BY modelname, with one reconnect/retry on lost connections. */
    private List<String> loadModelNames(DBSSchema schema) throws SQLException, DBCException, ModelNamesLoadException {
        DBRProgressMonitor monitor = new VoidProgressMonitor();
        try {
            return loadModelNamesOnce(schema, monitor);
        } catch (SQLException | DBCException firstError) {
            if (!isConnectionLost(firstError)) {
                throw firstError;
            }
            boolean reconnectOk = tryReconnect(schema, monitor);
            if (!reconnectOk) {
                throw new ModelNamesLoadException(firstError, true, true);
            }
            try {
                return loadModelNamesOnce(schema, monitor);
            } catch (SQLException | DBCException retryError) {
                throw new ModelNamesLoadException(retryError, true, false);
            }
        }
    }

    /** Single attempt query without reconnect logic. */
    private List<String> loadModelNamesOnce(DBSSchema schema, DBRProgressMonitor monitor) throws SQLException, DBCException {
        DBPDataSource ds = schema.getDataSource();

        String qSchema = DBUtils.getQuotedIdentifier(schema);
        String qTable = DBUtils.getQuotedIdentifier(ds, "t_ili2db_model");
        String sql = "SELECT modelname, content FROM " + qSchema + "." + qTable + " ORDER BY modelname";

        Set<String> uniq = new LinkedHashSet<>();

        try (JDBCSession session = DBUtils.openMetaSession(monitor, ds, "Read INTERLIS model names");
                JDBCPreparedStatement stmt = session.prepareStatement(sql);
                JDBCResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String v = JDBCUtils.safeGetString(rs, 1);
                String c = JDBCUtils.safeGetString(rs, 2);
                if (v != null && !v.isEmpty()) {
                    if (!c.contains("CONTRACTED") && !c.contains("TYPE") && !c.contains("REFSYSTEM")
                            && !c.contains("SYMBOLOGY")) {
                        uniq.add(v);
                    }
                }
            }
        }
        return new ArrayList<>(uniq);
    }

    private static final class ModelNamesLoadException extends Exception {
        private static final long serialVersionUID = 1L;
        private final boolean reconnectAttempted;
        private final boolean reconnectFailed;

        private ModelNamesLoadException(Throwable cause, boolean reconnectAttempted, boolean reconnectFailed) {
            super(cause);
            this.reconnectAttempted = reconnectAttempted;
            this.reconnectFailed = reconnectFailed;
        }
    }
}
