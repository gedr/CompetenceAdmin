package minos.ui;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import minos.Start;
import minos.data.transport.Packet;
import minos.data.transport.PacketLoader;
import minos.data.transport.PacketUnloader;
import minos.entities.Person;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.ComponentDlg;
import minos.ui.dialogs.ErrorDlg;
import minos.ui.panels.ActorsInfoPanel;
import minos.ui.panels.CompetencePanel;
import minos.ui.panels.MeasurePanel;
import minos.ui.panels.PersonPanel;
import minos.ui.panels.PostProfilePanel;
import minos.ui.panels.ProfilePatternPanel;
import minos.ui.panels.RolePanel;
import minos.ui.panels.UnloadConfigPanel;
import minos.utils.ProgramConfig;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;

import com.alee.extended.layout.ToolbarLayout;
import com.alee.extended.statusbar.WebMemoryBar;
import com.alee.extended.statusbar.WebStatusBar;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.desktoppane.WebDesktopPane;
import com.alee.laf.desktoppane.WebInternalFrame;
import com.alee.laf.filechooser.WebFileChooser;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuBar;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.separator.WebSeparator;

public class MainWnd extends WebFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static Logger log = LoggerFactory.getLogger( MainWnd.class );	
	
	private static final String CMD_COMPETENCE 	= "1";
	private static final String CMD_PROFILE 	= "2";
	private static final String CMD_POST 		= "3";
	private static final String CMD_EVENT 		= "4";
	private static final String CMD_ROLE 		= "5";
	private static final String CMD_USER 		= "6";
	private static final String CMD_UNLOAD		= "7";
	private static final String CMD_LOAD		= "8";
	private static final String CMD_CASCADE 	= "9";
	private static final String CMD_VWINDOWS 	= "A";
	private static final String CMD_HWINDOWS 	= "B";
	private static final String CMD_PARAM 		= "C";
	
	private static final String FRAME_COMPETENCES 		= "Компетенции";
	private static final String FRAME_PROFILE_PATTERNS 	= "Шаблоны профилей";
	private static final String FRAME_POST_PROFILES 	= "Профили должностей";
	private static final String FRAME_MEASURES			= "Мероприятия оценки";
	private static final String FRAME_ROLES				= "Роли";
	private static final String FRAME_USERS				= "Пользователи";
	private static final String FRAME_PARAM				= "Параметры теста";
	
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

	public MainWnd() {
		super( "Компетенции: администрирование" );
		this.cfg = ( ProgramConfig ) ResourceKeeper.getObject( OType.PROGRAM_CONFIG );
		WebLookAndFeel.install();
		setDefaultCloseOperation( WebFrame.DISPOSE_ON_CLOSE );
		WebLookAndFeel.setDecorateFrames( true );		
        setIconImages( WebLookAndFeel.getImages () );        
        setBounds( cfg != null ? cfg.getMainWindowBound() : new Rectangle( 100, 100, 450, 300 ) );        

        addWindowListener(new WindowAdapter() {

        	@Override
        	public void windowClosing(WindowEvent arg) {   
        		super.windowClosing(arg);
        		if( ( log != null ) && log.isDebugEnabled() ) log.debug( "MainWindow closing" );
        		if ( cfg != null ) cfg.setMainWindowBound( MainWnd.this.getBounds() );
        		Start.getInstance().wakeup();
        	}
        });   
		desktop = new WebDesktopPane();
		statusBar = new WebStatusBar ();		       		        

        // Simple memory bar
        WebMemoryBar memoryBar = new WebMemoryBar ();
        memoryBar.setPreferredWidth( memoryBar.getPreferredSize ().width + 20 );
        
        statusBar.add( new WebLabel( "DB:" + cfg.getDBConnectionConfig().getDbName() + 
        		"  User: " + ( ( Person ) ResourceKeeper.getObject( OType.CURRENT_PERSON ) ).
        		getSurnameAndInitials( false, true ) + "  [" + ResourceKeeper.getObject( OType.CURRENT_LOGIN ) + "]" ) );
        statusBar.add( memoryBar, ToolbarLayout.END );  
        

        MainWnd.this.setJMenuBar( makeMenu() );
        MainWnd.this.setLayout( new BorderLayout() );		        
        MainWnd.this.add( desktop, BorderLayout.CENTER );
        MainWnd.this.add( statusBar, BorderLayout.SOUTH );	

        setVisible(true);
        Thread.setDefaultUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException( Thread t, Throwable ex ) {
				log.error( "uncaughtException thread = " + t + "\nexception = " + ex );
				
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		WebInternalFrame frame;
		try {
			switch (e.getActionCommand()) {
			case CMD_COMPETENCE:
				frame = findFrame( FRAME_COMPETENCES, 0 );
				if ( frame != null ) desktop.moveToFront( frame );
				else frame = createFrame( FRAME_COMPETENCES, new CompetencePanel( this, true ), 0 );
				break;

			case CMD_PROFILE:
				frame = findFrame( FRAME_PROFILE_PATTERNS, 0 );
				if ( frame != null ) desktop.moveToFront( frame );
				else frame = createFrame( FRAME_PROFILE_PATTERNS, new ProfilePatternPanel( this ), 0 );
				break;

			case CMD_POST:
				frame = findFrame( FRAME_POST_PROFILES, 0 );
				if ( frame != null ) desktop.moveToFront( frame );
				else frame = createFrame( FRAME_POST_PROFILES, new PostProfilePanel( this ), 0 );
				break;

			case CMD_EVENT:
				frame = findFrame( FRAME_MEASURES, 0 );
				if ( frame != null ) desktop.moveToFront( frame );
				else frame = createFrame( FRAME_MEASURES, new MeasurePanel( this ), 0 );
				break;

			case CMD_ROLE:
				frame = findFrame( FRAME_ROLES, 0 );
				if ( frame != null ) desktop.moveToFront( frame );
				else frame = createFrame( FRAME_ROLES, new RolePanel( this ), 0 );
				break;

			case CMD_USER:
				frame = findFrame( FRAME_USERS, 0 );
				if ( frame != null ) desktop.moveToFront( frame );
				//else frame = createFrame( FRAME_MEASURES, new UsersPanel( this, false ), 0 );
				else frame = createFrame( FRAME_USERS, new PersonPanel( this, false ), 0 );
				break;

			case CMD_PARAM:
				frame = findFrame( FRAME_PARAM, 0 );
				if ( frame != null ) desktop.moveToFront( frame );
				else frame = createFrame( FRAME_PARAM, new ActorsInfoPanel( this ), 0 );
				break;

			case CMD_LOAD :
				loadData();
				break;

			case CMD_UNLOAD :
				unloadData();
				break;

			case CMD_CASCADE :
				orderWindows( 0 );
				break;

			case CMD_HWINDOWS :
				orderWindows( 1 );
				break;

			case CMD_VWINDOWS :
				orderWindows( 2 );
				break;

			}
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MainWnd.actionPerformed() : ", ex );
			ErrorDlg.show( this, "Ошибка", "Произошла ошибка при выполнении операции", ex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		}		
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
	private JMenuBar makeMenu() {
		WebMenu modelMenu = new WebMenu( "Модели" );
		modelMenu.add( new WebMenuItem( new ActionAdapter( "Компетенции", null, CMD_COMPETENCE, null, this, 0 ) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter( "Шаблоны профилей", null, CMD_PROFILE, null, this, 0 ) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter( "Профили должностей", null, CMD_POST, null, this, 0 ) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter( "Мероприятия по оценке", null, CMD_EVENT, null, this, 0 ) ) );
		
		WebMenu optionMenu = new WebMenu( "Настройки" );
		optionMenu.add( new WebMenuItem( new ActionAdapter("Параметры теста", null, CMD_PARAM, null, this, 0 ) ) );
		optionMenu.add( new WebSeparator() );
		optionMenu.add( new WebMenuItem( new ActionAdapter("Роли", null, CMD_ROLE, null, this, 0 ) ) );
		optionMenu.add( new WebMenuItem( new ActionAdapter("Пользователи", null, CMD_USER, null, this, 0 ) ) );
		optionMenu.add( new WebSeparator() );
		optionMenu.add( new WebMenuItem( new ActionAdapter( "Выгрзка данных", null, CMD_UNLOAD, null, this, 0 ) ) );
		optionMenu.add( new WebMenuItem( new ActionAdapter( "Загрузка данных", null, CMD_LOAD, null, this, 0 ) ) );

		WebMenu winMenu = new WebMenu("Окна");
		winMenu.add( new WebMenuItem( new ActionAdapter( "Каскад", null, CMD_CASCADE, null, this, 0 ) ) );
		winMenu.add( new WebMenuItem( new ActionAdapter( "Горизонтально", null, CMD_HWINDOWS, null, this, 0 ) ) );
		winMenu.add( new WebMenuItem( new ActionAdapter( "Вертикально", null, CMD_VWINDOWS, null, this, 0 ) ) );

		WebMenu helpMenu = new WebMenu("Помощь");
		
		WebMenuBar mb = new WebMenuBar();
		mb.add(modelMenu);
		mb.add(optionMenu);
		mb.add(winMenu);
		mb.add(helpMenu);
		return mb;
	}
	
	private void unloadData() throws Exception {
		UnloadConfigPanel ucp = new UnloadConfigPanel();
		if ( JOptionPane.OK_OPTION != ComponentDlg.show( this, "Выбор выгружаемых таблиц" , ucp, null ) ) return;
		PacketUnloader pu = new PacketUnloader();
		Packet pac = pu.unload( ucp.getTables() );
		PacketUnloader.saveToFileAsJson( pac, ucp.getSaveDir().getAbsolutePath() + "\\CompetenceData_" 
				+ pac.getCurrentUuid() + ".zip" );
	}

	private void loadData() throws Exception {
		WebFileChooser fileChooser = new WebFileChooser();
		fileChooser.setMultiSelectionEnabled ( false );
		if ( fileChooser.showOpenDialog( this ) != WebFileChooser.APPROVE_OPTION ) return;
		Packet pac = PacketLoader.loadFromJsonFile( fileChooser.getSelectedFile().getCanonicalPath() );
		PacketLoader pl = new PacketLoader();
		System.out.println( pac == null );
		pl.load( pac );
	}

	/**
	 * order JInternalFrame as tile or cascade
	 * @param type 0 - cascade; 1 - horizontal tile; 2 - vertical tile
	 */
	private void orderWindows( int type ) { 
		if ( desktop == null ) return;

		int count = desktop.getAllFrames().length;
		int x = 0;
		int y = 0;
		int w = desktop.getWidth() / ( type == 0 ? 2 : ( type == 1 ? 1 : count ) );
		int h = desktop.getHeight() / ( type == 0 ? 2 : ( type == 1 ? count : 1 ) );
		int offsx = ( type == 0 ? ( w / count ) : ( type == 1 ? 0 : desktop.getWidth() / count ) );
		int offsy = ( type == 0 ? ( h / count ): ( type == 1 ? desktop.getHeight() / count : 0 ) );
		for ( JInternalFrame f : desktop.getAllFrames() ) {
			try {
				f.setMaximum( false );
				f.reshape( x, y, w, h );
				x += offsx;
				y += offsy;
			} catch ( PropertyVetoException e ) { }
		}
	}
}