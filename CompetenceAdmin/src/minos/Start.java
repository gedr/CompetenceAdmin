package minos;

import java.sql.Timestamp;
import java.util.Calendar;

import minos.data.services.ORMHelper;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;
import minos.ui.MainWnd;
import minos.utils.ProgramConfig;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Start {
	private static Logger log = LoggerFactory.getLogger(Start.class);	
	private static Thread thread = null;
	private static boolean wakeupFlag = false;
	
	public static void wakeup() {
		wakeupFlag = true;
		thread.interrupt();
	}
	
	private static void initDoomsday() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(9999, 11, 30);
		Resources.getInstance().put(ResourcesConst.DOOMSDAY, new Timestamp( calendar.getTimeInMillis() ) );
	}
	
	public static void main(String[] args) {
		String cfgPath = ( ( ( args != null ) && ( args.length > 1 ) ) ? args[0] : "program.cfg" );
		ProgramConfig cfg = null;
		try {
			cfg = ProgramConfig.loadConfig( cfgPath );
			DOMConfigurator.configure( cfg.getLogConfigFile() );			
		} catch (Exception ex) {			
			ex.printStackTrace();
			return;
		}
		Resources.getInstance().put( ResourcesConst.PROGRAM_CONFIG, cfg );
		initDoomsday();
		
		new MainWnd( cfg );
		
		thread = Thread.currentThread();		
		while ( !wakeupFlag ) {			
			try {			
				if( log.isDebugEnabled() ) log.debug( "Statrt.main() :  it's bedtime" );
				Thread.sleep(Long.MAX_VALUE);				
			} catch (InterruptedException e) { 
				if( log.isDebugEnabled() ) log.debug( "Statrt.main() :  wakeup" );
			}			
		}
		
		if ( ORMHelper.getFactory() != null ) ORMHelper.getFactory().close();
		if ( ( cfg != null ) && ( cfgPath != null ) ) cfg.saveConfig( cfgPath );
		if( log.isDebugEnabled() ) log.debug( "Statrt.main() :  bye" );
		System.exit(0);	
	}
}