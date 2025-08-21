package ch.so.agi.dbeaver.ili2pg.jobs;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.base.Ili2dbException;
import ch.ehi.ili2db.gui.Config;
import ch.so.agi.dbeaver.ili2pg.log.Log;

public class Ili2pgExportJob extends Job {
    private static final String PLUGIN_ID = "ch.so.agi.dbeaver.ili2pg";
    
    private final Shell parentShell;
    private final DBSSchema schema;
    private final String modelName;

    public Ili2pgExportJob(Shell parentShell, DBSSchema schema, String modelName) {
        super("ili2pg export: " + schema.getName());
        this.parentShell = parentShell;
        this.schema = schema;
        this.modelName = modelName;
        setUser(true); // show in UI as a user job
        setPriority(LONG); // long-running
      }
    
    @Override
    protected IStatus run(IProgressMonitor monitor) {

        SubMonitor sub = SubMonitor.convert(monitor, "Exporting schema with ili2pg…", 100);
        try {
            if (sub.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            
            DBPDataSourceContainer c = schema.getDataSource().getContainer();
            DBPConnectionConfiguration cc = c.getActualConnectionConfiguration();

            String jdbcUrl = cc.getUrl();
            String port = cc.getHostPort();
            String user = cc.getUserName();
            String pwd = cc.getUserPassword();
            String schemaName = schema.getName();

            DBPDataSource ds = schema.getDataSource();
            try (JDBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), ds, this.getName())) {
                Connection conn = session.getOriginal(); 
                
                Config settings = createConfig();
                settings.setJdbcConnection(conn);
              
                settings.setFunction(Config.FC_EXPORT);
                settings.setModels(modelName);
                settings.setDbschema(schemaName);
                String userHome = System.getProperty("user.home");
                settings.setXtffile(Paths.get(userHome, schemaName + ".xtf").toAbsolutePath().toString());
                settings.setLogfile(Paths.get(userHome, schemaName + ".log").toAbsolutePath().toString());
                
                settings.setDburl(jdbcUrl);
                settings.setDbport(port);
                settings.setDbusr(user != null ? user : "");
                settings.setDbpwd(pwd != null ? pwd : "");
              
                Ili2db.readSettingsFromDb(settings);
                Ili2db.run(settings, null);
            } catch (DBCException | SQLException e) {
                e.printStackTrace();
                return Status.error(e.getMessage());
            } catch (Ili2dbException e) {
                return Status.error(e.getMessage());
            } 

            doneAsync("ili2pg export finished", "Model: " + modelName + "\nSchema: " + schema.getName());
            return Status.OK_STATUS;
        } catch (Exception e) {
            return error("Export failed: " + e.getMessage(), e);
        } finally {
            sub.done();
        }
    }
    
    private Config createConfig() {
        Config settings = new Config();
        new ch.ehi.ili2pg.PgMain().initConfig(settings);
        return settings;
    }
    
    private List<String> buildIli2pgCommand() {
        DBPDataSourceContainer c = schema.getDataSource().getContainer();
        DBPConnectionConfiguration cc = c.getActualConnectionConfiguration();

        // Prefer passing a JDBC URL if ili2pg supports --dburl; otherwise pass host/port/db separately.
        String jdbcUrl = cc.getUrl();              // e.g. jdbc:postgresql://host:5432/db
        String user    = cc.getUserName();
        String pwd     = cc.getUserPassword();     // DO NOT log this

        String schemaName = schema.getName();

        List<String> cmd = new ArrayList<>();
        cmd.add("ili2pg"); // or absolute path if not on PATH

        // Minimal example flags — adjust to your real workflow:
        cmd.add("--dburl");  cmd.add(jdbcUrl);
        cmd.add("--dbusr");  cmd.add(user != null ? user : "");
        cmd.add("--dbpwd");  cmd.add(pwd != null ? pwd : "");
        cmd.add("--export");                 // or other ili2pg action
        cmd.add("--models"); cmd.add(modelName);
        cmd.add("--schema"); cmd.add(schemaName);
        // cmd.add("--setupPgExt"); // etc., whatever you need
        // cmd.add("--export3"); cmd.add(outputPath);
        
        DBPDataSource ds = schema.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), ds, this.getName())) {
            Connection conn = session.getOriginal(); 
            
            Config settings = createConfig();
            settings.setJdbcConnection(conn);
          
            settings.setFunction(Config.FC_EXPORT);
            settings.setModels(modelName);
            settings.setDbschema(schemaName);
            settings.setXtffile(Paths.get("/Users/stefan/tmp/", schemaName + ".xtf").toAbsolutePath().toString());
            
            settings.setDburl(jdbcUrl);
            settings.setDbport(cc.getHostPort());
            settings.setDbusr(user != null ? user : "");
            settings.setDbpwd(pwd != null ? pwd : "");
          
            Ili2db.readSettingsFromDb(settings);
            Ili2db.run(settings, null);
            

        } catch (DBCException | SQLException e) {
            e.printStackTrace();
            return null;
        } catch (Ili2dbException e) {
            e.printStackTrace();
            return null;
        } 
        
        

        return cmd;
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
        Display.getDefault().asyncExec(() -> MessageDialog.openInformation(
                parentShell != null ? parentShell : Display.getDefault().getActiveShell(), title, body));
    }
}
