package ch.so.agi.dbeaver.ili2pg.jobs;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
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

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.base.Ili2dbException;
import ch.ehi.ili2db.gui.Config;
import ch.so.agi.dbeaver.ili2pg.log.EclipseConsoleLogListener;

public class Ili2pgExportJob extends Job {
    private static final String PLUGIN_ID = "ch.so.agi.dbeaver.ili2pg";

    private final Shell parentShell;
    private final DBSSchema schema;
    private final String modelName;
    private final Config settings;

    public Ili2pgExportJob(Shell parentShell, DBSSchema schema, String modelName, Config settings) {
        super("ili2pg export: " + schema.getName());
        this.parentShell = parentShell;
        this.schema = schema;
        this.modelName = modelName;
        this.settings = settings;
        setUser(true);   // show progress to the user
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
            final String schemaName = schema.getName();
            DBPDataSourceContainer c = schema.getDataSource().getContainer();
            DBPConnectionConfiguration cc = c.getActualConnectionConfiguration();

            String jdbcUrl = cc.getUrl();
            String hostPort = Objects.toString(cc.getHostPort(), "");
            String user     = cc.getUserName();

            DBPDataSource ds = schema.getDataSource();

            EclipseConsoleLogListener listener = new EclipseConsoleLogListener(out, err);
            EhiLogger.getInstance().addListener(listener);

            try (JDBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), ds, this.getName())) {
                Connection conn = session.getOriginal(); // <-- do not close

                //Config settings = createConfig();
                settings.setJdbcConnection(conn);

                settings.setFunction(Config.FC_EXPORT);
                settings.setModels(modelName);
                settings.setDbschema(schemaName);

                String userHome = System.getProperty("user.home");
                String xtfPath  = Paths.get(userHome, schemaName + ".xtf").toAbsolutePath().toString();
                String logPath  = Paths.get(userHome, schemaName + ".log").toAbsolutePath().toString();
                listener.setLogfileName(logPath);
                settings.setXtffile(xtfPath);
                settings.setLogfile(logPath);

                settings.setDburl(jdbcUrl);
                settings.setDbport(hostPort);
                settings.setDbusr(user != null ? user : "");
                settings.setDbpwd(cc.getUserPassword() != null ? cc.getUserPassword() : "");

                Ili2db.readSettingsFromDb(settings);
                Ili2db.run(settings, null);
            } catch (DBCException | SQLException e) {
                err.println("! JDBC/DB error: " + e.getMessage());
                return Status.error(e.getMessage(), e);
            } catch (Ili2dbException e) {
                err.println("Error: " + e.getMessage());
                return Status.error(e.getMessage(), e);
            } finally {
                EhiLogger.getInstance().removeListener(listener);
            }

            doneAsync("ili2pg export finished",
                      "Model: " + modelName + "\nSchema: " + schema.getName());
            //out.println("=== ili2pg export completed " + LocalDateTime.now() + " ===");
            return Status.OK_STATUS;

        } catch (Exception e) {
            return error("Export failed: " + e.getMessage(), e);
        } 
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
