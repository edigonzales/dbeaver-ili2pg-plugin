package ch.so.agi.dbeaver.ili2pg.handlers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.handlers.HandlerUtil;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import ch.so.agi.dbeaver.ili2pg.log.Log;
import ch.so.agi.dbeaver.ili2pg.ui.Ili2pgExportDialog;
import ch.ehi.ili2db.gui.Config;
import ch.so.agi.dbeaver.ili2pg.jobs.Ili2pgExportJob;

public class ExportSchemaHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        System.err.println("******************************************** execute");
        
        String cmdId = event.getCommand().getId();
        boolean withOptions = "ch.so.agi.dbeaver.ili2pg.commands.exportSchemaWithOptions".equals(cmdId);
        
        Shell shell = HandlerUtil.getActiveShell(event);
        
        ISelection sel = HandlerUtil.getCurrentSelection(event);
        if (!(sel instanceof IStructuredSelection) || ((IStructuredSelection) sel).isEmpty()) {
            return null; // nothing selected
        }
        Object first = ((IStructuredSelection) sel).getFirstElement();
        
        // 1) Resolve the schema from the current selection
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
        
        // 2) Read model names from schema.t_ili2db_model
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
        if (!withOptions) {
            // Falls Export ohne Optionen muss unter Umständen trotzdem
            // das Modell-Auswahl-Fenster erscheinen.
            String chosen = null;
            if (modelNames.size() > 1) {
                ElementListSelectionDialog dlg = new ElementListSelectionDialog(shell, new LabelProvider());
                dlg.setTitle("Select ili2pg model");
                dlg.setMessage("Choose a model from schema: " + schema.getName());
                dlg.setMultipleSelection(false);
                dlg.setElements(modelNames.toArray(new String[0]));
                if (dlg.open() != Window.OK) {
                    return null;
                }
                chosen = (String) dlg.getFirstResult();
            } else {
                chosen = modelNames.get(0);
            }
            if (chosen.contains("{")) {
                chosen = chosen.substring(0, chosen.indexOf("{"));            
            }
            Log.info("chosen: " + chosen);
            new Ili2pgExportJob(shell, schema, chosen, settings).schedule();            
        } else {
            System.err.println("********************");
            
            Ili2pgExportDialog dlg = new Ili2pgExportDialog(shell, schema.getName(), modelNames);
            if (dlg.open() != Window.OK) return null;
            
            String modelName = dlg.getSelectedModel();
            
            if (dlg.isDisableValidation()) {
                settings.setValidation(false);
            }
            
            System.err.println("***" + dlg.getDatasets().length());
            System.err.println("***" + dlg.isDisableValidation());
            
            
            return null;
        }
        

        return null;
    }
    
    private Config createConfig() {
        Config settings = new Config();
        new ch.ehi.ili2pg.PgMain().initConfig(settings);
        return settings;
    }
    
    private DBSSchema extractSchema(Object element) {
        // 1) Try direct adaptation (works for many navigator nodes)
        if (element instanceof IAdaptable) {
            DBSSchema adapted = ((IAdaptable) element).getAdapter(DBSSchema.class);
            if (adapted != null) {
                return adapted;
            }
        }

        // 2) If it's a DBNDatabaseItem, dive into its DBSObject and walk up
        if (element instanceof DBNDatabaseItem) {
            DBSObject obj = ((DBNDatabaseItem) element).getObject();
            for (DBSObject p = obj; p != null; p = p.getParentObject()) {
                if (p instanceof org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema) {
                    return (DBSSchema) p;
                } 
            }
        }

        // Not a schema (e.g., databases without schemas, or a folder node)
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
