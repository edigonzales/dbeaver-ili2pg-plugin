package ch.so.agi.dbeaver.ili2pg.log;

import org.eclipse.ui.console.MessageConsoleStream;
import ch.ehi.basics.logging.AbstractFilteringListener;

public class EclipseConsoleLogListener extends AbstractFilteringListener {

    private final MessageConsoleStream out;
    private final MessageConsoleStream err;

    public EclipseConsoleLogListener(MessageConsoleStream out, MessageConsoleStream err) {
        this.out = out;
        this.err = err;
    }

    @Override
    public void outputMsgLine(int kind, int level, String msg) {
        if (msg.endsWith("\n")) {
            err.print(msg);
        } else {
            err.println(msg);
        }
    } 
}
