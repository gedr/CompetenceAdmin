package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.security.AccessControlException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityNotFoundException;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Triplet;
import ru.gedr.util.tuple.Tuple;
import ru.gedr.util.tuple.Tuple.TupleType;
import ru.gedr.util.tuple.Unit;
import minos.data.exporter.ActorsExporter;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.ActorsJpaController;
import minos.data.orm.controllers.ActorsPerformanceJpaController;
import minos.data.orm.controllers.MeasureJpaController;
import minos.data.services.FilialInfo;
import minos.entities.Actors;
import minos.entities.ActorsInfo;
import minos.entities.ActorsPerformance;
import minos.entities.Division;
import minos.entities.Measure;
import minos.entities.Person;
import minos.entities.Profile;
import minos.entities.ProfilePatternElement;
import minos.entities.VarietyConst;
import minos.ui.ComponentFabrica;
import minos.ui.MinosCellRenderer;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.ActorsDlg;
import minos.ui.dialogs.ComponentDlg;
import minos.ui.dialogs.ErrorDlg;
import minos.ui.dialogs.MeasureDlg;
import minos.ui.models.MainTreeNode;
import minos.ui.models.dataproviders.MeasureDataProvider;
import minos.utils.AuxFunctions;
import minos.utils.IconJoiner;
import minos.utils.Permission.Block;
import minos.utils.Permission.Operation;
import minos.utils.ProgramConfig;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import com.alee.extended.button.WebSplitButton;
import com.alee.extended.filechooser.WebDirectoryChooser;
import com.alee.extended.layout.ToolbarLayout;
import com.alee.extended.progress.WebProgressOverlay;
import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.WebAsyncTree;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.splitpane.WebSplitPane;
import com.alee.laf.table.WebTable;
import com.alee.laf.toolbar.WebToolBar;
import com.alee.utils.swing.DialogOptions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class MeasurePanel extends BasisPanel implements ActionListener, OrmCommand, TreeSelectionListener, 
TableColumnModelListener, PropertyChangeListener, TreeCellRenderer {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;

	private static final int TOOLBAR_ICONS_SIZE = 24;
	private static final int TABLE_ICONS_SIZE = 24;

	private static final String CMD_ADD_ACTORS 		= "1";
	private static final String CMD_EDIT_ACTORS 	= "2";
	private static final String CMD_COPY_ACTORS 	= "3";
	private static final String CMD_REMOVE_ACTORS 	= "4";
	private static final String CMD_RELOAD 			= "5";
	private static final String CMD_PROFILE_EDIT	= "6";
	private static final String CMD_MINOS_EDIT		= "7";
	private static final String CMD_SINNER_EDIT		= "8";
	private static final String CMD_ADD_MEASURE 	= "9";
	private static final String CMD_EDIT_MEASURE 	= "0";
	private static final String CMD_DELETE_MEASURE 	= "A";
	private static final String CMD_COMPILE_ACTORS 	= "B";
	private static final String CMD_SAVE_ALL 		= "C";
	private static final String CMD_ACTORS_REFRESH	= "D";
	private static final String CMD_SET_LOCK 		= "Завершить тест";
	private static final String CMD_SET_UNLOCK 		= "Возобновить тест";
	private static final String CMD_LOCK_UNLOCK		= "E";
	private static final String CMD_PRINT_ALL 		= "F";
	private static final String CMD_EXPORT_ALL 		= "G";

	public static final String SAVE_START			= "SaveStart";
	public static final String SAVE_SUCCESS 		= "SaveSuccess";
	public static final String SAVE_FAIL			= "SaveFail";

	public static final String ASSEMBLE_START		= "AssembleStart";
	public static final String ASSEMBLE_SUCCESS 	= "AssembleSuccess";
	public static final String ASSEMBLE_FAIL		= "AssembleFail";

	private static final Logger log = LoggerFactory.getLogger( MeasurePanel.class );


	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private Window owner = null;
	private WebAsyncTree<AsyncUniqueNode> tree = null;
	private WebTable table = null;
	private TableRowSorter<ActorsTM> sorter;	
	private WebComboBox editorReserveLevel;
	private WebComboBox editorReserveMode;
	private WebComboBox editorReserveType;
	private WebLabel rlbl;
	private WebSplitButton splitButton;
	private WebProgressOverlay wordReportProgress;
	private WebProgressOverlay excelReportProgress;

	private boolean lockOrUnlock  = true; //true is lock; false is unlock
	private PersonPanel personPanel;
	private PostProfilePanel profilePanel;

	private Division curBOffice = null;
	private List<Measure> selectedMeasures;
	private List<Pair<Operation, Measure>> measureOperation = Collections.emptyList();
	private Multimap<Integer, Triplet<Operation, Actors, Double>> cacheActors; // key is measure.id
	private Map<Long, double[]> testResultMap;// key is actors.id value is test result by variety

	private ProgramConfig pconf;
	private MeasureDataProvider mdp;

	private boolean visibleDeletedActors = false;
	private ActorsInfo emptyActorsInfo;

	private EventListenerList ell;	
	private Map<Integer, Icon> measureIcons; // key = state<<16 + operation
	private Icon[] crudIcons;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public MeasurePanel( Window owner ) { 
		super( true );
		this.owner = owner;
		selectedMeasures = new ArrayList<>();
		cacheActors = ArrayListMultimap.create();
		pconf = ( ProgramConfig ) ResourceKeeper.getObject( ResourceKeeper.OType.PROGRAM_CONFIG );

		crudIcons = new Icon[4];
		crudIcons[Operation.CREATE.getValue()] = ResourceKeeper.getIcon( IType.NEW, TABLE_ICONS_SIZE );
		crudIcons[Operation.READ.getValue()] = null;
		crudIcons[Operation.UPDATE.getValue()] = ResourceKeeper.getIcon( IType.PENCIL, TABLE_ICONS_SIZE );
		crudIcons[Operation.DELETE.getValue()] = ResourceKeeper.getIcon( IType.REMOVE, TABLE_ICONS_SIZE );;

		WebSplitPane split = new WebSplitPane( WebSplitPane.HORIZONTAL_SPLIT, 
				new WebScrollPane( makeMeasureTree() ), 
				new WebScrollPane( makeActorsTable() ) );
		split.setDividerLocation( pconf == null ? 100 : pconf.getMeasureDivider() );
		split.setOneTouchExpandable( true );		 
		split.setContinuousLayout( true );
		split.addPropertyChangeListener( this );
		emptyActorsInfo = ComponentFabrica.createActorsInfoComboBox( false, false, ActorsInfo.VARIETY_SPEC ).getItemAt( 0 );

		ell = new EventListenerList();
		addActionLictener( this );

		setLayout( new BorderLayout() );
		add( split, BorderLayout.CENTER );
		add( makeToolbar(), BorderLayout.NORTH );

		initCurrentBranchOffice();
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void actionPerformed( ActionEvent e ) {
		try {
			switch ( e.getActionCommand() ) {
				case CMD_ADD_ACTORS:
					if ( addActors( null ) ) AuxFunctions.repaintComponent( table );
					break;

				case CMD_COPY_ACTORS :
					if ( copyActors() ) AuxFunctions.repaintComponent( table );
					break;

				case CMD_EDIT_ACTORS :
					if ( editActors() ) AuxFunctions.repaintComponent( table );
					break;

				case CMD_REMOVE_ACTORS :
					if ( removeActors( null ) ) AuxFunctions.repaintComponent( table );
					break;

				case CMD_ADD_MEASURE :
					if ( addMeasure() ) AuxFunctions.repaintComponent( tree );
					break;

				case CMD_EDIT_MEASURE :
					if ( editMeasure() ) AuxFunctions.repaintComponent( tree );
					break;

				case CMD_DELETE_MEASURE :
					if ( deleteMeasure() ) tree.reloadRootNode();
					break;

				case CMD_MINOS_EDIT :
					showPersonDialog( true );
					break;

				case CMD_SINNER_EDIT :
					showPersonDialog( false );
					break;

				case CMD_PROFILE_EDIT :
					showProfileDialog();
					break;

				case CMD_SET_LOCK :
					splitButton.setIcon( ResourceKeeper.getIcon( IType.LOCK, TOOLBAR_ICONS_SIZE ) );
					splitButton.setToolTipText( CMD_SET_LOCK );
					lockOrUnlock = true;
					if ( lockUnlock() ) AuxFunctions.repaintComponent( table );
					break;

				case CMD_SET_UNLOCK :
					splitButton.setIcon( ResourceKeeper.getIcon( IType.UNLOCK, TOOLBAR_ICONS_SIZE ) );
					splitButton.setToolTipText( CMD_SET_UNLOCK );
					lockOrUnlock = false;
					if ( lockUnlock() ) AuxFunctions.repaintComponent( table );
					break;

				case CMD_LOCK_UNLOCK :
					if ( ( splitButton.getMousePosition().getX() < splitButton.getHeight() ) 
							&&  lockUnlock() ) AuxFunctions.repaintComponent( table );
					break;

				case CMD_RELOAD :
					reload( true );
					break;

				case CMD_ACTORS_REFRESH :
					AuxFunctions.repaintComponent( table );
					break;

				case CMD_SAVE_ALL : 
					saveAll();
					break;

				case CMD_COMPILE_ACTORS : 
					assembleTest();
					break;

				case ASSEMBLE_START : 
					setVisibleGlass( true );
					break;

				case ASSEMBLE_FAIL :
					setVisibleGlass( false );
					throw new IllegalStateException( e.getActionCommand() );

				case ASSEMBLE_SUCCESS : 
					setVisibleGlass( false );
					AuxFunctions.repaintComponent( table );
					break;

				case CMD_PRINT_ALL :
					printAll( ( Component ) e.getSource() );
					break;

				case CMD_EXPORT_ALL:
					exportAll( ( Component ) e.getSource() );
					break;

			}
		} catch ( AccessControlException aex ) { 
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasurePanel.actionPerformed() : ", aex );
			ErrorDlg.show( owner, "Ошибка", "Нет прав на выполнение операции", aex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasurePanel.actionPerformed() : ", ex );
			ErrorDlg.show( owner, "Ошибка", "Произошла ошибка при выполнении операции", ex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		}		
	}

	@Override
	public void execute( Object obj ) throws Exception {
		if ( ( ( Tuple ) obj ).getType() == TupleType.UNIT ) {
			for ( Pair<Operation, Measure> p : measureOperation ) {
				if ( ( p.getSecond().getId() == 0 ) && ( p.getFirst() == Operation.DELETE ) ) continue;
				if ( p.getFirst() == Operation.CREATE ) MeasureJpaController.getInstance().create( p.getSecond(), true, 
						false, false );
				if ( p.getFirst() == Operation.UPDATE ) MeasureJpaController.getInstance().update( p.getSecond(), true, 
						false, false );
				if ( p.getFirst() == Operation.DELETE ) MeasureJpaController.getInstance().delete( p.getSecond(), true, 
						false, false );
			}
			for( Triplet<Operation, Actors, Double> t : cacheActors.values() ) {
				if ( ( t.getSecond().getId() == 0 ) && ( t.getFirst() == Operation.DELETE ) ) continue;
				if ( t.getFirst() == Operation.CREATE ) ActorsJpaController.getInstance().create( t.getSecond(), true, 
						false, false );
				if ( t.getFirst() == Operation.UPDATE ) ActorsJpaController.getInstance().update( t.getSecond(), true, 
						false, false );
				if ( t.getFirst() == Operation.DELETE ) ActorsJpaController.getInstance().delete( t.getSecond(), true, 
						false, false );
			}
		}

		if ( ( ( Tuple ) obj ).getType() == TupleType.PAIR ) {
			@SuppressWarnings("unchecked")
			Pair<List<Actors>, List<ActorsPerformance>> p = ( Pair<List<Actors>, List<ActorsPerformance>> ) obj;
			for ( Actors a : p.getFirst() )  ActorsJpaController.getInstance().update( a, true, false, false );
			for ( ActorsPerformance ap : p.getSecond() )  ActorsPerformanceJpaController.getInstance().update( ap, 
					true, false, false );
		}
	}

	@Override
	public void propertyChange( PropertyChangeEvent e ) {
		if ( e.getPropertyName().equals( WebSplitPane.DIVIDER_LOCATION_PROPERTY ) 
				&& ( pconf != null ) 
				&& ( e.getNewValue() instanceof Integer ) ) {
			pconf.setMeasureDivider( ( Integer ) e.getNewValue() );					
		}		
	}

	@Override
	public void valueChanged( TreeSelectionEvent e ) {
		for ( TreePath p : e.getPaths() ) {
			Object obj = p.getLastPathComponent();
			if ( ( obj == null ) || !( obj instanceof MainTreeNode ) ) continue;
			obj = ( ( MainTreeNode ) obj ).getUserObject();
			if ( ( obj == null ) || !( obj instanceof Measure ) ) continue;

			Measure m = ( Measure ) obj;
			if ( e.isAddedPath( p ) ) selectedMeasures.add( m );
			else selectedMeasures.remove( m );
		}

		new SwingWorker<Object, Boolean>() {

			@Override
			protected Object doInBackground() throws Exception {
				try {
					publish( Boolean.TRUE );					
					loadActors();		
					return Boolean.TRUE;
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
					if ( o.equals( Boolean.TRUE ) ) selectDisplayActors();
					if ( o instanceof Exception ) throw ( ( Exception ) o );
				} catch ( Exception ex ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "loadActors failed", ex );
					ErrorDlg.show( owner, "Ошибка", "В процессе загрузки произошла ошибка", ex, 
							ResourceKeeper.getIcon( IType.ERROR, 32 ) );
				} finally {
					setVisibleGlass( false );
				}
			}
		}.execute();
	}


	@Override
	public Component getTreeCellRendererComponent( JTree tree, Object value, boolean isSelected, 
			boolean expanded, boolean leaf, int row, boolean hasFocus ) {
		if ( rlbl == null ) {
			rlbl = new WebLabel();
			rlbl.setPreferredSize( new Dimension( 1024, 24 ) );
		}
		if ( measureIcons == null ) measureIcons = new TreeMap<>();
		rlbl.setBackground( isSelected ? Color.LIGHT_GRAY : Color.WHITE );
		rlbl.setText( "" );
		rlbl.setIcon( null );

		if ( ( value == null ) || !( value instanceof MainTreeNode ) ) return rlbl;
		if ( ( ( MainTreeNode ) value ).getUserObject() instanceof Division ) {
			Division d = ( Division ) ( ( MainTreeNode ) value ).getUserObject();
			rlbl.setText( d.getFullName() );
			rlbl.setIcon( ResourceKeeper.getIcon( IType.OFFICE, TABLE_ICONS_SIZE ) );
			return rlbl;
		}
		if ( ( ( MainTreeNode ) value ).getUserObject() instanceof Measure ) {
			Measure m = ( Measure ) ( ( MainTreeNode ) value ).getUserObject();
			Operation op = getOperationForMeasure( m );
			Icon lblIcon = null;
			java.util.Date now = new Date();			
			if ( ( m.getJournal() != null ) && m.getJournal().getDeleteMoment().before( now ) ) {
				int shift = 0;  
				lblIcon = measureIcons.get( shift );
				if ( lblIcon == null ) {
					lblIcon = IconJoiner.HJoiner( 0, 0, 1, ResourceKeeper.getIcon( IType.MEASURE, TABLE_ICONS_SIZE ), 
							ResourceKeeper.getIcon( IType.DELETE, TABLE_ICONS_SIZE ) );
					measureIcons.put( shift, lblIcon );
				}
			}
			if ( ( lblIcon == null ) && m.getStop().before( now ) ) {
				int shift = ( 1 << 16 ) + op.getValue();  
				lblIcon = measureIcons.get( shift );
				if ( lblIcon == null ) {
					lblIcon = IconJoiner.HJoiner( 0, 0, 1, ResourceKeeper.getIcon( IType.MEASURE, TABLE_ICONS_SIZE ), 
							ResourceKeeper.getIcon( IType.CHECKERED_FLAG, TABLE_ICONS_SIZE ), 
							crudIcons[op.getValue()] );
					measureIcons.put( shift, lblIcon );
				}
			}
			if ( lblIcon == null ) {
				int shift = ( ( m.getStart().before( now ) ? 2 : 3 ) << 16 ) + op.getValue();
				lblIcon = measureIcons.get( shift );
				if ( lblIcon == null ) {
					lblIcon = IconJoiner.HJoiner( 0, 0, 1, ResourceKeeper.getIcon( IType.MEASURE, TABLE_ICONS_SIZE ), 
							ResourceKeeper.getIcon( ( m.getStart().before( now ) ? IType.BULB_ON : IType.BULB_OFF ), 
									TABLE_ICONS_SIZE ), crudIcons[op.getValue()] );
					measureIcons.put( shift, lblIcon );
				}
			}
			rlbl.setText( m.getName() );
			rlbl.setIcon( lblIcon );
			//rlbl.setBorder( BorderFactory.createBevelBorder( BevelBorder.RAISED ) );
			return rlbl;
		}
		return rlbl;
	}


	@Override
	public void columnMarginChanged( ChangeEvent e ) { 
		if ( pconf == null ) return;
		TableColumnModel tcm = ( TableColumnModel ) e.getSource(); 
		int[] arr = pconf.getActorsColumnSize();
		if ( ( arr == null ) || ( arr.length != tcm.getColumnCount() ) ) {
			arr = new int[ tcm.getColumnCount() ];
			pconf.setActorsColumnSize( arr );
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
	public void addActionLictener( ActionListener l ) {
		ell.add( ActionListener.class, l );
	}

	public void removeActionLictener( ActionListener l ) {
		ell.remove( ActionListener.class, l );
	}

	/**
	 * make Toolbar panel
	 * @return
	 */
	private Component makeToolbar() {
		WebPopupMenu popupMenu = new WebPopupMenu ();
		ImageIcon lock = ResourceKeeper.getIcon( IType.LOCK, TOOLBAR_ICONS_SIZE );
		ImageIcon unlock = ResourceKeeper.getIcon( IType.UNLOCK, TOOLBAR_ICONS_SIZE );
		popupMenu.add( new ActionAdapter( CMD_SET_LOCK, lock, CMD_SET_LOCK, CMD_SET_LOCK, this, 0 ) );
		popupMenu.add( new ActionAdapter( CMD_SET_UNLOCK, unlock, CMD_SET_UNLOCK, CMD_SET_UNLOCK, this, 0 ) );
		splitButton = new WebSplitButton( new ActionAdapter( "", lockOrUnlock ? lock : unlock, 
				CMD_LOCK_UNLOCK, ( lockOrUnlock ? "Завершить" : "Возобновить" ) + " тест", this, 0 ) );
		splitButton.setPopupMenu( popupMenu );

		wordReportProgress = new WebProgressOverlay();
		wordReportProgress.setConsumeEvents( false );
		wordReportProgress.setComponent( new WebButton( new ActionAdapter( null, 
				ResourceKeeper.getIcon( IType.WORD, TOOLBAR_ICONS_SIZE ), CMD_PRINT_ALL, "распечатать отчет", this, 0 ) ) );

		excelReportProgress = new WebProgressOverlay();
		excelReportProgress.setConsumeEvents( false );
		excelReportProgress.setComponent( new WebButton( new ActionAdapter( null, 
				ResourceKeeper.getIcon( IType.EXCEL, TOOLBAR_ICONS_SIZE ), CMD_EXPORT_ALL, "Вывод информации в Excel", 
				this, 0 ) ) );

		WebToolBar tb = new WebToolBar();    
		tb.add( new ActionAdapter( "refresh all", ResourceKeeper.getIcon( IType.REFRESH, TOOLBAR_ICONS_SIZE ), 
				CMD_RELOAD, "обновить", this, 0 ) );
		tb.add( new ActionAdapter( "save all", ResourceKeeper.getIcon( IType.SAVE_ALL, TOOLBAR_ICONS_SIZE ), 
				CMD_SAVE_ALL, "сохранить все изменения", this, 0 ) );

		tb.addSeparator( 7 );

		tb.add( wordReportProgress );
		tb.add( excelReportProgress );

		tb.addSeparator( 7 );

		tb.add( new ActionAdapter( "add measure", ResourceKeeper.getIcon( IType.MEASURE_ADD, TOOLBAR_ICONS_SIZE ), 
				CMD_ADD_MEASURE, "создать мероприятие", this, 0 ) );
		tb.add( new ActionAdapter( "edit measure", ResourceKeeper.getIcon( IType.MEASURE_EDIT, TOOLBAR_ICONS_SIZE ), 
				CMD_EDIT_MEASURE, "редактировать мероприятие", this, 0 ) );
		tb.add( new ActionAdapter( "delete measure", ResourceKeeper.getIcon( IType.MEASURE_DELETE, TOOLBAR_ICONS_SIZE ), 
				CMD_DELETE_MEASURE, "удаление мероприятий", this, 0 ) );
		tb.addSeparator( 7 );

		tb.add( new ActionAdapter( "add users", ResourceKeeper.getIcon( IType.USERS_ADD, TOOLBAR_ICONS_SIZE ), 
				CMD_ADD_ACTORS, "добавить", this, 0 ) );
		tb.add( new ActionAdapter( "edit users", ResourceKeeper.getIcon( IType.USERS_EDIT, TOOLBAR_ICONS_SIZE ), 
				CMD_EDIT_ACTORS, "редактировать", this, 0 ) );
		tb.add( new ActionAdapter( "copy users", ResourceKeeper.getIcon( IType.USERS_COPY, TOOLBAR_ICONS_SIZE ), 
				CMD_COPY_ACTORS, "копировать", this, 0 ) );
		tb.add( new ActionAdapter( "delete users", ResourceKeeper.getIcon( IType.USERS_DELETE, TOOLBAR_ICONS_SIZE ), 
				CMD_REMOVE_ACTORS, "удаление", this, 0 ) );
		tb.add( new ActionAdapter( "run", ResourceKeeper.getIcon( IType.RUN, TOOLBAR_ICONS_SIZE ), 
				CMD_COMPILE_ACTORS, "формирование теста", this, 0 ) );
		tb.add( splitButton );
		tb.addSeparator( 7 );

		@SuppressWarnings("unchecked")
		TableRowSorter<ActorsTM> trs = ( TableRowSorter<ActorsTM> ) table.getRowSorter();
		tb.add( ComponentFabrica.createTableFilter( trs, 24, "Введите строку фильтра" ), ToolbarLayout.FILL );

		return tb;
	}

	/**
	 * make Panel and add Components for Measure's Entity work
	 * @return new JPanel object or null
	 */	
	private Component makeMeasureTree() {
		mdp = new MeasureDataProvider();
		tree = new WebAsyncTree<>( mdp );
		tree.setRootVisible( false );
		tree.setCellRenderer( this );
		tree.addTreeSelectionListener( this ) ;
		return tree;
	}

	/**
	 * make Panel and add Components for Actors' Entity work
	 * @return new JPanel object or null
	 */
	private Component makeActorsTable() {
		ActorsTM tm = new ActorsTM();
		sorter = new TableRowSorter<ActorsTM>( tm );
		MinosCellRenderer<Actors> render = new MinosCellRenderer<>( 16 );

		table = new WebTable( tm );
		table.setRowSorter( sorter );
		table.setAutoResizeMode( WebTable.AUTO_RESIZE_OFF );
		table.getColumnModel().addColumnModelListener( this );
		table.getColumnModel().getColumn( ActorsTM.STATUS_COLUMN ).setCellRenderer( tm );
		table.getColumnModel().getColumn( ActorsTM.MINOS_COLUMN ).setCellRenderer( render );
		table.getColumnModel().getColumn( ActorsTM.SINNER_COLUMN ).setCellRenderer( render );
		table.getColumnModel().getColumn( ActorsTM.RESERVE_LEVEL_COLUMN ).setCellRenderer( render );
		table.getColumnModel().getColumn( ActorsTM.RESERVE_TYPE_COLUMN ).setCellRenderer( render );
		table.getColumnModel().getColumn( ActorsTM.RESERVE_MODE_COLUMN ).setCellRenderer( render );
		if ( AuxFunctions.isPermission( Block.ACTORS_INNER, Operation.CREATE ) 
				|| AuxFunctions.isPermission( Block.ACTORS_INNER, Operation.UPDATE )
				|| AuxFunctions.isPermission( Block.ACTORS_OUTER, Operation.CREATE )
				|| AuxFunctions.isPermission( Block.ACTORS_OUTER, Operation.UPDATE ) ) {			
			makeTableEditors();
		}
		AuxFunctions.initTableColumnWidth( table.getColumnModel(), pconf.getActorsColumnSize(), 100 );
		table.getInputMap(JTable.WHEN_FOCUSED).put( KeyStroke.getKeyStroke( KeyEvent.VK_F5, 0 ), CMD_ACTORS_REFRESH );
		table.getActionMap().put( CMD_ACTORS_REFRESH, new ActionAdapter( null, null, CMD_ACTORS_REFRESH, null, this, 0 ) );
		return table;
	}

	private void makeTableEditors() {
		editorReserveLevel = ( WebComboBox ) ComponentFabrica.createActorsInfoComboBox( true, false,
				ActorsInfo.VARIETY_SPEC, ActorsInfo.VARIETY_LEVEL );
		DefaultCellEditor dce = new DefaultCellEditor( editorReserveLevel );
		dce.setClickCountToStart( 2 );
		table.getColumnModel().getColumn( ActorsTM.RESERVE_LEVEL_COLUMN ).setCellEditor( dce );

		editorReserveMode = ( WebComboBox ) ComponentFabrica.createActorsInfoComboBox( true, false,
				ActorsInfo.VARIETY_SPEC, ActorsInfo.VARIETY_MODE );
		dce = new DefaultCellEditor( editorReserveMode );
		dce.setClickCountToStart( 2 );
		table.getColumnModel().getColumn( ActorsTM.RESERVE_MODE_COLUMN ).setCellEditor( dce );

		editorReserveType = ( WebComboBox ) ComponentFabrica.createActorsInfoComboBox( true, false,
				ActorsInfo.VARIETY_SPEC, ActorsInfo.VARIETY_TYPE );
		dce = new DefaultCellEditor( editorReserveType );
		dce.setClickCountToStart( 2 );
		table.getColumnModel().getColumn( ActorsTM.RESERVE_TYPE_COLUMN ).setCellEditor( dce );

		JTextField txt = ComponentFabrica.createTableCellEditorForDialog( ActionAdapter.build( "...", null, 
				CMD_MINOS_EDIT, "выбор эксперта", this, 0 ) );
		dce = new DefaultCellEditor( txt );
		dce.setClickCountToStart( 2 );
		table.getColumnModel().getColumn( ActorsTM.MINOS_COLUMN ).setCellEditor( dce );

		txt = ComponentFabrica.createTableCellEditorForDialog( ActionAdapter.build( "...", null, CMD_SINNER_EDIT, 
				"выбор оцениваемого", this, 0 ) );
		dce = new DefaultCellEditor( txt );
		dce.setClickCountToStart( 2 );
		table.getColumnModel().getColumn( ActorsTM.SINNER_COLUMN ).setCellEditor( dce );

		txt = ComponentFabrica.createTableCellEditorForDialog( ActionAdapter.build( "...", null, CMD_PROFILE_EDIT, 
				"выбор профиля", this, 0 ) );
		dce = new DefaultCellEditor( txt );
		dce.setClickCountToStart( 2 );
		table.getColumnModel().getColumn( ActorsTM.PROFILE_COLUMN ).setCellEditor( dce );
	}

	/**
	 * load Division entity for current branch office code
	 * @return Division entity
	 */
	private Division initCurrentBranchOffice() {
		try {
			Byte b = ResourceKeeper.getObject( OType.DEFAULT_BRANCH_OFFICE_CODE );
			List<FilialInfo> lfi = ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO );
			for ( FilialInfo fi : lfi ) {
				if ( fi.getCode() == b ) {
					curBOffice = ( Division ) OrmHelper.findEntity( Division.class, fi.getRootDivisionCode() );
					break;
				}
			}
			if ( curBOffice == null ) throw new EntityNotFoundException( "Division entity for current branch office not found" );
			return curBOffice;
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasurePanel.initCurrentBranchOffice() : ", ex );
			throw ex;	
		}
	}

	/**
	 * load Actors entities for selected Measures
	 */
	private void loadActors() {
		if ( ( selectedMeasures == null ) || ( selectedMeasures.size() == 0 ) ) return;
		List<Measure> load = null;
		load = new ArrayList<>() ;
		for ( Measure m : selectedMeasures ) {				
			if ( !cacheActors.containsKey( m.getId() ) ) load.add( m );
		}
		List<Actors> lst = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_ACTORS ), 
				Actors.class, 
				new Pair<Object, Object>( "measures", load ),
				new Pair<Object, Object>( "dts", !visibleDeletedActors ? new Timestamp( System.currentTimeMillis() ) 
				: ResourceKeeper.getObject( OType.WAR ) ) );
		if ( ( lst == null ) || ( lst.size() == 0 ) ) return;

		List<Long> ll = new ArrayList<>();
		for ( Actors a : lst ) {
			cacheActors.put( a.getMeasure().getId(), new Triplet<>( Operation.READ, a, -1.0 ) );
			if ( a.getStatus() == Actors.STATUS_ACTIVE ) ll.add( a.getId() );
		}
		if ( ll.size() == 0 ) return;
		String s = Arrays.toString( ll.toArray( new Long[0] ) ).replace( "[", " " ).replace( "]", " " );
		List<Object[]> los = OrmHelper.findByQuery( QueryType.SQL, 
				String.format( ResourceKeeper.getQuery( QType.SQL_LOAD_ESTIMATION ), s ), 
				Object[].class );
		if ( ( los == null ) || ( los.size() == 0 ) ) return;

		if ( testResultMap == null ) testResultMap = new TreeMap<>();
		long id = 0;
		double[] val = null ;
		int pos = 0;
		for ( Object[] o : los ) {
			Number n = ( Number ) o[0];
			if ( n.longValue() != id ) {
				id = n.longValue();
				val = new double[( VarietyConst.VARIETY_COUNT - 1 ) * 2];
				Arrays.fill( val, 0.0 );
				pos = 0;
				testResultMap.put( id, val );
			}
			n = ( Number ) o[1];
			pos = ( n.intValue() - 1 ) * 2;
			val[pos] = ( ( Number ) o[2] ).doubleValue();
			val[pos + 1] = ( ( Number ) o[3] ).doubleValue();
		}
	}

	/**
	 * make list of actors for display in table
	 * @return list of actors for selected measures or empty list
	 */
	private void selectDisplayActors() {
		List<Triplet<Operation, Actors, Double>> lst = Collections.emptyList();
		if ( ( selectedMeasures != null ) && ( selectedMeasures.size() > 0 ) ) {
			lst = new ArrayList<>();
			for ( Measure m : selectedMeasures ) {
				if ( !cacheActors.containsKey( m.getId() ) ) continue;
				lst.addAll( cacheActors.get( m.getId() ) );			
			}		
		}
		if ( table.getCellEditor() != null ) table.getCellEditor().cancelCellEditing();

		// temporal disconnect all renderer because call convertRowIndexToModel()
		table.setVisible( false );
		( ( ActorsTM ) table.getModel() ).setListActors( lst );
		table.getRowSorter().allRowsChanged();
		table.setVisible( true );
	}

	/**
	 * create new Actors entity in DB
	 * @param src - Actors entity for copy 
	 * @return true if create new entity successfully; otherwise false
	 * @throws Exception
	 */
	private boolean addActors( Actors src ) throws Exception {
		Measure measure = null;
		if ( src != null ) {
			measure = src.getMeasure();
		} else {
			if ( !AuxFunctions.checkRightSelect( owner, tree, false, false, 
					"Необходимо выделить одно мероприятие по оценке", Measure.class ) ) return false;
			measure =  ( Measure ) tree.getSelectedNode().getUserObject();
		}		
		Date now = new Date();
		if (  measure.getStop().before( now ) 
				|| ( ( measure.getJournal() != null ) && measure.getJournal().getDeleteMoment().before( now ) )
				|| !AuxFunctions.isPermission( ( curBOffice.equals( measure.getBranchOffice() ) ? Block.ACTORS_INNER 
						: Block.ACTORS_OUTER ), Operation.CREATE ) ) {
			throw new AccessControlException( "MeasurePanel.addActors() : add Actors permission denied" );
		}
		Timestamp doomsday = ( Timestamp ) ResourceKeeper.getObject( OType.DOOMSDAY );
		Actors a = new Actors( ( src == null ? null : src.getMinos() ), null, 
				( src == null ? ( short ) 2 : src.getGauge() ), 
				doomsday, Actors.STATUS_BUILDING, measure, 
				( src == null ? null : src.getProfile() ), null, 
				( src == null ? emptyActorsInfo : src.getTestMode() ), 
				( src == null ? emptyActorsInfo : src.getReserveLevel() ), 
				( src == null ? emptyActorsInfo : src.getReserveType() ), null );
		a.setAssembly( doomsday );
		a.setSinnerType( src == null ? Actors.SINNER_TYPE_UNKNOWN : src.getSinnerType() );
		if ( a.getSinnerType() == Actors.SINNER_TYPE_INNER ) a.setInternalSinner( src.getInternalSinner() );
		if ( a.getSinnerType() == Actors.SINNER_TYPE_ALIEN ) {
			a.setAlienSinner( src.getAlienSinner() );
			a.setAlienSinnerVersion( src.getAlienSinnerVersion() );
		}
		cacheActors.put( measure.getId(), new Triplet<>( Operation.CREATE, a, 0.0 ) );
		//a = ActorsJpaController.getInstance().create( a, true, false, true );		
		selectDisplayActors();
		return true;
	}

	/**
	 * copy Actors entity from existing entity
	 * @return true if create successfully; otherwise false
	 * @throws Exception
	 */
	private boolean copyActors() throws Exception {
		if ( table.getSelectedRowCount() != 1 ) {
			WebOptionPane.showMessageDialog( owner, "Необходимо выбрать 1 строку", "Ошибка", WebOptionPane.OK_OPTION, 
					ResourceKeeper.getIcon( IType.ERROR, 32 ) );
			return false;
		}
		return addActors( ( ( ActorsTM ) table.getModel() ).getActors( table.getSelectedRow() ) );
	}

	private boolean editActorsPerformance( Actors a ) {
		ComponentDlg.show( owner, "Тест", new ActorsPerfomancePanel( a ), null );
		return false;
	}

	private boolean editActors() {
		ActorsTM model = ( ActorsTM ) table.getModel();
		if ( ( table.getSelectedRowCount() == 1 ) && ( new Date().after( model.getActors( 
				table.convertRowIndexToModel( table.getSelectedRow() ) ).getAssembly() ) ) ) {
			return editActorsPerformance( model.getActors( table.convertRowIndexToModel( table.getSelectedRow() ) ) );
		}

		if ( table.getSelectedRowCount() < 1 ) return false;
		List<Actors> la = new ArrayList<>();
		for ( int i : table.getSelectedRows() ) la.add( model.getActors( table.convertRowIndexToModel( i ) ) );

		Unit<PersonPanel> upp = new Unit<PersonPanel>( personPanel );
		la = ActorsDlg.show( owner, "Эксперт-оцениваемый", la, false, upp );
		personPanel = upp.getFirst();

		if ( ( la == null ) || ( la.size() == 0 ) ) return false;
		for ( int i : table.getSelectedRows() ) {
			int index = table.convertRowIndexToModel( i );
			Triplet<Operation, Actors, Double> t =  model.getListActors().get( index );
			if ( !model.isCellEditable( index, ActorsTM.MINOS_COLUMN ) ) continue;
			// save change for editable Actors object
			Actors dst = t.getSecond();
			Actors src = la.get( 0 );
			boolean change = false;
			if ( ( src.getMinos() != null ) && !AuxFunctions.equals( src.getMinos(), dst.getMinos(), true ) ) {
				dst.setMinos( src.getMinos() );
				change = true;
			}
			if ( ( src.getTestMode().getId() != ComponentFabrica.NULL_ACTORS_INFO_ID ) 
					&& !AuxFunctions.equals( src.getTestMode(), dst.getTestMode(), true ) ) {
				dst.setTestMode( src.getTestMode() );
				change = true;
			}
			if ( ( src.getReserveLevel().getId() != ComponentFabrica.NULL_ACTORS_INFO_ID ) 
					&& !AuxFunctions.equals( src.getReserveLevel(), dst.getReserveLevel(), true ) ) {
				dst.setReserveLevel( src.getReserveLevel() );
				change = true;
			}
			if ( ( src.getReserveType().getId() != ComponentFabrica.NULL_ACTORS_INFO_ID ) 
					&& !AuxFunctions.equals( src.getReserveType(), dst.getReserveType(), true ) ) {
				dst.setReserveType( src.getReserveType() );
				change = true;
			}
			if ( ( src.getProfile() != null ) && !AuxFunctions.equals( src.getProfile(), dst.getProfile(), true ) ) {
				dst.setProfile( src.getProfile() );
				change = true;
			}
			if ( ( src.getSinnerType() == Actors.SINNER_TYPE_INNER ) 
					&& ( ( src.getSinnerType() != dst.getSinnerType() ) 
							|| !AuxFunctions.equals( dst.getInternalSinner(), src.getInternalSinner(), true ) ) ) {
				dst.setSinnerType( src.getSinnerType() );
				dst.setInternalSinner( src.getInternalSinner() );
				change = true;
			}
			if ( ( src.getSinnerType() == Actors.SINNER_TYPE_ALIEN ) 
					&& ( ( src.getSinnerType() != dst.getSinnerType() ) 
							|| !AuxFunctions.equals( dst.getAlienSinner(), src.getAlienSinner(), true ) 
							|| ( src.getAlienSinnerVersion() != dst.getAlienSinnerVersion() ) ) ) {
				dst.setSinnerType( src.getSinnerType() );
				dst.setAlienSinner( src.getAlienSinner() );
				dst.setAlienSinnerVersion( src.getAlienSinnerVersion() );
				change = true;
			}
			if ( ( t.getFirst() == Operation.READ ) && change ) t.setFirst( Operation.UPDATE );
		}
		return true;
	}

	private boolean removeActors( Measure m ) {
		if ( m == null ) {
			if ( table.getSelectedRowCount() < 0 ) return false;
			for ( int i : table.getSelectedRows() ) {
				( ( ActorsTM ) table.getModel() ).deleteRow( table.convertRowIndexToModel( i ) );
			}
			return true;
		}
		ActorsTM model = ( ActorsTM ) table.getModel();
		for ( Triplet<Operation, Actors, Double> t : model.getListActors() ) {
			if ( ( m.getId() == 0 ) && ( m == t.getSecond().getMeasure() ) ) t.setFirst( Operation.DELETE );
			if ( ( m.getId() != 0 ) && ( m.equals( t.getSecond().getMeasure() ) ) ) t.setFirst( Operation.DELETE );
		}
		return true;
	}

	/**
	 * add new Measure entity
	 * @return true if create new Measure entity
	 * @throws Exception
	 */
	private boolean addMeasure() throws Exception  {
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, false, 
				"Необходимо выделить филиал", Division.class ) ) return false;
		Division sd = ( Division ) tree.getSelectedNode().getUserObject();
		Block blk = isInnerDivision( sd ) ? Block.MEASURE_INNER : Block.MEASURE_OUTER;
		if ( !AuxFunctions.isPermission( blk, Operation.CREATE ) ) {
			throw new AccessControlException( "MeasurePanel.addMeasure() : create measure permission denied" );
		}
		Measure nm = MeasureDlg.show( owner, "Новый мероприятие", null, false );
		if ( nm == null ) return false;
		nm.setBranchOffice( sd );
		mdp.addOuterMeasure( nm );
		setOperationForMeasure( nm, Operation.CREATE );
		tree.reloadSelectedNodes();
		return true;
	}

	/**
	 * edit selected Measure entity
	 * @return true if operation complete successfully
	 * @throws Exception
	 */
	private boolean editMeasure() throws Exception  {
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, false, 
				"Необходимо выделить мероприятие", Measure.class ) ) return false;
		Measure sm = ( Measure ) tree.getSelectedNode().getUserObject();

		Division sd = ( Division ) tree.getSelectedNode().getParent().getUserObject();
		Block blk = isInnerDivision( sd ) ? Block.MEASURE_INNER : Block.MEASURE_OUTER;
		boolean readOnly = !AuxFunctions.isPermission( blk, Operation.UPDATE ) 
				|| ( getOperationForMeasure( sm ) == Operation.DELETE )
				|| ( ( sm.getJournal() != null ) 
						&& ( sm.getJournal().getDeleteMoment().before( new java.util.Date() ) ) );

		Measure nm = MeasureDlg.show( owner, ( readOnly ? "Просмотр" : "Редактирование" ) + " мероприятия", sm, readOnly );
		if ( nm == null ) return false;
		if ( getOperationForMeasure( sm ) == Operation.READ ) setOperationForMeasure( sm, Operation.UPDATE );

		//MeasureJpaController.getInstance().update( nm, true, false, true );
		return true;
	}

	/**
	 * delete selected Measure entity 
	 * @return true if operation complete successfully
	 * @throws Exception
	 */
	private boolean deleteMeasure() throws Exception {
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, true, 
				"Необходимо выделить мероприятия", Measure.class ) ) return false;
		boolean[] flag = new boolean [] { false, false, false }; // 0- inner; 1 -outer; 2-create
		int countDeleted = 0;
		java.util.Date now = new Date();
		for ( AsyncUniqueNode node : tree.getSelectedNodes() ) {
			if ( !( node.getUserObject() instanceof Measure ) ) continue;
			Measure m = ( Measure ) node.getUserObject();
			flag[( isInnerDivision( ( Division ) node.getParent().getUserObject() ) ? 0 : 1 )] = true;
			if ( getOperationForMeasure( m ) == Operation.CREATE ) flag[2] = true;
			if ( ( getOperationForMeasure( m ) == Operation.DELETE )
					|| ( ( m.getJournal() != null ) 
							&& ( m.getJournal().getDeleteMoment().before( now ) ) ) ) countDeleted++;
		}
		if ( countDeleted == tree.getSelectionCount() ) return false;
		if ( ( flag[0] && !AuxFunctions.isPermission( Block.MEASURE_INNER, Operation.DELETE ) 
				&& !AuxFunctions.isPermission( Block.ACTORS_INNER, Operation.DELETE ) )	 
				|| ( flag[1] && !AuxFunctions.isPermission( Block.MEASURE_OUTER, Operation.DELETE ) 
						&& !AuxFunctions.isPermission( Block.ACTORS_OUTER, Operation.DELETE ) ) ) {
			throw new AccessControlException( "MeasurePanel.deleteMeasure() : delete measure permission denied" );
		}
		for ( AsyncUniqueNode node : tree.getSelectedNodes() ) {
			if ( !( node.getUserObject() instanceof Measure ) ) continue;
			Measure m = ( Measure ) node.getUserObject();

			if ( ( getOperationForMeasure( m ) == Operation.DELETE )
					|| ( ( m.getJournal() != null ) 
							&& ( m.getJournal().getDeleteMoment().before( now ) ) ) ) continue;

			setOperationForMeasure( m, Operation.DELETE );
			removeActors( m );
		}
		return true;
	}

	private Operation getOperationForMeasure( Measure m ) {
		for ( Pair<Operation, Measure> p : measureOperation ) {
			if ( ( m.getId() == 0 ) &&  ( m == p.getSecond() ) ) return p.getFirst();
			if ( ( m.getId() != 0 ) &&  ( m.equals( p.getSecond() ) ) ) return p.getFirst();
		}
		return Operation.READ;
	}

	private void setOperationForMeasure( Measure m, Operation op ) {
		for ( Pair<Operation, Measure> p : measureOperation) {
			if ( m == p.getSecond() ) { 
				p.setFirst( op );
				return;
			}
		}
		if ( measureOperation.size() == 0 ) measureOperation = new ArrayList<>();
		measureOperation.add( new Pair<>( op, m ) );
	}

	/**
	 * get inner or outer division
	 * @param d is existing division
	 * @return true if division belong DEFAULT_BRANCH_OFFICE_CODE; otherwise false
	 */
	private boolean isInnerDivision( Division d ) {
		Byte b = ResourceKeeper.getObject( OType.DEFAULT_BRANCH_OFFICE_CODE );
		List<FilialInfo> lfi = ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO );
		for ( FilialInfo fi : lfi ) {
			if ( ( fi.getCode() == b ) && ( d.getId() >= fi.getDivisionCodeMin() ) 
					&& ( d.getId() <= fi.getDivisionCodeMax() ) ) return true;
		}
		return false;
	}

	/**
	 * show dialog for select Minos or Sinner person
	 * @param isMinos is selected flag for Minos
	 */
	private void showPersonDialog( boolean isMinos ) {
		if ( personPanel == null ) {
			personPanel = new PersonPanel( owner, true );
			personPanel.setPreferredSize( new Dimension( 500, 300 ) );
		}
		if ( JOptionPane.OK_OPTION == ComponentDlg.show( owner, 
				"Выбор " + ( isMinos ? "эксперта" : "оцениваемого" ), personPanel, null ) ) {
			Person p = personPanel.getSeleсtedPerson();
			if ( p != null ) table.getModel().setValueAt( p, 
					table.convertRowIndexToModel( table.getEditingRow() ), 
					table.convertColumnIndexToModel( table.getEditingColumn() ) );
		}	
		table.getCellEditor().cancelCellEditing(); 
	}

	/**
	 * show dialog for select Profile
	 */
	private void showProfileDialog() {
		if ( profilePanel == null ) {
			profilePanel = new PostProfilePanel( owner, true );
			profilePanel.setPreferredSize( new Dimension( 400, 300 ) );
		}
		if ( JOptionPane.OK_OPTION == ComponentDlg.show( owner, "Выбор профиля", profilePanel, null ) ) {
			Profile p = profilePanel.getSelectedProfile();
			if ( p != null ) table.getModel().setValueAt( p, 
					table.convertRowIndexToModel( table.getEditingRow() ), 
					table.convertColumnIndexToModel( table.getEditingColumn() ) );
		}	
		table.getCellEditor().cancelCellEditing(); 
	}

	/**
	 * lock or unlock actors test 
	 * @return true if change successfully
	 */
	private boolean lockUnlock() {
		if ( table.getSelectedRowCount() < 1 ) return false;
		ActorsTM model = ( ActorsTM ) table.getModel();
		Timestamp now = new Timestamp( System.currentTimeMillis() );
		for ( int i : table.getSelectedRows() ) {
			int row = table.convertRowIndexToModel( i );
			Triplet<Operation, Actors, Double> t = model.getListActors().get( row );
			Block blk = ( t.getSecond().getMeasure().getBranchOffice().equals( curBOffice ) ? Block.ACTORS_INNER : 
				Block.ACTORS_OUTER );
			if ( !AuxFunctions.isPermission( blk, Operation.UPDATE ) 
					|| ( t.getSecond().getStatus() != Actors.STATUS_ACTIVE ) ) continue;
			if ( lockOrUnlock && t.getSecond().getFinish().after( now ) ) {
				t.getSecond().setFinish( now );
				t.setFirst( Operation.UPDATE );
			}
			if ( !lockOrUnlock && t.getSecond().getFinish().before( now ) ) {
				t.getSecond().setFinish( ( Timestamp ) ResourceKeeper.getObject( OType.DAMNED_FUTURE ) );
				t.setFirst( Operation.UPDATE );
			}
		}		
		return true;
	}

	private void reload( boolean checkChange ) {
		boolean actorsChange = false;
		for ( Triplet<Operation, Actors, Double> t : cacheActors.values() ) {
			if ( t.getFirst() != Operation.READ ) actorsChange = true;
		}
		boolean measureChange = ( ( measureOperation != null ) && ( measureOperation.size() > 0 ) );
		if ( checkChange && ( actorsChange || measureChange ) ) {
			int res = WebOptionPane.showConfirmDialog( owner, "У Вас есть несохраненые данные, "
					+ "\nпосле обновления они будут утеряны. \nОбновить данные ?", "", WebOptionPane.YES_NO_OPTION, 
					WebOptionPane.QUESTION_MESSAGE );
			if ( WebOptionPane.YES_OPTION != res ) return;
		}

		mdp.clearOuterMeasure();
		table.setVisible( false );
		( ( ActorsTM ) table.getModel() ).setListActors( Collections.<Triplet<Operation, Actors, Double>>emptyList() );
		table.setVisible( true );
		if ( selectedMeasures != null ) selectedMeasures.clear();
		if ( testResultMap != null ) testResultMap.clear();
		if ( measureOperation != null ) measureOperation.clear();
		if ( cacheActors != null ) cacheActors.clear();
		tree.reloadRootNode();
	}

	/**
	 * save all changes in DB
	 */
	private void saveAll() {
		new SwingWorker<Object, Boolean>() {

			@Override
			protected Object doInBackground() throws Exception {
				try {
					publish( Boolean.TRUE );					
					OrmHelper.executeAsTransaction( MeasurePanel.this, new Unit<Boolean>( true ) );		
					return Boolean.TRUE;
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
					if ( o.equals( Boolean.TRUE ) ) reload( false );

					if ( o instanceof Exception ) throw ( ( Exception ) o );
				} catch ( Exception ex ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "save failed", ex );
					ErrorDlg.show( owner, "Ошибка", "В процессе загрузки произошла ошибка", ex, 
							ResourceKeeper.getIcon( IType.ERROR, 32 ) );
				} finally {
					setVisibleGlass( false );
				}
			}
		}.execute();
	}

	/**
	 * assembly test for selected Actors 
	 * @throws Exception
	 */
	private void assembleTest() throws Exception {
		if ( table.getSelectedRowCount() < 1 ) return;
		if ( !checkCompile() ) {
			WebOptionPane.showMessageDialog( owner, "Сборка теста невозможна \n(Причины: не заполнены все поля и/или "
					+ "\nссылается на несохраненые данные)", "Ошибка", WebOptionPane.ERROR_MESSAGE );
			return;
		}
		new Thread( new Runnable() {

			@Override
			public void run() {
				try {
					fireStartLoading( ASSEMBLE_START );
					List<Long> ll = getProfiles();
					if ( ( ll == null ) || ( ll.size() == 0 ) ) {
						throw new IllegalStateException( "MeasurePanle.compile() : wrong profiles" );
					}
					List<Profile> lp = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
							ResourceKeeper.getQuery( QType.JPQL_LOAD_PROFILE_FOR_TEST_ASSEMBLER ), 
							Profile.class, 
							new Pair<Object, Object>( "profile_ids", ll ) );
					if ( ( lp == null ) || ( lp.size() == 0 ) ) {
						throw new EntityNotFoundException( "MeasurePanle.compile() : cannot find all profiles" );
					}
					List<ActorsPerformance> lap = assembleTest( lp );
					List<Actors> la = new ArrayList<>();
					for ( ActorsPerformance ap : lap ) {
						if ( !la.contains( ap.getActor() ) ) la.add( ap.getActor() );
					}
					OrmHelper.executeAsTransaction( MeasurePanel.this, 
							new Pair<List<Actors>, List<ActorsPerformance>>( la, lap ) );

					fireLoadSuccess( ASSEMBLE_SUCCESS );
				} catch ( Exception ex ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasurePanle.compile() : ", ex );
					fireLoadFail( ASSEMBLE_FAIL );
				}
			}
		}).start();
		return;
	}

	/**
	 * check all constraint before create test
	 * @return true if constraint completed
	 */
	private boolean checkCompile() {
		ActorsTM model = ( ActorsTM ) table.getModel();
		for ( int i : table.getSelectedRows() ) {
			int row = table.convertRowIndexToModel( i );
			Triplet<Operation, Actors, Double> t = model.getListActors().get( row );
			Actors a = t.getSecond();
			if ( ( a.getStatus() != Actors.STATUS_BUILDING ) ||( t.getFirst() != Operation.READ ) 
					|| ( a.getMeasure() == null ) || ( a.getMeasure().getId() == 0 ) 
					|| ( getOperationForMeasure( a.getMeasure() ) != Operation.READ )
					|| ( a.getMinos() == null ) 
					|| ( a.getSinnerType() == Actors.SINNER_TYPE_UNKNOWN )
					|| ( ( a.getSinnerType() == Actors.SINNER_TYPE_INNER ) && ( a.getInternalSinner() == null ) )
					|| ( ( a.getSinnerType() == Actors.SINNER_TYPE_ALIEN ) && ( a.getAlienSinner() == null ) )
					|| ( a.getProfile() == null )
					|| ( a.getTestMode().getVariety() != ActorsInfo.VARIETY_MODE )
					|| ( a.getReserveLevel().getVariety() != ActorsInfo.VARIETY_LEVEL )
					|| ( a.getReserveType().getVariety() != ActorsInfo.VARIETY_TYPE ) ) return false;
		}
		return true;
	}

	/**
	 * select profiles from selected actors
	 * @return list of profile
	 */
	private List<Long> getProfiles() {
		List<Long> lp = new ArrayList<>();
		ActorsTM model = ( ActorsTM ) table.getModel();
		for ( int i : table.getSelectedRows() ) {
			int row = table.convertRowIndexToModel( i );
			long l = model.getListActors().get( row ).getSecond().getProfile().getId();
			if ( !lp.contains( l ) ) lp.add( l );
		}
		return lp;
	}

	/**
	 * make list of <code>ActorsPerformance</code> for selected <code>Actors</code>
	 * @param lp is list of Profiles, what contain <code>ProfilePatternElement</code>s
	 * @return list of new <code>ActorsPerformance</code> object 
	 */
	private List<ActorsPerformance> assembleTest( List<Profile> lp ) {
		ActorsTM model = ( ActorsTM ) table.getModel();
		List<ActorsPerformance> lap = new ArrayList<>();
		for ( int i : table.getSelectedRows() ) {
			int row = table.convertRowIndexToModel( i );
			Actors a = model.getListActors().get( row ).getSecond();
			for ( Profile p : lp ) {
				if ( !a.getProfile().equals( p ) ) continue;
				for ( ProfilePatternElement ppe : p.getProfilePattern().getProfilePatternElements() ) {
					if ( ppe.getStatus() != ProfilePatternElement.STATUS_ACTIVE ) continue;
					lap.add( new ActorsPerformance( null, 0.0, ppe, a ) );
				}
				a.setStatus( Actors.STATUS_ACTIVE );
				a.setAssembly( new Timestamp( System.currentTimeMillis() ) );
				a.setFinish( ( Timestamp ) ResourceKeeper.getObject( OType.DAMNED_FUTURE ) );
				break;
			}
		}
		return lap;
	}

	private void printAll( Component c ) {
		if ( table.getSelectedRowCount() < 1 ) return;
		WebDirectoryChooser dirChooser = new WebDirectoryChooser( owner, "Укажите папку для выгрузки" );
		dirChooser.setVisible( true );
		if ( dirChooser.getResult() != DialogOptions.OK_OPTION ) return;
		final File file = dirChooser.getSelectedDirectory();

		ActorsTM model = ( ActorsTM ) table.getModel();
		final List<Actors> la = new ArrayList<>();
		for ( int i : table.getSelectedRows() ) {
			int row = table.convertRowIndexToModel( i );
			la.add( model.getListActors().get( row ).getSecond() );
		}

		wordReportProgress.setShowLoad( true );
		new SwingWorker<Object, Boolean>() {

			@Override
			protected Object doInBackground() throws Exception {
				try {
					ActorsExporter we = new ActorsExporter();
					we.exportToWord( la, file.getAbsolutePath() + "\\" );
					return Boolean.TRUE;
				} catch ( Exception ex ) {					
					return ex;
				}
			}

			@Override
			protected void done() {
				super.done();
				try {
					wordReportProgress.setShowLoad( false );
					Object o = get();
					if ( o instanceof Exception ) throw ( ( Exception ) o );
				} catch ( Exception ex ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "save failed", ex );
					ErrorDlg.show( owner, "Ошибка", "В процессе работы произошла ошибка", ex, 
							ResourceKeeper.getIcon( IType.ERROR, 32 ) );
				} 
			}
		}.execute();
	}

	private void exportAll( Component c ) {
		if ( table.getSelectedRowCount() < 1 ) return;


		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setSelectedFile( new File( "report.xlsx" ) );
		//WebFileChooser fileChooser = new WebFileChooser ();
		//if ( fileChooser.showSaveDialog( owner ) != WebFileChooser.APPROVE_OPTION ) return;

		if ( fileChooser.showSaveDialog( owner ) != JFileChooser.APPROVE_OPTION ) return;
		final File file = fileChooser.getSelectedFile ();

		ActorsTM model = ( ActorsTM ) table.getModel();
		final List<Actors> la = new ArrayList<>();
		for ( int i : table.getSelectedRows() ) {
			int row = table.convertRowIndexToModel( i );
			la.add( model.getListActors().get( row ).getSecond() );
		}

		excelReportProgress.setShowLoad( true );
		new SwingWorker<Object, Boolean>() {

			@Override
			protected Object doInBackground() throws Exception {
				try {
					ActorsExporter we = new ActorsExporter();
					we.exportToExcel( la, file.getAbsolutePath() );
					return Boolean.TRUE;
				} catch ( Exception ex ) {					
					return ex;
				}
			}

			@Override
			protected void done() {
				super.done();
				try {
					excelReportProgress.setShowLoad( false );
					Object o = get();
					if ( o instanceof Exception ) throw ( ( Exception ) o );
				} catch ( Exception ex ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "save failed", ex );
					ErrorDlg.show( owner, "Ошибка", "В процессе загрузки произошла ошибка", ex, 
							ResourceKeeper.getIcon( IType.ERROR, 32 ) );
				} 
			}
		}.execute();
	}



	private void fireStartLoading( String cmd ) {
		ActionEvent ae = new ActionEvent( this, 1, cmd );
		for ( ActionListener al : ell.getListeners( ActionListener.class ) ) al.actionPerformed( ae );
	}

	private void fireLoadSuccess( String cmd ) {
		ActionEvent ae = new ActionEvent( this, 1, cmd );
		for ( ActionListener al : ell.getListeners( ActionListener.class ) ) al.actionPerformed( ae );	
	}

	private void fireLoadFail( String cmd ) {
		ActionEvent ae = new ActionEvent( this, 1, cmd );
		for ( ActionListener al : ell.getListeners( ActionListener.class ) ) al.actionPerformed( ae );	
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private class ActorsTM extends AbstractTableModel implements TableCellRenderer {
		// =============================================================================================================
		// Constants
		// =============================================================================================================
		private static final long serialVersionUID = 1L;

		private static final int COLUMN_COUNT 		= 9;

		public static final int STATUS_COLUMN 		= 0;
		public static final int KOD_COLUMN 			= 1;
		public static final int MINOS_COLUMN 		= 2;
		public static final int SINNER_COLUMN 		= 3;
		public static final int RESERVE_TYPE_COLUMN	= 4;
		public static final int RESERVE_MODE_COLUMN	= 5;
		public static final int RESERVE_LEVEL_COLUMN= 6;
		public static final int PROFILE_COLUMN		= 7;
		public static final int ESTIMATION_COLUMN	= 8;

		// =============================================================================================================
		// Fields
		// =============================================================================================================
		List<Triplet<Operation, Actors, Double>> data;
		private Class<?>[] columnClasses;
		private String[] columnNames;
		Icon[] icons;
		private JLabel lbl;

		// =============================================================================================================
		// Constructors
		// =============================================================================================================
		public ActorsTM() {
			super();
			data = Collections.emptyList();

			columnNames = new String[COLUMN_COUNT];
			columnNames[STATUS_COLUMN] 			= "Статус";
			columnNames[KOD_COLUMN] 					= "Код";
			columnNames[MINOS_COLUMN] 				= "Эксперт";
			columnNames[SINNER_COLUMN] 			= "Оцениваемый";
			columnNames[RESERVE_TYPE_COLUMN]		= "Резерв";
			columnNames[RESERVE_MODE_COLUMN] 	= "Вид";
			columnNames[RESERVE_LEVEL_COLUMN] 	= "Уровень";
			columnNames[PROFILE_COLUMN] 			= "Профиль";
			columnNames[ESTIMATION_COLUMN] 		= "Оценка";

			columnClasses = new Class<?>[COLUMN_COUNT];
			columnClasses[STATUS_COLUMN] 			= ImageIcon.class;
			columnClasses[KOD_COLUMN] 				= Long.class;
			columnClasses[MINOS_COLUMN] 			= Person.class;
			columnClasses[SINNER_COLUMN] 			= Object.class;
			columnClasses[RESERVE_TYPE_COLUMN]	= ActorsInfo.class;
			columnClasses[RESERVE_MODE_COLUMN] 	= ActorsInfo.class;
			columnClasses[RESERVE_LEVEL_COLUMN] = ActorsInfo.class;
			columnClasses[PROFILE_COLUMN] 			= String.class;
			columnClasses[ESTIMATION_COLUMN]		= String.class;

			lbl = new JLabel();
			int iconSize = 16;
			Icon runIcon = ( Icon ) ResourceKeeper.getIcon( IType.RUN, iconSize );
			Icon hammerIcon = ( Icon ) ResourceKeeper.getIcon( IType.HAMMER, iconSize );
			Icon lockIcon = ( Icon ) ResourceKeeper.getIcon( IType.LOCK, iconSize );
			Icon unlockIcon = ( Icon ) ResourceKeeper.getIcon( IType.UNLOCK, iconSize );
			Icon pencilIcon = ( Icon ) ResourceKeeper.getIcon( IType.PENCIL, iconSize );
			Icon removeIcon = ( Icon ) ResourceKeeper.getIcon( IType.REMOVE, iconSize );
			Icon newIcon = ( Icon ) ResourceKeeper.getIcon( IType.NEW, iconSize );
			icons = new Icon [] { 
					IconJoiner.HJoiner( 0, 0, 1, runIcon, lockIcon ),
					IconJoiner.HJoiner( 0, 0, 1, runIcon, unlockIcon ),
					IconJoiner.HJoiner( 0, 0, 1, runIcon, lockIcon, pencilIcon ),
					IconJoiner.HJoiner( 0, 0, 1, runIcon, unlockIcon, pencilIcon ),
					IconJoiner.HJoiner( 0, 0, 1, runIcon, lockIcon, removeIcon ),
					IconJoiner.HJoiner( 0, 0, 1, runIcon, unlockIcon, removeIcon ),
					hammerIcon,
					IconJoiner.HJoiner( 0, 0, 1, hammerIcon, newIcon ),
					IconJoiner.HJoiner( 0, 0, 1, hammerIcon, pencilIcon ),
					IconJoiner.HJoiner( 0, 0, 1, hammerIcon, removeIcon ) 
			};
		}
		// =============================================================================================================
		// Getter & Setter
		// =============================================================================================================
		public void setListActors( List<Triplet<Operation, Actors, Double>> la ) {
			List<Triplet<Operation, Actors, Double>> tmp = data;
			data = la;
			tmp.clear();
		}

		public List<Triplet<Operation, Actors, Double>> getListActors() {
			return data;
		}

		public Actors getActors( int index ) {
			if ( ( index < 0 ) || ( index >= data.size() ) ) return null;
			return data.get( index ).getSecond();
		}

		// =============================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =============================================================================================================
		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public Class<?> getColumnClass( int col ) {
			return columnClasses[col];
		}

		@Override
		public String getColumnName( int col ) {
			return columnNames[col];
		}

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public Object getValueAt( int row, int col ) {
			if ( ( data == null ) || ( row < 0 ) || ( row >= data.size() ) ) return null;
			Actors a = data.get( row ).getSecond();
			switch ( col ) {
				case KOD_COLUMN : 
					return Long.valueOf( a.getId() );

				case MINOS_COLUMN : 
					return a.getMinos();

				case SINNER_COLUMN : return ( ( a.getSinnerType() == Actors.SINNER_TYPE_UNKNOWN ) ? null : 
					( ( a.getSinnerType() == Actors.SINNER_TYPE_ALIEN ) ? a.getAlienSinner() : a.getInternalSinner() ) );

				case RESERVE_TYPE_COLUMN : 
					return a.getReserveType();

				case RESERVE_MODE_COLUMN : 
					return a.getTestMode();

				case RESERVE_LEVEL_COLUMN : 
					return a.getReserveLevel();

				case PROFILE_COLUMN : 
					return ( a.getProfile() == null ? "<не определен>" : a.getProfile().getProfilePattern().getName() );

				case ESTIMATION_COLUMN : 	
					if ( data.get( row ).getThird() < 0 ) {
						if ( ( testResultMap == null ) || !testResultMap.containsKey( a.getId() ) ) {
							data.get( row ).setThird( 0.0 );
						} else {
							double[] d = testResultMap.get( a.getId() );
							double res = 0.0;
							double min = 0.0;
							for ( int i = 0; i < ( d.length / 2 ); i++ ) {
								res += d[i * 2];
								min += d[i * 2 + 1];
							}
							data.get( row ).setThird( min == 0 ? 0.0 : ( res / min * 100 ) );
						}
					}
					if ( data.get( row ).getThird() == 0.0 ) return "";
					if ( data.get( row ).getThird() > 0.0 ) return String.format( "%10.2f", data.get( row ).getThird() );
					break;
			}		
			return null;
		}

		@Override
		public boolean isCellEditable( int row, int col ) {	
			if ( ( data == null ) || ( row < 0 ) || ( row >= data.size() ) 
					|| ( col == STATUS_COLUMN ) || ( col == KOD_COLUMN ) || ( col == ESTIMATION_COLUMN )
					|| ( data.get( row ).getFirst() == Operation.DELETE ) ) return false;
			Actors a = data.get( row ).getSecond();
			Block blk = ( a.getMeasure().getBranchOffice().equals( curBOffice ) ? Block.ACTORS_INNER : 
				Block.ACTORS_OUTER );
			return ( data.get( row ).getFirst() == Operation.CREATE ) 
					|| ( data.get( row ).getFirst() == Operation.UPDATE ) 
					|| ( ( data.get( row ).getFirst() == Operation.READ ) 
							&&( a.getStatus() == Actors.STATUS_BUILDING ) 
							&& AuxFunctions.isPermission( blk, Operation.UPDATE ) );
		}

		@Override
		public void setValueAt( Object value, int row, int col ) {
			if ( ( row < 0 ) || ( row >= data.size() ) || ( col < 0 ) || ( col >= columnNames.length ) 
					|| ( value == null ) ) return;
			Actors a = data.get( row ).getSecond();
			switch (col) {
				case MINOS_COLUMN : 
					if ( ( value instanceof Person ) && !( ( Person ) value ).equals( a.getMinos() ) ) {
						a.setMinos( ( Person ) value );
						if ( data.get( row ).getFirst() == Operation.READ ) data.get( row ).setFirst( Operation.UPDATE );
					}
					break;

				case SINNER_COLUMN : 	
					if ( ( value instanceof Person ) && !( ( Person ) value ).equals( a.getInternalSinner() ) ) {
						a.setInternalSinner( ( Person ) value );
						a.setSinnerType( Actors.SINNER_TYPE_INNER );
						a.setAlienSinner( null );
						a.setAlienSinnerVersion( ( short ) 0 );
						if ( data.get( row ).getFirst() == Operation.READ ) data.get( row ).setFirst( Operation.UPDATE );
					}
					break;

				case RESERVE_TYPE_COLUMN :
					if ( ( value instanceof ActorsInfo ) && !( ( ActorsInfo ) value ).equals( a.getReserveType() ) ) {
						a.setReserveType( ( ActorsInfo ) value );
						if ( data.get( row ).getFirst() == Operation.READ ) data.get( row ).setFirst( Operation.UPDATE );
					}
					break;

				case RESERVE_MODE_COLUMN : 	
					if ( ( value instanceof ActorsInfo ) && !( ( ActorsInfo ) value ).equals( a.getTestMode() ) ) {
						a.setTestMode( ( ActorsInfo ) value );
						if ( data.get( row ).getFirst() == Operation.READ ) data.get( row ).setFirst( Operation.UPDATE );
					}
					break;

				case RESERVE_LEVEL_COLUMN : 
					if ( ( value instanceof ActorsInfo ) && !( ( ActorsInfo ) value ).equals( a.getReserveLevel() ) ) {
						a.setReserveLevel( ( ActorsInfo ) value );
						if ( data.get( row ).getFirst() == Operation.READ ) data.get( row ).setFirst( Operation.UPDATE );
					}
					break;

				case PROFILE_COLUMN : 	
					if ( ( value instanceof Profile ) && !( ( Profile ) value ).equals( a.getProfile() ) ) {
						a.setProfile( ( Profile ) value );
						if ( data.get( row ).getFirst() == Operation.READ ) data.get( row ).setFirst( Operation.UPDATE );
					}				
					break;

				default:
					break;
			}
		}

		@Override
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, 
				boolean hasFocus, int row, int column ) {
			java.util.Date now = new Date();
			row = table.convertRowIndexToModel( row );
			Actors a = data.get( row ).getSecond();

			if ( a.getStatus() == Actors.STATUS_ACTIVE ) {
				switch( data.get( row ).getFirst() ) {
					case READ : 
						lbl.setIcon( icons[( a.getFinish().before( now ) ? 0 : 1 )] );
						break;
					case UPDATE : 
						lbl.setIcon( icons[( a.getFinish().before( now ) ? 2 : 3 )] );
						break;
					case DELETE :
						lbl.setIcon( icons[( a.getFinish().before( now ) ? 4 : 5 )] );
						break;
					case CREATE:
						break;
				}
			}
			if ( a.getStatus() == Actors.STATUS_BUILDING ) {
				switch( data.get( row ).getFirst() ) {
					case READ : 
						lbl.setIcon( icons[6]  );
						break;
					case CREATE : 
						lbl.setIcon( icons[7] );
						break;
					case UPDATE : 
						lbl.setIcon( icons[8] );
						break;
					case DELETE : 
						lbl.setIcon( icons[9] );
						break;
				}
			}
			return lbl;
		}

		// =============================================================================================================
		// Methods
		// =============================================================================================================
		public void deleteRow( int row ) {
			if ( ( data == null ) || ( row < 0 ) || ( row >= data.size() ) ) return;
			data.get( row ).setFirst( Operation.DELETE );
		}
	}
}