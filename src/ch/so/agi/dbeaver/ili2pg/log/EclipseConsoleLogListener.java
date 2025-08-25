package ch.so.agi.dbeaver.ili2pg.log;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.text.DateFormat;

import org.eclipse.ui.console.MessageConsoleStream;

import ch.ehi.basics.logging.AbstractFilteringListener;
import ch.ehi.basics.logging.AbstractStdListener;
import ch.ehi.basics.logging.LogEvent;
import ch.interlis.iox_j.logging.StdLogger;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import ch.ehi.basics.logging.AbstractFilteringListener;
import ch.ehi.basics.logging.FileListener;
import ch.ehi.basics.logging.LogEvent;
import ch.ehi.basics.logging.StdListener;
import ch.ehi.basics.logging.StdLogEvent;
import ch.interlis.iox.IoxLogEvent;

public class EclipseConsoleLogListener extends AbstractFilteringListener {
    private int ioxErrc = 0;
    private String logfile = null;
    private final static String INFO = "Info";
    
    private static MessageConsoleStream out;
    private static MessageConsoleStream err;
    
    public EclipseConsoleLogListener(MessageConsoleStream out, MessageConsoleStream err) {
        this.out = out;
        this.err = err;
    }

    public void setLogfileName(String fileName){
        this.logfile = fileName;
    }
    
    @Override
    public void outputEvent(LogEvent event, ArrayList msgv) {
        // get event tag
        String msgTag = getMessageTag(event);
        if (msgTag == null){
            msgTag="";
        } else {
            msgTag=msgTag+": ";
        }
        
        // get timetstamp
        String msgTimestamp = null;
        msgTimestamp = getTimestamp();
        if (msgTimestamp == null){
            msgTimestamp="";
        } else {
            msgTimestamp=msgTimestamp+": ";
        }
        
        String objRef = null;
        if(event instanceof IoxLogEvent){
            if(isError(event.getEventKind())){
                ioxErrc++;
            }
            objRef = "";
            IoxLogEvent ioxEvent=(IoxLogEvent) event;
            if (ioxEvent.getSourceLineNr() != null){
                objRef=objRef+"line "+ioxEvent.getSourceLineNr()+": ";
            }
            if (ioxEvent.getSourceObjectTag() != null){
                objRef=objRef+ioxEvent.getSourceObjectTag()+": ";
            }
            if (ioxEvent.getSourceObjectTechId() != null){
                objRef=objRef+ioxEvent.getSourceObjectTechId()+": ";
            }
            if (ioxEvent.getSourceObjectXtfId() != null){
                objRef=objRef+"tid "+ioxEvent.getSourceObjectXtfId()+": ";
            }
            if (ioxEvent.getSourceObjectUsrId() != null){
                objRef=objRef+ioxEvent.getSourceObjectUsrId()+": ";
            }
        } else {
            objRef = "";
        }
        
        // output all lines
        Iterator msgi=msgv.iterator();
        while (msgi.hasNext()) {
            String msg = (String)msgi.next();
            outputMsgLine(event.getEventKind(), event.getCustomLevel(), msgTimestamp+msgTag+objRef+msg);
        }
        if (event instanceof IoxLogEvent && logfile!=null && ioxErrc==1 && isError(event.getEventKind())){
            outputMsgLine(LogEvent.ADAPTION,0,msgTimestamp+INFO+": see <"+logfile+"> for more validation results");
        }
    }

    @Override
    public void outputMsgLine(int kind, int level, String msg) {
        if(msg.endsWith("\n")){
            err.print(msg);
        }else{
            err.println(msg);
        }
    }
    
    @Override
    public String getMessageTag(LogEvent event){
        switch(event.getEventKind()){
        case LogEvent.ERROR: 
                return "Error";
        case LogEvent.ADAPTION: 
                return "Warning";
        default:
                return INFO;
        }
    }
    
    private boolean isError(int kind) {
        return kind==IoxLogEvent.ERROR || kind==IoxLogEvent.WARNING;
    }
    
    @Override
    public boolean skipEvent(LogEvent event){
        if(event instanceof IoxLogEvent){
            if(ioxErrc>0 && logfile!=null && isError(event.getEventKind())){
                return true;
            }
        }
        return super.skipEvent(event);
    }
}