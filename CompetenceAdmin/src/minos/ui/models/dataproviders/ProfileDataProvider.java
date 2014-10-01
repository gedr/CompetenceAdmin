package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Division;
import minos.entities.EstablishedPost;
import minos.entities.Post;
import minos.entities.Profile;
import minos.ui.models.MainTreeNode;
import minos.ui.models.dataproviders.EPostDataProvider.EPostGroup;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.QType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.alee.extended.tree.ChildsListener;

public class ProfileDataProvider extends BasisDataProvider<MainTreeNode> {	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( ProfileDataProvider.class );
	private BasisDataProvider<MainTreeNode> bdp = null;	

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ProfileDataProvider( BasisDataProvider<MainTreeNode> bdp ) {		
		this ( null, bdp );
	}

	public ProfileDataProvider( Timestamp timepoint, BasisDataProvider<MainTreeNode> bdp ) {
		super( timepoint );
		if ( bdp == null ) {
			String errmsg = "ProfileDataProvider() : bdp is null";
			if ( ( log != null) && log.isErrorEnabled() ) log.error( errmsg );
			throw new NullPointerException( errmsg );
		}
		this.bdp = bdp;		
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void setCurrentTimePoint(Timestamp timePoint) {		
		bdp.setCurrentTimePoint( timePoint );
	}

	@Override
	public MainTreeNode getRoot() {
		return bdp.getRoot();
	}

	@Override
	public boolean isLeaf( MainTreeNode node ) {
		return ( ( ( node == null ) || ( node.getUserObject() == null ) ) ? true 
				: ( node.getUserObject() instanceof Profile  ? true : false ) );
	}

	@Override
	public void loadChilds( MainTreeNode node, final ChildsListener<MainTreeNode> listener ) {
		try {
			if ( ( node == null ) || ( node.getUserObject() == null ) ) {
				throw new IllegalArgumentException( "ProfileDataProvider.loadChilds() : node is illegal" );
			}
			if ( node.getUserObject() instanceof EstablishedPost ) {
				loadPFEP( node, listener );
				return;
			}
			if ( node.getUserObject() instanceof EPostGroup ) {
				loadPFGEP( node, listener );
				return;
			}
			if ( node.getUserObject() instanceof Post ) {
				loadPFP( node, listener );
				return;
			}
			bdp.loadChilds(node, listener);
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ProfileDataProvider.loadChilds() : ", ex );
			listener.childsLoadFailed( ex );
		}
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * load Profile entities for EstablishedPost parent node
	 * @param node - current node
	 * @param listener - event listener
	 */
	private void loadPFEP( MainTreeNode node, final ChildsListener<MainTreeNode> listener ) {
		List<EstablishedPost> lep = new ArrayList<>();
		lep.add( ( EstablishedPost ) node.getUserObject() );
		List<Profile> res = loadProfile( lep );
		listener.childsLoadCompleted( ( ( res == null ) || ( res.size() == 0 ) ) ? BASIS_EMPTY_LIST : convertList( res ) );
	}	

	/**
	 * load Profile elements for EPostGroup parent node
	 * @param node - current node
	 * @param listener - event listener
	 */
	private void loadPFGEP( MainTreeNode node, final ChildsListener<MainTreeNode> listener ) {
		List<EstablishedPost> lep = ( ( EPostGroup ) node.getUserObject() ).getEstablishedPosts();
		if ( ( lep == null ) || ( lep.size() == 0 ) ) {
			listener.childsLoadFailed(new IllegalArgumentException( "ProfileDataProvider.loadPFGEP() :"
					+ " EPostGroup have empty list" ) );
			return;
		}
		List<Profile> res = loadProfile( lep );
		if ( ( res == null ) || ( res.size() == 0 ) ) {
			listener.childsLoadCompleted( BASIS_EMPTY_LIST );
			return;
		}
		List<MainTreeNode> lst = new ArrayList<>();
		for ( Profile p : res ) {
			boolean found = false;
			for ( MainTreeNode mtn : lst ) {
				if ( ( ( Profile ) mtn.getUserObject() ).getProfilePattern().getId() != p.getProfilePattern().getId() ) continue;
				found = true;
				break;
			}
			if ( !found ) lst.add( new MainTreeNode( p ) );
		}
		listener.childsLoadCompleted( lst );
	}

	/**
	 * load Profile entities for Post parent node
	 * @param node - current node
	 * @param listener - event listener
	 */
	private void loadPFP( MainTreeNode node, final ChildsListener<MainTreeNode> listener ) {
		List<Post> lp = new ArrayList<>();
		lp.add( ( Post ) node.getUserObject() );
		List<Division> ld = new ArrayList<>();
		ld.add( ( Division ) node.getParent().getUserObject() );
		List<Profile> res = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_PROFILE_BY_POST ),				
				Profile.class, 
				new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
				new Pair<Object, Object>( "posts", lp ),
				new Pair<Object, Object>( "divisions", ld ) );
		listener.childsLoadCompleted( ( ( res == null ) || ( res.size() == 0 ) ) ? BASIS_EMPTY_LIST : convertList( res ) );
	}	

	
	private List<Profile> loadProfile( List<EstablishedPost> lep ) {
		return OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_PROFILE_BY_EPOST ),				
				Profile.class, 
				new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
				new Pair<Object, Object>( "eposts", lep ) );
	}
}