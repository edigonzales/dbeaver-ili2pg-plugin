package ch.so.agi.dbeaver.ili2pg.jobs;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.base.Ili2dbException;
import ch.ehi.ili2db.gui.Config;
import ch.so.agi.dbeaver.ili2pg.log.EclipseConsoleLogListener;
import ch.so.agi.dbeaver.ili2pg.log.Log;
import ch.so.agi.dbeaver.ili2pg.ui.Ili2pgPreferencePage;

public class Ili2pgJob extends Job {
    public enum Mode { SCHEMA_IMPORT, IMPORT, EXPORT, VALIDATE }
    
    private static final String PLUGIN_ID = "ch.so.agi.dbeaver.ili2pg";

    private final Shell parentShell;
    private DBSSchema schema;
    private DBSInstance database; 
    private final Config settings;
    private final Mode mode; 

    public Ili2pgJob(Shell parentShell, DBSObject dbsObject, Config settings, Mode mode) {
        //super((mode == Mode.EXPORT ? "ili2pg export: " : "ili2pg validate: ") + schema.getName());
        super("ili2pg job");
        this.parentShell = parentShell;
        if (dbsObject instanceof DBSSchema) {
            this.schema = (DBSSchema) dbsObject;
        } else {
            this.database = (DBSInstance) dbsObject;
        }
        this.settings = settings;
        this.mode = mode;
        setUser(true); // show progress to the user
        setPriority(LONG);
        // Keep the job result visible in the Progress view after finish
        setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        MessageConsole console = getConsole("ili2pg");
        console.clearConsole();
        showConsole(console);
        
        try (MessageConsoleStream out = console.newMessageStream();
                MessageConsoleStream err = console.newMessageStream()) {
            
            EclipseConsoleLogListener listener = new EclipseConsoleLogListener(out, err);
            EhiLogger.getInstance().addListener(listener);

            if (mode == Mode.SCHEMA_IMPORT) {
                DBPDataSourceContainer container = database.getDataSource().getContainer();
                String targetDatabaseName = resolveTargetDatabaseName(database);
                DBPConnectionConfiguration cc = resolveEffectiveConnectionConfig(container, targetDatabaseName);
                if (cc == null) {
                    return Status.error("No connection configuration available for schema import.");
                }
                logConnectionSelection(mode, targetDatabaseName, cc);

                String jdbcUrl = cc.getUrl();
                String hostPort = Objects.toString(cc.getHostPort(), "");
                String user = cc.getUserName() != null ? cc.getUserName() : "";
                String pwd = cc.getUserPassword() != null ? cc.getUserPassword() : "";

                try (Connection connection = DriverManager.getConnection(jdbcUrl, user, pwd)) {
//                    connection.setAutoCommit(false);

                    settings.setJdbcConnection(connection);
                    settings.setDburl(jdbcUrl);
                    settings.setDbport(hostPort);
                    settings.setDbusr(user);
                    settings.setDbpwd(pwd);

                    String userHome = System.getProperty("user.home");
                    String logPath  = Paths.get(userHome, settings.getDbschema() + ".log").toAbsolutePath().toString();
                    listener.setLogfileName(logPath);
                    settings.setLogfile(logPath);     

                    settings.setFunction(Config.FC_SCHEMAIMPORT);
                    
                    Ili2db.readSettingsFromDb(settings);
                    Ili2db.run(settings, null);
                } catch (SQLException e) {
                    err.println("! JDBC/DB error: " + e.getMessage());
                    return Status.error(e.getMessage(), e);
                } catch (Ili2dbException e) {
                    err.println("Error: " + e.getMessage());
                    return Status.error(e.getMessage(), e);
                } finally {
                    EhiLogger.getInstance().removeListener(listener);
                }
            } else if (mode == Mode.IMPORT) {
                final String schemaName = schema.getName();
                DBPDataSourceContainer c = schema.getDataSource().getContainer();
                String targetDatabaseName = resolveTargetDatabaseName(schema);
                DBPConnectionConfiguration cc = resolveEffectiveConnectionConfig(c, targetDatabaseName);
                if (cc == null) {
                    return Status.error("No connection configuration available for import.");
                }
                logConnectionSelection(mode, targetDatabaseName, cc);

                String jdbcUrl = cc.getUrl();
                String hostPort = Objects.toString(cc.getHostPort(), "");
                String user = cc.getUserName() != null ? cc.getUserName() : "";
                String pwd = cc.getUserPassword() != null ? cc.getUserPassword() : "";

                ClassLoader myBundleClassLoader = getClass().getClassLoader();
                Class.forName("org.postgresql.Driver", true, myBundleClassLoader);
                try (Connection connection = DriverManager.getConnection(jdbcUrl, user, pwd)) {
//                    connection.setAutoCommit(false);

                    settings.setJdbcConnection(connection);
                    settings.setDbschema(schemaName);
                    settings.setDburl(jdbcUrl);
                    settings.setDbport(hostPort);
                    settings.setDbusr(user);
                    settings.setDbpwd(pwd);

                    String userHome = System.getProperty("user.home");
                    String logPath  = Paths.get(userHome, schemaName + ".log").toAbsolutePath().toString();
                    listener.setLogfileName(logPath);
                    settings.setLogfile(logPath);     

                    settings.setFunction(Config.FC_IMPORT);
                    Ili2db.readSettingsFromDb(settings);
                    Ili2db.run(settings, null);
                } catch (SQLException e) {
                    err.println("JDBC/DB error: " + e.getMessage());
                    return Status.error(e.getMessage(), e);
                } catch (Ili2dbException e) {
                    err.println("Error: " + e.getMessage());
                    return Status.error(e.getMessage(), e);
                } finally {
                    EhiLogger.getInstance().removeListener(listener);
                }
            } else {
                final String schemaName = schema.getName();
                DBPDataSourceContainer c = schema.getDataSource().getContainer();
                String targetDatabaseName = resolveTargetDatabaseName(schema);
                DBPConnectionConfiguration cc = resolveEffectiveConnectionConfig(c, targetDatabaseName);
                if (cc == null) {
                    return Status.error("No connection configuration available for export/validate.");
                }
                logConnectionSelection(mode, targetDatabaseName, cc);

                String jdbcUrl = cc.getUrl();
                String hostPort = Objects.toString(cc.getHostPort(), "");
                String user = cc.getUserName() != null ? cc.getUserName() : "";
                String pwd = cc.getUserPassword() != null ? cc.getUserPassword() : "";

                ClassLoader myBundleClassLoader = getClass().getClassLoader();
                Class.forName("org.postgresql.Driver", true, myBundleClassLoader);
                try (Connection connection = DriverManager.getConnection(jdbcUrl, user, pwd)) {
//                    connection.setAutoCommit(false);

                    settings.setJdbcConnection(connection);
                    settings.setDbschema(schemaName);
                    settings.setDburl(jdbcUrl);
                    settings.setDbport(hostPort);
                    settings.setDbusr(user);
                    settings.setDbpwd(pwd);

                    String userHome = System.getProperty("user.home");
                    String logPath  = Paths.get(userHome, schemaName + ".log").toAbsolutePath().toString();
                    listener.setLogfileName(logPath);
                    settings.setLogfile(logPath);

                    String xtfPath = null;
                    if (mode == Mode.EXPORT) {
                        settings.setFunction(Config.FC_EXPORT);
                        // Use xtffile from settings if set (e.g., from dialog), otherwise use preference
                        if (settings.getXtffile() == null || settings.getXtffile().isEmpty()) {
                            ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, Ili2pgPreferencePage.PLUGIN_ID);
                            String exportDir = store.getString(Ili2pgPreferencePage.P_EXPORT_DIR);
                            if (exportDir == null || exportDir.isBlank()) {
                                exportDir = userHome;
                            }
                            xtfPath = Paths.get(exportDir, schemaName + ".xtf").toAbsolutePath().toString();
                            settings.setXtffile(xtfPath);
                        }
                    } else {
                        settings.setFunction(Config.FC_VALIDATE);
                    }

                    Ili2db.readSettingsFromDb(settings);
                    Ili2db.run(settings, null);
                    
                    // Check for INTERLIS errors/warnings after export or validation
                    int errorCount = listener.getErrorCount();
                    
                    if (mode == Mode.EXPORT && xtfPath != null) {
                        if (errorCount > 0) {
                            doneAsync("ili2pg export finished with warnings/errors", "Exported to: " + xtfPath + "\n\nFound " + errorCount + " validation issue(s). See console for details.");
                        } else {
                            doneAsync("ili2pg export finished", "Exported to: " + xtfPath);
                        }
                    } else if (mode == Mode.VALIDATE) {
                        if (errorCount > 0) {
                            doneAsync("ili2pg validation finished", "Found " + errorCount + " validation issue(s). See console for details.");
                        } else {
                            doneAsync("ili2pg validation finished", "Validation completed successfully. No issues found.");
                        }
                    }
                    return Status.OK_STATUS;
                } catch (SQLException e) {
                    err.println("! JDBC/DB error: " + e.getMessage());
                    return Status.error(e.getMessage(), e);
                } catch (Ili2dbException e) {
                    err.println("Error: " + e.getMessage());
                    return Status.error(e.getMessage(), e);
                } finally {
                    EhiLogger.getInstance().removeListener(listener);
                }
            }
            
            // Should not reach here, but required for compilation
            return Status.OK_STATUS;
        } catch (Exception e) {
            return error("ili2pg job failed: " + e.getMessage(), e);
        }
    }

    private void logConnectionSelection(Mode mode, String targetDatabaseName, DBPConnectionConfiguration connectionConfiguration) {
        String effectiveDatabaseName = resolveDatabaseNameFromConnectionConfiguration(connectionConfiguration);
        Log.info("ili2pg " + mode + ": target DB='" + targetDatabaseName + "', effective connection DB='"
                + effectiveDatabaseName + "'.");
    }

    private DBPConnectionConfiguration resolveEffectiveConnectionConfig(DBPDataSourceContainer container, String targetDatabaseName) {
        if (container == null) {
            return null;
        }
        DBPConnectionConfiguration actual = container.getActualConnectionConfiguration();
        if (actual == null) {
            return null;
        }
        DBPConnectionConfiguration effective = new DBPConnectionConfiguration(actual);
        String normalizedTargetDatabaseName = normalizeDatabaseName(targetDatabaseName);
        if (normalizedTargetDatabaseName == null) {
            return effective;
        }

        effective.setDatabaseName(normalizedTargetDatabaseName);
        String jdbcUrl = effective.getUrl();
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            try {
                String updatedJdbcUrl = PostgreUtils.updateDatabaseNameInURL(jdbcUrl, normalizedTargetDatabaseName);
                if (updatedJdbcUrl != null && !updatedJdbcUrl.isBlank()) {
                    effective.setUrl(updatedJdbcUrl);
                }
            } catch (Exception e) {
                Log.warn("Could not update JDBC URL with target DB '" + normalizedTargetDatabaseName + "': "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        return effective;
    }

    private String resolveTargetDatabaseName(DBSObject dbsObject) {
        if (dbsObject == null) {
            return null;
        }
        if (dbsObject instanceof DBSSchema schemaObject) {
            for (DBSObject p = schemaObject; p != null; p = p.getParentObject()) {
                if (p instanceof org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase) {
                    return normalizeDatabaseName(p.getName());
                }
            }
            return null;
        }
        if (dbsObject instanceof DBSInstance instance) {
            return normalizeDatabaseName(instance.getName());
        }
        return null;
    }

    private String resolveDatabaseNameFromConnectionConfiguration(DBPConnectionConfiguration connectionConfiguration) {
        if (connectionConfiguration == null) {
            return null;
        }
        String dbName = normalizeDatabaseName(connectionConfiguration.getDatabaseName());
        if (dbName != null) {
            return dbName;
        }
        return normalizeDatabaseName(extractDatabaseNameFromJdbcUrl(connectionConfiguration.getUrl()));
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

    private Config createConfig() {
        Config settings = new Config();
        new ch.ehi.ili2pg.PgMain().initConfig(settings);
        return settings;
    }

    private IStatus error(String msg) {
        doneAsync("ili2pg export failed", msg);
        return new Status(IStatus.ERROR, PLUGIN_ID, msg);
    }

    private IStatus error(String msg, Throwable t) {
        doneAsync("ili2pg export failed", msg);
        return new Status(IStatus.ERROR, PLUGIN_ID, msg, t);
    }

    private void doneAsync(String title, String body) {
        Display.getDefault().asyncExec(() ->
            MessageDialog.openInformation(
                parentShell != null ? parentShell : Display.getDefault().getActiveShell(),
                title, body));
    }

    // ---- Console helpers ----------------------------------------------------

    private MessageConsole getConsole(String name) {
        IConsoleManager mgr = ConsolePlugin.getDefault().getConsoleManager();
        for (IConsole c : mgr.getConsoles()) {
            if (name.equals(c.getName())) {
                return (MessageConsole) c;
            }
        }
        MessageConsole console = new MessageConsole(name, null);
        mgr.addConsoles(new IConsole[]{ console });
        return console;
    }

    private void showConsole(MessageConsole console) {
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
    }
}
