package minos.ui.frames;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JMenuBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.Start;
import minos.ui.adapters.ActionAdapter;
import minos.ui.panels.CompetencePanel;

import com.alee.extended.layout.ToolbarLayout;
import com.alee.extended.statusbar.WebMemoryBar;
import com.alee.extended.statusbar.WebStatusBar;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.desktoppane.WebDesktopPane;
import com.alee.laf.desktoppane.WebInternalFrame;
import com.alee.laf.menu.WebMenu;
import com.alee.laf.menu.WebMenuBar;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.rootpane.WebFrame;

public class MainWnd extends WebFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static Logger log = LoggerFactory.getLogger( MainWnd.class );	

	
	private static final String COMPETENCE_CMD = "C";
	private static final String PROFILE_CMD = "P";
	private static final String POST_CMD = "D";
	private static final String EVENT_CMD = "E";
	private static final String ROLE_CMD = "R";
	private static final String USER_CMD = "U";

	private WebDesktopPane desktop = null;

	public MainWnd() {
		super( "Компетенции: администрирование" );
		WebLookAndFeel.install();
		setDefaultCloseOperation( WebFrame.DISPOSE_ON_CLOSE );
		WebLookAndFeel.setDecorateFrames( true );		
        setIconImages( WebLookAndFeel.getImages () );
        setBounds( 100, 100, 450, 300 );
        
        setJMenuBar( initMenu() );
        setLayout( new BorderLayout() );
        
        desktop = new WebDesktopPane();

		// Simple status bar
        WebStatusBar statusBar = new WebStatusBar ();
        add(statusBar);

        // Simple memory bar
        WebMemoryBar memoryBar = new WebMemoryBar ();
        memoryBar.setPreferredWidth ( memoryBar.getPreferredSize ().width + 20 );
        statusBar.add ( memoryBar, ToolbarLayout.END );
        
        add(desktop, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        
    	addWindowListener(new WindowAdapter() {
    		
    		@Override
    		public void windowClosing(WindowEvent arg) {
    			super.windowClosing(arg);
    			if( log.isInfoEnabled() ) log.info( "main window closing" );
    			//ProgramConfig config = (ProgramConfig) Resources.getInstance().getResource(ResourcesConst.PROGRAM_CONFIG);
    			//config.setMainWindowWidth(getWidth());
    			//config.setMainWindowHeight(getHeight());
    			//Resources.getInstance().putResource(ResourcesConst.MAIN_FRAME_ACTIVE, false);
    			Start.wakeup();
    		}
    	});
        
        setVisible(true);
	}

	private JMenuBar initMenu() {
		WebMenu modelMenu = new WebMenu("Модели");
		modelMenu.add( new WebMenuItem( new ActionAdapter("Компетенции", null, COMPETENCE_CMD, null, this, 0) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter("Профили", null, PROFILE_CMD, null, this, 0) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter("Должности", null, POST_CMD, null, this, 0) ) );
		modelMenu.add( new WebMenuItem( new ActionAdapter("Мероприятия", null, EVENT_CMD, null, this, 0) ) );
		
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

	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case COMPETENCE_CMD:
			WebInternalFrame ifrm = new WebInternalFrame("Компетенции", true, true, true, false);
			ifrm.add( new CompetencePanel( this ) );
			ifrm.setBounds(10,  10,  200, 400);
			desktop.add(ifrm);
			ifrm.setVisible(true);
			break;
		default:
			break;
		}
	}

}
