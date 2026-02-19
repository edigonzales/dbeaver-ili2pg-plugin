package ch.so.agi.dbeaver.ili2pg.handlers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
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

            List<String> modelNames;
            try {
                modelNames = loadModelNames(schema);
            } catch (Exception e) {
                MessageDialog.openError(shell, "ili2pg", "Failed to load model names: " + e.getMessage());
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
            
            // Read model names from schema.t_ili2db_model
            List<String> modelNames;
            try {
                modelNames = loadModelNames(schema);
            } catch (Exception e) {
                MessageDialog.openError(shell, "ili2pg", "Failed to load model names: " + e.getMessage());
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
    
    /** Query SELECT modelname FROM <schema>.t_ili2db_model ORDER BY modelname. 
     * @throws DBCException */
    private List<String> loadModelNames(DBSSchema schema) throws SQLException, DBCException {
        DBRProgressMonitor monitor = new VoidProgressMonitor();
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
}
