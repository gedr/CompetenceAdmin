package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.TextArea;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.orm.controllers.ProfileJpaController;
import minos.data.services.FilialInfo;
import minos.entities.Division;
import minos.entities.EstablishedPost;
import minos.entities.Post;
import minos.entities.Profile;
import minos.entities.ProfilePattern;
import minos.ui.MinosCellRenderer;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.ComponentDlg;
import minos.ui.dialogs.ErrorDlg;
import minos.ui.dialogs.SwitchOptionDlg;
import minos.ui.models.MainTreeNode;
import minos.ui.models.dataproviders.BasisDataProvider;
import minos.ui.models.dataproviders.DivisionDataProvider;
import minos.ui.models.dataproviders.EPostDataProvider;
import minos.ui.models.dataproviders.EPostDataProvider.EPostGroup;
import minos.ui.models.dataproviders.IndicatorDataProvider;
import minos.ui.models.dataproviders.PostDataProvider;
import minos.ui.models.dataproviders.ProfileDataProvider;
import minos.ui.models.dataproviders.ProfilePatternElementDataProvider;
import minos.utils.AuxFunctions;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper;
import minos.utils.Permission.Block;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.QType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Triplet;
import ru.gedr.util.tuple.Tuple;
import ru.gedr.util.tuple.Unit;
import ru.gedr.util.tuple.Tuple.TupleType;

import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.WebAsyncTree;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.tabbedpane.WebTabbedPane;
import com.alee.laf.toolbar.WebToolBar;

public class PostProfilePanel extends WebPanel implements ActionListener, PropertyChangeListener, OrmCommand {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	
	private static final String CMD_PROFILE_ADD 	= "1";
	private static final String CMD_PROFILE_DEL 	= "2";
	private static final String CMD_CONFIG 			= "3";
	
	private static final Logger log = LoggerFactory.getLogger( PostProfilePanel.class );
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private Window owner;
	private WebTabbedPane tabbedPane;
	private WebAsyncTree<MainTreeNode> treeEP; // tree for established post
	private WebAsyncTree<MainTreeNode> treeP; // tree for post
	
	private DivisionDataProvider ddpEP = null;
	private DivisionDataProvider ddpP = null;
	private EPostDataProvider epdp = null;
	private PostDataProvider pdp = null;
	private ProfileDataProvider ppdpEP = null;
	private ProfileDataProvider ppdpP = null;
	private ProfilePatternElementDataProvider ppedpEP = null;
	private ProfilePatternElementDataProvider ppedpP = null;
	private IndicatorDataProvider idpEP = null;
	private IndicatorDataProvider idpP = null;
	
	private ProfilePatternPanel ppp = null;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public PostProfilePanel( Window owner ) {
		this( owner, false );
	}

	public PostProfilePanel( Window owner, boolean onlyView ) {
		super();
		Component cmnt = makeTree( false );
		if ( cmnt == null ) {
			cmnt = new TextArea( "access denied" );
		} else {
			tabbedPane = new WebTabbedPane();
			tabbedPane.add( new WebScrollPane( cmnt ), 0 );
			tabbedPane.add( new WebScrollPane( makeTree( true ) ), 1 );
			tabbedPane.setTitleAt( 0, "Штатные должности" );
			tabbedPane.setTitleAt( 1, "Обычные должности" );
			cmnt = tabbedPane;
		}
		
		setLayout( new BorderLayout() );
		add( makeToolbar( onlyView ), BorderLayout.NORTH );
		add( cmnt, BorderLayout.CENTER );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			switch ( e.getActionCommand() ) {
			case CMD_CONFIG :
				showConfig( ( Component ) e.getSource() );
				break;

			case CMD_PROFILE_ADD :
				boolean b = ( tabbedPane.getSelectedIndex() == 0 ? addProfileForEPost() : addProfileForPost() );
				if ( b ) reloadSelectedNode( tabbedPane.getSelectedIndex() == 0 ? treeEP : treeP );
				break;

			case CMD_PROFILE_DEL :
				if ( delProfile() ) reloadSelectedNode( tabbedPane.getSelectedIndex() == 0 ? treeEP : treeP );
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
	
	@Override
	public void propertyChange( PropertyChangeEvent e ) {
		if ( SwitchOptionDlg.OPTIONS_CHANGE.equals( e.getPropertyName() ) && ( e.getNewValue() != null ) ) {			
			@SuppressWarnings("unchecked")
			List<Triplet<String, String, Boolean>> val = ( List<Triplet<String, String, Boolean>> ) e.getNewValue();
			applyOption( val );
			( tabbedPane.getSelectedIndex() == 0 ? treeEP : treeP ).reloadRootNode();			
		}		
	}

	@Override
	public void execute( Object obj ) throws Exception {
		Tuple t = ( Tuple ) obj;
		if ( ( t.getType() == TupleType.UNIT ) || ( t.getType() == TupleType.PAIR ) ) {
			@SuppressWarnings("unchecked")
			Unit<List<Profile>> u = ( Unit<List<Profile>> ) t;
			for ( Profile pr : u.getFirst() ) ProfileJpaController.getInstance().delete( pr, true, false, false );
		}
		if ( t.getType() == TupleType.PAIR ) {
			@SuppressWarnings("unchecked")
			Pair<List<Profile>, List<Profile>> p = ( Pair<List<Profile>, List<Profile>> ) t;
			for ( Profile pr : p.getSecond() ) ProfileJpaController.getInstance().create( pr, true, false, false );
			return;			
		}		
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * return selected profile in tree
	 * @return
	 */
	public Profile getSelectedProfile() {
		if ( tabbedPane.getSelectedIndex() < 0 ) return null;
		WebAsyncTree<MainTreeNode> tree = tabbedPane.getSelectedIndex() == 0 ? treeEP : treeP;
		if ( ( tree.getSelectionCount() != 1 ) || ( tree.getSelectedNode() == null ) 
				|| !( tree.getSelectedNode() instanceof MainTreeNode ) 
				|| ( ( ( MainTreeNode ) tree.getSelectedNode() ).getUserObject() == null )
				|| !( ( ( MainTreeNode ) tree.getSelectedNode() ).getUserObject() instanceof Profile ) ) return null;
		return ( Profile ) ( ( MainTreeNode ) tree.getSelectedNode() ).getUserObject();
	}
	
	/**
	 * make tree for display division-post-profile
	 * @param flagPost - if true to use post; otherwise use established post
	 * @return new tree or null
	 */
	private Component makeTree( boolean flagPost )  {
		BasisDataProvider<MainTreeNode> bdp = null;
		try {
			// initialize divisions reader
			if ( !AuxFunctions.isPermission( Block.DIVISION, Operation.READ ) ) {
				throw new AccessControlException( "PostProfilePanel.makeTree() : read DIVISION permission denied"  );
			}
			Byte b = ( Byte ) ResourceKeeper.getObject( ResourceKeeper.OType.DEFAULT_BRANCH_OFFICE_CODE );
			List<FilialInfo> lfi = ResourceKeeper.getObject( ResourceKeeper.OType.BRANCH_OFFICES_INFO );
			FilialInfo found = null;
			for ( FilialInfo fi : lfi ) 
				if ( fi.getCode() == b ) {
					found = fi;
					break;
				}
			if  ( flagPost ) ddpP = new DivisionDataProvider( found.getRootDivisionCode() );
			else ddpEP = new DivisionDataProvider( found.getRootDivisionCode() );			
			bdp = ( flagPost ? ddpP : ddpEP );
			
			// initialize Post or EstablishedPost reader
			if ( !AuxFunctions.isPermission( Block.EPOST, Operation.READ ) ) {
				throw new AccessControlException( "PostProfilePanel.makeTree() : read EPOST permission denied"  );
			} 
			if ( flagPost ) pdp = new PostDataProvider( ddpP ); 
			else epdp = new EPostDataProvider( ddpEP );
			bdp = ( flagPost ? pdp : epdp );

			// initialize profiles reader
			if ( !AuxFunctions.isPermission( Block.PROFILE, Operation.READ ) ) {
				throw new AccessControlException( "PostProfilePanel.makeTree() : read PROFILE permission denied"  );
			} 
			if  ( flagPost ) ppdpP = new ProfileDataProvider( pdp );				
			else ppdpEP = new ProfileDataProvider( epdp );
			bdp = ( flagPost ? ppdpP : ppdpEP );

			// initialize ProfilePatternElement and indicator reader
			List<Short> vrts = AuxFunctions.getAllowedReadVariety( true );
			if ( ( ( flagPost && ( ppdpP != null ) ) || ( !flagPost && ( ppdpEP != null ) ) ) && ( vrts.size() > 0 ) ) {
				if  ( flagPost ) ppedpP = new ProfilePatternElementDataProvider( ppdpP, vrts );				
				else ppedpEP = new ProfilePatternElementDataProvider( ppdpEP, vrts );

				if  ( flagPost ) idpP = new IndicatorDataProvider( ppedpP );				
				else idpEP = new IndicatorDataProvider( ppedpEP );

				bdp = ( flagPost ? idpP : idpEP );
			}
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "PostProfilePanel.makeTree() : "
					+ "full init tree failed", ex );
		}
		if ( bdp == null ) return null;
		
		WebAsyncTree<MainTreeNode> tree = new WebAsyncTree<>( bdp );
		tree.setCellRenderer( new MinosCellRenderer<Object>( 24 ) );
		if  ( flagPost ) treeP = tree;				
		else treeEP = tree;
		return tree;
	}

	/**
	 * make toolbar
	 * @param onlyView - show command button or only option button 
	 * @return toolbar component
	 */
	private Component makeToolbar( boolean onlyView ) {    
		WebToolBar tb = new WebToolBar();		
		if ( !onlyView ) tb.add( new ActionAdapter( "make profile", ResourceKeeper.getIcon( IType.PROFILE_ADD, 32 ), 
				CMD_PROFILE_ADD, "Привязка к должности шаблона профиля", this, 0 ) );
		if ( !onlyView ) tb.add( new ActionAdapter( "delete", ResourceKeeper.getIcon( IType.PROFILE_DELETE, 32 ), 
				CMD_PROFILE_DEL, "Удаление профиля", this, 0 ) );
		tb.add( new ActionAdapter( "config", ResourceKeeper.getIcon( IType.PREFERENCES, 32 ), 
				CMD_CONFIG, "Кофигурация", this, 0 ) );
		return tb;
	}
	
	/**
	 * initialize Options for option dialog
	 * @return list of options
	 */
	private List<Triplet<String, String, Boolean>> initOptions() {
		if ( tabbedPane.getSelectedIndex() < 0 ) return null;
		if ( ( treeEP == null ) || ( treeP == null ) || ( ddpEP == null ) || ( ddpP == null ) ) return null;
		String g1 = "Отделы";
		String g2 = "Штатные должности";
		String g3 = "Должности";
		int ind = tabbedPane.getSelectedIndex();

		List<Triplet<String, String, Boolean>> options = new ArrayList<>();
		options.add( new Triplet<>( g1, "Показывать просроченые отделы", 
				( ind == 0 ? ddpEP : ddpP ).isVisibleOverdueDivisions() ) );
		options.add( new Triplet<>( g1, "Показывать удаленые отделы", 
				( ind == 0 ? ddpEP : ddpP ).isVisibleDeletedDivisions() ) );
		options.add( new Triplet<>( g1, "Показывать отделы, не согласовнае ОТИЗом", 
				( ind == 0 ? ddpEP : ddpP ).isVisibleDisapproveDivisions() ) );

		if ( ( epdp != null ) &&  ( tabbedPane.getSelectedIndex() == 0 ) ) {
			options.add( new Triplet<>( g2, "Показывать просроченые должности", epdp.isVisibleOverdueEPosts() ) );
			options.add( new Triplet<>( g2, "Показывать удаленые должности", epdp.isVisibleDeletedEPosts() ) );
			options.add( new Triplet<>( g2, "Показывать должности, не согласовнае ОТИЗом", epdp.isVisibleDisapproveEPosts() ) );
			options.add( new Triplet<>( g2, "Группировка должностей", epdp.isVisibleGroupEPost() ) );
		}
		if ( ( pdp != null ) &&  ( tabbedPane.getSelectedIndex() == 1 ) ) {
			options.add( new Triplet<>( g3, "Показывать просроченые организационные единицы", pdp.isVisibleOverdueOrgUnit() ) );
			options.add( new Triplet<>( g3, "Показывать должности уволеных", pdp.isVisibleFired() ) );
			options.add( new Triplet<>( g3, "Показывать должности временщиков", pdp.isVisibleTemporary() ) );
			options.add( new Triplet<>( g3, "Показывать просроченые должности", pdp.isVisibleOverduePost() ) );
			options.add( new Triplet<>( g3, "Показывать удаленые должности", pdp.isVisibleDeletePost() ) );
		}
		return options;
	}
	
	/**
	 * apply options for current tree 
	 * @param option changed options
	 */
	private void applyOption( List<Triplet<String, String, Boolean>> option ) {
		if (  ( ddpP == null ) || ( ddpEP == null ) || ( option == null ) || ( option.size() == 0 ) ) return;

		int tind = tabbedPane.getSelectedIndex();
		int ind = -1;
		for ( Triplet<String, String, Boolean> t : option ) {
			ind++;
			if ( ( ind > 2 ) && ( epdp == null ) ) break;
			if ( ( t == null ) || ( t.getThird() == null ) ) continue;
			switch ( ind ) {
			case 0 : 
				( tind == 0 ? ddpEP : ddpP ).setVisibleOverdueDivisions( t.getThird() );
				break;
			case 1 : 
				( tind == 0 ? ddpEP : ddpP ).setVisibleDeletedDivisions( t.getThird() );
				break;
			case 2 :
				( tind == 0 ? ddpEP : ddpP ).setVisibleDisapproveDivisions( t.getThird() );
				break;
			case 3 : 
				if ( tind == 0 ) epdp.setVisibleOverdueEPosts( t.getThird() );
				if ( tind == 1 ) pdp.setVisibleOverdueOrgUnit( t.getThird() );
				break;
			case 4 : 
				if ( tind == 0 ) epdp.setVisibleDeletedEPosts( t.getThird() );
				if ( tind == 1 ) pdp.setVisibleFired( t.getThird() );
				break;
			case 5 :
				if ( tind == 0 ) epdp.setVisibleDisapproveEPosts( t.getThird() );
				if ( tind == 1 ) pdp.setVisibleTemporary( t.getThird() );
				break;
			case 6 :
				if ( tind == 0 ) epdp.setVisibleGroupEPost( t.getThird() );
				if ( tind == 1 ) pdp.setVisibleOverduePost( t.getThird() );
				break;
			case 7 :
				if ( tind == 1 ) pdp.setVisibleDeletePost( t.getThird() );
				break;
			}
		}
	}

	/**
	 * show config dialog
	 * @param hostComponent 
	 */
	private void showConfig( Component hostComponent ) {
		List<Triplet<String, String, Boolean>> options = initOptions();
		if ( options == null ) return;
		Component c = this;
		while ( true ) {
			if ( c instanceof Window ) break;
			c = c.getParent();
		}
		SwitchOptionDlg.show( ( Window ) c, options, hostComponent, this );
	}
	
	/**
	 * add new profile for epost
	 * @return
	 * @throws Exception
	 */
	private boolean addProfileForEPost() throws Exception {
		if ( !AuxFunctions.isPermission( Block.PROFILE, Operation.CREATE ) ) {
			throw new AccessControlException( "PostProfilePanel.addProfileForEPost() : create Profile permission denied"  );
		}
		if ( !AuxFunctions.checkRightSelect( owner, treeEP, false, true, "Нужно выделить штатные должности или их группу", 
				EstablishedPost.class, EPostGroup.class ) ) return false;

		ProfilePattern pp = selectPP();
		if ( pp == null ) return false;
		
		List<EstablishedPost> lep = new ArrayList<>();		
		for ( MainTreeNode node : treeEP.getSelectedNodes() ) {
			if ( ( node == null )  
					|| ( !( node.getUserObject() instanceof EstablishedPost ) 
							&& !( node.getUserObject() instanceof EPostGroup ) ) ) continue; 
			if ( node.getUserObject() instanceof EstablishedPost ) lep.add( ( EstablishedPost ) node.getUserObject() );
			if ( node.getUserObject() instanceof EPostGroup ) {
				lep.addAll( ( ( EPostGroup ) node.getUserObject() ).getEstablishedPosts() );
			}
		}
		if ( lep.size() < 1 ) return false;
		List<Profile> newlp = new ArrayList<>();
		for ( EstablishedPost ep : lep ) newlp.add( new Profile( ep, pp, null ) );

		List<Profile> dellp = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_REFRESH_CURRENT_PROFILE_BEFORE_DELETE_BY_EPOST ),  
				Profile.class,
				new Pair<Object, Object>( "eposts", lep ) );
		OrmHelper.executeAsTransaction( this, new Pair<List<Profile>, List<Profile>>( dellp != null ? dellp 
				: Collections.<Profile>emptyList() , newlp ) );
		return true;
	}

	/**
	 * add new profile for post and division
	 * @return true if 
	 * @throws Exception
	 */
	private boolean addProfileForPost() throws Exception {
		if ( !AuxFunctions.isPermission( Block.PROFILE, Operation.CREATE ) ) {
			throw new AccessControlException( "PostProfilePanel.addProfileForEPost() : create Profile permission denied"  );
		}
		if ( !AuxFunctions.checkRightSelect( owner, treeP, false, true, "Нужно выделить должности", 
				Post.class ) ) return false;
		
		ProfilePattern pp = selectPP();
		if ( pp == null ) return false;

		List<Post> lp = new ArrayList<>();
		List<Division> ld = new ArrayList<>();
		for ( MainTreeNode node : treeP.getSelectedNodes() ) {
			if ( ( node == null ) || ( !( node.getUserObject() instanceof Post ) ) )  continue;
			lp.add( ( Post ) node.getUserObject() );
			ld.add( ( Division ) node.getParent().getUserObject() );
		}
		if ( lp.size() < 1 ) return false;
		List<Profile> newlp = new ArrayList<>();
		for ( int i = 0; i < lp.size(); i++ ) newlp.add( new Profile( ld.get( i ), lp.get( i ), pp, null ) );

		List<Profile> lprf = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_REFRESH_CURRENT_PROFILE_BEFORE_DELETE_BY_POST ),  
				Profile.class,
				new Pair<Object, Object>( "divisions", ld ),
				new Pair<Object, Object>( "posts", lp ) );
		
		List<Profile> dellp = new ArrayList<>(); 
		for ( Profile p :lprf ) {
			for ( int i = 0; i < lp.size(); i++ ) {
				if ( !p.getDivision().equals( ld.get( i ) ) || !p.getPost().equals( lp.get( i ) ) ) continue;
				dellp.add( p );
				break;
			}
		}
		OrmHelper.executeAsTransaction( this, new Pair<List<Profile>, List<Profile>>( dellp, newlp ) );
		return true;
	}

	/**
	 * open dialog for select ProfilePattern
	 * @return selected active ProfilePattern entity or null
	 */
	private ProfilePattern selectPP(){
		if ( ppp == null ) ppp = new ProfilePatternPanel( owner );
		if ( JOptionPane.OK_OPTION != ComponentDlg.show( owner, "Выбор шаблона", ppp, null ) ) return null;
		List<ProfilePattern> lpp = ppp.getSelectedProfilePattern();
		if ( ( lpp == null ) || ( lpp.size() != 1 ) || ( lpp.get( 0 ).getStatus() != ProfilePattern.STATUS_ACTIVE ) ) {
			WebOptionPane.showMessageDialog( owner, "Нужно выбрать только один активированный шаблон", "Ошибка", 
					WebOptionPane.ERROR_MESSAGE );
			return null;
		}
		return lpp.get( 0 );		
	}

	/**
	 * delete profile
	 * @return true if deleted successfully
	 * @throws Exception
	 */
	private boolean delProfile() throws Exception {
		if ( tabbedPane.getSelectedIndex() < 0 ) return false;
		if ( !AuxFunctions.isPermission( Block.PROFILE, Operation.DELETE ) ) {
			throw new AccessControlException( "PostProfilePanel.delProfile() : delete Profile permission denied"  );
		}
		if ( !AuxFunctions.checkRightSelect( owner, tabbedPane.getSelectedIndex() == 0 ? treeEP : treeP, false, true, 
				"Нужно выделить профили для удаления", Profile.class ) ) return false;
		List<Profile> lp = new ArrayList<>();
		for ( AsyncUniqueNode node : ( tabbedPane.getSelectedIndex() == 0 ? treeEP : treeP ).getSelectedNodes() ) {
			if ( ( node == null ) || ( node.getUserObject() == null ) 
					|| !( node.getUserObject() instanceof Profile ) ) continue;
			lp.add( ( Profile ) node.getUserObject() );
		}
		if ( lp.size() == 0 ) return false;
		OrmHelper.executeAsTransaction( this, new Unit<List<Profile>>( lp ) );		
		return true;
	}
	
	private void reloadSelectedNode( WebAsyncTree<MainTreeNode> tree ) {
		if ( tree.getSelectionCount() < 1 ) return;
		for ( MainTreeNode node : tree.getSelectedNodes() ) {
			while ( true ) {
				if ( ( node.getUserObject() instanceof EstablishedPost ) 
						|| ( node.getUserObject() instanceof EPostGroup )
						|| ( node.getUserObject() instanceof Post ) ) {
					tree.reloadNode( node );
					break;
				} else {
					node = ( MainTreeNode ) node.getParent();
					if ( node == null ) break;
				}
			}
		}
	}
}