package ch.so.agi.dbeaver.ili2pg.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;


public class ExportSchemaHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        System.err.println("******************************************** execute");
        
        ISelection sel = HandlerUtil.getCurrentSelection(event);
        if (!(sel instanceof IStructuredSelection) || ((IStructuredSelection) sel).isEmpty()) {
            return null; // nothing selected
        }
        Object first = ((IStructuredSelection) sel).getFirstElement();
        DBSSchema schema = extractSchema(first);
        if (schema == null) {
            IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
            MessageDialog.openInformation(
                    window.getShell(),
                    "",
                    "Not a PostgreSQL schema."); 

            return null;
        }

//        DBSSchema schema = null;
//        if (first instanceof IAdaptable) {
//            schema = ((IAdaptable) first).getAdapter(DBSSchema.class);
//            System.err.println("schema: " + schema);
//
//        }
//        if (schema == null && first instanceof DBSSchema) {
//            schema = (DBSSchema) first;
//        }
//
//        if (schema != null) {
//            
//            IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//            MessageDialog.openInformation(
//                    window.getShell(),
//                    "dbeaver-Ili2pg-plugin",
//                    schema.toString());
//
//            
//        }
        
        
//        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//        MessageDialog.openInformation(
//                window.getShell(),
//                "dbeaver-Ili2pg-plugin",
//                schema.toString());

        
        return null;
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
}
