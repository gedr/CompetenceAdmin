package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityNotFoundException;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.event.EventListenerList;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Quintet;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.PersonAddonJpaController;
import minos.data.services.FilialInfo;
import minos.entities.Person;
import minos.entities.EstablishedPost;
import minos.entities.Division;
import minos.entities.PersonAddon;
import minos.entities.Role;
import minos.ui.ComponentFabrica;
import minos.ui.MinosCellRenderer;
import minos.ui.adapters.ActionAdapter;
import minos.ui.adapters.MinosTransferable;
import minos.ui.dialogs.ErrorDlg;
import minos.ui.models.MinosListCellEditor;
import minos.utils.AuxFunctions;
import minos.utils.Permission.Block;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import com.alee.extended.layout.ToolbarLayout;
import com.alee.extended.panel.GroupPanel;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.list.WebList;
import com.alee.laf.list.editor.ListEditListener;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.table.WebTable;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.text.WebTextField;
import com.alee.laf.toolbar.WebToolBar;
import com.alee.managers.popup.PopupWay;
import com.alee.managers.popup.WebButtonPopup;
import com.google.gson.Gson;

public class UsersPanel extends BasisPanel implements Runnable, ActionListener, ListEditListener, OrmCommand {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;

	public static final String PERSON_START_LOAD 	= "StartPersonLoad";
	public static final String PERSON_LOAD_SUCCESS 	= "LoadPersonSuccess";
	public static final String PERSON_LOAD_FAIL		= "LoadPersonFail";
	
	private static final String PROMT_NEW_LOGIN = "<html><i><font color=gray>Новый логин</font></i></html> ";
	private static final String CMD_EDIT_LOGINS 			= "E";
	private static final String CMD_OK_LOGIN 				= "O";
	private static final String CMD_SAVE_PERSON_ADDON 		= "S";
	private static final String CMD_DELETE_PERSON_ADDON 	= "D";
	private static final String CMD_RELOAD_PERSON_ADDON 	= "R";
	
	private static final Role EMPTY_ROLE = new Role( "Пусто", null, null );
	
	private static final Logger log = LoggerFactory.getLogger( UsersPanel.class );
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private WebTable tbl;
	private WebList loginList;
	private WebTextField loginsEditField;
	private WebButtonPopup loginPopup;
	private WebComboBox roleComboBox;

	private Window owner;

	private Thread loadingThread;

	private EventListenerList ell;	
	boolean visibleOnlyPersons;
	
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public UsersPanel( Window owner, boolean visibleOnlyPersons ) {
		this( owner, visibleOnlyPersons, true );
	}

	public UsersPanel( Window owner, boolean visibleOnlyPersons, boolean loadNow ) {
		super( true );		
		this.owner = owner;
		this.visibleOnlyPersons = visibleOnlyPersons;

		ell = new EventListenerList();
		
		setLayout( new BorderLayout() );
		if ( !AuxFunctions.isPermission( Block.LOGIN, Operation.READ ) ) {
			add( new WebScrollPane( new WebTextArea( "Запрещено чтение" ) ), 
					BorderLayout.CENTER );
			return;
		}		
		setLayout( new BorderLayout() );
		add( new WebScrollPane( makeTable() ), BorderLayout.CENTER );
		add( makeToolbar(), BorderLayout.NORTH ); 
		addAncestorListener( this );
		addActionLictener( this );
		if ( loadNow ) load();		
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	@SuppressWarnings("unchecked")
	public void editStarted( int index ) {
		// clear text in TextEditor component for last element
		if ( index == loginList.getModel().getSize() - 1 ) {
			( (JTextField ) loginList.getCellEditor().getCellEditor( loginList, index, "" ) ).setText( "" );
		}
	}

	/**
	 * save or remove login and generate new element if needed
	 */
	@Override
	public void editFinished( int index, Object oldValue, Object newValue ) {
		@SuppressWarnings("unchecked")
		DefaultListModel<String> model = ( DefaultListModel<String> ) loginList.getModel();
		String s = ( String ) newValue;
		if ( index == model.getSize() - 1 ) {
			if ( !s.trim().isEmpty() ) model.addElement( PROMT_NEW_LOGIN );
			else model.set( index, PROMT_NEW_LOGIN );
			return;
		}
		if ( ( index < model.getSize() - 1 ) && s.trim().isEmpty() ) {
			model.remove( index );
			return;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void editCancelled( int index ) {
		// make promt string if cancelled last element
		if ( index == loginList.getModel().getSize() - 1 ) {
			( ( DefaultListModel<String> ) loginList.getModel() ).set( index, PROMT_NEW_LOGIN );
		}
	}

	@Override
	public void run() { // load data from DB in another thread
		List<Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division>> lq;
		try {
			fireStartLoading();
			
			lq = loadPersons();
			( ( PersonInfoTM ) tbl.getModel() ).setPersons( lq );
			if ( !visibleOnlyPersons ) {
				final List<Role> lst = loadRoles();
				@SuppressWarnings("unchecked")
				DefaultComboBoxModel<Role> model = ( DefaultComboBoxModel<Role> ) roleComboBox.getModel();
				model.removeAllElements();
				model.addElement( EMPTY_ROLE );
				for ( Role r : lst ) model.addElement( r );
			}

			fireLoadSuccess();
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "UserPanel.run() : ", ex );
			lq = Collections.emptyList();
			( ( PersonInfoTM ) tbl.getModel() ).setPersons( lq );
			fireLoadFail();
		}
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		try {
			switch ( e.getActionCommand() ) {
			case CMD_EDIT_LOGINS : 
				initLoginsEditorForSelectedPerson();
				break;

			case CMD_OK_LOGIN :
				convertLoginListToString();
				break;

			case CMD_RELOAD_PERSON_ADDON :
				reload();
				break;

			case CMD_DELETE_PERSON_ADDON:
				if ( deletePersonAddons() ) AuxFunctions.repaintComponent( tbl );
				break;

			case CMD_SAVE_PERSON_ADDON : 
				if ( savePersonAddons() ) AuxFunctions.repaintComponent( tbl );
				break;
				
			case PERSON_START_LOAD:
				setVisibleGlass( true );
				break;
				
			case PERSON_LOAD_FAIL :
				setVisibleGlass( false );
				if ( tbl != null ) AuxFunctions.repaintComponent( tbl );
				throw new EntityNotFoundException( "PERSON_LOAD_FAIL" );
			
			case PERSON_LOAD_SUCCESS :
				setVisibleGlass( false );
				if ( tbl != null ) AuxFunctions.repaintComponent( tbl );
				break;
			}
		} catch ( AccessControlException aex ) { 
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "UsersPanel.actionPerformed() : ", aex );
			ErrorDlg.show( owner, "Ошибка", "Нет прав на выполнение операции", aex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "UsersPanel.actionPerformed() : ", ex );
			ErrorDlg.show( owner, "Ошибка", "Произошла ошибка при выполнении операции", ex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		}		
	} 
	@Override
	public void internalFrameClosing( InternalFrameEvent e ) { 
		saveBeforeExit();
	}
	
	@Override
	public void windowClosing( WindowEvent e ) { 
		saveBeforeExit();
	}

	@Override
	public void execute( Object obj ) throws Exception {
		PersonInfoTM model = ( PersonInfoTM ) tbl.getModel();
		for ( Pair<PersonAddon, Operation> p : model.getChanges() ) {
			if ( p.getFirst() == null ) continue;
			if ( p.getSecond() == Operation.CREATE ) PersonAddonJpaController.getInstance().create( p.getFirst(), 
					true, false, false );
			if ( p.getSecond() == Operation.UPDATE ) PersonAddonJpaController.getInstance().update( p.getFirst(), 
					true, false, false );
			if ( p.getSecond() == Operation.DELETE ) PersonAddonJpaController.getInstance().delete( p.getFirst(), 
					true, false, false );
		}
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public Person getSeleсtedPerson() {
		int row = tbl.getSelectedRow();
		return row < 0 ? null : ( ( PersonInfoTM ) tbl.getModel() ).getPersons()
				.get( tbl.convertRowIndexToModel( row ) ).getSecond() ;
	}
	
	/**
	 * operation reload 
	 */
	public void reload() {
		if ( ( ( loginsEditField != null ) && loginsEditField.isShowing() )
				|| ( ( roleComboBox != null ) && roleComboBox.isShowing() )
				|| ( ( ( PersonInfoTM ) tbl.getModel() ).isPersonAddonChanged() 
						&& ( WebOptionPane.OK_OPTION != WebOptionPane.showConfirmDialog( owner, 
								"Есть несохраннеые данные. \nПри обновлении они будут потеряны. \nПродолжить ?", 
								"Предупреждение", 
								WebOptionPane.OK_CANCEL_OPTION, WebOptionPane.QUESTION_MESSAGE ) ) ) ) return;
		( ( PersonInfoTM ) tbl.getModel() ).clearAllChange();
		load();
	}

	/**
	 * create complete toolbar component
	 * @return WebToolBar component
	 */
	@SuppressWarnings("unchecked")
	private Component makeToolbar() {
		WebToolBar tb = new WebToolBar();
		if ( !visibleOnlyPersons ) {
			tb.add( ActionAdapter.build( "save", ResourceKeeper.getIcon( IType.SAVE_ALL, 24 ), CMD_SAVE_PERSON_ADDON, 
					"Сохранение созданных, изменненных, ролей и логинов", this, 0 ) );
			tb.add( ActionAdapter.build( "delete", ResourceKeeper.getIcon( IType.DELETE, 24 ), CMD_DELETE_PERSON_ADDON, 
					"Удаление ролей и логинов", this, 0 ) );		
		}
		tb.add( ActionAdapter.build( "reload", ResourceKeeper.getIcon( IType.REFRESH, 24 ), CMD_RELOAD_PERSON_ADDON,
				"Перегрузить данные", this, 0 ) );
		tb.add( ComponentFabrica.createTableFilter( ( TableRowSorter<PersonInfoTM> ) tbl.getRowSorter(), 
				24, "фильтр" ), ToolbarLayout.FILL );
		return tb;
	}

	/**
	 * initialize table component
	 * @return WebTable component
	 */
	private Component makeTable() {
		PersonInfoTM model =  new PersonInfoTM( !visibleOnlyPersons );
		tbl = new WebTable( model );		
		tbl.setName( "PERSONS_NAME" );
		tbl.setAutoResizeMode( WebTable.AUTO_RESIZE_OFF );
		tbl.setRowSorter( new TableRowSorter<PersonInfoTM>( model ) );		
		tbl.setDefaultRenderer( Object.class, new MinosCellRenderer<Object>( 24 ) );
		if ( !visibleOnlyPersons ) {
			tbl.setDragEnabled( true );
			tbl.setTransferHandler( new PersonTransferHandler() );
			DefaultCellEditor dce = new DefaultCellEditor( makeLoginEditor() );
			dce.setClickCountToStart( 2 );
			tbl.getColumnModel().getColumn( PersonInfoTM.LOGIN_COLUMN ).setCellEditor( dce );
			
			dce = new DefaultCellEditor( makeRoleEditor() );
			dce.setClickCountToStart( 2 );
			tbl.getColumnModel().getColumn( PersonInfoTM.ROLE_COLUMN ).setCellEditor( dce );

			tbl.getColumnModel().getColumn( PersonInfoTM.ROLE_COLUMN )
			.setCellRenderer( new MinosCellRenderer<Role>( 16 ) );
		}
		return tbl;
	}
	
	@SuppressWarnings("unchecked")
	private WebComboBox makeRoleEditor() {
		roleComboBox = new WebComboBox( new DefaultComboBoxModel<Role>() );
		roleComboBox.setDrawBorder( false );
		roleComboBox.setDrawFocus( false );
		roleComboBox.setRenderer( new MinosCellRenderer<Role>( 16 ) );
		return roleComboBox;
	}
	
	/**
	 * initialize components and reference for edit logins for selected person
	 * @return JTextField component for start edit
	 */
	private JTextField makeLoginEditor() {
		WebButton btn2 = new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 16 ), 
				CMD_OK_LOGIN, "Сохранение результата", this, 0 ) );

		loginList = new WebList( new DefaultListModel<String>() );
        loginList.setEditable( true );
        loginList.addListEditListener( this );
        loginList.setCellEditor( new MinosListCellEditor( "Новый логин" ) );
		
		WebScrollPane pane = new WebScrollPane( loginList );
		pane.setPreferredSize( new Dimension( 175, 100 ) );
        
		GroupPanel content = new GroupPanel( 2, false, pane, btn2 );
        content.setMargin( 5 );
        
		WebButton btn1 = new WebButton( ActionAdapter.build( "...", null, CMD_EDIT_LOGINS, null, this, 0 ) );
		btn1.setFocusable( false );
		btn1.setLeftRightSpacing ( 0 );

		loginsEditField = new WebTextField( false );	
		loginsEditField.setEditable( false );
		loginsEditField.setTrailingComponent( btn1 );
		loginsEditField.setDrawFocus( false );
		loginsEditField.setDrawShade( false );

        loginPopup = new WebButtonPopup( btn1, PopupWay.leftCenter );
        loginPopup.setContent ( content );
        loginPopup.setDefaultFocusComponent ( loginList );
		return loginsEditField;
	}

	/**
	 * run thread for loading Actors entities from DB
	 */
	public void load() {
		if ( ( loadingThread != null ) && loadingThread.isAlive() ) return;
		loadingThread = new Thread( this );
		loadingThread.start();
	}

	/**
	 * load list of Person, EstablishedPost and Division entities
	 * @return
	 */
	private List<Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division>> loadPersons() {
		Pair<Integer, Integer> pii = getPersonIds();
		String jpql = ResourceKeeper.getQuery( visibleOnlyPersons ? QType.JPQL_LOAD_PERSONS_ONLY 
				: QType.JPQL_LOAD_PERSONS_AND_PERSON_ADDON );
		List<Object[]> lst = OrmHelper.findByQueryWithParam( QueryType.JPQL, jpql, Object[].class, 
				new Pair<Object, Object>( "stats", Arrays.asList( Person.STATUS_ACTIVE ) ),
				new Pair<Object, Object>( "startPersonID", pii.getFirst() ),
				new Pair<Object, Object>( "stopPersonID", pii.getSecond() ) );
		if ( ( lst == null ) || ( lst.size() == 0 ) ) return Collections.emptyList();
		
		List<FilialInfo> lfi = ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO );
		List<Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division>> rs = new ArrayList<>();
		for ( Object[] objs : lst ) {
			Person p = ( Person ) objs[0];
			if ( p == null ) continue;
			EstablishedPost ep = ( EstablishedPost ) objs[1];
			Division d = ( Division ) objs[2];
			for ( FilialInfo fi : lfi ) {
				if ( ( fi.getDivisionCodeMin() <= d.getId() ) && ( d.getId() <= fi.getDivisionCodeMax() )
						&& ( 0 < fi.getCode() ) && ( fi.getCode() < 100 ) ) {
					rs.add( new Quintet<>( fi, p, p.getPersonAddon(), ep, d ) );
				}
			}
		}	
		return rs;
	}
	
	/**
	 * load Role entities
	 * @return role's list
	 */
	private List<Role> loadRoles() {
		List<Role> lst = OrmHelper.findByQuery( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_ROLE ), Role.class );
		return ( ( ( lst != null ) && ( lst.size() > 0 ) ) ? lst : Collections.<Role>emptyList() );
	}

	/**
	 * make range of Person IDs for current branch office
	 * @return Pair<min_person_id, max_person_id>
	 */
	private Pair<Integer, Integer> getPersonIds() {
		List<FilialInfo> lfi = ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO );
		Byte cbo = ResourceKeeper.getObject( ResourceKeeper.OType.DEFAULT_BRANCH_OFFICE_CODE );
		for ( FilialInfo fi : lfi ) {
			if ( fi.getCode() == cbo ) return new Pair<>( fi.getPersonCodeMin(), fi.getPersonCodeMax() );
		}
		return null;
	}
	
	/**
	 * initialize Editor for User's login list
	 */
	private void initLoginsEditorForSelectedPerson() {
		@SuppressWarnings("unchecked")
		DefaultListModel<String> model = ( DefaultListModel<String> ) loginList.getModel();
		model.removeAllElements();
		if ( ( loginsEditField.getText() != null ) && !loginsEditField.getText().trim().isEmpty() ) {
			Gson gson = new Gson();			
			String[] ls = gson.fromJson( loginsEditField.getText().replace( "\\", "\\\\" ), String[].class );
			for ( String s : ls ) model.addElement( s );
		}
		model.addElement( PROMT_NEW_LOGIN );
	}
	
	/**
	 * convert Logins in JList to JSON string
	 */
	private void convertLoginListToString() {
		@SuppressWarnings("unchecked")
		DefaultListModel<String> m = ( DefaultListModel<String> ) loginList.getModel();
		if ( ( m == null ) || ( m.getSize() < 2 ) ) {
			loginsEditField.setText( "" );
		} else {
			String[] ss = new String[m.getSize() - 1];
			for ( int i = 0; i < m.getSize() - 1; i++ ) ss[i] = m.get( i );
			Gson gson = new Gson();
			loginsEditField.setText( gson.toJson( ss ).replace( "\\\\", "\\" ) );
		}
		if ( ( loginPopup != null ) && loginPopup.isVisible() ) loginPopup.hidePopup();
	}

	/**
	 * delete PersonAddon entities for selected row 
	 * @return true if operation complete successfully
	 * @throws Exception
	 */
	private boolean deletePersonAddons() throws Exception {
		if ( visibleOnlyPersons || ( tbl.getSelectedRowCount() == 0 )
				|| ( ( loginsEditField != null ) && loginsEditField.isShowing() ) 
				|| ( ( roleComboBox != null ) && roleComboBox.isShowing() ) ) return false;	
		PersonInfoTM model = ( PersonInfoTM ) tbl.getModel();
		boolean delPerm = AuxFunctions.isPermission( Block.LOGIN, Operation.DELETE );
		boolean userPersonAddon = false;
		boolean otherPersonAddon = false;
		int mri = 0; // model row index 
		for ( int row : tbl.getSelectedRows() ) {
			mri = tbl.convertRowIndexToModel( row );
			if ( delPerm ) { 
				model.deletePersonAddon( mri );
				continue;
			}
			Operation op = model.getRowState( mri );
			if (  op == Operation.CREATE ) userPersonAddon = true;
			if ( ( op == Operation.READ ) || ( op == Operation.UPDATE ) ) otherPersonAddon = true;
		}
		if ( !userPersonAddon && !otherPersonAddon ) return true;
		if ( userPersonAddon ) {
			int res = WebOptionPane.showConfirmDialog( owner, "У Вас нет прав на удаление. \n "
					+ "Однако вы можете удалить роли, \n  созданные Вами и не сохраненые в БД. \n Удалить их ?", 
					"Запрос", WebOptionPane.OK_CANCEL_OPTION, WebOptionPane.QUESTION_MESSAGE );
			if ( res == WebOptionPane.OK_OPTION ) otherPersonAddon = false;
		}
		if ( otherPersonAddon ) throw new AccessControlException( "UserPanel.deletePersonAddon() :"
				+ " delete PersonAddon permission denied" );

		for ( int row : tbl.getSelectedRows() ) {
			mri = tbl.convertRowIndexToModel( row );
			if ( model.getRowState( mri ) == Operation.CREATE ) model.deletePersonAddon( mri );
		}		
		return true;
	}
	
	/**
	 * save created/updated/deleted PersonAddon entities
	 * @return true if operation complete successfully 
	 * @throws Exception
	 */
	private boolean savePersonAddons() throws Exception {
		PersonInfoTM model = ( PersonInfoTM ) tbl.getModel();
		if ( !model.isPersonAddonChanged() )return false;
		OrmHelper.executeAsTransaction( this, null );
		model.clearAllChange();
		return true;
	}
	
	private void saveBeforeExit() {
		if ( visibleOnlyPersons || !( ( PersonInfoTM ) tbl.getModel() ).isPersonAddonChanged() 
				|| ( WebOptionPane.YES_OPTION != WebOptionPane.showConfirmDialog( owner, 
							"Есть несохраннеые данные. \nСохранить изменения ?", 
							"Запрос", WebOptionPane.YES_NO_OPTION, WebOptionPane.QUESTION_MESSAGE ) ) )  return;
		actionPerformed( new ActionEvent( this, 1, CMD_SAVE_PERSON_ADDON ) );
	}

	private void fireStartLoading() {
		ActionEvent ae = new ActionEvent( this, 1, PERSON_START_LOAD );
		for ( ActionListener al : ell.getListeners( ActionListener.class ) ) al.actionPerformed( ae );
	}

	private void fireLoadSuccess() {
		ActionEvent ae = new ActionEvent( this, 1, PERSON_LOAD_SUCCESS );
		for ( ActionListener al : ell.getListeners( ActionListener.class ) ) al.actionPerformed( ae );	
	}

	private void fireLoadFail() {
		ActionEvent ae = new ActionEvent( this, 1, PERSON_LOAD_FAIL );
		for ( ActionListener al : ell.getListeners( ActionListener.class ) ) al.actionPerformed( ae );	
	}

	public void addActionLictener( ActionListener l ) {
		ell.add( ActionListener.class, l );
	}

	public void removeActionLictener( ActionListener l ) {
		ell.remove( ActionListener.class, l );
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private class PersonTransferHandler extends  TransferHandler {
		// =============================================================================================================
		// Constants
		// =============================================================================================================
		private static final long serialVersionUID = 1L;
		
		// =============================================================================================================
		// Fields
		// =============================================================================================================
		private DataFlavor df;

		// =================================================================================================================
		// Constructors
		// =================================================================================================================
		public PersonTransferHandler() {
			try {
				df = new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType + ";class=" + PersonAddon.class.getName() );
			} catch ( Exception ex ) {
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( "PersonTransferHandler() : ", ex );
			}
		}

		// =================================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =================================================================================================================
		@Override
		public int getSourceActions( JComponent arg ) {
			return TransferHandler.LINK;
		}

		@Override
		protected Transferable createTransferable( JComponent arg ) {
			if ( ( tbl.getSelectedRowCount() < 1 ) ) return null;
			for ( int i : tbl.getSelectedRows() ) {
				if ( tbl.isCellEditable( i, tbl.convertColumnIndexToView( PersonInfoTM.ROLE_COLUMN ) ) ) {
					return new MinosTransferable( new PersonAddon() );
				}
			}
			return null;					
		}
		
		@Override
		protected void exportDone( JComponent source, Transferable data, int action ) {
			super.exportDone( source, data, action );
			try {
				Object obj = data.getTransferData( df );
				if ( ( obj == null ) || !( obj instanceof PersonAddon ) 
						|| ( ( ( PersonAddon ) obj ).getRole() == null ) ) return;

				for ( int i : tbl.getSelectedRows() ) {
					int col = tbl.convertColumnIndexToView( PersonInfoTM.ROLE_COLUMN );
					if ( tbl.isCellEditable( i, col ) ) tbl.setValueAt( ( ( PersonAddon ) obj ).getRole(), i, col );
				}
				AuxFunctions.repaintComponent( tbl );
			} catch ( Exception ex ) {
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( "PersonTransferHandler.exportDone() : ", ex );
			}
		}			
	}

	
	private class PersonInfoTM extends DefaultTableModel {
		// =============================================================================================================
		// Constants
		// =============================================================================================================
		private static final long serialVersionUID = 1L;
		
		private static final int FIO_COLUMN 		= 0;
		private static final int POSITION_COLUMN 	= 1;
		private static final int DIVISION_COLUMN	= 2;
		private static final int BOFFICE_COLLUMN 	= 3;
		private static final int LOGIN_COLUMN 		= 4;
		private static final int ROLE_COLUMN 		= 5;
		private static final int STATUS_COLUMN 		= 6;

		// =============================================================================================================
		// Fields
		// =============================================================================================================
		private String[] columnNames;
		private boolean visibleRoleLogins;
		private List<Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division>> persons;
		private Map<Integer, Pair<PersonAddon, Operation>> changes;
		
		// =================================================================================================================
		// Constructors
		// =================================================================================================================
		public PersonInfoTM( boolean visibleRoleLogins ) {
			this.visibleRoleLogins = visibleRoleLogins;
			columnNames =  new String[visibleRoleLogins ? 7 : 4];
			columnNames[FIO_COLUMN] 		= "ФИО";
			columnNames[POSITION_COLUMN] 	= "Должность";
			columnNames[DIVISION_COLUMN] 	= "Подразделение";
			columnNames[BOFFICE_COLLUMN] 	= "Филиал";
			if ( visibleRoleLogins ) {
				columnNames[LOGIN_COLUMN] 	= "Логины";
				columnNames[ROLE_COLUMN] 	= "Роль";
				columnNames[STATUS_COLUMN] 	= "Статус";
				changes = new TreeMap<>();
			} 
			persons = Collections.emptyList();
		}

		// =================================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =================================================================================================================
		@Override
		public Class<?> getColumnClass( int col ) {
			return col == ROLE_COLUMN ? Role.class : ( col == STATUS_COLUMN ? Operation.class : String.class );
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName( int col ) {
			return columnNames[col];
		}

		@Override
		public int getRowCount() {
			return ( persons == null ? 0 : persons.size() );
		}

		@Override
		public Object getValueAt( int row, int col ) {
			if ( ( row < 0 ) || ( row >= persons.size() ) || ( col < 0 ) || ( col >= columnNames.length ) ) return null;
			Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division> q = persons.get( row );
			if ( q == null ) return null;
			switch( col ) {
			case FIO_COLUMN 	 : return q.getSecond() == null ? null : q.getSecond().getFullName();
			case POSITION_COLUMN : return q.getFourth() == null ? null : q.getFourth().getName();
			case DIVISION_COLUMN : return q.getFifth() == null ? null : q.getFifth().getFullName();
			case BOFFICE_COLLUMN : return q.getFirst() == null ? null : q.getFirst().getName();
			case LOGIN_COLUMN 	 : return q.getThird() == null ? null : q.getThird().getLogins();
			case ROLE_COLUMN 	 : return q.getThird() == null ? null : q.getThird().getRole();
			case STATUS_COLUMN	 : return ( ( ( changes == null ) || !changes.containsKey( q.getSecond().getId() ) )
				? Operation.READ : changes.get( q.getSecond().getId() ).getSecond() );
			}
			return null;
		}

		@Override
		public boolean isCellEditable( int row, int col ) {
			if ( visibleRoleLogins && ( ( col == ROLE_COLUMN ) || ( col == LOGIN_COLUMN ) )
					&& ( ( ( persons.get( row ).getThird() == null ) 
							&& AuxFunctions.isPermission( Block.LOGIN, Operation.CREATE ) ) 
							|| ( ( persons.get( row ).getThird() != null )
									&& AuxFunctions.isPermission( Block.LOGIN, Operation.UPDATE ) ) ) ) return true;				
			return false;
		} 

		@Override
		public void setValueAt( Object value, int row, int col ) {
			Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division> q = persons.get( row );
			Operation op = ( ( ( changes == null ) || ( changes.get( q.getSecond().getId() ) == null ) )? null 
					: changes.get( q.getSecond().getId() ).getSecond() ); 
			if ( ( value == null ) || ( row < 0 ) || ( row >= persons.size() )
					|| ( ( col != LOGIN_COLUMN ) && ( col != ROLE_COLUMN ) ) 
					|| ( ( col == LOGIN_COLUMN ) && ( ( String ) value ).trim().isEmpty() ) 
					|| ( Operation.DELETE == op ) ) return;
			
			if ( q.getThird() == null ) { // need new PersonAddon entity
				q.setThird( new PersonAddon() );
				q.getThird().setPerson( q.getSecond() );				
				changes.put( q.getSecond().getId(), new Pair<>( q.getThird(), Operation.CREATE ) );
			}
			boolean flagUpdate = false;
			if ( ( col == LOGIN_COLUMN ) 
					&& ( ( ( q.getThird().getLogins() == null ) && ( value != null ) )
							|| ( ( q.getThird().getLogins() != null ) 
									&& !q.getThird().getLogins().equals( value ) ) ) ) {
				q.getThird().setLogins( ( String ) value );
				flagUpdate = true;
			}					
			if ( ( col == ROLE_COLUMN )
					&& ( ( ( q.getThird().getRole() == null ) && ( value != null ) )
							|| ( ( q.getThird().getRole() != null) && !q.getThird().getRole().equals( value ) ) ) ) {
				Role r = ( Role ) value;
				q.getThird().setRole( r == EMPTY_ROLE ? null : r );
				flagUpdate = true;
			}
			if ( flagUpdate && !changes.containsKey( q.getSecond().getId() ) ) {
				changes.put( q.getSecond().getId(), new Pair<>( q.getThird(), Operation.UPDATE ) );
			}
		}

		// =============================================================================================================
		// Methods
		// =============================================================================================================
		public void setPersons( List<Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division>> lst ) {
			List<Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division>> tmp = persons;			
			persons = lst;
			if ( ( tmp != null ) && ( tmp.size() > 0 ) ) tmp.clear();
		}

		public List<Quintet<FilialInfo, Person, PersonAddon, EstablishedPost, Division>> getPersons() {
			return persons;
		}

		public boolean isPersonAddonChanged() {
			return ( ( changes == null ) || ( changes.size() == 0 ) ) ? false : true; 
		}
		
		public void clearAllChange() {
			if ( changes != null ) changes.clear();
		}
		
		public Operation getRowState( int modelRow ) {
			if ( ( persons == null ) || ( 0 > modelRow ) || ( modelRow <= persons.size() ) ) return null;
			if ( ( changes == null ) 
					|| !changes.containsKey( persons.get( modelRow ).getSecond().getId() ) ) return Operation.READ;
			return changes.get( persons.get( modelRow ).getSecond().getId() ).getSecond();
		}
		
		public void deletePersonAddon( int modelRow ) {
			if ( !visibleRoleLogins || ( persons == null ) || ( modelRow < 0 ) || ( persons.size() <= modelRow ) 
					|| persons.get( modelRow ).getThird() == null ) return;
			if ( changes == null ) changes = new TreeMap<>();
			int pid = persons.get( modelRow ).getSecond().getId();
			if ( !changes.containsKey( pid ) ) {
				changes.put( pid, new Pair<>( persons.get( modelRow ).getThird(), Operation.DELETE ) );
			} else {
				Pair<PersonAddon, Operation> p = changes.get( pid );
				if ( Operation.CREATE == p.getSecond() ) changes.remove( pid );
				else p.setSecond( Operation.DELETE );
			}
			persons.get( modelRow ).setThird( null );
		}
		
		public Collection<Pair<PersonAddon, Operation>> getChanges() {
			if ( ( changes == null ) || ( changes.size() == 0 ) ) return Collections.emptyList();
			return changes.values();
		}
	}
}
