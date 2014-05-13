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
	private static Logger log = LoggerFactory.getLogger( Start.class );	
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
	/*
	private static void test() {
		List<Triplet<Integer, String, Byte[]>> p = new ArrayList<>();
		p.add( new Triplet<Integer, String, Byte[]>(1, "01 Администрация", new Byte[] {0, 1} ) );
		p.add( new Triplet<Integer, String, Byte[]>(2, "02 Арзамасское ЛПУМГ", new Byte[] {2} ) );
		p.add( new Triplet<Integer, String, Byte[]>(3, "03 Владимирское ЛПУМГ", new Byte[] {3} ) );
		p.add( new Triplet<Integer, String, Byte[]>(4, "04 Волжское ЛПУМГ", new Byte[] {4} ) );
		p.add( new Triplet<Integer, String, Byte[]>(5, "05 Вятское ЛПУМГ", new Byte[] {5} ) );
		p.add( new Triplet<Integer, String, Byte[]>(6, "06 Заволжское ЛПУМГ", new Byte[] {6} ) );
		p.add( new Triplet<Integer, String, Byte[]>(7, "07 Ивановское ЛПУМГ", new Byte[] {7} ) );
		p.add( new Triplet<Integer, String, Byte[]>(8, "08 Кировское ЛПУМГ", new Byte[] {8} ) );
		p.add( new Triplet<Integer, String, Byte[]>(9, "09 Моркинское ЛПУМГ", new Byte[] {9} ) );
		p.add( new Triplet<Integer, String, Byte[]>(10, "10 Пензенское ЛПУМГ", new Byte[] {10} ) );
		p.add( new Triplet<Integer, String, Byte[]>(11, "11 Пильнинское ЛПУМГ", new Byte[] {11} ) );
		p.add( new Triplet<Integer, String, Byte[]>(12, "12 Починковское ЛПУМГ", new Byte[] {12} ) );
		p.add( new Triplet<Integer, String, Byte[]>(13, "13 Приокское ЛПУМГ", new Byte[] {13} ) );
		p.add( new Triplet<Integer, String, Byte[]>(14, "14 Семеновское ЛПУМГ", new Byte[] {14} ) );
		p.add( new Triplet<Integer, String, Byte[]>(15, "15 Сеченовское ЛПУМГ", new Byte[] {15} ) );
		p.add( new Triplet<Integer, String, Byte[]>(16, "16 Торбеевское ЛПУМГ", new Byte[] {16} ) );
		p.add( new Triplet<Integer, String, Byte[]>(17, "17 Чебоксарское ЛПУМГ", new Byte[] {17} ) );
		p.add( new Triplet<Integer, String, Byte[]>(20, "20 Управление материально-технического снабжения", new Byte[] {20} ) );
		p.add( new Triplet<Integer, String, Byte[]>(21, "21 Управление технологического транспорта и специальной техники", new Byte[] {21} ) );
		p.add( new Triplet<Integer, String, Byte[]>(22, "22 Центр по подготовке кадров", new Byte[] {22} ) );
		p.add( new Triplet<Integer, String, Byte[]>(23, "23 База отдыха для детей и родителей Волга", new Byte[] {23} ) );
		p.add( new Triplet<Integer, String, Byte[]>(24, "24 Детская оздоровительная база отдыха Ласточка", new Byte[] {24} ) );
		p.add( new Triplet<Integer, String, Byte[]>(27, "27 Инженерно-технический центр", new Byte[] {27} ) );
		p.add( new Triplet<Integer, String, Byte[]>(28, "28 Управление аварийно-восстановительных работ", new Byte[] {28} ) );
		p.add( new Triplet<Integer, String, Byte[]>(29, "29 Волгоавтогаз", new Byte[] {29} ) );
		
		Gson gson = new Gson();
		String str = gson.toJson( p );
		System.out.println( str );
		
		java.lang.reflect.Type t = new TypeToken<List<Triplet<Integer, String, Byte[]>>>(){}.getType();
		List<Triplet<Integer, String, Byte[]>> lpsb = gson.fromJson( str , t);
		
		for ( Triplet<Integer, String, Byte[]> pp : lpsb ) {
			System.out.println( pp.getFirst()+ "   :    " + pp.getSecond() + "   :   [");
			for ( Byte b : pp.getThird() ) System.out.print( b + ", ");
			System.out.println( "]");
			
		}

		System.exit(0);
	}
*/	
	public static void main(String[] args) {
		//test();
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