package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.IllegalComponentStateException;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.AccessControlException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityNotFoundException;
import javax.swing.DropMode;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.CatalogJpaController;
import minos.data.orm.controllers.ProfilePatternElementJpaController;
import minos.data.orm.controllers.ProfilePatternJpaController;
import minos.entities.Catalog;
import minos.entities.Competence;
import minos.entities.Level;
import minos.entities.ProfilePattern;
import minos.entities.ProfilePatternElement;
import minos.ui.MinosCellRenderer;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.CatalogDlg;
import minos.ui.dialogs.ComponentDlg;
import minos.ui.dialogs.ErrorDlg;
import minos.ui.dialogs.ProfilePatternDlg;
import minos.ui.dialogs.ProfilePatternElementDlg;
import minos.ui.models.dataproviders.CatalogDataProvider;
import minos.ui.models.dataproviders.IndicatorDataProvider;
import minos.ui.models.dataproviders.ProfilePatternDataProvider;
import minos.ui.models.dataproviders.ProfilePatternElementDataProvider;
import minos.ui.models.MainTreeNode;
import minos.utils.AuxFunctions;
import minos.utils.ResourceKeeper;
import minos.utils.Permission.Block;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Quartet;
import ru.gedr.util.tuple.Triplet;
import ru.gedr.util.tuple.Tuple;
import ru.gedr.util.tuple.Tuple.TupleType;
import ru.gedr.util.tuple.Unit;

import com.alee.extended.tree.AsyncTreeDataProvider;
import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.WebAsyncTree;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.toolbar.WebToolBar;

public class ProfilePatternPanel extends WebPanel implements ActionListener, OrmCommand {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID 			= 1L;
	
	private static final String CMD_CATALOG_ADD			= "1";
	private static final String CMD_PP_ADD				= "2";
	private static final String CMD_PPE_ADD				= "3";
	private static final String CMD_PP_ACTIVATE 		= "4";
	private static final String CMD_PPE_ATTR			= "5";
	private static final String CMD_EDIT				= "6";
	private static final String CMD_DELETE 				= "7";

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( ProfilePatternPanel.class );
	private Window owner;
	private WebAsyncTree<MainTreeNode> tree;
	
	private CatalogDataProvider cdp = null;
	private ProfilePatternDataProvider ppdp = null;
	private ProfilePatternElementDataProvider ppedp = null;
	private IndicatorDataProvider idp = null;
	private List<Short> vrt = null;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ProfilePatternPanel(Window owner) {
		super();
		this.owner = owner;

		setLayout( new BorderLayout() );
		add( makeToolbar(), BorderLayout.NORTH );
		Component comp = makeTree();		
		comp = ( comp == null ? new WebTextArea( "Нет прав на чтение структуры" ) : new WebScrollPane( comp ) );
		add( comp, BorderLayout.CENTER );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			switch ( e.getActionCommand() ) {
			case CMD_CATALOG_ADD:
				if ( addCatalog() ) tree.reloadRootNode();
				break;

			case CMD_PP_ADD:
				if ( addPP() ) tree.reloadRootNode();
				break;

			case CMD_PPE_ADD:
				addPPE();
				break;

			case CMD_PPE_ATTR:
				viewStrAttrs();
				break;

			case CMD_EDIT:
				if ( dispatchEdit() ) AuxFunctions.repaintComponent( tree );
				break;

			case CMD_DELETE:
				if ( dispatchDelete() ) tree.reloadRootNode();
				break;

			case CMD_PP_ACTIVATE:
				if ( activatePP() ) tree.reloadRootNode();
				break;
			}
		} catch ( AccessControlException ace ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ProfilePatternPanel.actionPerformed() : ", ace );
			ErrorDlg.show(owner, "Ошибка", "Нет прав на выполнение операции", ace, 
					ResourceKeeper.getIcon( IType.ERROR, 24 ) );			
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ProfilePatternPanel.actionPerformed() : ", ex );
			ErrorDlg.show(owner, "Ошибка", "Произошла ошибка при выполнении перации", ex, 
					ResourceKeeper.getIcon( IType.ERROR, 24 ) );			
		}
	}
	
	/**
	 * save new entities in one transaction
	 */
	@Override
	public void execute( Object obj ) throws Exception {
		if ( obj == null ) return;
		if ( ( ( Tuple ) obj ).getType() == TupleType.UNIT ) {
			@SuppressWarnings("unchecked")
			List<ProfilePatternElement> lppe = ( ( Unit<List<ProfilePatternElement>> ) obj ).getFirst();
			for ( ProfilePatternElement ppe : lppe ) {
				ProfilePatternElementJpaController.getInstance().create( ppe, true, false, false );
			}
		}
		if ( ( ( Tuple ) obj ).getType() == TupleType.PAIR ) {
			@SuppressWarnings("unchecked")
			Pair<List<ProfilePattern>, List<ProfilePatternElement>> p = 
					( Pair<List<ProfilePattern>, List<ProfilePatternElement>> ) obj;
			for ( ProfilePatternElement ppe : p.getSecond() ) ProfilePatternElementJpaController.getInstance().update( ppe, 
					true, false, false );
			for ( ProfilePattern pp : p.getFirst() ) ProfilePatternJpaController.getInstance().update( pp, 
					true, false, false );
		}
		if ( ( ( Tuple ) obj ).getType() == TupleType.QUARTET ) {
			@SuppressWarnings("unchecked")
			Quartet<List<Catalog>, List<ProfilePattern>, List<ProfilePatternElement>, Boolean> q = 
					( Quartet<List<Catalog>, List<ProfilePattern>, List<ProfilePatternElement>, Boolean> ) obj;
			boolean fct = deleteEntities( q.getFirst() );
			boolean fcm = deleteEntities( q.getSecond() );
			boolean fin = deleteEntities( q.getThird() );
			q.setFourth( fct || fcm || fin );			
		}
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	private Component makeTree() {		
		if ( !AuxFunctions.isPermission( Block.COMMON_CATALOG, Operation.READ ) ) return null;
		cdp = new CatalogDataProvider( Arrays.asList( Catalog.EMPTY ) );	
		AsyncTreeDataProvider<MainTreeNode> dp = cdp;		

		if ( AuxFunctions.isPermission(	Block.PROFILE_PATTERN, Operation.READ ) ) {
			ppdp = new ProfilePatternDataProvider( cdp );
			dp = ppdp;
		}
		vrt = AuxFunctions.getAllowedReadVariety( true );
		if ( ( ppdp != null ) && AuxFunctions.isPermission(	Block.PROFILE_PATTERN_ELEMENT, Operation.READ ) 
				&& ( vrt.size() > 0 ) ) {
			ppedp = new ProfilePatternElementDataProvider( ppdp, vrt );
			idp  = new IndicatorDataProvider( ppedp );
			dp = idp;
		}
		tree = new WebAsyncTree<>( dp );
		tree.setCellRenderer( new MinosCellRenderer<>( 24 ) );
		tree.setRootVisible( false );
		tree.setDragEnabled( true );
		tree.setDropMode( DropMode.ON );
		tree.setTransferHandler( new ProfilePatternPanelTransferHandler() );
		return tree;
	}

	/**
	 * Initialized toolbar component
	 * @return WebToolBar component
	 */
	private Component makeToolbar() {
		WebToolBar tb = new WebToolBar();		
		tb.add( new ActionAdapter( "add catalog", ResourceKeeper.getIcon( IType.CATALOG_ADD, 32 ), 
				CMD_CATALOG_ADD, "Добавление нового подкаталога", this, 0 ) );
		tb.add( new ActionAdapter( "add profile pattern", ResourceKeeper.getIcon( IType.PROFILE_PATTERN_ADD, 32 ), 
				CMD_PP_ADD, "Добавление нового шаблона профиля", this, 0 ) );
		tb.add( new ActionAdapter( "add profile pattern element", ResourceKeeper.getIcon( ResourceKeeper.IType.COMPETENCE_ADD, 32 ), 
				CMD_PPE_ADD, "Добавление нового элемента шаблона профиля", this, 0 ) );
		tb.add( new ActionAdapter( "profile pattern element's attributes", ResourceKeeper.getIcon( IType.PAPER_CLIP, 32 ), 
				CMD_PPE_ATTR, "Изменение атрибутов элемента шаблона профиля", this, 0 ) );
		tb.add( new ActionAdapter( "active", ResourceKeeper.getIcon( ResourceKeeper.IType.FORK, 32 ), 
				CMD_PP_ACTIVATE, "Активировать шаблон профиля", this, 0 ) );
		tb.add( new ActionAdapter( "edit", ResourceKeeper.getIcon( ResourceKeeper.IType.EDIT, 32), 
				CMD_EDIT, "Редактирование элемента", this, 0 ) );
		tb.add( new ActionAdapter( "delete", ResourceKeeper.getIcon( ResourceKeeper.IType.DELETE, 32 ), 
				CMD_DELETE, "Удаление элемента", this, 0 ) );
		return tb;
	}

	/**
	 * lookup Catalog node
	 * @return Catalog entity or null
	 */
	private Catalog getRighCatalog() {		
		if ( tree.getSelectionCount() == 0 ) return ( Catalog ) cdp.getRoot().getUserObject() ;
		MainTreeNode mtn = tree.getSelectedNode();
		while ( ( mtn != null ) && !( mtn.getUserObject() instanceof Catalog ) ) {
			mtn = ( MainTreeNode ) ( mtn.getParent() == null ? null : mtn.getParent() );
		}
		Catalog c = ( Catalog ) ( ( ( mtn == null ) || ( mtn.getUserObject() == null ) ) ? null : mtn.getUserObject() );
		if ( c != null )  c = ( Catalog ) OrmHelper.findEntity( Catalog.class, c.getId(), "ancestor", "variery" );
		return c;		
	}

	/**
	 * add new Catalog entity 
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean addCatalog() throws Exception {		
		if ( ( tree == null ) || ( cdp == null ) ) return false; 
		if ( !AuxFunctions.isPermission( Block.COMMON_CATALOG, Operation.CREATE ) ) {
			throw new AccessControlException( "ProfilePatternPanel.addCatalog() : create Catalog permission denied" );
		}
		if ( !AuxFunctions.checkRightSelect( owner, tree, true, false, 
				"Необходимо выделить каталог, \nшаблон профиля, элемент шаблона \nлибо убрать все выделения",
				Catalog.class, ProfilePattern.class, ProfilePatternElement.class ) ) return false;
		
		Catalog prnt = getRighCatalog();
		if ( prnt == null ) {
			throw new EntityNotFoundException( "ProfilePatternPanel.addCatalog() : root catalog entity not found" );
		}
		Catalog nctlg = CatalogDlg.show( owner, "Новый каталог", null, false );
		if ( nctlg == null ) return false;
		nctlg.setParentCatalog( prnt );
		CatalogJpaController.getInstance().create( nctlg, true, false, true );
		return true;
	}
	
	/**
	 * add new ProfilePattern entity
	 * @return true if create success; otherwise false
	 * @throws Exception 
	 */
	private boolean addPP() throws Exception {
		if ( ( tree == null ) || ( ppdp == null ) ) return false;
		if ( !AuxFunctions.isPermission( Block.PROFILE_PATTERN, Operation.CREATE ) ) {
			throw new AccessControlException( "ProfilePatternPanel.addPP() : create ProfilePattern permission denied" );
		}
		if ( !AuxFunctions.checkRightSelect( owner, tree, true, false, 
				"Необходимо выделить каталог, шаблон профиля, элемент шаблона либо убрать все выделения",
				Catalog.class, ProfilePattern.class, ProfilePatternElement.class ) ) return false;
		
		Catalog rctlg = getRighCatalog();
		if ( rctlg == null ) {
			throw new EntityNotFoundException( "ProfilePatternPanel.addPP() : root catalog entity not found" );
		}
		ProfilePattern pp = ProfilePatternDlg.show( owner, "Новый шаблон профиля", null, false ); 
		if ( pp == null ) return false;
		pp.setItem( ProfilePatternJpaController.getInstance().getMaxItem( rctlg ) );
		pp.setCatalog( rctlg );
		ProfilePatternJpaController.getInstance().create( pp, true, false, true );		
		return true;
	}

	/**
	 * add new ProfilePatternElement entity
	 * @return true if create success; otherwise false
	 * @throws Exception 
	 */
	private boolean addPPE() throws Exception {
		if ( ( tree == null ) || ( ppedp == null ) ) return false;
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, false, 
				"Нужно выделить редактируемый Шаблон профиля", ProfilePattern.class ) ) return false;
		if ( !AuxFunctions.isPermission( Block.PROFILE_PATTERN_ELEMENT, Operation.CREATE ) ) {
			throw new AccessControlException( "create ProfilePatternElement permission denied" );
		}
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, false, 
				"Необходимо выделить шаблон профиля или элемент шаблона",
				ProfilePattern.class, ProfilePatternElement.class ) ) return false;
		CompetencePanel cpan = new CompetencePanel( null, false );
		if ( JOptionPane.OK_OPTION != ComponentDlg.show( owner, "Выбор компетенций", cpan, null ) ) return false;
		List<Competence> lc = cpan.getSelectedCompetence();
		if ( ( lc == null ) || ( lc.size() == 0 ) ) return false;
		ProfilePattern pp = ( ProfilePattern ) tree.getSelectedNode().getUserObject();
		createPPE( pp, lc );
		return true;
	}
	
	/** 
	 * dispatch edit object
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean dispatchEdit() throws Exception {
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, true, 
						"Необходимо выделить каталог \nили шаблон профиля \nили элементы шаблона",
						Catalog.class, ProfilePattern.class, ProfilePatternElement.class ) ) return false;
		if ( tree.getSelectionCount() > 1 ) {
			for ( MainTreeNode mtn : tree.getSelectedNodes() ) {
				if ( !( mtn.getUserObject() instanceof ProfilePatternElement ) ) {
					WebOptionPane.showMessageDialog( owner, "Редактирование нескольких элементов "
							+ "\nвозможно только для элементов шаблона", "Ошибка", WebOptionPane.ERROR_MESSAGE );
					return false;
				}
			}
			return editPPE();
		}
		if ( tree.getSelectedNode().getUserObject() instanceof Catalog ) return  editCatalog();
		if ( tree.getSelectedNode().getUserObject() instanceof ProfilePattern ) return  editPP();
		if ( tree.getSelectedNode().getUserObject() instanceof ProfilePatternElement ) return  editPPE();
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
			throw new EntityNotFoundException( "ProfilePatternPanel.editCatalog() : Competence entity not found id=" + id );
		}
		if ( old.getAncestor() != null ) {
			throw new IllegalComponentStateException( "ProfilePatternPanel.editCatalog() : entity have not null ancestor" );
		}
		boolean readOnly = !AuxFunctions.isPermission( Block.COMMON_CATALOG, Operation.UPDATE );		
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
	private boolean editPP() throws Exception {
		int id = ( ( ProfilePattern ) tree.getSelectedNode().getUserObject() ).getId();
		ProfilePattern old = ( ProfilePattern ) OrmHelper.findEntity( ProfilePattern.class, id,
				"name", "description",  "summary", "filialMask", "journal" );		
		if ( old == null ) {
			throw new EntityNotFoundException( "ProfilePatternPanel.editPP() :  ProfilePattern entity not found id=" + id );
		}
		boolean readOnly = ( ( old.getStatus() != ProfilePattern.STATUS_BUILDING ) 
				|| !AuxFunctions.isPermission( Block.PROFILE_PATTERN, Operation.UPDATE ) );

		ProfilePattern newpp = ProfilePatternDlg.show( owner, 
				( readOnly ? "Просмотр" : "Редактирование" ) + " шаблона профиля", old, readOnly );
		if ( newpp == null ) return false;
		newpp.setCatalog( ( Catalog ) tree.getSelectedNode().getParent().getUserObject() );
		newpp = ProfilePatternJpaController.getInstance().update( newpp, true, false, true );
		tree.getSelectedNode().setUserObject( newpp );
		return true;
	}

	/**
	 * edit indicator entity
	 * @return true if success
	 * @throws Exception 
	 */
	private boolean editPPE() throws Exception {
		int id = ( ( ProfilePatternElement ) tree.getSelectedNode().getUserObject() ).getId();
		ProfilePatternElement old = ( ProfilePatternElement ) OrmHelper.findEntity( ProfilePatternElement.class, id, 
				"minLevel", "journal", "competence", "competence.name", "competence.description" );		
		if ( old == null ) {
			throw new EntityNotFoundException( "ProfilePatternPanel.editPPE() : ProfilePatternElement entity"
					+ " not found id=" + id );
		}
		boolean readOnly = ( ( old.getStatus() != ProfilePatternElement.STATUS_BUILDING )
				|| !AuxFunctions.isPermission( Block.PROFILE_PATTERN_ELEMENT, Operation.UPDATE ) );
		
		ProfilePatternElement newppe = ProfilePatternElementDlg.show( owner, 
				( readOnly ? "Просмотр" : "Редактирование" ) + " элемента шаблона", old, readOnly );
		if ( newppe == null ) return false;		
		newppe = ProfilePatternElementJpaController.getInstance().update( newppe, true, false, true );
		tree.getSelectedNode().setUserObject( newppe );
		return true;
	}

	/**
	 * dispatch delete object
	 * @return true if delete successfully
	 * @throws Exception
	 */
	private boolean dispatchDelete() throws Exception {
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, true, 
				"Необходимо выделить каталоги и/или шаблоны и/или элементы шаблонов",
				Catalog.class, ProfilePattern.class, ProfilePatternElement.class ) ) return false;		
		Triplet<List<Catalog>, List<ProfilePattern>, List<ProfilePatternElement>> t = 
				getSelectedElements( true, true, true );
		t.setFirst( preDeleteCatalog( t.getFirst() ) );
		t.setSecond( preDeletePP( t.getSecond(), true ) );
		t.setThird( preDeletePPE( t.getThird() ) );
		if ( ( t.getFirst().size() +  t.getSecond().size() + t.getThird().size() ) < 1 ) return false;
		Quartet<List<Catalog>, List<ProfilePattern>, List<ProfilePatternElement>, Boolean> q =
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
		if ( !AuxFunctions.isPermission( Block.COMMON_CATALOG, Operation.DELETE ) ) {
			throw new AccessControlException( "delete Catalog permission denied" );
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
	 * reload ProfilePattern entity
	 * @return true if success
	 * @throws Exception 
	 */
	private List<ProfilePattern> preDeletePP( List<ProfilePattern> lst, boolean checkPermission ) throws Exception {
		if ( ( lst == null ) || ( lst.size() == 0 ) ) return Collections.emptyList();
		if ( checkPermission && !AuxFunctions.isPermission( Block.PROFILE_PATTERN, Operation.DELETE ) ) {
			throw new AccessControlException( "delete ProfilePattern permission denied" );
		}
		List<Integer> pp_ids = new ArrayList<>();
		for ( ProfilePattern pp : lst ) pp_ids.add( pp.getId() );
		
		List<ProfilePattern> lpp = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_REFRESH_PP_BEFORE_DELETE ), 
				ProfilePattern.class, 
				new Pair<Object, Object>( "pp_ids", pp_ids ),
				new Pair<Object, Object>( "status", Arrays.asList( ProfilePattern.STATUS_ACTIVE, 
						ProfilePattern.STATUS_BUILDING ) ) );
		return ( ( lpp != null ) && ( lpp.size() > 0 ) ) ? lpp : Collections.<ProfilePattern>emptyList();
	}

	/**
	 * reload ProfilePatternElement entity
	 * @return true if success
	 * @throws Exception 
	 */
	private List<ProfilePatternElement> preDeletePPE( List<ProfilePatternElement> lst ) throws Exception {
		if ( ( lst == null ) || ( lst.size() == 0 ) ) return Collections.emptyList();
		if ( !AuxFunctions.isPermission( Block.PROFILE_PATTERN_ELEMENT, Operation.DELETE ) ) {
			throw new AccessControlException( "delete ProfilePatternElement permission denied" );
		}
		List<Integer> ppe_ids = new ArrayList<>();
		for ( ProfilePatternElement ppe : lst ) ppe_ids.add( ppe.getId() );

		List<ProfilePatternElement> lppe = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_REFRESH_PPE_BEFORE_DELETE ), 
				ProfilePatternElement.class, 
				new Pair<Object, Object>( "ppe_ids", ppe_ids ),
				new Pair<Object, Object>( "status", Arrays.asList( ProfilePatternElement.STATUS_ACTIVE, 
						ProfilePatternElement.STATUS_BUILDING ) ) );
		return ( ( lppe != null ) && ( lppe.size() > 0 ) ) ? lppe : Collections.<ProfilePatternElement>emptyList();
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
		} else if ( entities.get( 0 ) instanceof ProfilePattern ) {
			for ( T t : entities ) ProfilePatternJpaController.getInstance().delete( ( ProfilePattern ) t, true, 
					false, false );
		} else if ( entities.get( 0 ) instanceof ProfilePatternElement ) {
			for ( T t : entities ) ProfilePatternElementJpaController.getInstance().delete( ( ProfilePatternElement ) t, 
					true, false, false );
		}
		return true;
	}

	/**
	 * create new ProfilePatternElement entities from Competence's list and 
	 * @param pp is parent ProfilePattern entity
	 * @param lc is list of Competence entities for create new ProfilePatternElement entities
	 * @throws Exception
	 */
	private void createPPE( ProfilePattern pp, List<Competence> lc ) throws Exception {
		if ( ( pp == null ) || ( lc == null ) || ( lc.size() == 0 ) ) return;
		List<Short> vrt = new ArrayList<>();
		for ( Competence c : lc ) {
			if ( !vrt.contains( c.getVariety() ) ) vrt.add( c.getVariety() );
		}
		List<Object[]> lppe = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_PPE_AND_COMPETENCE ), 
				Object[].class,
				new Pair<Object, Object>( "pps", Arrays.asList( pp ) ),
				new Pair<Object, Object>( "vrt", vrt ),
				new Pair<Object, Object>( "ts", new Timestamp( System.currentTimeMillis() ) ),
				new Pair<Object, Object>( "dts", ResourceKeeper.getObject( OType.WAR ) ) );
		if ( lppe == null ) lppe = Collections.emptyList();

		List<ProfilePatternElement> newPPEs = new ArrayList<>();
		List<Level> ll = ResourceKeeper.getObject( OType.LEVELS_CACHE );
		Map<Short, Short> map = new TreeMap<>();
		for ( Competence c : lc ) {
			boolean found = false;
			for ( Object[] o : lppe ) {
				if ( ( o[0] != null ) && ( ( ( ProfilePatternElement ) o[0] ).getCompetence() != null ) 
					&& ( ( ProfilePatternElement ) o[0] ).getCompetence().equals( c ) ) {
					found = true;
					break;
				}
			}
			if ( found ) continue;
			Short item = map.get( c.getVariety() );
			if ( item == null )  item = ProfilePatternElementJpaController.getInstance().getMaxItem( c.getVariety(), pp );
			newPPEs.add( new ProfilePatternElement( ++item, ProfilePatternElement.STATUS_BUILDING, c, 
					ll.get( Math.round( Level.LEVEL_COUNT / 2 ) ), pp, null ) );
			map.put( c.getVariety(), item );
		}
		map.clear();
		if ( newPPEs.size() > 0 ) OrmHelper.executeAsTransaction( this, 
				new Unit<List<ProfilePatternElement>>( newPPEs ) );
		tree.reloadRootNode();
	}

	
	/**
	 * make catalog's set, competence's set and indicator's set from selected tree nodes
	 * @return Triplet of catalog's set, competence's set and indicator's set 
	 */
	private Triplet<List<Catalog>, List<ProfilePattern>, List<ProfilePatternElement>> 
	getSelectedElements( boolean checkCatalogs, boolean checkPP, boolean checkPPE ) {
		Triplet<List<Catalog>, List<ProfilePattern>, List<ProfilePatternElement>>  t = 
				new Triplet<>( Collections.<Catalog>emptyList(), Collections.<ProfilePattern>emptyList(), 
						Collections.<ProfilePatternElement>emptyList() );
		for ( MainTreeNode node : tree.getSelectedNodes() ) {
			if ( ( node == null ) || ( node.getUserObject() == null ) ) continue;
			if ( checkCatalogs && ( node.getUserObject() instanceof Catalog ) 
					&& !t.getFirst().contains( node.getUserObject() ) ) {
				if ( t.getFirst().size() == 0 ) t.setFirst( new ArrayList<Catalog>() );
				t.getFirst().add( ( Catalog ) node.getUserObject() );
			}				
			if ( checkPP && ( node.getUserObject() instanceof ProfilePattern ) 				
					&& !t.getSecond().contains( node.getUserObject() ) )  {
				if ( t.getSecond().size() == 0 ) t.setSecond( new ArrayList<ProfilePattern>() );
				t.getSecond().add( ( ProfilePattern ) node.getUserObject() );
			}				
			if ( checkPPE && ( node.getUserObject() instanceof ProfilePatternElement ) 
					&& !t.getThird().contains( node.getUserObject() ) ) {
				if ( t.getThird().size() == 0 ) t.setThird( new ArrayList<ProfilePatternElement>() );
				t.getThird().add( ( ProfilePatternElement ) node.getUserObject() );
			}
		}		
		if ( checkCatalogs ) t.setFirst( getSubCatalogs( t.getFirst() ) );
		if ( checkPP ) {
			List<ProfilePattern> lpp = getPpFromCatalog( t.getFirst() );
			for ( ProfilePattern pp : lpp ) {
				if ( t.getSecond().size() == 0 ) {
					t.setSecond( new ArrayList<ProfilePattern>() );
					t.getSecond().add( pp );
					continue;
				}
				if ( !t.getSecond().contains( pp ) ) t.getSecond().add( pp );
			}
		}
		if ( checkPPE ) {
			List<ProfilePatternElement> lppe = getPpeFromPp( t.getSecond() );
			for ( ProfilePatternElement ppe : lppe ) {
				if ( t.getThird().size() == 0 ) {
					t.setThird( new ArrayList<ProfilePatternElement>() );
					t.getThird().add( ppe );
					continue;
				}
				if ( !t.getThird().contains( ppe ) ) t.getThird().add( ppe );
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
		if ( ( catalogs == null ) || ( catalogs.size()== 0 ) ) return Collections.emptyList();
		List<Catalog> lst = new ArrayList<>( catalogs );
		for ( Catalog c : catalogs ) {
			List<Catalog> subCatalogs = OrmHelper.findByQueryWithParam( QueryType.SQL, 
					ResourceKeeper.getQuery( QType.SQL_LOAD_ALL_SUB_CATALOGS ), 
					Catalog.class, 
					new Pair<Object, Object>( 1, c.getId() ),
					new Pair<Object, Object>( 2, Catalog.STATUS_ACTIVE ) );
			if ( ( subCatalogs == null ) || ( subCatalogs.size() == 0 ) ) continue;
			for ( Catalog sc : subCatalogs ) {
				if ( !lst.contains( sc ) ) lst.add( sc );
			}
		}
		return lst;
	}

	/**
	 * make competence's set from catalog's set
	 * @param catalogs - existing set of Catalog's entity 
	 * @return competence's set or null
	 */
	private List<ProfilePattern> getPpFromCatalog( List<Catalog> catalogs ) {
		if ( ( catalogs == null ) || ( catalogs.size() == 0 ) ) return Collections.emptyList();
		List<ProfilePattern> pps = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_PP ), 
				ProfilePattern.class, 
				new Pair<Object, Object>( "catalogs", catalogs ),
				new Pair<Object, Object>( "ts", new Timestamp( System.currentTimeMillis() ) ),
				new Pair<Object, Object>( "dts", ResourceKeeper.getObject( OType.WAR ) ) );
		return ( pps != null ? pps : Collections.<ProfilePattern>emptyList() );
	}
	
	/**
	 * make ProfilePatternElement's set from ProfilePattern's set
	 * @param lpp - existing set of ProfilePattern's entity
	 * @return ProfilePatternElement's set  or empty set
	 */
	private List<ProfilePatternElement> getPpeFromPp( List<ProfilePattern> lpp ) {
		if ( ( lpp == null ) || ( lpp.size() == 0 ) ) return Collections.emptyList();
		List<Object[]> los = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_PPE_AND_COMPETENCE ), 
				Object[].class, 
				new Pair<Object, Object>( "pps", lpp ),
				new Pair<Object, Object>( "vrt", vrt ),					
				new Pair<Object, Object>( "ts", new Timestamp( System.currentTimeMillis() ) ),
				new Pair<Object, Object>( "dts", ResourceKeeper.getObject( OType.WAR ) ) );
		if ( ( los == null ) || ( los.size() == 0 ) ) return Collections.emptyList();
		List<ProfilePatternElement> lppe = new ArrayList<>();
		for ( Object[] o : los ) lppe.add( ( ProfilePatternElement ) o[0] );
		return lppe;
	}

	/**
	 * make list of selected ProfilePattern 
	 * @return list of selected ProfilePattern
	 */
	public List<ProfilePattern> getSelectedProfilePattern() {
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, true,
				"Необходимо выделить каталоги и/или шаблоны профилей",
				Catalog.class, ProfilePattern.class ) ) return Collections.emptyList();
		Triplet<List<Catalog>, List<ProfilePattern>, List<ProfilePatternElement>> t = getSelectedElements( true, true, false );
		return t.getSecond();
	}

	
	/**
	 * display dialog for editing StringAttr entities for ProfilePatternElement entity
	 * @throws Exception
	 */
	private void viewStrAttrs() throws Exception {		
		if ( !AuxFunctions.isPermission( Block.STR_ATTR, Operation.READ ) ) {
			throw new AccessControlException( "view String Attributes of ProfilePatternElement permission denied" );
		}
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, false,
				"Просмотр текстовых атрибутов можно \nтолько у элементов шаблона профиля",
				ProfilePatternElement.class ) ) return;
		
		ProfilePatternElement ppe = ( ProfilePatternElement ) tree.getSelectedNode().getUserObject();
		//boolean readOnly = 

		
		ComponentDlg.show( owner, "Документы основания", new StringAttrPanel( owner, ppe, true ), null );
	}
	
	/**
	 * activate ProfilePattern object
	 * @return true if activate successfully
	 * @throws Exception
	 */
	private boolean activatePP() throws Exception {
		if ( !AuxFunctions.isPermission( Block.ACTIVATE_PROFILE_PATTERN, Operation.CREATE ) ) {
			throw new AccessControlException( "activate ProfilePattern permission denied" );
		}
		if ( !AuxFunctions.checkRightSelect( owner, tree, false, true, 
				"Для активации нужно выбрать только шаблоны", ProfilePattern.class ) ) return false;
		
		List<ProfilePattern> lpp = new ArrayList<>();
		for ( AsyncUniqueNode node : tree.getSelectedNodes() ) {
			if ( ( node == null ) || !( node.getUserObject() instanceof ProfilePattern ) 
					|| ( ( ( ProfilePattern ) node.getUserObject() ).getStatus() != ProfilePattern.STATUS_BUILDING ) ) {
				continue;
			}
			lpp.add( ( ProfilePattern ) node.getUserObject() );
		}
		lpp = new ArrayList<>( preDeletePP( lpp, false ) ); // reload ProfilePattern entity
		if ( lpp.size() == 0 ) return false;
		for ( int i = lpp.size(); i > 0; i-- ) {
			if ( lpp.get( i - 1 ).getStatus() != ProfilePattern.STATUS_BUILDING ) {
				lpp.remove( i - 1 );
			} else {
				lpp.get( i - 1 ).setStatus( ProfilePattern.STATUS_ACTIVE );
			}
		}		
		List<ProfilePatternElement> lppe = getPpeFromPp( lpp );
		if ( lppe.size() > 0 ) lppe = new ArrayList<>( lppe );
		if ( ( lppe != null ) && ( lppe.size() > 0 ) ) {
			for ( int i = lppe.size(); i > 0; i-- ) {
				if ( lppe.get( i - 1 ).getStatus() != ProfilePatternElement.STATUS_BUILDING ) {
					lppe.remove( i - 1 );
				} else {
					lppe.get( i - 1 ).setStatus( ProfilePatternElement.STATUS_ACTIVE );
				}
			}	
		}
		OrmHelper.executeAsTransaction( this, new Pair<List<ProfilePattern>, List<ProfilePatternElement>>( lpp, lppe ) );
		return true;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private class ProfilePatternPanelTransferHandler extends  TransferHandler {
		// =============================================================================================================
		// Constants
		// =============================================================================================================
		private static final long serialVersionUID = 1;

		// =============================================================================================================
		// Fields
		// =============================================================================================================
		private DataFlavor df;
		private Thread saver; // thread for save new ProfilePatternElement entities

		// =============================================================================================================
		// Constructors
		// =============================================================================================================
		public ProfilePatternPanelTransferHandler() {
			try {
				df = new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType + ";class=" 
						+ CompetencePanel.class.getName() );
			} catch ( Exception thex ) {
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ProfilePatternPanelTransferHandler() :"
						+ " create DataFlavor for CompetencePanel.class generate error : ", thex ); 
				df = null;
			}	
		}

		// =============================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =============================================================================================================
		@Override
		public boolean canImport( TransferSupport support ) {
			if ( ( saver != null ) && saver.isAlive() ) return false;
			if ( !AuxFunctions.isPermission( Block.PROFILE_PATTERN_ELEMENT, Operation.CREATE ) ) return false;
			return ( df == null ? false : support.getTransferable().isDataFlavorSupported( df ) );
		}

		@Override
		public boolean importData( TransferSupport support ) {
			try {
				final CompetencePanel cp = ( CompetencePanel ) support.getTransferable().getTransferData( df );
				if ( cp == null ) return false;
				TreePath path = ( ( JTree ) support.getComponent() ).getDropLocation().getPath();
				for ( Object o : path.getPath() ) {
					if ( ( o instanceof MainTreeNode ) 
							&& ( ( ( MainTreeNode ) o).getUserObject() instanceof ProfilePattern ) ) {
						final ProfilePattern pp = ( ProfilePattern ) ( ( MainTreeNode ) o ).getUserObject();
						if ( pp.getStatus() != ProfilePattern.STATUS_BUILDING ) return false;
						new Thread ( new Runnable() {
							
							@Override
							public void run() {
								try {
									createPPE( pp, cp.getSelectedCompetence() );
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} ).start();
						return true;
					}
				}
				return false;
			} catch ( Exception ex ) {
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ProfilePatternPanelTransferHandler."
						+ "importData() : ", ex );
				return false;
			}
		}
	}
}