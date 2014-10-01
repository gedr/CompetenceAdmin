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
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.swing.Icon;
import javax.swing.JSeparator;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.ActorsInfoJpaController;
import minos.entities.ActorsInfo;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.ErrorDlg;
import minos.utils.AuxFunctions;
import minos.utils.Permission.Block;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import com.alee.extended.panel.WebButtonGroup;
import com.alee.laf.button.WebToggleButton;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.table.WebTable;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.toolbar.WebToolBar;

public class ActorsInfoPanel extends BasisPanel implements ActionListener, OrmCommand {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	
	private static final int TOOLBAR_ICON_SIZE = 24;
	
	private static final String CMD_ADD		= "1";
	private static final String CMD_REMOVE	= "2";
	private static final String CMD_SAVE 	= "3";
	private static final String CMD_REFRESH = "4";	
	
	private static final String CMD_RESERVE = "A";
	private static final String CMD_MODE 	= "B";
	private static final String CMD_LEVEL 	= "C";

	private static final Logger log = LoggerFactory.getLogger( ActorsInfoPanel.class );

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private WebToolBar tb;
	private Window owner;
	private WebTable tbl;
	
	private List<Pair<ActorsInfo, Operation>> data;

	private boolean visibleDeleted = false;
	private boolean flagSaveBeforeExit = true;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ActorsInfoPanel( Window owner ) {
		super( true );
		this.owner = owner;

		setLayout( new BorderLayout() );
		if ( !AuxFunctions.isPermission( Block.ACTORS_INFO, Operation.READ ) ) {
			add( new WebTextArea( "Нет прав на чтение") );
			return;
		}
		loadActorsInfo();
		add( makeToolBar(), BorderLayout.NORTH );
		add( makeBody(), BorderLayout.CENTER );
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean getVisibleDeleted() {
		return visibleDeleted;
	}

	public void setVisibleDeleted( boolean val ) {
		if ( this.visibleDeleted == val ) return;
		this.visibleDeleted = val;
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void actionPerformed( ActionEvent e ) {
		try {
			switch ( e.getActionCommand() ) {
			case CMD_LEVEL : 
				if ( tbl.isEditing() ) tbl.getCellEditor().cancelCellEditing();
				( ( ActorsInfoTM ) tbl.getModel() ).setVariety( ActorsInfo.VARIETY_LEVEL );
				break;

			case CMD_RESERVE : 
				if ( tbl.isEditing() ) tbl.getCellEditor().cancelCellEditing();
				( ( ActorsInfoTM ) tbl.getModel() ).setVariety( ActorsInfo.VARIETY_TYPE );
				break;

			case CMD_MODE : 
				if ( tbl.isEditing() ) tbl.getCellEditor().cancelCellEditing();
				( ( ActorsInfoTM ) tbl.getModel() ).setVariety( ActorsInfo.VARIETY_MODE );
				break;

			case CMD_ADD :
				if ( !add() ) return;			
				break;

			case CMD_REMOVE :
				if ( !remove() ) return;
				break;

			case CMD_SAVE : 
				save( false );
				break;

			case CMD_REFRESH : 
				refresh();
				break;

			}

			tbl.revalidate();
			tbl.repaint();
		} catch ( AccessControlException aex ) { 
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ActorsInfoPanel.actionPerformed() : ", aex );
			ErrorDlg.show( owner, "Ошибка", "Нет прав на выполнение операции", aex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ActorsInfoPanel.actionPerformed() : ", ex );
			ErrorDlg.show( owner, "Ошибка", "Произошла ошибка при выполнении операции", ex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		}		
	}

	@Override
	public void execute( Object obj ) throws Exception {
		ActorsInfoJpaController c =  ActorsInfoJpaController.getInstance();
		for ( Pair<ActorsInfo, Operation> p : data ) {
			if ( p.getSecond() == Operation.CREATE )c.create( p.getFirst(), true, false, false );
			if ( p.getSecond() == Operation.UPDATE )c.update( p.getFirst(), true, false, false );
			if ( ( p.getSecond() == Operation.DELETE ) 
					&& ( p.getFirst().getId() != 0 ) ) c.delete( p.getFirst(), true, false, false );
		}
	}
	
	@Override
	public void internalFrameClosing( InternalFrameEvent e ) { 
		if ( flagSaveBeforeExit ) save( true );
	}

	@Override
	public void windowClosing( WindowEvent e ) { 
		if ( flagSaveBeforeExit ) save( true );
	}


	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * make tool bar component and initialize button command 
	 * @return ToolBar object 
	 */
	private Component makeToolBar() {
		tb = new WebToolBar();		
		tb.add( new ActionAdapter( "add", ResourceKeeper.getIcon( IType.ADD, TOOLBAR_ICON_SIZE), 
				CMD_ADD, "Добавление нового элемента", this, 0) ); 
		tb.add( new ActionAdapter( "remove", ResourceKeeper.getIcon( IType.REMOVE, TOOLBAR_ICON_SIZE ), 
				CMD_REMOVE, "Добавление новой компетенции в каталог", this, 0) ); 
		tb.add( new JSeparator() );
		tb.add( new ActionAdapter( "save", ResourceKeeper.getIcon( IType.SAVE_ALL, TOOLBAR_ICON_SIZE ), 
				CMD_SAVE, "Сохранение изменений", this, 0) ); 
		tb.add( new ActionAdapter( "refresh", ResourceKeeper.getIcon( IType.REFRESH, TOOLBAR_ICON_SIZE ), 
				CMD_REFRESH, "Загрузить данные", this, 0) ); 

		return tb;
	}

	/**
	 * make panel for display ActorsInfo entities 
	 * @return panel contain WebTable and Buttons
	 */
	private Component makeBody() {
		tbl = new WebTable( new ActorsInfoTM( ActorsInfo.VARIETY_TYPE ) );
		tbl.getColumnModel().getColumn( 0 ).setMaxWidth( 28 );
		tbl.setRowHeight( 28 );

		WebToggleButton b1 = new WebToggleButton( new ActionAdapter( "Резерв", null, CMD_RESERVE, null, this, 0 ) );
		WebToggleButton b2 = new WebToggleButton( new ActionAdapter( "Вид", null, CMD_MODE, null, this, 0 ) );
		WebToggleButton b3 = new WebToggleButton( new ActionAdapter( "Уровень", null, CMD_LEVEL, null, this, 0 ) );		 
		WebButtonGroup textGroup = new WebButtonGroup( true, b1, b2, b3 );
		textGroup.setButtonsDrawFocus( false );
		b1.setSelected( true );

		WebPanel panel = new WebPanel( new BorderLayout() );
		panel.add( textGroup, BorderLayout.NORTH );
		panel.add( new WebScrollPane( tbl ), BorderLayout.CENTER );

		return panel;
	}

	/**
	 * load ActorsInfo entities from DB
	 */
	private void loadActorsInfo() {
		new SwingWorker<Object, Boolean>() {

			@Override
			protected Object doInBackground() throws Exception {
				try {
					publish( Boolean.TRUE );					
					Timestamp now = new Timestamp( System.currentTimeMillis() );
					List<Short> vrts = Arrays.asList( ActorsInfo.VARIETY_LEVEL, ActorsInfo.VARIETY_MODE, 
							ActorsInfo.VARIETY_TYPE );
					List<ActorsInfo> lst = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
							ResourceKeeper.getQuery( QType.JPQL_LOAD_ACTORS_INFO ), 
							ActorsInfo.class,
							new Pair<Object, Object>( "varieties", vrts ),
							new Pair<Object, Object>( "ts", now ),				
							new Pair<Object, Object>( "dts", visibleDeleted ? now 
									: ResourceKeeper.getObject( OType.WAR ) ) ); 
					if ( ( lst == null ) || ( lst.size() < 1 ) ) {
						throw new EntityNotFoundException( "ActorsInfoPanel.loadActorsInfo()" );
					}
					return lst;
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
					List<ActorsInfo> lst = ( List<ActorsInfo> ) o;
					List<Pair<ActorsInfo, Operation>> tmp = new ArrayList<>();
					for ( ActorsInfo ai : lst ) tmp.add( new Pair<>( ai, Operation.READ ) );
					data = tmp;
					tbl.revalidate();
					tbl.repaint();
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

	/**
	 * find in common list ActorsInfo entity and Operation type for variety and index
	 * @param variety - ActorsInfo variety
	 * @param index - list index for ActorsInfo variety
	 * @return pair ActorsInfo entity and Operation type ; otherwise null
	 */
	private Pair<ActorsInfo, Operation> getActorsInfoByVarietyAndIndex( short variety, int index ) {
		if ( data == null ) return null;
		int num = 0;
		for ( Pair<ActorsInfo, Operation> p : data ) {
			if ( p.getFirst().getVariety() == variety ) {
				if ( num == index ) return p;
				num++;
			}
		}
		return null;
	}

	/**
	 * get count ActorsInfo entities by variety
	 * @param variety type
	 * @return count ActorsInfo entities having variety ; otherwise -1
	 */
	private int getActorsInfoCountByVariety( short variety ) {
		if ( data == null ) return -1;
		int cnt = 0;
		for ( Pair<ActorsInfo, Operation> p : data ) {
			if ( p.getFirst().getVariety() == variety ) cnt++;
		}
		return cnt;
	}
	
	/**
	 * save all new or update or remove ActorsInfo objects in DB
	 * @throws Exception
	 */
	private void save( boolean flagExit ) {
		if ( flagExit ) flagSaveBeforeExit = false;
		if ( flagExit && checkDataChange() 
				&& WebOptionPane.YES_OPTION != WebOptionPane.showConfirmDialog( owner, "Сохраниить изменнения?", 
						"Запрос", WebOptionPane.YES_NO_OPTION, WebOptionPane.QUESTION_MESSAGE ) ) return;
		if ( ( data == null ) || ( data.size() < 1 ) ) return;
		
		new SwingWorker<Object, Boolean>() {

			@Override
			protected Object doInBackground() throws Exception {
				try {
					publish( Boolean.TRUE );					
					OrmHelper.executeAsTransaction( ActorsInfoPanel.this, null );
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
					if ( o instanceof Exception ) throw ( ( Exception ) o );
					for ( int i = data.size() - 1; i >= 0; i-- ) {
						Pair<ActorsInfo, Operation> p = data.get( i );
						if ( ( p.getSecond() == Operation.CREATE ) 
								|| ( p.getSecond() == Operation.UPDATE ) ) p.setSecond( Operation.READ );
						if ( p.getSecond() == Operation.DELETE ) {
							if ( p.getFirst().getId() == 0 ) {
								data.remove( i );
							} else {
								if ( visibleDeleted ) p.setSecond( Operation.READ );
								else data.remove( i );
							}
						}
					}
					tbl.revalidate();
					tbl.repaint();
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
	
	/**
	 * check data list for change ( operation create or update or delete ) 
	 * @return true if objects in list was changed; otherwise false
	 */
	private boolean checkDataChange() {
		if ( data == null ) return false;
		for ( Pair<ActorsInfo, Operation> p : data ) {
			if ( ( p.getSecond() == Operation.CREATE ) || ( p.getSecond() == Operation.UPDATE )
					|| ( ( p.getSecond() == Operation.DELETE ) && ( p.getFirst().getId() != 0 ) ) ) return true;
		}
		return false;
	}
	
	/**
	 * remove ActorsInfo objects
	 */
	private boolean remove() {
		if ( tbl.getSelectedRow() < 0 ) return false;
		if ( !AuxFunctions.isPermission( Block.ACTORS_INFO, Operation.DELETE ) ) {
			for ( int i : tbl.getSelectedRows() ) {
				if ( data.get( tbl.convertRowIndexToModel( i ) ).getFirst().getId() != 0 ) {
					WebOptionPane.showMessageDialog( owner, "Ошибка", "Нет прав на удаление элемента", 
							WebOptionPane.ERROR_MESSAGE );
					return false;
				}
			}
		}
		short vrt = ( ( ActorsInfoTM ) tbl.getModel() ).getVariety();
		java.util.Date now = new java.util.Date();
		for ( int i : tbl.getSelectedRows() ) {
			Pair<ActorsInfo, Operation> p = getActorsInfoByVarietyAndIndex( vrt, tbl.convertRowIndexToModel( i ) );
			if ( ( p.getFirst().getJournal() != null  ) 
					&& p.getFirst().getJournal().getDeleteMoment().before( now ) ) continue;
			p.setSecond( Operation.DELETE );
		}
		return true;
	}

	/**
	 * add new ActorsInfo object
	 */
	private boolean add() {
		if ( !AuxFunctions.isPermission( Block.ACTORS_INFO, Operation.CREATE ) ) {
			WebOptionPane.showMessageDialog( owner, "Ошибка", "Нет прав на создание элемента", 
					WebOptionPane.ERROR_MESSAGE );
			return false;
		}
		data.add( new Pair<>( new ActorsInfo( "", ( ( ActorsInfoTM ) tbl.getModel() ).getVariety(), null ), 
				Operation.CREATE ) );
		return true;
	}
	
	private void refresh() {
		if ( checkDataChange() 
				&& WebOptionPane.YES_OPTION != WebOptionPane.showConfirmDialog( owner, 
						"Обновление приведет к потере \nнесохраненных данных. \nПродолжить?", "Запрос", 
						WebOptionPane.YES_NO_OPTION, WebOptionPane.QUESTION_MESSAGE ) ) return;
		loadActorsInfo();
	}


	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private class ActorsInfoTM implements TableModel {
		private short variety;
		private Icon[] icons;
		
		// =============================================================================================================
		// Constructors
		// =============================================================================================================
		public ActorsInfoTM( short variety ) {
			this.variety = variety;
			icons = new Icon[] { ResourceKeeper.getIcon( IType.NEW, 24 ), 
					ResourceKeeper.getIcon( IType.PENCIL, 24 ),
					ResourceKeeper.getIcon( IType.REMOVE, 24 ) };
		}

		// =============================================================================================================
		// Getter & Setter
		// =============================================================================================================
		public short getVariety() {
			return variety;
		}

		public void setVariety( short variety ) {
			this.variety = variety;
		}

		// =============================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =============================================================================================================
		@Override
		public Class<?> getColumnClass( int columnIndex ) {
			return columnIndex == 0 ? Icon.class : String.class;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName( int columnIndex ) {
			return columnIndex == 0 ? " " : "Название";
		}

		@Override
		public int getRowCount() {
			int cnt = getActorsInfoCountByVariety( variety );
			return cnt < 0 ? 0 : cnt;
		}

		@Override
		public Object getValueAt( int rowIndex, int columnIndex ) {
			if ( columnIndex == 0 ) {
				Operation op = getActorsInfoByVarietyAndIndex( variety, rowIndex ).getSecond();
				return ( op == Operation.CREATE ? icons[0] :
					( op == Operation.UPDATE ? icons[1] : ( op == Operation.DELETE ? icons[2] : null ) ) );
			}
			return getActorsInfoByVarietyAndIndex( variety, rowIndex ).getFirst().getName();
		}

		@Override
		public boolean isCellEditable( int rowIndex, int columnIndex ) {
			Pair<ActorsInfo, Operation> p = getActorsInfoByVarietyAndIndex( variety, rowIndex );
			if ( ( columnIndex == 0 ) || ( p.getSecond() == Operation.DELETE )
					|| ( ( p.getSecond() != Operation.CREATE ) 
							&& p.getFirst().getJournal().getDeleteMoment().before( new java.util.Date() ) )
					|| ( ( p.getFirst().getId() != 0 ) 
							&& !AuxFunctions.isPermission( Block.ACTORS_INFO, Operation.UPDATE ) ) ) return false;
			return true;
		}

		@Override
		public void setValueAt( Object aValue, int rowIndex, int columnIndex ) {
			String str = ( ( String ) aValue ).trim();
			Pair<ActorsInfo, Operation> p = getActorsInfoByVarietyAndIndex( variety, rowIndex );
			if ( ( str == null ) || str.isEmpty() || p.getFirst().getName().equals( str ) ) return;			
			if ( p.getSecond() == Operation.READ ) p.setSecond( Operation.UPDATE );
			p.getFirst().setName( str );			
		}
		
		@Override
		public void addTableModelListener( TableModelListener l ) { /* not used */ }

		@Override
		public void removeTableModelListener( TableModelListener l ) { /* not used */ }
	}
}