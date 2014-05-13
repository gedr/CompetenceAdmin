package minos.ui;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.swing.JMenuBar;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Triplet;
import minos.Start;
import minos.data.services.ORMHelper;
import minos.data.services.ORMHelper.QueryType;
import minos.entities.Level;
import minos.entities.Person;
import minos.entities.Role;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.DBConnectionDlg;
import minos.ui.panels.CompetencePanel;
import minos.ui.panels.MeasurePanel;
import minos.ui.panels.PostProfilePanel;
import minos.ui.panels.ProfilePatternPanel;
import minos.utils.DBConnectionConfig;
import minos.utils.ProgramConfig;

import com.alee.extended.layout.ToolbarLayout;
import com.alee.extended.statusbar.WebMemoryBar;
import com.alee.extended.statusbar.WebStatusBar;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.desktoppane.WebDesktopPane;
import com.alee.laf.desktoppane.WebInternalFrame;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuBar;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebFrame;
import com.alee.utils.SwingUtils;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

public class MainWnd extends WebFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static Logger log = LoggerFactory.getLogger( MainWnd.class );	
	
	private static final String COMPETENCE_CMD = "C";
	private static final String PROFILE_CMD = "P";
	private static final String POST_CMD = "D";
	private static final String EVENT_CMD = "E";
	private static final String ROLE_CMD = "R";
	private static final String USER_CMD = "U";
	
	private static final String FRAME_COMPETENCES 		= "Компетенции";
	private static final String FRAME_PROFILE_PATTERNS 	= "Шаблоны профилей";
	private static final String FRAME_POST_PROFILES 	= "Профили должностей";
	private static final String FRAME_MEASURES			= "Мероприятия оценки";
	
	private ProgramConfig cfg = null;
	private WebDesktopPane desktop = null;
	private WebStatusBar statusBar = null;
	private List<Pair<WebInternalFrame, Long>> frames = Collections.synchronizedList( new ArrayList<Pair<WebInternalFrame, Long>>() );
	private InternalFrameListener ifl = new InternalFrameAdapter() {

		@Override
		public void internalFrameClosing( InternalFrameEvent e ) {
			for ( Pair<WebInternalFrame, Long> p : frames ) {
				if ( ( p != null ) && ( p.getFirst() != null ) 
						&& p.getFirst().getName().equals( e.getInternalFrame().getName() ) ) {
					frames.remove( p );
					break;
				}
			}			
			super.internalFrameClosing( e );
		}
	};

	public MainWnd( ProgramConfig programConfig ) {
		super( "Компетенции: администрирование" );
		this.cfg = programConfig;
		WebLookAndFeel.install();
		setDefaultCloseOperation( WebFrame.DISPOSE_ON_CLOSE );
		WebLookAndFeel.setDecorateFrames( true );		
        setIconImages( WebLookAndFeel.getImages () );        
        setBounds( cfg != null ? cfg.getMainWindowBound() : new Rectangle( 100, 100, 450, 300 ) );        

        addWindowListener(new WindowAdapter() {

        	@Override
        	public void windowClosing(WindowEvent arg) {        		
        		if( log.isDebugEnabled() ) log.debug( "main window closing" );
        		if ( cfg != null ) cfg.setMainWindowBound( MainWnd.this.getBounds() );
        		Start.wakeup();
        		super.windowClosing(arg);
        	}
        });   

        setVisible(true);
        new Thread( new Runnable() {

        	@Override
        	public void run() {
        		init();				
        	}
        } ).start();              
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		WebInternalFrame frame;
		switch (e.getActionCommand()) {
		case COMPETENCE_CMD:
			frame = findFrame( FRAME_COMPETENCES, 0 );
			if ( frame != null ) desktop.moveToFront( frame );
			else frame = createFrame( FRAME_COMPETENCES, new CompetencePanel( this ), 0 );
			break;

		case PROFILE_CMD:
			frame = findFrame( FRAME_PROFILE_PATTERNS, 0 );
			if ( frame != null ) desktop.moveToFront( frame );
			else frame = createFrame( FRAME_PROFILE_PATTERNS, new ProfilePatternPanel( this ), 0 );
			break;

		case POST_CMD:
			frame = findFrame( FRAME_POST_PROFILES, 0 );
			if ( frame != null ) desktop.moveToFront( frame );
			else frame = createFrame( FRAME_POST_PROFILES, new PostProfilePanel( this ), 0 );
			break;
		
		case EVENT_CMD:
			frame = findFrame( FRAME_MEASURES, 0 );
			if ( frame != null ) desktop.moveToFront( frame );
			else frame = createFrame( FRAME_MEASURES, new MeasurePanel( this ), 0 );
			break;

		default:
			break;
		}
	}
	
	/**
	 * initializing DB and UI
	 */
	private void init() {		
		if ( !initDb() ) {
			WebOptionPane.showMessageDialog( MainWnd.this, " Не создано соединения для подключения к БД", "Ошибка", 
					WebOptionPane.ERROR_MESSAGE );
			return;
		}
		
		if ( !initUser() ) {
			WebOptionPane.showMessageDialog( MainWnd.this, "Не загружается текущий пользователь", "Ошибка", 
					WebOptionPane.ERROR_MESSAGE );
			return;
		}
		
		if ( !initLevelsCache() ) {
			WebOptionPane.showMessageDialog( MainWnd.this, " Не загружаются уровни компетенций", "Ошибка", 
					WebOptionPane.ERROR_MESSAGE );
			return;
		}
		
		if ( !loadCommonCfg() ) {
			WebOptionPane.showMessageDialog( MainWnd.this, " Не загружаются конфигурации из таблицы ProgramCommonConfig", "Ошибка", 
					WebOptionPane.ERROR_MESSAGE );
			return;			
		}
		
		SwingUtils.invokeLater( new Runnable() {
			
			@Override
			public void run() {			    
				desktop = new WebDesktopPane();
				statusBar = new WebStatusBar ();		       		        

		        // Simple memory bar
		        WebMemoryBar memoryBar = new WebMemoryBar ();
		        memoryBar.setPreferredWidth( memoryBar.getPreferredSize ().width + 20 );
		        statusBar.add( memoryBar, ToolbarLayout.END );  

		        MainWnd.this.setJMenuBar( initMenu() );
		        MainWnd.this.setLayout( new BorderLayout() );		        
		        MainWnd.this.add( desktop, BorderLayout.CENTER );
		        MainWnd.this.add( statusBar, BorderLayout.SOUTH );	
			}
		} );    		
	}
		
	/**
	 * search internal frame in open internal frame's list
	 * @param frameName - name internal frame
	 * @param id - actors' id
	 * @return existing internal frame or null
	 */
	private WebInternalFrame findFrame( String frameName, long id ) {		
		for ( Pair<WebInternalFrame, Long> p : frames ) {
			if ( ( p != null ) && ( p.getFirst() != null ) 
					&& p.getFirst().getName().equals( frameName ) 
					&& ( p.getSecond() != null ) && ( p.getSecond() == id ) )
				return p.getFirst();
		}
		return null;
	}
	
	/**
	 * create new internal frame and add to list 
	 * @param name - internal frame's name
	 * @param panel - contents of internal frame
	 * @param val - actor's id or null
	 * @return new internal frame
	 */
	private WebInternalFrame createFrame( String name, WebPanel panel, long val ) {
		WebInternalFrame frame = new WebInternalFrame( name, true, true, true, false );
		frame.setName( name );		
		frame.setContentPane( panel );
		frame.setBounds( 10, 10, MainWnd.this.getWidth() * 3 / 4 + 10, MainWnd.this.getHeight() * 3 / 4 + 10 );
		frame.addInternalFrameListener( ifl );		
		frames.add( new Pair<WebInternalFrame, Long>( frame, Long.valueOf( val ) ) );
		desktop.add( frame );
		frame.setVisible( true );
		return frame;
	}
	
	/**
	 * initializing main menu
	 * @return new main menu
	 */
	private JMenuBar initMenu() {
		WebMenu modelMenu = new WebMenu("Модели");
		modelMenu.add( new WebMenuItem( new ActionAdapter("Компетенции", null, COMPETENCE_CMD, null, this, 0) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter("Шаблоны профилей", null, PROFILE_CMD, null, this, 0) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter("Профили должностей", null, POST_CMD, null, this, 0) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter("Мероприятия по оценке", null, EVENT_CMD, null, this, 0) ) );
		
		WebMenu optionMenu = new WebMenu("Настройки");
		optionMenu.add( new WebMenuItem(new ActionAdapter("Роли", null, ROLE_CMD, null, this, 0) ) );
		optionMenu.add( new WebMenuItem(new ActionAdapter("Пользователи", null, USER_CMD, null, this, 0) ) );

		WebMenu helpMenu = new WebMenu("Помощь");
		
		WebMenuBar mb = new WebMenuBar();
		mb.add(modelMenu);
		mb.add(optionMenu);
		mb.add(helpMenu);
		return mb;
	}

	/**
	 * initializing DB connection (show dialog, make DataSource and pass to openJPA)
	 * @return true if success
	 */
	private boolean initDb() {
		DBConnectionConfig dbc = ( cfg == null ? null : cfg.getDBConnectionConfig() );
		if ( ( cfg == null ) || ( cfg.getDBConnectionConfig() == null ) || cfg.isShowDBConnectionConfig() ) {
			dbc = DBConnectionDlg.showDBConnectionDlg( this, dbc );
			if ( dbc == null ) {
				if ( ( log != null ) && log.isInfoEnabled() ) log.info( "MainWnd.initDb() : DBConnectionDlg return null " );
				return false; 
			}
		}
		cfg.setDBConnectionConfig( dbc );
		
		try {
			DataSource ds = fillDataSource( dbc );
			if ( ds == null ) {
				if ( ( log != null ) && log.isInfoEnabled() ) log.info( "MainWnd.initDb() : DataSource is null " );
				return false; 
			}
			
			// create EntityManagerFactory object 
			Properties prop = new Properties();
			prop.put( "openjpa.ConnectionFactory", ds ); // DataSource pass to openJPA
			EntityManagerFactory factory = Persistence.createEntityManagerFactory( "CompetenceEntitiesPU", prop );
			ORMHelper.setFactory( factory );
			return true;
		} catch (Exception e) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error("MainWnd.initDb() : ", e );
		}
		return false;
	}

	/**
	 * fill DataSource structure for connection with DB 
	 * @param dbcfg - parameters for DataSource
	 * @return complete DataSource's object or null 
	 */
	private DataSource fillDataSource( DBConnectionConfig dbcfg ) { 
		if ( dbcfg == null ) return null;
		
		SQLServerDataSource msds = new SQLServerDataSource(); // driver depended
		msds.setServerName( dbcfg.getServerAddress() );
		msds.setDatabaseName( dbcfg.getDbName() ); 
		msds.setPortNumber( dbcfg.getDbPort() );
		if ( dbcfg.getDbInstance() != null ) msds.setInstanceName( dbcfg.getDbInstance() );
		msds.setIntegratedSecurity( dbcfg.isIntegratedSecurity() );
		if ( !dbcfg.isIntegratedSecurity() ) {
			msds.setUser( dbcfg.getLogin() );
			msds.setPassword( dbcfg.getPassword() );				
		}

		// see http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/dbsupport_sqlserver.html			
		msds.setSelectMethod("cursor");
		msds.setSendStringParametersAsUnicode(false);
		return msds;
	}

	/**
	 * receive info about current user
	 * @return true if success
	 */
	private boolean initUser() {
		try {
			List<Object[]> lst = ORMHelper.executeQuery( QueryType.SQL, "SELECT HOST_NAME(), SUSER_NAME()", Object[].class );
			if( ( lst == null ) || ( lst.size() != 1 ) || ( lst.get(0).length != 2 ) || 
					( lst.get(0)[0] == null ) || ( lst.get(0)[1] == null ) ) {
				if( log.isErrorEnabled() ) log.error( "MainWnd.initUser() : request return incorrect host, login" );
				return false;
			}
			String host = (String) lst.get(0)[0];
			String login = (String) lst.get(0)[1];
			
			String sql = Joiner.on("").
					join( "SELECT person_id, role_id FROM Minos.PersonAddon WHERE logins LIKE '%",  login, "%' " ).
					toString();
			lst = ORMHelper.executeQuery( QueryType.SQL, sql, Object[].class );
			if( (lst == null) || (lst.size() != 1) || (lst.get(0).length != 2) ) {
				if( log.isErrorEnabled() ) log.error("MainWnd.initUser() : request return incorrect person_id, role_id");
				return false;
			}
			Person person = (Person) ORMHelper.findEntity( Person.class, (Integer) lst.get(0)[0] );
			Role role = (Role) ORMHelper.findEntity( Role.class, (Integer) lst.get(0)[1] );
			Resources.getInstance().put( ResourcesConst.CURRENT_HOST, host );
			Resources.getInstance().put( ResourcesConst.CURRENT_LOGIN, login );
			Resources.getInstance().put( ResourcesConst.CURRENT_PERSON, person );
			Resources.getInstance().put( ResourcesConst.CURRENT_ROLE, role );			
			return true;
		} catch (Exception e) {
			if( log.isErrorEnabled() ) log.error("MainWnd.initUser() :", e);
		}
		return false;
	}

	/**
	 * caching Level table
	 * @return true if success
	 */
	private boolean initLevelsCache() {
		try {
			List<Level> lst = ( List<minos.entities.Level> ) ORMHelper.executeQuery( QueryType.NAMED, "Level.findAll", Level.class );		
			List<Level> lvls = new ArrayList<>( lst );
			Resources.getInstance().put( ResourcesConst.LEVELS_CACHE, lvls );
			return true;
		} catch (Exception e) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MainWnd.initLevelsCache()" + e );
		}
		return false;
	}
	
	/**
	 * load common configuration data
	 * @return true if load success
	 */
	private boolean loadCommonCfg() {
		try {
			List<Object[]> lst = ORMHelper.executeQuery( QueryType.SQL, "SELECT name, resourceKod, value FROM minos.ProgramCommonConfig", null );
			if ( ( lst == null ) || ( lst.size() < 4 )  || ( lst.get( 0 ).length != 3 ) ) {
				if( log.isErrorEnabled() ) log.error( "MainWnd.loadCommonCfg() : request return incorrect name, resourceKod, value" );
				return false;
			}
			
			for ( Object[] objs : lst ) {
				int rkod = (int) objs[1];
				String val = ( String ) objs[2];
				if ( ( rkod == ResourcesConst.CURRENT_FILIAL ) || ( rkod == ResourcesConst.STRUCTURE_ROOT ) ) {
					Resources.getInstance().put( rkod, Integer.parseInt( val ) );
					continue;
				}
				
				if ( rkod == ResourcesConst.BASE_POST_LIST ) {
					Gson gson = new Gson();					
					java.lang.reflect.Type t = new TypeToken<List<Pair<String, Byte>>>(){}.getType();
					List<Pair<String, Byte>> lpsb = gson.fromJson( val, t );
					Resources.getInstance().put( rkod, lpsb );
					continue;
				}

				if ( rkod == ResourcesConst.FILIALS_LIST ) {
					Gson gson = new Gson();					
					java.lang.reflect.Type t = new TypeToken<List<Pair<String, Long>>>(){}.getType();
					List<Pair<String, Long>> lpsl = gson.fromJson( val, t );
					Resources.getInstance().put( rkod, lpsl );
					continue;
				}
				
				if ( rkod == ResourcesConst.FILIALS_PREFIX ) {
					Gson gson = new Gson();
					java.lang.reflect.Type t = new TypeToken<List<Triplet<Integer, String, Byte[]>>>(){}.getType();
					List<Triplet<Integer, String, Byte[]>> lpsb = gson.fromJson( val, t );
					Resources.getInstance().put( rkod, lpsb );
				}
			}			
			return true;
		} catch (Exception e) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MainWnd.loadCommonCfg() : " + e );
		}		
		return false;
	}
}