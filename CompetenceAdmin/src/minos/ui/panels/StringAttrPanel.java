package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.security.AccessControlException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityNotFoundException;
import javax.swing.event.EventListenerList;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.PpeStrAtrJpaController;
import minos.data.orm.controllers.StringAttrJpaController;
import minos.entities.PpeStrAtr;
import minos.entities.ProfilePatternElement;
import minos.entities.StringAttr;
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
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.table.WebTable;
import com.alee.laf.toolbar.WebToolBar;

public class StringAttrPanel extends BasisPanel implements OrmCommand, ActionListener, Runnable {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	
	private static final int PPE_PANEL = 1;
	
	private static final int ICON_SIZE = 24;
	
	private static final String CMD_RELOAD	= "R";
	private static final String CMD_SAVE	= "S";
	private static final String CMD_DELETE	= "D";
	
	public static final String STRING_ATTR_START_LOAD 		= "StartStringAttrLoad";
	public static final String STRING_ATTR_LOAD_SUCCESS 	= "LoadStringAttrSuccess";
	public static final String STRING_ATTR_LOAD_FAIL		= "LoadStringAttrFail";
	public static final String STRING_ATTR_START_SAVE 		= "StartStringAttrSave";
	public static final String STRING_ATTR_SAVE_SUCCESS 	= "SaveStringAttrSuccess";
	public static final String STRING_ATTR_SAVE_FAIL		= "SaveStringAttrFail";

	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( StringAttrPanel.class );
	private WebTable tbl;
	private int panelType = 0;
	private boolean visibleSaveBtn;
	private ProfilePatternElement ppe;
	private List<PpeStrAtr> lpsa;
	private Thread threadSaveLoad;
	private Boolean flagSaveLoad; // true - save thread; false - load thread; null - no operation
	private EventListenerList ell;
	private Window owner;
	
	
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public StringAttrPanel( Window owner, ProfilePatternElement ppe, boolean visibleSaveButton ) {
		super( true );
		if ( ppe == null ) throw new NullArgumentException( "StringAttrPanel() : param is null" );	
		
		if ( !AuxFunctions.isPermission( Block.STR_ATTR, Operation.READ ) ) {
			add( new WebLabel("Нет прав на чтение") );
			return;
		}
		this.panelType = PPE_PANEL;
		this.ppe = ppe;
		this.visibleSaveBtn = visibleSaveButton;
		this.owner = owner;
		ell = new EventListenerList();
		addActionLictener( this );
		
		setLayout( new BorderLayout() );
		boolean flag = ( ppe.getStatus() == ProfilePatternElement.STATUS_BUILDING );
		makeTable( flag && AuxFunctions.isPermission( Block.STR_ATTR, Operation.CREATE ), 
				flag && AuxFunctions.isPermission( Block.STR_ATTR, Operation.UPDATE ) );
		add( new WebScrollPane( tbl ), BorderLayout.CENTER );
		add( makeToolbar( visibleSaveButton ), BorderLayout.NORTH );
		load();
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		List<Pair<PpeStrAtr, Boolean>> lst = ( List<Pair<PpeStrAtr, Boolean>> ) obj;		
		for ( Pair<PpeStrAtr, Boolean> p : lst ) {
			if ( !p.getSecond() ) {
				PpeStrAtrJpaController.getInstance().delete( p.getFirst(), true, false, false );
			} else {
				StringAttr sa = StringAttrJpaController.getInstance().create( p.getFirst().getStringAttr(), true, 
						false, false );
				p.getFirst().setStringAttr( sa );
				PpeStrAtrJpaController.getInstance().create( p.getFirst(), true, false, false );
			}
		}
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		try {
			switch ( e.getActionCommand() ) {
			case CMD_RELOAD : 
				load();
				break;

			case CMD_SAVE : 
				save();
				break;

			case CMD_DELETE : 
				delete();
				break;

			case STRING_ATTR_START_LOAD:
			case STRING_ATTR_START_SAVE:
				setVisibleGlass( true );
				System.out.println( "STRING_ATTR_START_LOAD");
				break;
				
			case STRING_ATTR_SAVE_FAIL:
			case STRING_ATTR_LOAD_FAIL :
				setVisibleGlass( false );
				if ( tbl != null ) {
					( ( StringAttrTM ) tbl.getModel() ).setRows( Collections.<StringAttr>emptyList() );
					AuxFunctions.repaintComponent( tbl );
				}
				throw new EntityNotFoundException( "STRING_ATTR_LOAD_FAIL" );
			
			case STRING_ATTR_SAVE_SUCCESS :
				clearModelAfterSaveSuccess();
			case STRING_ATTR_LOAD_SUCCESS:			
				setVisibleGlass( false );
				if ( tbl != null ) AuxFunctions.repaintComponent( tbl );
				break;
			}
		} catch ( AccessControlException aex ) { 
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "StringAttrPanel.actionPerformed() : ", aex );
			ErrorDlg.show( owner, "Ошибка", "Нет прав на выполнение операции", aex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "StringAttrPanel.actionPerformed() : ", ex );
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
	public void run() {
		try {
			fireStartLoading();

			if ( !flagSaveLoad && ( panelType == PPE_PANEL ) ) loadPpe();
			if (  flagSaveLoad && ( panelType == PPE_PANEL ) ) savePpe();
			
			fireLoadSuccess();
		} catch ( Exception ex ) {
			if ( ( log != null) && log.isErrorEnabled() ) log.error( "StringAttrPanel.run() : ", ex );
			fireLoadFail();
		} finally {
			flagSaveLoad = null;
		}
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	private Component makeTable( boolean createEnabled, boolean updateEnabled ) {
		StringAttrTM model = new StringAttrTM( new String[] { "Документы" }, false, true, createEnabled, updateEnabled );
		tbl = new WebTable( model );
		return tbl;
	}
	
	private Component makeToolbar(  boolean visibleSaveButton ) {
		WebToolBar tb = new WebToolBar();		
		if ( visibleSaveButton ) tb.add( new ActionAdapter( "save", ResourceKeeper.getIcon( IType.SAVE_ALL, 
				ICON_SIZE ), CMD_SAVE, "Сохранение элемента", this, 0 ) );
		tb.add( new ActionAdapter( "reload", ResourceKeeper.getIcon( ResourceKeeper.IType.REFRESH, ICON_SIZE ), 
				CMD_RELOAD, "Перегрузить элемента", this, 0 ) );
		tb.add( new ActionAdapter( "delete", ResourceKeeper.getIcon( ResourceKeeper.IType.DELETE, ICON_SIZE ), 
				CMD_DELETE, "Удаление элемента", this, 0 ) );
		return tb;
	}
	
	private boolean delete() {
		if ( ( tbl == null ) || ( tbl.getSelectedRowCount() == 0 ) ) return false;
		if ( ppe.getStatus() != ProfilePatternElement.STATUS_BUILDING ) {
			WebOptionPane.showMessageDialog( owner, "Шаблон профиля не в режиме редактироваия", "Ошибка", 
					WebOptionPane.ERROR_MESSAGE );
			return false;
		}
		StringAttrTM model = ( StringAttrTM ) tbl.getModel();
		boolean delPerm = AuxFunctions.isPermission( Block.STR_ATTR, Operation.DELETE );
		int mri = 0; // model row index 
		boolean createRows = false;
		boolean updateRows = false;
		for ( int row : tbl.getSelectedRows() ) {
			mri = tbl.convertRowIndexToModel( row );
			if ( delPerm ) { 
				model.deleteRow( mri );
				continue;
			}
			Operation op = model.getOperation().get( mri );
			if ( op == Operation.CREATE ) createRows = true;
			if ( op == Operation.UPDATE ) updateRows = true;
		}
		if ( !createRows && !updateRows ) return true;
		if ( createRows && updateRows ) {
			int res = WebOptionPane.showConfirmDialog( owner, "У Вас нет прав на удаление. \n "
					+ "Однако вы можете удалить записи, \n  созданные Вами и не сохраненые в БД. \n Удалить их ?", 
					"Запрос", WebOptionPane.OK_CANCEL_OPTION, WebOptionPane.QUESTION_MESSAGE );
			if ( res == WebOptionPane.OK_OPTION ) updateRows = false;
		}
		if ( updateRows ) throw new AccessControlException( "StringAttrPanel.delete() :"
				+ " delete StringAttr permission denied" );

		for ( int row : tbl.getSelectedRows() ) {
			mri = tbl.convertRowIndexToModel( row );
			if ( model.getOperation().get( mri ) == Operation.CREATE ) model.deleteRow( mri );
		}		
		return true;
	}

	/**
	 * load PpeStrAtr entity for ProfilePatternElement entity 
	 * @param ppe is existing ProfilePatternElement entity
	 * @return
	 */
	public void load() { 
		if ( ( ( threadSaveLoad != null ) && threadSaveLoad.isAlive() ) || ( tbl == null )  
				|| ( ( panelType == PPE_PANEL ) && ( ppe == null ) ) ) return;
		flagSaveLoad = false;
		threadSaveLoad = new Thread( this );
		threadSaveLoad.start();
	}

	/**
	 * save was changed StringAtrr entities in DB
	 * @param ppe - current ProfilePatternElement entity
	 * @param result - list of StringAttr entities after user edit
	 * @throws Exception
	 */
	public void save() {
		if ( ( ( threadSaveLoad != null ) && threadSaveLoad.isAlive() )
				|| ( tbl == null ) || !( ( StringAttrTM ) tbl.getModel() ).isChanged() 
				|| ( ( panelType == PPE_PANEL ) && ( ( ppe == null ) || ( lpsa == null ) )) ) return;
		flagSaveLoad = true;
		threadSaveLoad = new Thread( this );
		threadSaveLoad.start();
	}
	
	/**
	 * make list of StringAttr entity from list of PpeStrAtr entity
	 * @param lpsa is list of PpeStrAtr entity
	 * @param enableClone is flag for clone all StringAttr entities
	 * @return list of StringAttr entity or null
	 */
	private List<StringAttr> convert( List<PpeStrAtr> lpsa ) {
		if ( ( lpsa == null ) || ( lpsa.size() == 0 ) ) return null;
		List<StringAttr> lsa = new ArrayList<>();
		for ( PpeStrAtr psa : lpsa ){
			if ( psa.getStringAttr() == null ) continue;
			lsa.add( psa.getStringAttr() );
		}
		return ( ( lsa.size() > 0 ) ? lsa : null );
	}
	 
	private void loadPpe() {
		lpsa = ( List<PpeStrAtr> ) OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_PPE_ATTRS ), 
				PpeStrAtr.class, 
				new Pair<Object, Object>( "ppes", Arrays.asList( ppe ) ),
				new Pair<Object, Object>( "ts", new Timestamp( System.currentTimeMillis() ) ),
				new Pair<Object, Object>( "dts", ResourceKeeper.getObject( OType.WAR ) ) );
		( ( StringAttrTM ) tbl.getModel() ).setRows( convert( lpsa ) );
	}
	
	private void savePpe() throws Exception {
		List<StringAttr> lst = ( ( StringAttrTM ) tbl.getModel() ).getRows();
		Map<Integer, Operation> mop = ( ( StringAttrTM ) tbl.getModel() ).getOperation();
		List<Pair<PpeStrAtr, Boolean>> lp = new ArrayList<>();

		for ( int i : mop.keySet() ) {			
			Operation op = mop.get( i );
			StringAttr sa = lst.get( i );
			
			if ( op == Operation.UPDATE ) {
				// make clear clone ( without id ) for create new StringAttr entity
				sa = new StringAttr( sa.getKey(), sa.getValue(), sa.getItem(), ( short ) 0, null, null );
			}
			if ( ( op == Operation.CREATE ) || ( op == Operation.UPDATE ) ) {
				sa.setVariety( StringAttr.VARIETY_POFILE_PATTERN_ELEMENT_ATTRIBUTE );
				List<StringAttr> found = StringAttrJpaController.getInstance().searchPpeStringAttr( sa );
				if ( found != null ) lp.add( new Pair<>( new PpeStrAtr( found.size() == 0 ? sa : found.get( 0 ), ppe, 
						( short ) 0, null ), true ) );
			}
			if ( ( op == Operation.DELETE ) || ( op == Operation.UPDATE ) ) {
				StringAttr origin = lst.get( i );
				for ( PpeStrAtr psa : lpsa ) {
					if ( psa.getStringAttr().equals( origin ) ) {
						lp.add( new Pair<>( psa, false ) );
						break;
					}
				}
			}
		}
		OrmHelper.executeAsTransaction( this, lp );
	}
	
	/**
	 * clear in Table model all deleted entities and clear operation's map
	 */
	private void clearModelAfterSaveSuccess() {
		if ( ( tbl == null ) || !( ( StringAttrTM ) tbl.getModel() ).isChanged() ) return;
		Map<Integer, Operation> mop = ( ( StringAttrTM ) tbl.getModel() ).getOperation();
		List<Integer> li = Collections.emptyList();
		for ( int i : mop.keySet() ) {
			if ( mop.get( i ) != Operation.DELETE ) continue;
			if ( li.size() == 0 ) li = new ArrayList<>();
			li.add( i );			
		}
		Collections.sort( li );
		Collections.reverse( li );
		for ( int i : li ) ( ( StringAttrTM ) tbl.getModel() ).getRows().remove( i );
		mop.clear();
	}
	
	private void saveBeforeExit() {
		if ( visibleSaveBtn ) save();
	}

	private void fireStartLoading() {
		ActionEvent ae = new ActionEvent( this, 1, flagSaveLoad ? STRING_ATTR_START_SAVE : STRING_ATTR_START_LOAD );
		for ( ActionListener al : ell.getListeners( ActionListener.class ) ) al.actionPerformed( ae );
	}

	private void fireLoadSuccess() {
		ActionEvent ae = new ActionEvent( this, 1, flagSaveLoad ? STRING_ATTR_SAVE_SUCCESS : STRING_ATTR_LOAD_SUCCESS );
		for ( ActionListener al : ell.getListeners( ActionListener.class ) ) al.actionPerformed( ae );	
	}

	private void fireLoadFail() {
		ActionEvent ae = new ActionEvent( this, 1, flagSaveLoad ? STRING_ATTR_SAVE_FAIL : STRING_ATTR_LOAD_FAIL );
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
	private class StringAttrTM extends AbstractTableModel {
		// =============================================================================================================
		// Constants
		// =============================================================================================================
		private static final long serialVersionUID = 1;

		// =============================================================================================================
		// Fields
		// =============================================================================================================
		private String[] colName;
		private List<StringAttr> rows;
		private Map<Integer, Operation> ops;
		private boolean visibleKey;
		private boolean visibleValue;
		private boolean createEnabled;
		private boolean updateEnabled;

		// =============================================================================================================
		// Constructors
		// =============================================================================================================
		public StringAttrTM( String[] columnName, boolean visibleKey, boolean visibleValue, 
				boolean createEnabled, boolean updateEnabled ) {
			super();
			this.colName = columnName;
			this.visibleKey = visibleKey;
			this.visibleValue = visibleValue;
			this.createEnabled = createEnabled;
			this.updateEnabled = updateEnabled;
			if ( createEnabled ) {
				rows = new ArrayList<>();
				rows.add( new StringAttr( "", "", 1, ( short ) 0, null, null ) );
				ops = new TreeMap<>();
				ops.put( 0, Operation.CREATE );
			}
		}

		// =============================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =============================================================================================================
		@Override
		public int getColumnCount() {
			return colName.length;
		}

		@Override
		public Class<?> getColumnClass( int col ) {
			return String.class;
		}

		@Override
		public String getColumnName( int col ) {
			return colName[col];
		}

		@Override
		public int getRowCount() {			
			return rows == null ? 0 : rows.size();
		}

		@Override
		public boolean isCellEditable( int row, int col ) {
			if ( createEnabled && ( rows != null ) && ( row == ( rows.size() - 1 ) ) ) return true;
			if ( updateEnabled && ( rows != null ) && ( row >= 0 ) 
					&& ( row < ( rows.size() - ( createEnabled ? 1 : 0 ) ) ) ) return true;				
			return false;
		}

		@Override
		public Object getValueAt( int row, int col ) {
			if ( ( rows == null ) || ( row < 0 ) || ( row >= rows.size() ) ) return null;
			StringAttr sa = rows.get( row );
			if ( col == 1 ) return sa.getValue();
			if ( ( col == 0 ) && visibleKey ) return sa.getKey();
			if ( ( col == 0 ) && visibleValue && !visibleKey ) return sa.getValue();
			return null;
		}

		@Override
		public void setValueAt( Object val, int row, int col ) {
			if ( ( rows == null ) || ( row < 0 ) || ( row >= rows.size() ) 
					|| ( val == null ) || ( ( String ) val ).trim().isEmpty() 
					|| ( ( ops != null ) && ( ops.get( row ) == Operation.DELETE ) ) ) return;

			if ( ops == null ) ops = new TreeMap<Integer, Permission.Operation>();
			if ( ops.get( row ) == null ) {
				if ( createEnabled && ( row == ( rows.size() - 1 ) ) ) {
					ops.put( row, Operation.CREATE );
					rows.add( new StringAttr( "", "", 1, ( short ) 0, null, null ) );
					fireTableRowsInserted( rows.size() - 1, rows.size() - 1 );
					return;
				}
				ops.put( row, Operation.UPDATE );
				StringAttr old = rows.get( row );
				StringAttr nsa = new StringAttr( old.getKey(), old.getValue(), old.getItem(), old.getVariety(), 
						old.getExternalId(), old.getJournal() );
				nsa.setId( old.getId() );
				rows.set( row, nsa ); // save clone
			}
			if ( col == 1 ) rows.get( row ).setValue( ( String ) val );
			if ( ( col == 0 ) && visibleKey ) rows.get( row ).setKey( ( String ) val );
			if ( ( col == 0 ) && visibleValue && !visibleKey ) rows.get( row ).setValue( ( String ) val );
		}
		// =============================================================================================================
		// Methods
		// =============================================================================================================
		public List<StringAttr> getRows() {
			return rows;
		}

		public void deleteRow( int modelIndex ) {
			if ( ( rows == null ) || ( modelIndex < 0 ) || ( modelIndex >= rows.size() ) ) return;
			if ( ops == null  ) ops = new TreeMap<>();
			ops.put( modelIndex, Operation.DELETE );
		}

		/**
		 * set new list of StringAttr entities
		 * @param lst is list of StringAttr entities
		 */
		public void setRows( List<StringAttr> lst ) {
			List<StringAttr> tmp = rows;
			rows = lst;
			if ( createEnabled ) {
				if ( rows == null ) rows = new ArrayList<>();
				rows.add( new StringAttr( "", "", 1, ( short ) 0, null, null ) );
				if ( ops == null ) {
					ops = new TreeMap<>(); 
				} else {
					ops.clear();
				}
				ops.put( 0, Operation.CREATE );
			}
			if ( tmp != null ) tmp.clear();
		}
		
		/**
		 * check change in list of StringAttr entities
		 * @return true if list of StringAttr entities was changed; otherwise true
		 */
		public boolean isChanged() {
			return ( ops != null ) && ( ops.size() > 0 );
		}
		
		/**
		 * get map of operation on rows
		 * @return map<row_number, operation_type>
		 */
		public Map<Integer, Operation> getOperation() {
			return ops;
		}
	}
}