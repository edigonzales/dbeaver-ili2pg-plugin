package ch.so.agi.dbeaver.ili2pg.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class Ili2pgPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IEclipsePreferences d = DefaultScope.INSTANCE.getNode(Ili2pgPreferencePage.PLUGIN_ID);
//        d.putBoolean(Ili2pgPreferencePage.P_DISABLE_VALIDATION, false);
//        d.put(Ili2pgPreferencePage.P_DEFAULT_DATASET, "");
        d.put(Ili2pgPreferencePage.P_ILIDIR, "%ILI_DIR;http://models.interlis.ch/;%JAR_DIR");
    }
}
