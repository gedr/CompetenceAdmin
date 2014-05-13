package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.data.services.ORMHelper;
import minos.entities.Actors;
import minos.entities.Measure;
import minos.resource.managers.IconResource;
import minos.resource.managers.IconResource.IconType;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;
import minos.ui.adapters.ActionAdapter;
import minos.ui.models.ActorsTableModel;
import minos.ui.models.MainTreeNode;
import minos.ui.models.MeasureDataProvider;
import minos.utils.ProgramConfig;

import com.alee.extended.button.WebSplitButton;
import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.WebAsyncTree;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.splitpane.WebSplitPane;
import com.alee.laf.table.WebTable;
import com.alee.laf.text.WebTextField;
import com.alee.laf.toolbar.WebToolBar;

public class MeasurePanel extends WebPanel implements ActionListener {
	private static final long serialVersionUID = 1L;

	private static final String jpqlLoadActors = "select entity from Actors entity "
			+ " join fetch entity.measure "
			+ " join fetch entity.minos "
			+ " join fetch entity.internalSinner "
			+ " join fetch entity.journal "
			+ " join fetch entity.testMode "
			+ " join fetch entity.reserveLevel "
			+ " join fetch entity.reserveType "
			+ " join fetch entity.profile "
			+ " join fetch entity.profile.profilePattern "
			+ " where entity.measure.id = :mid %s";			
	private static final String jpglConditionUndeletedActros = " and entity.journal.deleteMoment > CURRENT_TIMESTAMP ";

	private static final String USE_SIMPLE_FILTER	= "S";
	private static final String USE_REGEX_FILTER	= "R";
	
	private static final String FILTER_CMD			= "F";
	private static final String ADD_ACTORS_CMD 		= "1";
	private static final String EDIT_ACTORS_CMD 	= "2";
	private static final String COPY_ACTORS_CMD 	= "3";
	private static final String REMOVE_ACTORS_CMD 	= "4";
	private static final String REFRESH_ACTORS_CMD 	= "5";

	private static Logger log = LoggerFactory.getLogger( MeasurePanel.class );
	
	private WebAsyncTree<AsyncUniqueNode> tree = null;
	private WebTable table = null;
	private ActorsTableModel tm = null;
	private WebTextField filterTextField;
	private WebSplitButton filterButton;
	private FilterType filterType = FilterType.SIMPLE;
	private TableRowSorter<ActorsTableModel> sorter;	
	private List<Integer> selectedMeasures = new ArrayList<>();
	private Map<Integer, List<Actors>> loadedActors = new TreeMap<>();	
	private boolean visibleDeletedActors = false;
	private ProgramConfig pconf;
	private RowFilter<ActorsTableModel, Integer> rowFilterByText = new RowFilter<ActorsTableModel, Integer>() {
		
		@Override
		public boolean include( RowFilter.Entry<? extends ActorsTableModel, ? extends Integer> entry ) {
			if ( ( filterTextField == null ) || ( filterTextField.getText().length() == 0 ) ) return true;
			for ( int i = 0; i < entry.getValueCount(); i++ ) {
				if ( entry.getStringValue( i ).contains( filterTextField.getText() ) ) return true;
			}
			return false;
		}		
	};

	public enum FilterType { SIMPLE, REGEX };
	
	public MeasurePanel( Window owner ) { 
		pconf = ( ProgramConfig ) Resources.getInstance().get( ResourcesConst.PROGRAM_CONFIG );
		init();		
	}

	
	private void init() {
		WebPanel ap = makeActorsSubPanel();
		WebPanel mp = makeMeasureSubPanel();
		if ( ( ap == null ) || ( mp == null ) ) return;
			
		WebSplitPane splitPane = new WebSplitPane( WebSplitPane.HORIZONTAL_SPLIT, mp, ap );
		splitPane.setDividerLocation( ( ( ProgramConfig ) Resources.getInstance().get( ResourcesConst.PROGRAM_CONFIG ) ).getMeasureDivider() );
		splitPane.setOneTouchExpandable( true );		 
		splitPane.setContinuousLayout( true );
		splitPane.addPropertyChangeListener( new PropertyChangeListener() {
			@Override
			public void propertyChange( PropertyChangeEvent e ) {
				if ( e.getPropertyName().equals( WebSplitPane.DIVIDER_LOCATION_PROPERTY ) && ( pconf != null ) 
						&& ( e.getNewValue() instanceof Integer ) ) {
					pconf.setMeasureDivider( ( Integer ) e.getNewValue() );
					System.out.println( e.getNewValue() );
				}
			}
		});
		add( splitPane );
	}

	/**
	 * make Panel and add Components for Measure's Entity work
	 * @return new JPanel object or null
	 */
	private WebPanel makeMeasureSubPanel() {
		try {			
			MeasureDataProvider mdp = new MeasureDataProvider();
			tree = new WebAsyncTree<>( mdp );
			tree.addTreeSelectionListener( new TreeSelectionListener() {			
				
				@Override
				public void valueChanged(TreeSelectionEvent e) {				
					for (TreePath p : e.getPaths() ) {
						if ( p.getLastPathComponent() instanceof MainTreeNode ) {
							MainTreeNode mtn = ( MainTreeNode ) p.getLastPathComponent();
							if ( mtn.getUserObject() instanceof Measure ) {
								Measure m = (Measure) mtn.getUserObject();
								if ( e.isAddedPath( p ) ) {
									selectedMeasures.add( m.getId() );
								} else {
									selectedMeasures.remove( (Object) m.getId() );
								}
							}
						}						
					}
					new Thread ( new Runnable() {
						
						@Override
						public void run() {
							loadActros();						
						}
					}).start();			
				}
			});
			WebPanel panel = new WebPanel( new BorderLayout() );
			panel.add( new WebScrollPane( tree ) );
			return panel;
		} catch ( Exception e ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasurePanel.makeMeasureSubPanel() : ", e );
		}
	
		return null;
	}
	
	/**
	 * make Panel and add Components for Actors' Entity work
	 * @return new JPanel object or null
	 */
	private WebPanel makeActorsSubPanel() {
		try {
			tm = new ActorsTableModel();
			table = new WebTable( tm );			
			table.setAutoResizeMode( WebTable.AUTO_RESIZE_OFF );
			tm.setColumnModel( table.getColumnModel() );
			table.setRowHeight( 22 );
			
			
			if ( ( pconf.getActorsColumnSize() != null ) && 
					( pconf.getActorsColumnSize().length > 0 ) ) {
				for ( int i = 0; i < table.getColumnModel().getColumnCount(); i++ ) {					
						table.getColumnModel().getColumn( i ).
						setPreferredWidth( i >= pconf.getActorsColumnSize().length ? 100 :  pconf.getActorsColumnSize()[ i ]);
				}					
			}
			
			sorter = new TableRowSorter<ActorsTableModel>( tm );
			table.setRowSorter( sorter );
			table.getColumnModel().addColumnModelListener( new TableColumnModelListener() {
				
				@Override
				public void columnSelectionChanged( ListSelectionEvent e ) { }
				
				@Override
				public void columnRemoved( TableColumnModelEvent e ) { }

				@Override
				public void columnAdded( TableColumnModelEvent e ) { }

				@Override
				public void columnMoved( TableColumnModelEvent e ) { }

				@Override
				public void columnMarginChanged( ChangeEvent e ) { 
					if ( pconf == null ) return;
					TableColumnModel tcm = (TableColumnModel) e.getSource(); 
					int[] arr = pconf.getActorsColumnSize();
					if ( ( arr == null ) || ( arr.length != tcm.getColumnCount() ) ) {
						arr = new int[ tcm.getColumnCount() ];
						pconf.setActorsColumnSize( arr );
					}
					for ( int i = 0; i < tcm.getColumnCount(); i++ ) arr[i] = tcm.getColumn( i ).getWidth();
				}
			} );


			
			
			
			
			
	        filterButton = new WebSplitButton( IconResource.getInstance().getIcon( IconType.FILTER, 32 ), this );
	        filterButton.setActionCommand( FILTER_CMD );

	        final WebPopupMenu popupMenu = new WebPopupMenu ();
	        popupMenu.add( new ActionAdapter( "Простой фильтр", null, USE_SIMPLE_FILTER, "Простой фильтр", this, 0 ) );
	        popupMenu.add( new ActionAdapter( "Регулярные выражения", null, USE_REGEX_FILTER, "Регулярные выражения", this, 0 ) );
	        filterButton.setPopupMenu ( popupMenu );

		    filterTextField = new WebTextField( 25 );
		    filterTextField.setInputPrompt( "Введите строку фильтра..." );
		    filterTextField.setInputPromptFont ( filterTextField.getFont ().deriveFont ( Font.ITALIC ) );
		    filterTextField.setMaximumSize( new Dimension( Integer.MAX_VALUE, 32 ) );
	        filterTextField.setTrailingComponent( filterButton );
	        
	        WebToolBar tb = new WebToolBar();    
			tb.add( new ActionAdapter("add", IconResource.getInstance().getIcon( IconType.ADD, 32 ), ADD_ACTORS_CMD, 
					"добавить", this, 0) );
			tb.add( new ActionAdapter("edit", IconResource.getInstance().getIcon( IconType.EDIT, 32 ), EDIT_ACTORS_CMD, 
					"редактировать", this, 0) );
			tb.add( new ActionAdapter("copy", IconResource.getInstance().getIcon( IconType.COMPETENCE_ADD, 32 ), COPY_ACTORS_CMD, 
					"копировать", this, 0) );
			tb.add( new ActionAdapter("delete", IconResource.getInstance().getIcon( IconType.DELETE, 32 ), REMOVE_ACTORS_CMD, 
					"удаление", this, 0) );
			tb.add( new ActionAdapter("refresh", IconResource.getInstance().getIcon( IconType.RELOAD, 32 ), REFRESH_ACTORS_CMD, 
					"обновить", this, 0) );

			tb.addSeparator();
	        tb.add( filterTextField, "FILL" );
	        
			WebPanel panel = new WebPanel( new BorderLayout() );
			panel.add( new WebScrollPane ( table ), BorderLayout.CENTER );
	        panel.add( tb, BorderLayout.NORTH );
	        return panel;
		} catch (Exception e) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasurePanel.makeActorsSubPanel() : ", e );
		}		
		return null;
	}
	
	/**
	 * load Actors entities for selected Measures
	 */
	private void loadActros() {
		if ( selectedMeasures.size() == 0 ) return;

		String jpql = String.format( jpqlLoadActors, ( visibleDeletedActors ? " " :  jpglConditionUndeletedActros ) );
		for ( Integer i : selectedMeasures ) {
			if ( !loadedActors.containsKey( i ) ) {				
				ORMHelper.openManager();
				List<Actors> res = ORMHelper.getCurrentManager().createQuery( jpql, Actors.class ).
						setParameter( "mid", i ).getResultList();
				ORMHelper.closeManager();
				loadedActors.put( i, res );
			}
		}
		
		final List<Actors> l = ( selectedMeasures.size() == 1 ? loadedActors.get( selectedMeasures.get( 0 ) ) : new ArrayList<Actors>() );
		if ( selectedMeasures.size() > 1 ) {
			for ( Integer i : selectedMeasures ) l.addAll( loadedActors.get( i ) );
		} 			
		
		SwingUtilities.invokeLater( new Runnable() {
			
			@Override
			public void run() {
				sorter.setRowFilter( null );
				tm.setActorsList( l );				
				table.getRowSorter().allRowsChanged();
				table.revalidate();
			}
		});
	}



	@Override
	public void actionPerformed(ActionEvent e) {
		switch ( e.getActionCommand() ) {
		case FILTER_CMD:
			if ( filterType == FilterType.SIMPLE ) sorter.setRowFilter( rowFilterByText );
			if ( filterType == FilterType.REGEX ) sorter.setRowFilter( RowFilter.regexFilter( filterTextField.getText() ) );			
			break;
			
		case USE_REGEX_FILTER:
			filterType = FilterType.REGEX;
			break;
			
		case USE_SIMPLE_FILTER:
			filterType = FilterType.SIMPLE;
			break;
		}
	}
}