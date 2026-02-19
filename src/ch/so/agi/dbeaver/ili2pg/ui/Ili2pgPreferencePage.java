package ch.so.agi.dbeaver.ili2pg.ui;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class Ili2pgPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PLUGIN_ID = "ch.so.agi.dbeaver.ili2pg";
//    public static final String P_DISABLE_VALIDATION = "disableValidationDefault";
//    public static final String P_DEFAULT_DATASET = "defaultDataset";
    public static final String P_ILIDIR = "ilidir";
    public static final String P_EXPORT_DIR = "exportDir";

    public Ili2pgPreferencePage() {
        super(GRID);
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, PLUGIN_ID));
        setDescription("Preferences for ili2pg export/import");
    }

    @Override
    public void createFieldEditors() {
//        addField(new BooleanFieldEditor(P_DISABLE_VALIDATION, "Disable validation by default", getFieldEditorParent()));
//        addField(new StringFieldEditor(P_DEFAULT_DATASET, "Default Dataset:", getFieldEditorParent()));
        addField(new StringFieldEditor(P_ILIDIR, "Model repositories:", getFieldEditorParent()));
        addField(new DirectoryFieldEditor(P_EXPORT_DIR, "Export directory:", getFieldEditorParent()));
    }

    @Override
    public void init(IWorkbench workbench) {
    }
}
