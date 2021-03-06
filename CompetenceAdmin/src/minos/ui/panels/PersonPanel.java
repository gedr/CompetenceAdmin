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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.PersonAddonJpaController;
import minos.data.services.FilialInfo;
import minos.entities.OrgUnit;
import minos.entities.Person;
import minos.entities.PersonAddon;
import minos.entities.Role;
import minos.ui.ComponentFabrica;
import minos.ui.MinosCellRenderer;
import minos.ui.adapters.ActionAdapter;
import minos.ui.adapters.MinosTransferable;
import minos.ui.dialogs.ErrorDlg;
import minos.ui.models.MinosListCellEditor;
import minos.utils.AuxFunctions;
import minos.utils.ProgramConfig;
import minos.utils.ResourceKeeper;
import minos.utils.Permission.Block;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

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

public class PersonPanel extends BasisPanel implements ActionListener, ListEditListener, OrmCommand, TableColumnModelListener {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;

	private static final String PROMT_NEW_LOGIN = "<html><i><font color=gray>����� �����</font></i></html> ";

	private static final String CMD_EDIT_LOGINS 			= "E";
	private static final String CMD_OK_LOGIN 				= "O";
	private static final String CMD_SAVE_PERSON_ADDON 		= "S";
	private static final String CMD_DELETE_PERSON_ADDON 	= "D";
	private static final String CMD_RELOAD_PERSON_ADDON 	= "R";
	
	private static final Role EMPTY_ROLE = new Role( "�����", null, null );
	
	private static final Logger log = LoggerFactory.getLogger( UsersPanel.class );
	
	private static final String JPQL_LOAD_ORG_UNIT_AND_PERSON_ADDON = " SELECT entity "
			+ " FROM OrgUnit entity "
			+ " INNER JOIN FETCH entity.person "
			+ " LEFT  JOIN FETCH entity.person.personAddon "
			+ " LEFT  JOIN FETCH entity.person.personAddon.role "
			+ " LEFT  JOIN FETCH entity.person.personAddon.logins "
			+ " INNER JOIN FETCH entity.division "
			+ " INNER JOIN FETCH entity.post "
			+ " WHERE entity.person.id BETWEEN :startPersonID AND :stopPersonID "
			+ " AND entity.person.status IN (:stats) "
			+ " AND ( (CURRENT_TIMESTAMP BETWEEN entity.person.beginDate AND entity.person.endDate) "
			+ " OR (:dts > entity.person.endDate) ) ";

	private static final String JPQL_LOAD_ORG_UNIT_ONLY = " SELECT entity "
			+ " FROM OrgUnit entity "
			+ " INNER JOIN FETCH entity.person "
			+ " INNER JOIN FETCH entity.division "
			+ " INNER JOIN FETCH entity.post "
			+ " WHERE entity.person.id BETWEEN :startPersonID AND :stopPersonID "
			+ " AND entity.person.status IN (:stats) "
			+ " AND ( (CURRENT_TIMESTAMP BETWEEN entity.person.beginDate AND entity.person.endDate) "
			+ " OR (:dts > entity.person.endDate) ) ";

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private WebTable tbl;
	private WebList loginList;
	private WebTextField loginsEditField;
	private WebButtonPopup loginPopup;
	private WebComboBox roleComboBox;

	private Window owner;
	
	private ProgramConfig pconf;

	private boolean visibleOnlyPersons;
	private boolean visibleDeleted;
	private boolean visibleDismiss;
	private boolean visibleSuspend;
	private boolean visiblePensioner;
	
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public PersonPanel( Window owner, boolean visibleOnlyPersons ) {
		this( owner, visibleOnlyPersons, true );
	}

	public PersonPanel( Window owner, boolean visibleOnlyPersons, boolean loadNow ) {
		super( true );		
		pconf = ( ProgramConfig ) ResourceKeeper.getObject( ResourceKeeper.OType.PROGRAM_CONFIG );
		this.owner = owner;
		this.visibleOnlyPersons = visibleOnlyPersons;

		setLayout( new BorderLayout() );
		if ( !visibleOnlyPersons && !AuxFunctions.isPermission( Block.LOGIN, Operation.READ ) ) {
			add( new WebScrollPane( new WebTextArea( "��������� ������" ) ), BorderLayout.CENTER );
			return;
		}		
		setLayout( new BorderLayout() );
		add( new WebScrollPane( makeTable() ), BorderLayout.CENTER );
		add( makeToolbar(), BorderLayout.NORTH ); 
		addAncestorListener( this );
		
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
	/*			
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
	*/
			}
		} catch ( AccessControlException aex ) { 
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "UsersPanel.actionPerformed() : ", aex );
			ErrorDlg.show( owner, "������", "��� ���� �� ���������� ��������", aex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "UsersPanel.actionPerformed() : ", ex );
			ErrorDlg.show( owner, "������", "��������� ������ ��� ���������� ��������", ex, 
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
	
	@Override
	public void columnMarginChanged( ChangeEvent e ) { 
		if ( pconf == null ) return;
		TableColumnModel tcm = ( TableColumnModel ) e.getSource(); 
		int[] arr = pconf.getPersonsColumnSize();
		if ( ( arr == null ) || ( arr.length != tcm.getColumnCount() ) ) {
			arr = new int[ tcm.getColumnCount() ];
			pconf.setPersonsColumnSize( arr );
		}
		for ( int i = 0; i < tcm.getColumnCount(); i++ ) arr[i] = tcm.getColumn( i ).getWidth();
	}

	@Override
	public void columnSelectionChanged( ListSelectionEvent e ) { /* not used */ }

	@Override
	public void columnRemoved( TableColumnModelEvent e ) { /* not used */ }

	@Override
	public void columnAdded( TableColumnModelEvent e ) { /* not used */ }

	@Override
	public void columnMoved( TableColumnModelEvent e ) { /* not used */ }
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public Person getSele�tedPerson() {
		int row = tbl.getSelectedRow();
		return row < 0 ? null : ( ( PersonInfoTM ) tbl.getModel() ).getPersons()
				.get( tbl.convertRowIndexToModel( row ) ).getSecond().getPerson() ;
	}
	
	/**
	 * operation reload 
	 */
	public void reload() {
		if ( ( ( loginsEditField != null ) && loginsEditField.isShowing() )
				|| ( ( roleComboBox != null ) && roleComboBox.isShowing() )
				|| ( ( ( PersonInfoTM ) tbl.getModel() ).isPersonAddonChanged() 
						&& ( WebOptionPane.OK_OPTION != WebOptionPane.showConfirmDialog( owner, 
								"���� ������������ ������. \n��� ���������� ��� ����� ��������. \n���������� ?", 
								"��������������", 
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
					"���������� ���������, �����������, ����� � �������", this, 0 ) );
			tb.add( ActionAdapter.build( "delete", ResourceKeeper.getIcon( IType.DELETE, 24 ), CMD_DELETE_PERSON_ADDON, 
					"�������� ����� � �������", this, 0 ) );		
		}
		tb.add( ActionAdapter.build( "reload", ResourceKeeper.getIcon( IType.REFRESH, 24 ), CMD_RELOAD_PERSON_ADDON,
				"����������� ������", this, 0 ) );
		tb.add( ComponentFabrica.createTableFilter( ( TableRowSorter<PersonInfoTM> ) tbl.getRowSorter(), 
				24, "������" ), ToolbarLayout.FILL );
		return tb;
	}

	/**
	 * initialize table component
	 * @return WebTable component
	 */
	private Component makeTable() {
		PersonInfoTM model =  new PersonInfoTM( !visibleOnlyPersons );
		tbl = new WebTable( model );		
		tbl.setAutoResizeMode( WebTable.AUTO_RESIZE_OFF );
		tbl.setRowSorter( new TableRowSorter<PersonInfoTM>( model ) );		
		tbl.setDefaultRenderer( Object.class, new MinosCellRenderer<Object>( 24 ) );		
		AuxFunctions.initTableColumnWidth( tbl.getColumnModel(), pconf.getPersonsColumnSize(), 100 );
		tbl.getColumnModel().addColumnModelListener( this );
		if ( !visibleOnlyPersons ) {
			tbl.setDragEnabled( true );
			tbl.setTransferHandler( new PersonTransferHandler() );

			DefaultCellEditor dce = new DefaultCellEditor( makeLoginEditor() );
			dce.setClickCountToStart( 2 );
			tbl.getColumnModel().getColumn( PersonInfoTM.LOGIN_COLUMN ).setCellEditor( dce );
			
			dce = new DefaultCellEditor( makeRoleEditor() );
			dce.setClickCountToStart( 2 );
			tbl.getColumnModel().getColumn( PersonInfoTM.ROLE_COLUMN ).setCellEditor( dce );

			tbl.getColumnModel().getColumn( PersonInfoTM.ROLE_COLUMN ).setCellRenderer( new MinosCellRenderer<Role>( 16 ) );
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
				CMD_OK_LOGIN, "���������� ����������", this, 0 ) );

		loginList = new WebList( new DefaultListModel<String>() );
        loginList.setEditable( true );
        loginList.addListEditListener( this );
        loginList.setCellEditor( new MinosListCellEditor( "����� �����" ) );
		
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

		new SwingWorker<Object, Boolean>() {

			@Override
			protected Object doInBackground() throws Exception {
				try {
					publish( Boolean.TRUE );					
					List<Pair<FilialInfo, OrgUnit>> lp = loadPersons();
					List<Role> lr = ( visibleOnlyPersons ? null : loadRoles() );
					return new Pair<List<Pair<FilialInfo, OrgUnit>>, List<Role>>( lp, lr );
				} catch ( Exception ex ) {					
					return ex;
				}
			}

			@Override
			protected void process( List<Boolean> chunks ) {
				super.process( chunks );
				if ( ( chunks == null ) || ( chunks.size() != 1 ) ) return;
				if ( chunks.get( 0 ).equals( Boolean.TRUE ) ) setVisibleGlass( true );
			}

			@Override
			protected void done() {
				super.done();
				
				try {
					Object o = get();					
					if ( o instanceof Exception ) throw ( ( Exception ) o );
					@SuppressWarnings("unchecked")
					Pair<List<Pair<FilialInfo, OrgUnit>>, List<Role>> p = 
							( Pair<List<Pair<FilialInfo, OrgUnit>>, List<Role>> ) o;
					 
					PersonInfoTM pitm = ( PersonInfoTM ) tbl.getModel();
					pitm.setPersons( p.getFirst() );
					pitm.removeAllChange();					
					if ( !visibleOnlyPersons && ( p.getSecond() != null ) ) {
						@SuppressWarnings("unchecked")
						DefaultComboBoxModel<Role> model = ( DefaultComboBoxModel<Role> ) roleComboBox.getModel();
						model.removeAllElements();
						model.addElement( EMPTY_ROLE );
						for ( Role r : p.getSecond() ) model.addElement( r );
					}
				
					tbl.revalidate();
					tbl.repaint();
				} catch ( Exception ex ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "loadActors failed", ex );
					ErrorDlg.show( owner, "������", "� �������� �������� ��������� ������", ex, 
							ResourceKeeper.getIcon( IType.ERROR, 32 ) );
				} finally {
					setVisibleGlass( false );
				}
			}
		}.execute();
	}

	/**
	 * load list of Person, EstablishedPost and Division entities
	 * @return
	 */
	private List<Pair<FilialInfo, OrgUnit>> loadPersons() {
		Pair<Integer, Integer> pii = getPersonIds();
		String jpql = visibleOnlyPersons ? JPQL_LOAD_ORG_UNIT_ONLY : JPQL_LOAD_ORG_UNIT_AND_PERSON_ADDON;
		
		List<Integer> stats = new ArrayList<>();
		stats.add( Person.STATUS_ACTIVE );
		if ( visibleDismiss ) stats.add( Person.STATUS_DISMISS );
		if ( visibleSuspend ) stats.add( Person.STATUS_SUSPEND );
		if ( visiblePensioner ) stats.add( Person.STATUS_PENSIONER );

		List<OrgUnit> lst = OrmHelper.findByQueryWithParam( QueryType.JPQL, jpql, OrgUnit.class, 
				new Pair<Object, Object>( "startPersonID", pii.getFirst() ),
				new Pair<Object, Object>( "stopPersonID", pii.getSecond() ),
				new Pair<Object, Object>( "stats", stats ),
				new Pair<Object, Object>( "dts", visibleDeleted ? new Timestamp( System.currentTimeMillis() ) : 
					ResourceKeeper.getObject( OType.WAR ) ) );

		if ( ( lst == null ) || ( lst.size() == 0 ) ) return Collections.emptyList();
		
		List<FilialInfo> lfi = ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO );
		List<Pair<FilialInfo, OrgUnit>> rs = new ArrayList<>();
		for ( OrgUnit ou : lst ) {
			for ( FilialInfo fi : lfi ) {
				if ( ( fi.getDivisionCodeMin() <= ou.getDivision().getId() ) 
						&& ( ou.getDivision().getId() <= fi.getDivisionCodeMax() )
						&& ( 0 < fi.getCode() ) && ( fi.getCode() < 100 ) ) {
					rs.add( new Pair<>( fi, ou ) );
					break;
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
				ResourceKeeper.getQuery( QType.JPQL_LOAD_ROLE ), 
				Role.class );
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
			int res = WebOptionPane.showConfirmDialog( owner, "� ��� ��� ���� �� ��������. \n "
					+ "������ �� ������ ������� ����, \n  ��������� ���� � �� ���������� � ��. \n ������� �� ?", 
					"������", WebOptionPane.OK_CANCEL_OPTION, WebOptionPane.QUESTION_MESSAGE );
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
	
	/**
	 * save changes logins and|or roles before exit
	 */
	private void saveBeforeExit() {
		if ( visibleOnlyPersons || !( ( PersonInfoTM ) tbl.getModel() ).isPersonAddonChanged() 
				|| ( WebOptionPane.YES_OPTION != WebOptionPane.showConfirmDialog( owner, 
							"���� ����������� ������. \n��������� ��������� ?", 
							"������", WebOptionPane.YES_NO_OPTION, WebOptionPane.QUESTION_MESSAGE ) ) )  return;
		actionPerformed( new ActionEvent( this, 1, CMD_SAVE_PERSON_ADDON ) );
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
		private List<Pair<FilialInfo, OrgUnit>> data;
		private Map<Integer, Pair<PersonAddon, Operation>> changes;
		
		// =================================================================================================================
		// Constructors
		// =================================================================================================================
		public PersonInfoTM( boolean visibleRoleLogins ) {
			this.visibleRoleLogins = visibleRoleLogins;
			columnNames =  new String[visibleRoleLogins ? 7 : 4];
			columnNames[FIO_COLUMN] 		= "���";
			columnNames[POSITION_COLUMN] 	= "���������";
			columnNames[DIVISION_COLUMN] 	= "�������������";
			columnNames[BOFFICE_COLLUMN] 	= "������";
			if ( visibleRoleLogins ) {
				columnNames[LOGIN_COLUMN] 	= "������";
				columnNames[ROLE_COLUMN] 	= "����";
				columnNames[STATUS_COLUMN] 	= "������";
				changes = new TreeMap<>();
			} 
			data = Collections.emptyList();
		}

		// =================================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =================================================================================================================
		@Override
		public Class<?> getColumnClass( int col ) {
			return ( col == ROLE_COLUMN ? Role.class : ( col == STATUS_COLUMN ? Operation.class : String.class ) );
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
			return ( data == null ? 0 : data.size() );
		}

		@Override
		public Object getValueAt( int row, int col ) {
			if ( ( row < 0 ) || ( row >= data.size() ) 
					|| ( col < 0 ) || ( col >= columnNames.length ) ) return null;
			Pair<FilialInfo, OrgUnit> p = data.get( row );
			if ( ( p == null ) || ( p.getSecond() == null ) ) return null;
			PersonAddon pa = ( p.getSecond().getPerson() == null ? null : p.getSecond().getPerson().getPersonAddon() );
			if ( !visibleOnlyPersons && ( pa == null ) 
					&& ( p.getSecond().getPerson() != null ) && ( changes != null ) ) {
				Pair<PersonAddon, Operation> para = changes.get( p.getSecond().getPerson().getId() );
				pa = ( para == null ? null : para.getFirst() ); 
			}			
			
			switch( col ) {
			case FIO_COLUMN : 
				return ( p.getSecond().getPerson() == null ? null : p.getSecond().getPerson().getFullName() );
			
			case POSITION_COLUMN : 
				return ( p.getSecond().getPost() == null ? null	: p.getSecond().getPost().getName() );
						
			case DIVISION_COLUMN : 
				return ( p.getSecond().getDivision() == null ? null	: p.getSecond().getDivision().getFullName() );

			case BOFFICE_COLLUMN : 
				return p.getFirst() == null ? null : p.getFirst().getName();

			case LOGIN_COLUMN : 
				return ( pa == null? null : pa.getLogins() ); 
			
			case ROLE_COLUMN : 
				return ( pa == null? null : pa.getRole() );

			case STATUS_COLUMN : 
				return ( ( ( changes == null ) ||  ( p.getSecond().getPerson() == null ) 
						|| !changes.containsKey( p.getSecond().getPerson().getId() ) ) ? Operation.READ 
								: changes.get( p.getSecond().getPerson().getId() ).getSecond() );
			}
			return null;
		}

		@Override
		public boolean isCellEditable( int row, int col ) {
			if ( visibleRoleLogins && ( ( col == ROLE_COLUMN ) || ( col == LOGIN_COLUMN ) )
					&& ( ( ( data.get( row ).getSecond().getPerson().getPersonAddon() == null ) 
							&& AuxFunctions.isPermission( Block.LOGIN, Operation.CREATE ) ) 
							|| ( ( data.get( row ).getSecond().getPerson().getPersonAddon() != null )
									&& AuxFunctions.isPermission( Block.LOGIN, Operation.UPDATE ) ) ) ) return true;				
			return false;
		} 

		@Override
		public void setValueAt( Object value, int row, int col ) {
			if ( ( value == null ) || ( row < 0 ) || ( row >= data.size() )
					|| ( ( col != LOGIN_COLUMN ) && ( col != ROLE_COLUMN ) ) 
					|| ( ( col == LOGIN_COLUMN ) && ( ( String ) value ).trim().isEmpty() ) ) return; 
			Pair<FilialInfo, OrgUnit> p = data.get( row );
			if ( ( p == null ) || ( p.getSecond() == null ) ) return;
			
			Operation op = ( ( ( changes == null ) || ( !changes.containsKey( p.getSecond().getPerson().getId() ) ) )
					? null : changes.get( p.getSecond().getPerson().getId() ).getSecond() ); 
			if ( Operation.DELETE == op ) return;
			
			PersonAddon pa = ( p.getSecond().getPerson() == null ? null : p.getSecond().getPerson().getPersonAddon() );
			if ( !visibleOnlyPersons && ( pa == null ) 
					&& ( p.getSecond().getPerson() != null ) && ( changes != null ) ) {
				Pair<PersonAddon, Operation> para = changes.get( p.getSecond().getPerson().getId() );
				pa = ( para == null ? null : para.getFirst() ); 
			}	
			if ( pa == null ) { // need new PersonAddon entity
				pa = new PersonAddon();
				pa.setPerson( p.getSecond().getPerson() );
				changes.put( p.getSecond().getPerson().getId(), new Pair<>( pa, Operation.CREATE ) );
			}
			boolean flagUpdate = false;
			if ( ( col == LOGIN_COLUMN ) && !AuxFunctions.equals( pa.getLogins(), value, true ) ) {
				pa.setLogins( ( String ) value );
				flagUpdate = true;
			}					
			if ( ( col == ROLE_COLUMN ) && !AuxFunctions.equals( pa.getRole(), value, true ) ) {
				Role r = ( Role ) value;
				pa.setRole( r == EMPTY_ROLE ? null : r );
				flagUpdate = true;
			}
			if ( flagUpdate && !changes.containsKey( p.getSecond().getPerson().getId() ) ) {
				changes.put( p.getSecond().getPerson().getId(), new Pair<>( pa, Operation.UPDATE ) );
			}
		}

		// =============================================================================================================
		// Methods
		// =============================================================================================================
		public void setPersons( List<Pair<FilialInfo, OrgUnit>> lst ) {
			List<Pair<FilialInfo, OrgUnit>> tmp = data;			
			data = lst;
			if ( ( tmp != null ) && ( tmp.size() > 0 ) ) tmp.clear();
		}

		public List<Pair<FilialInfo, OrgUnit>> getPersons() {
			return data;
		}

		public boolean isPersonAddonChanged() {
			if ( ( changes == null ) || ( changes.size() == 0 )  ) return false;
			for ( Pair<PersonAddon, Operation> p : getChanges() ) {
				if ( ( p != null ) && ( Operation.READ != p.getSecond() ) )  return true;
			}
			return false; 
		}
		
		public void clearAllChange() {
			for ( Pair<PersonAddon, Operation> p : getChanges() ) p.setSecond( Operation.READ );
		}

		public void removeAllChange() {
			if ( changes != null ) changes.clear();
		}

		public Operation getRowState( int modelRow ) {
			if ( ( data == null ) || ( 0 > modelRow ) || ( modelRow <= data.size() ) ) return null;
			if ( ( changes == null ) 
					|| !changes.containsKey( data.get( modelRow ).getSecond().getId() ) ) return Operation.READ;
			return changes.get( data.get( modelRow ).getSecond().getId() ).getSecond();
		}
		
		public void deletePersonAddon( int modelRow ) {
			if ( !visibleRoleLogins || ( data == null ) || ( modelRow < 0 ) || ( data.size() <= modelRow ) 
					|| data.get( modelRow ).getSecond().getPerson().getPersonAddon() == null ) return;
			if ( changes == null ) changes = new TreeMap<>();
			int pid = data.get( modelRow ).getSecond().getPerson().getId();
			if ( !changes.containsKey( pid ) ) {
				changes.put( pid, new Pair<>( data.get( modelRow ).getSecond().getPerson().getPersonAddon(), 
						Operation.DELETE ) );
			} else {
				Pair<PersonAddon, Operation> p = changes.get( pid );
				if ( Operation.CREATE == p.getSecond() ) changes.remove( pid );
				p.setSecond( Operation.DELETE );
			}
			//data.get( modelRow ).setThird( null );
		}
		
		public Collection<Pair<PersonAddon, Operation>> getChanges() {
			if ( ( changes == null ) || ( changes.size() == 0 ) ) return Collections.emptyList();
			return changes.values();
		}
	}
}