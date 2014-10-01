package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Tuple;
import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Quartet;
import ru.gedr.util.tuple.Triplet;
import ru.gedr.util.tuple.Tuple.TupleType;
import minos.data.importer.FileImporter;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.CatalogJpaController;
import minos.data.orm.controllers.CompetenceJpaController;
import minos.data.orm.controllers.IndicatorJpaController;
import minos.data.orm.controllers.PpeStrAtrJpaController;
import minos.data.orm.controllers.ProfilePatternElementJpaController;
import minos.data.orm.controllers.ProfilePatternJpaController;
import minos.data.orm.controllers.StringAttrJpaController;
import minos.entities.Catalog;
import minos.entities.Competence;
import minos.entities.Indicator;
import minos.entities.PpeStrAtr;
import minos.entities.ProfilePattern;
import minos.entities.ProfilePatternElement;
import minos.entities.StringAttr;
import minos.ui.MinosCellRenderer;
import minos.ui.adapters.ActionAdapter;
import minos.ui.adapters.MinosTransferable;
import minos.ui.dialogs.CatalogDlg;
import minos.ui.dialogs.CompetenceDlg;
import minos.ui.dialogs.ComponentDlg;
import minos.ui.dialogs.ErrorDlg;
import minos.ui.dialogs.IndicatorDlg;
import minos.ui.dialogs.TimePointDlg;
import minos.ui.models.MainTreeNode;
import minos.ui.models.dataproviders.CatalogDataProvider;
import minos.ui.models.dataproviders.CompetenceDataProvider;
import minos.ui.models.dataproviders.IndicatorDataProvider;
import minos.utils.AuxFunctions;
import minos.utils.Permission.Block;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import com.alee.extended.tree.AsyncTreeDataProvider;
import com.alee.extended.tree.WebAsyncTree;
import com.alee.laf.button.WebToggleButton;
import com.alee.laf.filechooser.WebFileChooser;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.splitpane.WebSplitPane;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.toolbar.WebToolBar;
import com.alee.laf.tree.WebTree;

public class CompetencePanel extends WebPanel implements ActionListener, OrmCommand {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	
	private static final int TOOLBAR_ICON_SIZE = 24;

	private static final String CMD_CATALOG_ADD 	= "1";
	private static final String CMD_COMPETENCE_ADD 	= "2";
	private static final String CMD_INDICATOR_ADD 	= "3";
	private static final String CMD_RELOAD 			= "4";
	private static final String CMD_EDIT 			= "5";
	private static final String CMD_CLOCK 			= "6";
	private static final String CMD_DELETE 			= "7";
	private static final String CMD_COMPETENCE_LOAD	= "8";

	private static final Logger log = LoggerFactory.getLogger( CompetencePanel.class );
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private WebAsyncTree<MainTreeNode> tree = null;
	private WebToggleButton clockBtn = null;
	private Window owner = null;
	private WebToolBar tb = null;

	private boolean visibleToolBar = true;

	private CatalogDataProvider ctdp = null;
	private CompetenceDataProvider cmdp = null;
	private IndicatorDataProvider idp = null;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public CompetencePanel( Window owner, boolean visibleToolBar ) {
		this.owner = owner;
		this.visibleToolBar  = visibleToolBar;

		setLayout( new BorderLayout() );
		Component comp = makeTree();
		add( ( comp == null ? new WebTextArea( "Нет прав на чтение структуры" ) : new WebScrollPane( comp ) ), 
				BorderLayout.CENTER );
		if ( ( comp != null ) && visibleToolBar ) add( makeToolBar(), BorderLayout.NORTH );
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean isVisibleToolBar() {
		return visibleToolBar;
	}

	public void setVisibleToolBar( boolean visibleToolBar ) {
		this.visibleToolBar = visibleToolBar;
		if ( tb != null ) {
			tb.setVisible( visibleToolBar );
			return;
		}
		if ( visibleToolBar ) {
			add( makeToolBar(), BorderLayout.NORTH );
			tb.setVisible( visibleToolBar );
		}
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			switch ( e.getActionCommand() ) {
			case CMD_CATALOG_ADD :	
				if ( addCatalog() ) tree.reloadNode( tree.getSelectedNode() );
				break;

			case CMD_COMPETENCE_ADD :
				if ( addCompetence() ) tree.reloadNode( tree.getSelectedNode() );
				break;

			case CMD_COMPETENCE_LOAD :
				if ( loadCompetence() ) tree.reloadNode( tree.getSelectedNode() );
				break;

			case CMD_INDICATOR_ADD :
				if ( addIndicator() ) tree.reloadNode( tree.getSelectedNode() );			
				break;

			case CMD_RELOAD :
				tree.reloadRootNode();
				break;

			case CMD_EDIT :
				if ( dispatchEdit() ) AuxFunctions.repaintComponent( tree );					
				break;

			case CMD_CLOCK :
				viewHistory();
				break;

			case CMD_DELETE :
				if ( dispatchDelete() ) AuxFunctions.repaintComponent( tree );
				break;

			default:
				break;
			}		
		} catch ( AccessControlException aex ) { 
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "CompetencePanel.actionPerformed() : ", aex );
			ErrorDlg.show( owner, "Ошибка", "Нет прав на выполнение операции", aex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "CompetencePanel.actionPerformed() : ", ex );
			ErrorDlg.show( owner, "Ошибка", "Произошла ошибка при выполнении перации", ex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		}		
	}

	/**
	 * delete Catalog entities, Competence entities, Indicator entities in one transaction
	 */
	@Override
	public void execute( Object obj ) throws Exception {
		if ( ( ( Tuple ) obj ).getType() == TupleType.QUARTET ) {
			@SuppressWarnings("unchecked")
			Quartet<List<Catalog>, List<Competence>, List<Indicator>, Boolean> q = 
			( Quartet<List<Catalog>, List<Competence>, List<Indicator>, Boolean> ) obj;
			boolean b1 = deleteEntities( q.getFirst() );
			boolean b2 = deleteEntities( q.getSecond() );
			boolean b3 = deleteEntities( q.getThird() );
			q.setFourth( b1 | b2 | b3 );
		}
		if ( ( ( Tuple ) obj ).getType() == TupleType.PAIR ) {
			@SuppressWarnings("unchecked")
			Pair<MainTreeNode, MainTreeNode> p = ( Pair<MainTreeNode, MainTreeNode> ) obj;
			saveLoadedElements( p.getFirst() );
			saveLoadedElements( p.getSecond() );
		}
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * make tool bar component and initialize button command 
	 * @return ToolBar object 
	 */
	private Component makeToolBar() {
		clockBtn = new WebToggleButton( null, ResourceKeeper.getIcon( ResourceKeeper.IType.CLOCK, TOOLBAR_ICON_SIZE ) );
		clockBtn.setShadeToggleIcon ( true );
		clockBtn.setActionCommand( CMD_CLOCK );
		clockBtn.addActionListener( this );
		tb = new WebToolBar();		
		tb.add( new ActionAdapter( "add catalog", 
				ResourceKeeper.getIcon( ResourceKeeper.IType.CATALOG_ADD, TOOLBAR_ICON_SIZE), 
				CMD_CATALOG_ADD, "Добавление нового подкаталога в каталог", this, 0) ); 
		tb.add( new ActionAdapter( "add competence", 
				ResourceKeeper.getIcon( ResourceKeeper.IType.COMPETENCE_ADD, TOOLBAR_ICON_SIZE ), 
				CMD_COMPETENCE_ADD, "Добавление новой компетенции в каталог", this, 0) ); 
		tb.add( new ActionAdapter( "load competence", 
				ResourceKeeper.getIcon( ResourceKeeper.IType.COMPETENCE_DOWNLOAD, TOOLBAR_ICON_SIZE ), 
				CMD_COMPETENCE_LOAD, "Загрузка компетенций из файла в указанй каталог", this, 0) ); 
		tb.add( new ActionAdapter( "add indicator", 
				ResourceKeeper.getIcon( ResourceKeeper.IType.INDICATOR_ADD, TOOLBAR_ICON_SIZE ), 
				CMD_INDICATOR_ADD, "Добавление нового индикатора в компетенцию", this, 0) ); 
		tb.add( new ActionAdapter( "edit", 
				ResourceKeeper.getIcon( ResourceKeeper.IType.EDIT, TOOLBAR_ICON_SIZE ), CMD_EDIT, 
				"Редактирование элемента", this, 0) ); 
		tb.add( new ActionAdapter( "delete", 
				ResourceKeeper.getIcon( ResourceKeeper.IType.DELETE, TOOLBAR_ICON_SIZE ), CMD_DELETE, 
				"Удаление элемента", this, 0) );
		tb.add( new ActionAdapter( "refresh", 
				ResourceKeeper.getIcon( ResourceKeeper.IType.REFRESH, TOOLBAR_ICON_SIZE ), CMD_RELOAD, 
				"Обновить данные", this, 0) );
		tb.add( clockBtn );
		return tb;
	}

	/** 
	 * initialized Tree component
	 * @return Tree object or null if permission denied
	 */
	private Component makeTree() {
		List<Short> ctv = AuxFunctions.getAllowedReadVariety( false );
		if ( ctv.size() == 0 ) return null;		
		ctdp = new CatalogDataProvider( ctv );
		AsyncTreeDataProvider<MainTreeNode> dp = ctdp;

		List<Short> cmv = AuxFunctions.getAllowedReadVariety( true );
		if ( cmv.size() != 0 ) {
			cmdp = new CompetenceDataProvider( ctdp, cmv );
			idp = new IndicatorDataProvider( cmdp ); 
			idp.setVisibleLevels( true );
			dp = idp;
		}		
		tree = new WebAsyncTree<>( dp );
		tree.setRootVisible( false );
		tree.setCellRenderer( new MinosCellRenderer<>( 24 ) );
		if ( visibleToolBar ) {
			tree.setDragEnabled( true );
			tree.setTransferHandler( new CompetencePanelTransferHandler() );
		}		
		return tree;
	}

	/**
	 * get Block object for variety
	 * @param variety - Entity's variety
	 * @param isCatalogVariety - flag define entity (catalog or competence)
	 * @return permission's block object
	 */
	private Block getBlock( Object obj ) {
		if ( obj == null ) throw new NullArgumentException( "CompetencePanel.getBlock() : obj is null" );
		boolean check = false;
		if ( obj instanceof Catalog ) {
			check = true;
			switch ( ( ( Catalog ) obj ).getVariety() ) {
			case Catalog.ADMINISTRATIVE_COMPETENCE : return  Block.ADMINISTRATIVE_CATALOG;
			case Catalog.PERSONALITY_BUSINESS_COMPETENCE : return  Block.PERSONALITY_CATALOG;
			case Catalog.PROFESSIONAL_COMPETENCE : return  Block.PROFESSIONAL_CATALOG;
			}
		}
		if ( obj instanceof Competence ) {
			check = true;
			switch ( ( ( Competence ) obj ).getVariety() ) {
			case Competence.ADMINISTRATIVE_COMPETENCE : return  Block.ADMINISTRATIVE_COMPETENCE;
			case Competence.PERSONALITY_BUSINESS_COMPETENCE : return  Block.PERSONALITY_BUSINESS_COMPETENCE;
			case Competence.PROFESSIONAL_COMPETENCE : return  Block.PROFESSIONAL_COMPETENCE;
			}
		}
		throw new IllegalArgumentException( "CompetencePanel.getBlock() : " + 
				( check ? "selected tree's node have illegal state of variety" : "obj have illegal type" ) );
	}

	/**
	 * show dialog for select time point in past
	 */
	private void viewHistory() {
		Timestamp tp = null;
		if ( clockBtn.isSelected() ) {
			tp = TimePointDlg.showTimePointDlgDlg( owner, "Время в прошлом" );
			if (tp == null ) {
				clockBtn.setSelected( false );
				return;
			}
		}
		if ( ( tp == null ) && ( ctdp.getCurrentTimePoint().after( new Date() ) ) ) return;
		ctdp.setCurrentTimePoint( tp );
		cmdp.setCurrentTimePoint( tp );
		idp.setCurrentTimePoint( tp );		
		tree.reloadRootNode();
	}

	/**
	 * check option history view
	 * @return true if  history view option is on
	 */
	private boolean checkHistoryView() {		
		if ( ctdp.getCurrentTimePoint().before( new Date() ) ) {
			WebOptionPane.showMessageDialog(owner, "При просмотре истории нельзя вносить изменения", "Предупреждение", 
					WebOptionPane.INFORMATION_MESSAGE );
			return true;
		}
		return false;		
	}

	/**
	 * add new catalog 
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean addCatalog() throws Exception {
		if ( checkHistoryView() || !AuxFunctions.checkRightSelect( owner, tree, false, false, 
				"Необходимо выделить каталог", Catalog.class ) ) return false;
		if ( !AuxFunctions.isPermission( getBlock( tree.getSelectedNode().getUserObject() ), Operation.CREATE ) ) {
			throw new AccessControlException( "CompetencePanel.addCatalog() : create Catalog permission denied"  );
		}
		Catalog freshCatalog = ( Catalog ) OrmHelper.findEntity( Catalog.class, 
				( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId(), 
				"ancestor", "variery" );
		if ( freshCatalog == null ) {
			throw new EntityNotFoundException( "CompetencePanel.addCatalog() : Catalog entity not found id=" 
					+ ( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId() );
		}
		if ( freshCatalog.getAncestor() != null ) {
			throw new IllegalComponentStateException( "CompetencePanel.addCatalog() : entity have not null ancestor" );
		}
		Catalog newCatalog = CatalogDlg.show( owner, "Новый каталог", null, false );
		if ( newCatalog == null ) return false;
		newCatalog.setParentCatalog( freshCatalog );
		newCatalog.setItem( CatalogJpaController.getInstance().getMaxItem( freshCatalog ) );
		CatalogJpaController.getInstance().create( newCatalog, true, false, true );
		return true;
	}	

	/**
	 * add new Competence
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean addCompetence() throws Exception {
		if ( checkHistoryView() || !AuxFunctions.checkRightSelect( owner, tree, false, false, 
				"Необходимо выделить каталог", Catalog.class ) ) return false;
		Competence cmtn = new Competence();
		cmtn.setVariety( ( ( Catalog ) tree.getSelectedNode().getUserObject() ).getVariety() );
		if ( !AuxFunctions.isPermission( getBlock( cmtn ), Operation.CREATE ) ) {
			throw new AccessControlException( "CompetencePanel.addCompetence() : create competence permission denied" );
		}
		Catalog freshCatalog = ( Catalog ) OrmHelper.findEntity( Catalog.class, 
				( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId(), 
				"ancestor", "variety" );
		if ( freshCatalog == null ) {
			throw new EntityNotFoundException( "CompetencePanel.addCompetence() : Competence entity not found id=" 
					+ ( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId() );
		}
		if ( freshCatalog.getAncestor() != null ) {
			throw new IllegalComponentStateException( "CompetencePanel.addCompetence() : entity have not null ancestor" );
		}
		Competence newCompetence = CompetenceDlg.show( owner, "Новая компетенция", null, false );
		if ( newCompetence == null ) return false;
		newCompetence.setCatalog( freshCatalog );
		newCompetence.setVariety( freshCatalog.getVariety() );
		newCompetence.setItem( CompetenceJpaController.getInstance().getMaxItem( freshCatalog ) );
		CompetenceJpaController.getInstance().create( newCompetence, true, false, true );
		return true;
	}

	/**
	 * add new indicator
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean addIndicator() throws Exception {
		if ( checkHistoryView() || !AuxFunctions.checkRightSelect( owner, tree, false, false, 
				"Необходимо выделить кометенцию", Competence.class ) ) return false;
		if ( !AuxFunctions.isPermission( getBlock( tree.getSelectedNode().getUserObject() ), Operation.CREATE ) ) {
			throw new AccessControlException( "CompetencePanel.addIndicator() : create indicator permission denied" );
		}
		int id = (  ( Competence ) tree.getSelectedNode().getUserObject() ).getId();
		Competence freshCompetence = ( Competence ) OrmHelper.findEntity( Competence.class, id, "ancestor", "variety" );
		if ( freshCompetence == null ) {
			throw new EntityNotFoundException( "CompetencePanel.addCompetence() : Competence entity not found id=" + id );
		}
		if ( freshCompetence.getAncestor() != null ) {
			throw new IllegalComponentStateException( "CompetencePanel.addCompetence() : entity have not null ancestor" );
		}
		Indicator newIndicator = IndicatorDlg.show( owner, "Новый индикатор", null, false );
		if ( newIndicator == null ) return false;
		newIndicator.setCompetence( freshCompetence );
		newIndicator.setItem( IndicatorJpaController.getInstance().getMaxItem( freshCompetence, newIndicator.getLevel() ) );
		IndicatorJpaController.getInstance().create( newIndicator, true, false, true );
		return true;
	}

	/** 
	 * dispatch edit object
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean dispatchEdit() throws Exception {
		if ( checkHistoryView() || !AuxFunctions.checkRightSelect( owner, tree, false, false, 
				"Необходимо выделить каталог и/или компетенцию и/или индикатор",
				Catalog.class, Competence.class, Indicator.class ) ) return false;		
		if ( tree.getSelectedNode().getUserObject() instanceof Catalog ) return  editCatalog();
		if ( tree.getSelectedNode().getUserObject() instanceof Competence ) return  editCompetence();
		if ( tree.getSelectedNode().getUserObject() instanceof Indicator ) return  editIndicator();
		return false;
	}

	/**
	 * edit catalog entity
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean editCatalog() throws Exception {
		int id = ( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId();
		Catalog old = ( Catalog ) OrmHelper.findEntity( Catalog.class, id, "parentCatalog", "journal", "ancestor" );		
		if ( old == null ) {
			throw new EntityNotFoundException( "CompetencePanel.editCatalog() : Competence entity not found id=" + id );
		}
		if ( old.getAncestor() != null ) {
			throw new IllegalComponentStateException( "CompetencePanel.editCatalog() : entity have not null ancestor" );
		}
		boolean readOnly = false;		
		if ( !AuxFunctions.isPermission( getBlock( old ), Operation.UPDATE ) || ( old.getId() < 5 ) ) readOnly = true;
		Catalog cat = CatalogDlg.show( owner, ( ( readOnly ? "Просмотр" : "Редактирование" ) + " каталога" ), old, readOnly );
		if ( cat == null ) return false;
		Pair<Catalog, Catalog> p = CatalogJpaController.getInstance().newVersion( old, cat, true, false, true );
		tree.getSelectedNode().setUserObject( p.getFirst() );
		return true;
	}

	/**
	 * edit competence entity
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean editCompetence() throws Exception {
		int id = ( ( Competence ) tree.getSelectedNode().getUserObject() ).getId();
		Competence old = ( Competence ) OrmHelper.findEntity( Competence.class, id, 
				"catalog", "journal", "ancestor", "description" );		
		if ( old == null ) {
			throw new EntityNotFoundException( "CompetencePanel.editCompetence() : Competence entity not found id=" + id );
		}
		if ( old.getAncestor() != null ) {
			throw new IllegalComponentStateException( "CompetencePanel.editCompetence() : entity have not null ancestor" );
		}
		boolean readOnly = false;		
		if ( !AuxFunctions.isPermission( getBlock( old ), Operation.UPDATE ) ) readOnly = true;
		Competence cmpt = CompetenceDlg.show( owner, ( ( readOnly ? "Просмотр" : "Редактирование" ) + " компетенции" ), 
				old, readOnly );
		if ( cmpt == null ) return false;
		Pair<Competence, Competence> p = CompetenceJpaController.getInstance().newVersion( old, cmpt, true, false, true );
		tree.getSelectedNode().setUserObject( p.getFirst() );
		return true;
	}

	/**
	 * edit indicator entity
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean editIndicator() throws Exception {
		long id = ( ( Indicator ) tree.getSelectedNode().getUserObject() ).getId();
		Indicator old = ( Indicator ) OrmHelper.findEntity( Indicator.class, id, "competence", "journal", "ancestor" );		
		if ( old == null ) {
			throw new EntityNotFoundException( "CompetencePanel.editIndicator() : Competence entity not found id=" + id );
		}
		if ( old.getAncestor() != null ) {
			throw new IllegalComponentStateException( "CompetencePanel.editIndicator() : entity have not null ancestor" );
		}
		boolean readOnly = false;		
		if ( !AuxFunctions.isPermission( getBlock( old.getCompetence() ), Operation.UPDATE ) ) readOnly = true;
		Indicator ind= IndicatorDlg.show( owner, ( ( readOnly ? "Просмотр" : "Редактирование" ) + " индикатора" ), 
				old, readOnly );
		if ( ind == null ) return false;
		Pair<Indicator, Indicator> p = IndicatorJpaController.getInstance().newVersion( old, ind, true, false, true );
		tree.getSelectedNode().setUserObject( p.getFirst() );
		return true;
	}

	/**
	 * dispatch delete object
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean dispatchDelete() throws Exception {
		if ( checkHistoryView() || !AuxFunctions.checkRightSelect( owner, tree, false, true, 
				"Необходимо выделить каталоги и/или компетенции и/или индикаторы",
				Catalog.class, Competence.class, Indicator.class ) ) return false;		
		Triplet<List<Catalog>, List<Competence>, List<Indicator>> t = getSelectedElements( true, true, true );
		t.setFirst( preDeleteCatalog( t.getFirst() ) );
		t.setSecond( preDeleteCompetence( t.getSecond() ) );
		t.setThird( preDeleteIndicator( t.getThird() ) );
		if ( ( t.getFirst().size() + t.getSecond().size() + t.getThird().size() ) == 0 ) return false;
		Quartet<List<Catalog>, List<Competence>, List<Indicator>, Boolean> q = 
				new Quartet<>( t.getFirst(), t.getSecond(), t.getThird(), false );
				OrmHelper.executeAsTransaction( this, q );
				return q.getFourth();
	}

	/**
	 * reload list of catalog entities and check DELETE permission and STATUS
	 * @param lst is list of catalog entities for delete
	 * @return fresh list of catalog entities 
	 * @throws Exception
	 */
	private List<Catalog> preDeleteCatalog( List<Catalog> lst ) throws Exception {
		if ( ( lst == null ) || ( lst.size() == 0 ) ) return Collections.emptyList();
		for ( Catalog c : lst ) {
			if ( !AuxFunctions.isPermission( getBlock( c ), Operation.DELETE ) || ( c.getId() < 5 ) ) {
				throw new AccessControlException( "CompetencePanel.deleteCatalog() : delete Catalog permission denied" );
			}			
		}
		List<Integer> cids = new ArrayList<>();
		for ( Catalog c : lst ) cids.add( c.getId() );

		List<Catalog> lc = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_REFRESH_CATALOGS_BEFORE_DELETE ), 
				Catalog.class, 
				new Pair<Object, Object>( "catalog_ids", cids ),
				new Pair<Object, Object>( "status", Arrays.asList( Catalog.STATUS_ACTIVE ) ) );
		return ( ( lc != null ) && ( lc.size() > 0 ) ) ? lc : Collections.<Catalog>emptyList();
	}

	/**
	 * reload list of Competence entities and check DELETE permission and STATUS
	 * @param lst is list of Competence entities for delete
	 * @return fresh list of Competence entities 
	 * @throws Exception
	 */
	private List<Competence> preDeleteCompetence( List<Competence> lst ) throws Exception {
		if ( ( lst == null ) || ( lst.size() == 0 ) ) return Collections.emptyList();
		for ( Competence c : lst ) {
			if ( !AuxFunctions.isPermission( getBlock( c ), Operation.DELETE ) ) {
				throw new AccessControlException( "CompetencePanel.preDeleteCompetence() : delete Competence permission denied" ); 
			}
		}
		List<Integer> cids = new ArrayList<>();
		for ( Competence c : lst ) cids.add( c.getId() );

		List<Competence> lc = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_REFRESH_COMPETENCE_BEFORE_DELETE ), 
				Competence.class, 
				new Pair<Object, Object>( "competence_ids", cids ),
				new Pair<Object, Object>( "status", Arrays.asList( Competence.STATUS_ACTIVE ) ) );
		return ( ( lc != null ) && ( lc.size() > 0 ) ) ? lc : Collections.<Competence>emptyList();
	}

	/**
	 * reload list of Indicator entities and check DELETE permission and STATUS
	 * @param lst is list of Indicator entities for delete
	 * @return fresh list of Indicator entities 
	 * @throws Exception
	 */
	private List<Indicator> preDeleteIndicator( List<Indicator> lst ) throws Exception {
		if ( ( lst == null ) || ( lst.size() == 0 ) ) return Collections.emptyList();
		for ( Indicator i : lst ) {
			if ( !AuxFunctions.isPermission( getBlock( i.getCompetence() ), Operation.DELETE ) ) {
				throw new AccessControlException( "CompetencePanel.deleteCompetence() : delete Indicator permission denied" ); 
			}
		}
		List<Long> iids = new ArrayList<>();
		for ( Indicator i : lst ) iids.add( i.getId() );
		
		List<Indicator> li = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_REFRESH_INDICATOR_BEFORE_DELETE ), 
				Indicator.class, 
				new Pair<Object, Object>( "indicator_ids", iids ),
				new Pair<Object, Object>( "status", Arrays.asList( Indicator.STATUS_ACTIVE ) ) );
		return ( ( li != null ) && ( li.size() > 0 ) ) ? li : Collections.<Indicator>emptyList();
	}

	/**
	 * delete list of entities
	 * @param entities list of Catalog or Competence or Indicator entities 
	 * @return
	 * @throws Exception
	 */
	private <T> boolean deleteEntities( List<T> entities )  throws Exception {
		if ( ( entities == null ) || ( entities.size() == 0 ) ) return false;
		if ( entities.get( 0 ) instanceof Catalog ) {
			for ( T t : entities ) CatalogJpaController.getInstance().delete( ( Catalog ) t, true, false, false );	
		} else if ( entities.get( 0 ) instanceof Competence ) {
			for ( T t : entities ) CompetenceJpaController.getInstance().delete( ( Competence ) t, true, false, false );
		} else if ( entities.get( 0 ) instanceof Indicator ) {
			for ( T t : entities ) IndicatorJpaController.getInstance().delete( ( Indicator ) t, true, false, false );
		}
		return true;
	}

	/**
	 * make catalog's set, competence's set and indicator's set from selected tree nodes
	 * @return Triplet of catalog's set, competence's set and indicator's set 
	 */
	private Triplet<List<Catalog>, List<Competence>, List<Indicator>> getSelectedElements( boolean checkCatalogs, 
			boolean checkCompetences, boolean checkIndicators) {
		Triplet<List<Catalog>, List<Competence>, List<Indicator>> t = new Triplet<>( Collections.<Catalog>emptyList(), 
				Collections.<Competence>emptyList(), Collections.<Indicator>emptyList() );
		for ( MainTreeNode node : tree.getSelectedNodes() ) {
			if ( node == null ) continue;
			if ( checkCatalogs && ( node.getUserObject() instanceof Catalog ) 
					&& ( !t.getFirst().contains( node.getUserObject() ) ) ) {
				if ( t.getFirst().size() == 0 ) t.setFirst( new ArrayList<Catalog>() );
				t.getFirst().add( ( Catalog ) node.getUserObject() );
			}			
			if ( checkCompetences && ( node.getUserObject() instanceof Competence ) 				
					&& ( !t.getSecond().contains( node.getUserObject() ) ) ) {
				if ( t.getSecond().size() == 0 ) t.setSecond( new ArrayList<Competence>() );
				t.getSecond().add( ( Competence ) node.getUserObject() );
			}			
			if ( checkIndicators && ( node.getUserObject() instanceof Indicator ) 
					&& ( !t.getThird().contains( node.getUserObject() ) ) ) {
				if ( t.getThird().size() == 0 ) t.setThird( new ArrayList<Indicator>() );
				t.getThird().add( ( Indicator ) node.getUserObject() );
			}
		}		
		if ( checkCatalogs ) t.setFirst( getSubCatalogs( t.getFirst() ) );
		if ( checkCompetences ) {
			List<Competence> lc = getCompetenceFromCatalog( t.getFirst() );
			for ( Competence c : lc ) {
				if ( t.getSecond().size() == 0 ) t.setSecond( new ArrayList<Competence>() );
				if ( !t.getSecond().contains( c ) ) t.getSecond().add( c );
			}
		}
		if ( checkIndicators ) {
			List<Indicator> li = getIndicatorFromCompetence( t.getSecond() );
			for ( Indicator i : li ) {
				if ( t.getThird().size() == 0 ) t.setThird( new ArrayList<Indicator>() );
				if ( !t.getThird().contains( i ) ) t.getThird().add( i );
			}
		}
		return t;	
	}

	/**
	 * make sub catalog's set from catalog's set
	 * @param catalogs - existing set of Catalog's entity 
	 * @return catalogs's set or null
	 */
	private List<Catalog> getSubCatalogs( List<Catalog> catalogs ) {
		if ( ( catalogs == null ) || ( catalogs.size() == 0 ) ) return Collections.emptyList();
		List<Catalog> lst = new ArrayList<>( catalogs );
		for ( Catalog c : catalogs ) {
			List<Catalog> subCatalogs = OrmHelper.findByQueryWithParam( QueryType.SQL, 
					ResourceKeeper.getQuery( QType.SQL_LOAD_ALL_SUB_CATALOGS ), 
					Catalog.class, 
					new Pair<Object, Object>( 1, c.getId() ),
					new Pair<Object, Object>( 2, Catalog.STATUS_ACTIVE ) );
			if ( ( subCatalogs == null ) || ( subCatalogs.size() == 0 ) ) continue;
			for ( Catalog sc : subCatalogs ) if ( !lst.contains( sc ) ) lst.add( sc );
		}
		return lst;
	}

	/**
	 * make competence's set from catalog's set
	 * @param catalogs - existing set of Catalog's entity 
	 * @return competence's set or null
	 */
	private List<Competence> getCompetenceFromCatalog( List<Catalog> catalogs ) {
		if ( ( catalogs == null ) || ( catalogs.size() == 0 ) ) return Collections.emptyList();
		List<Competence> cmpts = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_COMPETENCES ), 
				Competence.class, 
				new Pair<Object, Object>( "catalogs", catalogs ),
				new Pair<Object, Object>( "ts", new Timestamp( System.currentTimeMillis() ) ),
				new Pair<Object, Object>( "dts", ResourceKeeper.getObject( OType.WAR ) ) );
		return ( cmpts != null ? cmpts : Collections.<Competence>emptyList() );
	}

	/**
	 * make indicator's set from competence's set
	 * @param competences - existing set of Competence's entity
	 * @return indicator's set  or null
	 */
	private List<Indicator> getIndicatorFromCompetence( List<Competence> competences ) {
		if ( ( competences == null ) || ( competences.size() == 0 ) ) return Collections.emptyList();
		List<Indicator> li = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_INDICATORS ), 
				Indicator.class, 
				new Pair<Object, Object>( "competences", competences ),
				new Pair<Object, Object>( "levels", ResourceKeeper.getObject( OType.LEVELS_CACHE ) ),					
				new Pair<Object, Object>( "ts", new Timestamp( System.currentTimeMillis() ) ),
				new Pair<Object, Object>( "dts", ResourceKeeper.getObject( OType.WAR ) ) );
		return ( li != null ? li : Collections.<Indicator>emptyList() );
	}

	/**
	 * make list of selected competences 
	 * @return list of selected competences
	 */
	public List<Competence> getSelectedCompetence() {
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, true,
				"Необходимо выделить каталоги и/или компетенции",
				Catalog.class, Competence.class ) ) return Collections.emptyList();
		Triplet<List<Catalog>, List<Competence>, List<Indicator>> t = getSelectedElements( true, true, false );
		return t.getSecond();
	}
	
	private boolean loadCompetence() throws Exception {
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, false, "Необходимо выделить каталог", 
				Catalog.class ) ) return false;
		Catalog curCatalog = ( Catalog ) tree.getSelectedNode().getUserObject();
		WebFileChooser fileChooser = new WebFileChooser();
		fileChooser.setMultiSelectionEnabled ( false );
		if ( fileChooser.showOpenDialog( owner ) != WebFileChooser.APPROVE_OPTION ) return false;

		File file = fileChooser.getSelectedFile();
		Path path = Paths.get( file.getCanonicalPath() );
		FileImporter fi = new FileImporter();
		Pair<Catalog, Catalog> p = fi.loadTXTFile( path, ( Catalog ) tree.getSelectedNode().getUserObject() );
		if ( ( p == null ) || ( p.getFirst() == null ) ) return false;

		WebTree<MainTreeNode> t1 = new WebTree<>( makeCatalogNode( p.getFirst() ) );
		WebTree<MainTreeNode> t2 = null;
		t1.setCellRenderer( new MinosCellRenderer<>( 24 ) );

		Component cmpnt = new WebScrollPane( t1 );
		if ( p.getSecond() != null ) {
			t2 = new WebTree<>( makeCatalogNode( p.getSecond() ) );
			t2.setCellRenderer( new MinosCellRenderer<>( 24 ) );

			WebSplitPane splitPane = new WebSplitPane( WebSplitPane.HORIZONTAL_SPLIT, cmpnt, new WebScrollPane( t2 ) );
			splitPane.setPreferredSize( new Dimension( 450, 200 ) );
			splitPane.setDividerLocation( 250 );
			splitPane.setOneTouchExpandable( true );		 
			splitPane.setContinuousLayout( true );
			cmpnt = splitPane;
		}
		if ( JOptionPane.OK_OPTION != ComponentDlg.show( owner, "Структура для загрузки", cmpnt, null ) ) return false;
         
         Competence check = new Competence();
         check.setVariety( curCatalog.getVariety() );
         if ( !AuxFunctions.isPermission( getBlock( curCatalog ), Operation.CREATE ) 
        		 || !AuxFunctions.isPermission( getBlock( check ), Operation.CREATE )
        		 || ( ( p.getSecond() != null ) 
        				 && ( !AuxFunctions.isPermission( Block.PROFILE_PATTERN, Operation.CREATE )
        						 || !AuxFunctions.isPermission( Block.PROFILE_PATTERN_ELEMENT, Operation.CREATE )  ) ) ) {
        	 throw new AccessControlException( "CompetencePanel.loadCompetence() : create elements permission denied" );
         }
         
         searchCatalog( t1.getRootNode() );
         initItem( t1.getRootNode() );
         if ( t2 != null ) {
        	 searchCatalog( t2.getRootNode() );
        	 initItem( t2.getRootNode() );
         }
         OrmHelper.executeAsTransaction( this, new Pair<MainTreeNode, MainTreeNode>( t1.getRootNode(), 
        		 t2 == null ? null : t2.getRootNode() ) );   
         return true;
	}
	
	private MainTreeNode makeCatalogNode( Catalog c ) {
		MainTreeNode node = new MainTreeNode( c );
		if ( c.getSubCatalogs() != null ) {
			for ( Catalog sc : c.getSubCatalogs() ) node.add( makeCatalogNode( sc ) );
		}
		if ( c.getCompetences() != null ) {
			for ( Competence cm : c.getCompetences() ) node.add( makeCompetenceNode( cm ) );
		}
		if ( c.getProfilePatterns() != null ) {
			for ( ProfilePattern pp : c.getProfilePatterns() ) node.add( makeProfilePatternNode( pp ) );
		}

		return node;
	}

	private MainTreeNode makeCompetenceNode( Competence c ) {
		MainTreeNode node = new MainTreeNode( c );
		if ( c.getIndicators() != null ) {
			for ( Indicator i : c.getIndicators() ) node.add( new MainTreeNode( i ) );
		}
		return node;
	}
	
	private MainTreeNode makeProfilePatternNode( ProfilePattern pp ) {
		MainTreeNode node = new MainTreeNode( pp );
		if ( pp.getProfilePatternElements() != null ) {
			for ( ProfilePatternElement ppe : pp.getProfilePatternElements() ) {
				node.add( new MainTreeNode( ppe ) );
				checkStrinAttrs( ppe.getAttributes() );
			}
		}
		return node;
	}

	private void checkStrinAttrs( List<PpeStrAtr> lpsa ) {
		if ( ( lpsa == null ) || ( lpsa.size() == 0 ) ) return;
		for ( PpeStrAtr psa : lpsa ) {
			List<StringAttr> lsa = StringAttrJpaController.getInstance().searchPpeStringAttr( psa.getStringAttr() );
			if ( ( lsa == null ) || ( lsa.size() == 0 ) ) continue;
			psa.setStringAttr( lsa.get( 0 ) );
			if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "checkStrinAttrs() : StringAttr found in DB" );
		}
	}

	private void searchCatalog( MainTreeNode node ) {
		if (  !( node.getUserObject() instanceof Catalog ) ) return;
		Catalog c = ( Catalog ) node.getUserObject();
		if ( c.getId() != 0 ) {
			node.setAddon( OrmHelper.findEntity( Catalog.class, c.getId() ) );
		} else {
			MainTreeNode pnode = ( MainTreeNode ) node.getParent();
			Catalog pc = ( Catalog ) ( pnode.getAddon() != null ? pnode.getAddon() : pnode.getUserObject() );
			List<Catalog> lc = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
					ResourceKeeper.getQuery( QType.JPQL_FIND_CATALOG ), 
					Catalog.class, 
					new Pair<Object, Object>( "name", c.getName() ),
					new Pair<Object, Object>( "variety", c.getVariety() ),
					new Pair<Object, Object>( "parent", pc ),
					new Pair<Object, Object>( "status", Catalog.STATUS_ACTIVE ) );
			if ( ( lc == null ) || ( lc.size() == 0 ) ) return;
			node.setAddon( lc.get( 0 ) );		
			c.setId( lc.get( 0 ).getId() );
		}
		if ( node.getChildCount() < 1 ) return;
		for ( int i = 0; i < node.getChildCount(); i++ ) searchCatalog( ( MainTreeNode ) node.getChildAt( i ) );
	}
	
	private void initItem( MainTreeNode node ) {
		if ( !( node.getUserObject() instanceof Catalog ) ) return;
		Catalog c = ( Catalog ) node.getUserObject();
		Short num1 = ( c.getId() == 0 ? ( short ) 1 : null ) ;
		Short num2 = num1;
		if ( c.getSubCatalogs() != null ) {
			for ( Catalog sc : c.getSubCatalogs() ) {
				if ( sc.getId() != 0 ) continue;
				if ( num1 == null ) num1 = CatalogJpaController.getInstance().getMaxItem( c );
				sc.setItem( ++num1 );
			}				
		}
		if ( c.getCompetences() != null ) {
			for ( Competence cm : c.getCompetences() ) {
				if ( num2 == null ) num2 = CompetenceJpaController.getInstance().getMaxItem( c );
				cm.setItem( ++num2 );
			}				
		}
		if ( c.getProfilePatterns() != null ) {
			for ( ProfilePattern pp : c.getProfilePatterns() ) {
				if ( num2 == null ) num2 = ProfilePatternJpaController.getInstance().getMaxItem( c );
				pp.setItem( ++num2 );
			}				
		}
		if ( node.getChildCount() < 1 ) return;
		for ( int i = 0; i < node.getChildCount(); i++ ) initItem( ( MainTreeNode ) node.getChildAt( i ) );
	}
	
	private void saveLoadedElements( MainTreeNode node ) throws Exception {
		if ( node == null ) return;
		if ( node.getUserObject() instanceof Catalog ) {
			Catalog c = ( Catalog ) node.getUserObject();
			if ( c.getId() == 0 )  {
				Catalog p = ( Catalog ) ( ( MainTreeNode ) node.getParent() ).getAddon();
				Catalog nc = new Catalog( c.getName(), c.getItem(), c.getStatus(), c.getVariety(), c.getVersion(), p, 
						null, null, null, ( List<ProfilePattern> ) null );
				nc = CatalogJpaController.getInstance().create( nc, true, false, false );
				node.setAddon( nc );
				c.setId( nc.getId() );
			}
		}
		if ( node.getUserObject() instanceof Competence ) {
			Competence c = ( Competence ) node.getUserObject();
			c.setIndicators( null );
			Catalog p = ( Catalog ) ( ( MainTreeNode ) node.getParent() ).getAddon();
			c.setCatalog(  p );
			c = CompetenceJpaController.getInstance().create( c, true, false, false );
			node.setAddon( c );
			c.setId( c.getId() );
		}
		if ( node.getUserObject() instanceof Indicator ) {
			Indicator i = ( Indicator ) node.getUserObject();
			Competence c = ( Competence ) ( ( MainTreeNode ) node.getParent() ).getAddon();
			i.setCompetence( c );
			i = IndicatorJpaController.getInstance().create( i, true, false, false );
			node.setAddon( i );
			i.setId( i.getId() );
		}
		if ( node.getUserObject() instanceof ProfilePattern ) {
			ProfilePattern pp = ( ProfilePattern ) node.getUserObject();
			pp.setProfilePatternElements( null );
			Catalog c = ( Catalog ) ( ( MainTreeNode ) node.getParent() ).getAddon();
			pp.setCatalog( c );
			pp = ProfilePatternJpaController.getInstance().create( pp, true, false, false );
			node.setAddon( pp );
			pp.setId( pp.getId() );
		}
		if ( node.getUserObject() instanceof ProfilePatternElement ) {
			ProfilePatternElement ppe = ( ProfilePatternElement ) node.getUserObject();
			List<PpeStrAtr> lpsa = ppe.getAttributes();
			ppe.setAttributes( null );
			ProfilePattern pp = ( ProfilePattern ) ( ( MainTreeNode ) node.getParent() ).getAddon();
			ppe.setProfilePattern( pp );
			ppe = ProfilePatternElementJpaController.getInstance().create( ppe, true, false, false );
			node.setAddon( ppe );
			ppe.setId( ppe.getId() );
			if ( lpsa != null ) {
				short item = 1;
				for ( PpeStrAtr psa : lpsa ) {
					psa.setProfilePatternElement( ppe );
					psa.setItem( item++ );
					saveStringAttrs( psa );
				}
			}
		}		
		if ( node.getChildCount() < 1 ) return;
		for ( int i = 0; i < node.getChildCount(); i++ ) saveLoadedElements( ( MainTreeNode ) node.getChildAt( i ) );
	}

	private void saveStringAttrs( PpeStrAtr psa ) throws Exception {
		if ( psa.getStringAttr().getId() == 0 ) {
			psa.setStringAttr( StringAttrJpaController.getInstance().create( psa.getStringAttr(), true, false, false ) );
		}
		PpeStrAtrJpaController.getInstance().create( psa, true, false, false );
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private class CompetencePanelTransferHandler extends  TransferHandler {
		// =============================================================================================================
		// Constants
		// =============================================================================================================
		private static final long serialVersionUID = 1L;

		// =================================================================================================================
		// Constructors
		// =================================================================================================================
		public CompetencePanelTransferHandler() { }

		// =================================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =================================================================================================================
		@Override
		public int getSourceActions( JComponent arg ) {
			return TransferHandler.LINK;
		}

		@Override
		protected Transferable createTransferable( JComponent arg ) {
			return new MinosTransferable( CompetencePanel.this );
		}
	}
}