package ch.so.agi.dbeaver.ili2pg.log;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

public final class Log {
    private static final String PLUGIN_ID = "ch.so.agi.dbeaver.ili2pg";
    private static final ILog LOG = Platform.getLog(FrameworkUtil.getBundle(Log.class));

    public static void info(String msg) {
        LOG.log(new Status(IStatus.INFO, PLUGIN_ID, msg));
    }

    public static void warn(String msg) {
        LOG.log(new Status(IStatus.WARNING, PLUGIN_ID, msg));
    }

    public static void error(String msg, Throwable t) {
        LOG.log(new Status(IStatus.ERROR, PLUGIN_ID, msg, t));
    }
}