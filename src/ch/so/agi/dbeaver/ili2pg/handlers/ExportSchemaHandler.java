package ch.so.agi.dbeaver.ili2pg.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;


public class ExportSchemaHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        
        System.out.println("********************************************");
        System.err.println("********************************************");
        
        ISelection sel = HandlerUtil.getCurrentSelection(event);
        if (!(sel instanceof IStructuredSelection) || ((IStructuredSelection) sel).isEmpty()) {
            return null; // nothing selected
        }
        IStructuredSelection sSel = (IStructuredSelection) sel;
        
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        MessageDialog.openInformation(
                window.getShell(),
                "dbeaver-Ili2pg-plugin",
                sSel.toString());

        
        return null;
    }

}
