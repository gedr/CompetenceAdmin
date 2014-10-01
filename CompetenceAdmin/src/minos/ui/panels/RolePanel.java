package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Triplet;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.PersonAddonJpaController;
import minos.data.orm.controllers.RoleJpaController;
import minos.entities.PersonAddon;
import minos.entities.Role;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.ErrorDlg;
import minos.utils.AuxFunctions;
import minos.utils.Permission;
import minos.utils.ResourceKeeper;
import minos.utils.Permission.Block;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import com.alee.laf.label.WebLabel;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.splitpane.WebSplitPane;
import com.alee.laf.table.WebTable;
import com.alee.laf.toolbar.WebToolBar;

public class RolePanel extends WebPanel implements ActionListener, TableModel, TableModelListener, OrmCommand {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1;

	private static final String CMD_ADD		= "1";
	private static final String CMD_COPY	= "2";
	private static final String CMD_DELETE	= "3";
	private static final String CMD_SAVE	= "4";
	private static final String CMD_SAVE_ALL= "5";
	private static final String CMD_REFRESH	= "6";

	private static final int TOOLBAR_ICON_SIZE = 24;

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( RolePanel.class );

	private Window owner = null;
	private WebTable rtbl = null;
	private WebTable ptbl = null;
	private PremisionTM model = null;

	private List<Triplet<Role, Permission, Permission.Operation>> roles = null;
	private List<Role> newRoles;
	private String[] columnName; 

	private ImageIcon pencil;
	private ImageIcon noIcon;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public RolePanel( Window owner ) {
		super( );
		this.owner = owner;
		newRoles = Collections.emptyList();
		columnName = new String[] { null, "Название", "Описание" };
		pencil = ResourceKeeper.getIcon( ResourceKeeper.IType.PENCIL, 16 );
		noIcon = ResourceKeeper.getIcon( ResourceKeeper.IType.CANCEL, 16 );
		
		load();
		makeTables();
		WebSplitPane split = new WebSplitPane( WebSplitPane.VERTICAL_SPLIT, 
				new WebScrollPane( rtbl ), new WebScrollPane( ptbl ) );
		split.setDividerLocation(100);
		setLayout( new BorderLayout() );		
		add( split, BorderLayout.CENTER );
		add( makeToolBar(), BorderLayout.NORTH );
	}	

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public Class<?> getColumnClass( int col ) {
		return col == 0 ? ImageIcon.class : String.class;
	}

	@Override
	public String getColumnName( int col ) {
		return columnName[col];
	}

	@Override
	public int getColumnCount() {
		return columnName.length;
	}

	@Override
	public int getRowCount() {			
		return roles == null ? 0 : roles.size();
	}

	@Override
	public Object getValueAt( int row, int col ) {
		if ( ( roles == null ) || ( roles.get( row ) == null ) || ( row < 0 ) || ( row >= roles.size() ) ) return null;
		Triplet<Role, Permission, Operation> t = roles.get( row );
		if ( col == 0 ) {
			return ( t.getThird() == Operation.READ ? null : ( t.getThird() == Operation.DELETE ? noIcon : pencil ) );
		}
		return ( t.getFirst() == null ? "" : ( col == 1 ? t.getFirst().getName() : t.getFirst().getDescription() ) );
	}

	@Override
	public boolean isCellEditable( int row, int col ) {
		if ( ( roles == null ) || ( row < 0 ) || ( row >= roles.size() ) 
				|| ( col == 0 ) || ( roles.get( row ) == null )
				|| ( roles.get( row ).getThird() == Operation.DELETE )
				|| ( ( roles.get( row ).getThird() == Operation.READ ) 
						&& !AuxFunctions.isPermission( Block.ROLE, Operation.UPDATE ) ) ) return false;
		return true;
	}

	@Override
	public void setValueAt( Object aValue, int row, int col ) {
		if ( ( row < 0 ) || ( row >= roles.size() ) ) return;
		String s = ( String ) aValue;
		if ( col == 1 ) roles.get( row ).getFirst().setName( s );
		if ( col == 2 ) roles.get( row ).getFirst().setDescription( s );
		if ( roles.get( row ).getThird() == Operation.READ ) roles.get( row ).setThird( Operation.UPDATE );
		AuxFunctions.repaintComponent( rtbl );
	}

	@Override
	public void addTableModelListener( TableModelListener l ) { /* not used */ }

	@Override
	public void removeTableModelListener( TableModelListener l ) { /* not used */ }

	@Override
	public void tableChanged( TableModelEvent e ) {
		if ( ( e.getSource() == model ) && ( e.getType() == Operation.UPDATE.getValue() ) ) {
			AuxFunctions.repaintComponent( rtbl );
		}
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		try {
			int ind = 0;
			switch ( e.getActionCommand() ) {
			case CMD_ADD:
				addRole( null );
				break;

			case CMD_COPY:
				copyRole();
				break;

			case CMD_DELETE:
				deleteRole();
				break;

			case CMD_SAVE:				
				ind = rtbl.getSelectedRow();				
				if ( ind != -1 ) saveRole( ind );				
				break;

			case CMD_SAVE_ALL:
				saveRole( -1 );
				break;

			case CMD_REFRESH:
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

	/**
	 * save changed roles to DB in one transaction
	 * @param obj - is triplet ( 1st - list of index for changed rows, 
	 * 							 2nd - list of Role entities for delete,  
	 * 							 3rd - list of PersonAddon entities for change 
	 * @throws Exception
	 */
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<List<Integer>, List<Role>, List<PersonAddon>> tp = 
		( Triplet<List<Integer>, List<Role>, List<PersonAddon>> ) obj;
		for ( PersonAddon pa : tp.getThird() ) {
			pa.setRole( null );
			PersonAddonJpaController.getInstance().update( pa, true, false, false );
		}
		for ( Role r : tp.getSecond() ) RoleJpaController.getInstance().delete( r, true, false, false );

		for ( Integer i : tp.getFirst() ) {
			Triplet<Role, Permission, Permission.Operation> t = roles.get( i );
			if ( t.getThird() == Operation.DELETE ) continue;
			if ( t.getThird() == Operation.CREATE ) RoleJpaController.getInstance().create( t.getFirst(), 
					true, false, false );
			if ( t.getThird() == Operation.UPDATE ) RoleJpaController.getInstance().update( t.getFirst(), 
					true, false, false );
		}
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * load <code>Role</code> entities
	 */
	private void load() {
		OrmHelper.getFactory().getCache().evict( Role.class );
		List<Role> lst = OrmHelper.findByQuery( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_ROLE ), 
				Role.class );
		if ( ( lst == null ) || ( lst.size() == 0 ) ) {
			roles = Collections.emptyList();
			return;
		}		
		List<Triplet<Role, Permission, Permission.Operation>> l = new ArrayList<>();
		for ( Role role : lst ) l.add( new Triplet<>( role, new Permission( role ), Permission.Operation.READ ) );
		List<Triplet<Role, Permission, Permission.Operation>> tmp = roles;
		roles = l;		
		if ( tmp != null ) tmp.clear();
	}

	/**
	 * make JTable components for display Role entities and it Permissions 
	 */
	private void makeTables() {
		model = new PremisionTM();
		model.addTableModelListener( this );

		ptbl = new WebTable( model );		
		ptbl.getTableHeader().setReorderingAllowed( false );		
		ptbl.getColumnModel().getColumn( 0 ).setPreferredWidth( 300 );
		ptbl.setDefaultRenderer( String.class, model );
		ptbl.setDefaultRenderer( Boolean.class, model );

		rtbl = new WebTable( this );
		rtbl.setName( "ROLE_TABLE" );
		rtbl.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		rtbl.getTableHeader().setReorderingAllowed( false );
		rtbl.getSelectionModel().addListSelectionListener( model );
		rtbl.getColumnModel().getColumn( 0 ).setMaxWidth( 25 );
		rtbl.setDragEnabled( true );
		rtbl.setDropMode( DropMode.ON );
		rtbl.setTransferHandler( new RoleTableTransferHandler() );
		if ( ( roles != null ) && ( roles.size() > 0 ) ) rtbl.getSelectionModel().setSelectionInterval( 0, 0 );			
	}

	/**
	 * make JToolbar component and initialize buttons
	 * @return JToolbar component
	 */
	private WebToolBar makeToolBar() {
		WebToolBar tb = new WebToolBar();		
		tb.add( new ActionAdapter( "add", ResourceKeeper.getIcon(IType.ADD, TOOLBAR_ICON_SIZE ), CMD_ADD, 
				"Создание новой роли", this, 0 ) );
		tb.add( new ActionAdapter( "copy", ResourceKeeper.getIcon(IType.COPY, TOOLBAR_ICON_SIZE ), CMD_COPY, 
				"Создание копии текущей роли", this, 0 ) );
		tb.add( new ActionAdapter( "save", ResourceKeeper.getIcon(IType.SAVE, TOOLBAR_ICON_SIZE ), CMD_SAVE, 
				"Сохранение новых и измененых ролей", this, 0 ) );
		tb.add( new ActionAdapter( "save all", ResourceKeeper.getIcon(IType.SAVE_ALL, TOOLBAR_ICON_SIZE ), CMD_SAVE_ALL, 
				"Сохранение всех изменений", this, 0 ) );
		tb.add( new ActionAdapter( "delete", ResourceKeeper.getIcon(IType.DELETE, TOOLBAR_ICON_SIZE ), CMD_DELETE, 
				"Удаление текущей роли", this, 0 ) );		
		tb.add( new ActionAdapter( "refresh", ResourceKeeper.getIcon(IType.REFRESH, TOOLBAR_ICON_SIZE ), CMD_REFRESH, 
				"Перегрузить список ролей", this, 0 ) );
		return tb;
	}
 
	/**
	 * add new Role entity in list
	 * @param src - Role entity for clone
	 * @throws Exception
	 */
	private void addRole( Role src ) throws Exception {
		if ( !AuxFunctions.isPermission( Block.ROLE, Operation.CREATE ) ) {
			throw new AccessControlException( "create Role permission denied" );
		}
		Role role = null;
		byte[] arr = null;
		if ( src != null ) {
			arr = src.getFlag().clone();
			role = new Role( "<Копия> " + src.getName(), src.getDescription(), arr );			
		} else {	
			arr = new byte[Role.FLAG_LENGTH];
			Arrays.fill( arr, ( byte ) 0 );
			role = new Role( "Новая роль", ( new Date() ).toString(), arr );			
		}
		if ( newRoles.size() == 0 ) newRoles = new ArrayList<>();
		newRoles.add( role );
		if ( ( roles == null ) || ( roles.size() == 0 ) ) roles = new ArrayList<>();
		roles.add( new Triplet<>( role, new Permission( role ), Permission.Operation.CREATE ) );		
		AuxFunctions.repaintComponent( rtbl );
	}

	/**
	 * make copy from selected Role
	 * @throws Exception
	 */
	private void copyRole() throws Exception {
		int ind = rtbl.getSelectedRow();
		if ( ind == -1 ) {
			ErrorDlg.show( owner, "Ошибка", "Необходимо выбрать существующую роль для создания копии", 
					null, ResourceKeeper.getIcon( IType.ERROR, 48 ) );
			return;
		}
		ind = rtbl.convertRowIndexToModel( ind );
		addRole( roles.get( ind ).getFirst() );
	}
	
	/**
	 * mark Role entity as DELETE
	 * @throws Exception
	 */
	private void deleteRole() throws Exception {
		if ( rtbl.getSelectedRowCount() < 1 ) return;
		List<Role> lr = new ArrayList<>();
		for ( int row : rtbl.getSelectedRows() ) {
			Triplet<Role, Permission, Permission.Operation> t = roles.get( rtbl.convertRowIndexToModel( row ) );
			if ( ( t == null ) || ( t.getFirst() == null ) 
					|| ( t.getThird() == Operation.DELETE ) || ( t.getThird() == Operation.CREATE ) ) continue;
			lr.add( t.getFirst() );			
		}
		if ( ( lr.size() > 0 ) && !AuxFunctions.isPermission( Block.ROLE, Operation.DELETE ) ) {
			throw new AccessControlException( "delete Role permission denied" );			
		}
		if( lr.size() > 0 ) {
			Number num = ( Number ) OrmHelper.executeQueryWithParam( QueryType.JPQL, 
					ResourceKeeper.getQuery( QType.JPQL_COUNT_PERSON_ADDON_BY_ROLES ),
					new Pair<Object, Object>( "roles", lr ) );
			if ( ( num.longValue() > 0 ) && !AuxFunctions.isPermission( Block.LOGIN, Operation.UPDATE ) ) {
				throw new AccessControlException( "update PersonAddon permission denied" );
			}
			if ( ( num.longValue() > 0 ) 
					&& WebOptionPane.OK_OPTION != WebOptionPane.showConfirmDialog( owner, 
							"Есть пользователи у которых установлены удаляемые роли."
									+ "\nПосле удаления у пользователей будут установлены пустые роли."
									+ "\nПродолжить удаление?", "Запрос", 
									WebOptionPane.OK_CANCEL_OPTION, WebOptionPane.QUESTION_MESSAGE ) ) return;
		}
		for ( int row : rtbl.getSelectedRows() ) {
			Triplet<Role, Permission, Permission.Operation> t = roles.get( rtbl.convertRowIndexToModel( row ) );
			if ( ( t == null ) || ( t.getFirst() == null ) || ( t.getThird() == Operation.DELETE ) ) continue;
			t.setThird( Operation.DELETE );
		}
	}

	/**
	 * save changed roles in DB
	 * @param rolesIndex - index elements in model for save , if  rolesIndex<0 then save all changed elements
	 * @throws Exception
	 */
	private void saveRole( int rolesIndex ) throws Exception {
		if ( rolesIndex >= roles.size() ) return;
		final List<Integer> chng = getChagedRoles( rolesIndex );
		if ( ( chng == null ) || ( chng.size() < 1 ) ) return;
		
		List<Role> delRoles = getTrueDeletedRoles( chng );
		List<PersonAddon> lpa = Collections.emptyList();
		if ( delRoles.size() > 0 ) {
			lpa = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
					ResourceKeeper.getQuery( QType.JPQL_LOAD_PERSON_ADDON_BY_ROLES ), 
					PersonAddon.class, 
					new Pair<Object, Object>( "roles", delRoles ) );
			if ( lpa == null ) lpa = Collections.emptyList();
		}
		
		OrmHelper.executeAsTransaction( this, 
				new Triplet<List<Integer>, List<Role>, List<PersonAddon>>( chng, delRoles, lpa ) );

		// delete elements, mark as DELETE, from model list
		for ( Integer i : chng ) {
			Triplet<Role, Permission, Permission.Operation> t = roles.get( rtbl.convertRowIndexToModel( i ) );						
			for ( int ri = 0; ri < newRoles.size(); ri++ ) {
				if ( newRoles.get( ri ) == t.getFirst() ) {
					newRoles.remove( ri );
					break;
				}
			}
			if ( t.getThird() == Operation.DELETE ) {
				roles.remove( rtbl.convertRowIndexToModel( i ) );
			} else {
				t.setThird( Operation.READ );
			}
		}
		AuxFunctions.repaintComponent( rtbl );
	}

	/**
	 * prepare list was changed roles
	 * @return list of indexes for roles 
	 */
	private List<Integer> getChagedRoles( int index ) {
		List<Integer> inds = Collections.emptyList();
		int i = ( index < 0 ? 0 : index );
		int stop = ( index < 0 ? ( roles.size() - 1 ) : index );
		while ( i <= stop ) {
			Triplet<Role, Permission, Permission.Operation> t = roles.get( rtbl.convertRowIndexToModel( i ) );
			if ( ( t.getThird() == Operation.CREATE ) || ( t.getThird() == Operation.UPDATE ) 
					|| ( t.getThird() == Operation.DELETE ) ) {
				if ( inds.size() == 0 ) inds = new ArrayList<>();
				inds.add( i );
			}
			i++;
		}
		Collections.sort( inds ); // ascending order
		Collections.reverse( inds ); // descending order
		return inds;
	}
	
	/**
	 * prepare list of Role entities, that user read from DB and delete
	 * @param inds - list of changed Role entities
	 * @return list of Role entities for delete from DB
	 */
	private List<Role> getTrueDeletedRoles( List<Integer> inds ) {
		List<Role> delRoles = Collections.emptyList();
		for ( Integer i : inds ) {
			Triplet<Role, Permission, Permission.Operation> t = roles.get( i );
			if ( ( t.getThird() == Operation.DELETE ) && !isCreateDeleteRole( t.getFirst() ) ) {
				if ( delRoles.size() == 0 ) delRoles = new ArrayList<>();
				delRoles.add( t.getFirst() );
			}
		}
		return delRoles;
	}

	/**
	 * check Role object
	 * @param r - Role entity
	 * @return true if Role entity was create and delete, and not be saved in DB
	 */
	private boolean isCreateDeleteRole( Role role ) {
		if ( ( newRoles == null ) || ( newRoles.size() == 0 ) ) return false;
		for ( Role r : newRoles ) {
			if ( r == role )  return true;
		}
		return false;
	}
		
	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private class RoleTableTransferHandler extends TransferHandler {
		// =============================================================================================================
		// Constants
		// =============================================================================================================
		private static final long serialVersionUID = 1;

		// =============================================================================================================
		// Fields
		// =============================================================================================================
		private DataFlavor df;

		// =============================================================================================================
		// Constructors
		// =============================================================================================================
		public RoleTableTransferHandler() {
			try {
				df = new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType + ";class=" 
						+ PersonAddon.class.getName() );
			} catch ( Exception thex ) {
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( "rtbl :"
						+ " create DataFlavor for PersonAddon.class generate error : ", thex ); 
				df = null;
			}	
		}

		// =============================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =============================================================================================================
		@Override
		public boolean canImport( TransferSupport support ) {
			return ( df == null ? false : support.getTransferable().isDataFlavorSupported( df ) );
		}

		@Override
		public boolean importData( TransferSupport support ) {
			try {
				PersonAddon pa = ( PersonAddon ) support.getTransferable().getTransferData( df );
				if ( pa == null ) return false;
				int row = ( ( JTable ) support.getComponent() ).getDropLocation().getRow();
				pa.setRole( roles.get( rtbl.convertRowIndexToModel( row ) ).getFirst() );					
				return true;
			} catch ( Exception ex ) {
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( "rtbl.importData() : ", ex );
				return false;
			}
		}
	}


	private class PremisionTM extends AbstractTableModel implements ListSelectionListener, TableCellRenderer {
		// =============================================================================================================
		// Constants
		// =============================================================================================================
		private static final long serialVersionUID = 1;

		// =============================================================================================================
		// Fields
		// =============================================================================================================
		private int rolesIndex = -1;
		private List<Pair<String, Permission.Block>> rows; 
		private List<Pair<String, Permission.Operation>> columns;

		private WebLabel label;

		private ImageIcon okIcon;
		private ImageIcon okdIcon;
		private ImageIcon nodIcon;
		
		private Color firstColColor;

		// =============================================================================================================
		// Constructors
		// =============================================================================================================
		public PremisionTM() {
			super();
			firstColColor = new Color( 218, 218, 218 );
			label = new WebLabel();
			label.setVerticalAlignment( JLabel.CENTER );
			label.setOpaque( true );
			okIcon = ResourceKeeper.getIcon( ResourceKeeper.IType.OK, 16 );
			noIcon = ResourceKeeper.getIcon( ResourceKeeper.IType.CANCEL, 16 );
			if ( okIcon != null ) okdIcon = new ImageIcon( GrayFilter.createDisabledImage( okIcon.getImage() ) );
			if ( noIcon != null ) nodIcon = new ImageIcon( GrayFilter.createDisabledImage( noIcon.getImage() ) );

			rows = new ArrayList<>();			
			rows.add( new Pair<>( "Каталоги шаблонов профилей", Block.COMMON_CATALOG ) );
			rows.add( new Pair<>( "Каталоги профессиональных компетенций", Block.PROFESSIONAL_CATALOG ) );
			rows.add( new Pair<>( "Каталоги персональных компетенций", Block.PERSONALITY_CATALOG ) );
			rows.add( new Pair<>( "Каталоги административных компетенций", Block.ADMINISTRATIVE_CATALOG ) );
			rows.add( new Pair<>( "Профессиональные компетенции", Block.PROFESSIONAL_COMPETENCE ) );
			rows.add( new Pair<>( "Персональные компетенции", Block.PERSONALITY_BUSINESS_COMPETENCE ) );
			rows.add( new Pair<>( "Административные компетенции", Block.ADMINISTRATIVE_COMPETENCE ) );
			rows.add( new Pair<>( "Шаблоны профилей", Block.PROFILE_PATTERN ) );
			rows.add( new Pair<>( "Активация шаблона профиля", Block.ACTIVATE_PROFILE_PATTERN ) );
			rows.add( new Pair<>( "Элементы шаблонов профилей", Block.PROFILE_PATTERN_ELEMENT ) );
			rows.add( new Pair<>( "Аттрибуты профилей", Block.STR_ATTR ) );
			rows.add( new Pair<>( "Профили", Block.PROFILE ) );
			rows.add( new Pair<>( "Оценочные мероприятия (внутри филиала)", Block.MEASURE_INNER ) );
			rows.add( new Pair<>( "Оценочные мероприятия (других филиалов)", Block.MEASURE_OUTER ) );
			rows.add( new Pair<>( "Эксперты и оцениваемые (внутри филиала)", Block.ACTORS_INNER ) );
			rows.add( new Pair<>( "Эксперты и оцениваемые (других филиалов)", Block.ACTORS_OUTER ) );
			rows.add( new Pair<>( "Оценки экспертов (внутри филиала)", Block.PERFORMANCE_INNER ) );
			rows.add( new Pair<>( "Оценки экспертов (других филиалов)", Block.PERFORMANCE_OUTER ) );
			rows.add( new Pair<>( "Роли", Block.ROLE ) );
			rows.add( new Pair<>( "Логины", Block.LOGIN ) );
			rows.add( new Pair<>( "Справочники", Block.ACTORS_INFO ) );
			rows.add( new Pair<>( "Подразделения", Block.DIVISION ) );
			rows.add( new Pair<>( "Штатные должности", Block.EPOST ) );
			

			columns = new ArrayList<>();
			columns.add( new Pair<>( "Название", Operation.READ ) );
			columns.add( new Pair<>( "Создание", Operation.CREATE ) );
			columns.add( new Pair<>( "Чтение", Operation.READ ) );
			columns.add( new Pair<>( "Изменение", Operation.UPDATE ) );
			columns.add( new Pair<>( "Удаление", Operation.DELETE ) );
		}

		// =============================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =============================================================================================================
		@Override
		public int getColumnCount() {
			return columns.size();
		}

		@Override
		public Class<?> getColumnClass( int columnIndex ) {
			return columnIndex == 0 ? String.class : Boolean.class;
		}

		@Override
		public String getColumnName( int columnIndex ) {
			return columns.get( columnIndex ).getFirst();
		}

		@Override
		public int getRowCount() {			
			return ( ( ( rolesIndex < 0 ) || ( rolesIndex >=  roles.size() ) ) ? 0 : rows.size() );
		}

		@Override
		public boolean isCellEditable( int rowIndex, int columnIndex ) {
			if ( ( columnIndex == 0 ) || ( roles == null ) 
					|| ( rolesIndex < 0 ) || ( rolesIndex >=  roles.size() ) ) return false;
			if ( roles.get( rolesIndex ).getThird() == Operation.DELETE ) return false;
			if ( ( roles.get( rolesIndex ).getThird() == Operation.READ ) 
					&& !AuxFunctions.isPermission( Block.ROLE, Operation.UPDATE ) ) return false; 
			return ( ( Permission ) ResourceKeeper.getObject( OType.PERMISSION_CONSTRAINT ) )
					.isEnabled( rows.get( rowIndex ).getSecond(), columns.get( columnIndex).getSecond() );
		}

		@Override
		public Object getValueAt( int rowIndex, int columnIndex ) {
			if ( ( rolesIndex < 0 ) || ( rolesIndex >=  roles.size() ) ) return null;
			if ( columnIndex == 0 ) return rows.get( rowIndex ).getFirst();
			return roles.get( rolesIndex ).getSecond().isEnabled( rows.get( rowIndex ).getSecond(), 
					columns.get( columnIndex ).getSecond() );			
		}

		@Override
		public void setValueAt( Object val, int row, int col ) {		
			if ( ( rolesIndex < 0 ) || ( rolesIndex >=  roles.size() ) ) return;
			roles.get( rolesIndex ).getSecond().setEnabled( rows.get( row ).getSecond(), 
					columns.get( col ).getSecond(), ( Boolean ) val );
			if ( roles.get( rolesIndex ).getThird() == Operation.READ ) {
				roles.get( rolesIndex ).setThird( Operation.UPDATE );
				fireTableChanged( new TableModelEvent( this, row, row, col, Operation.UPDATE.getValue() ) );
			}
		}

		@Override
		public void valueChanged( ListSelectionEvent e ) {
			rolesIndex = rtbl.getSelectedRow();
			AuxFunctions.repaintComponent( ptbl );
		}

		@Override
		public Component getTableCellRendererComponent( JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column ) {
			if ( ( rolesIndex < 0 ) || ( rolesIndex >=  roles.size() ) ) return null;
			if ( column == 0 ) {
				label.setHorizontalAlignment( JLabel.LEFT );
				label.setBackground( firstColColor );
				label.setIcon( null );
				label.setText( ( String ) value );
				return label; 
			}			
			Block bk = rows.get( row ).getSecond();
			Operation op = columns.get( column ).getSecond();
			boolean enabled = ( ( Permission ) ResourceKeeper.getObject( OType.PERMISSION_CONSTRAINT ) )
					.isEnabled( bk, op ); 
			boolean selected = ( enabled ? ( boolean ) value 
					: ( ( Permission ) ResourceKeeper.getObject( OType.PERMISSION_DEFAULT ) ).isEnabled( bk, op ) );
			label.setBackground( isSelected ?  Color.blue : Color.WHITE );
			label.setText( null );
			label.setIcon( selected ? ( enabled ? okIcon : okdIcon ) : ( enabled ? noIcon : nodIcon ) );
			label.setHorizontalAlignment( JLabel.CENTER );			
			return label;
		}
	}
}