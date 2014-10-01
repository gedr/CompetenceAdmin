package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Division;
import minos.entities.EstablishedPost;
import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.alee.extended.tree.ChildsListener;

public class EPostDataProvider extends BasisDataProvider<MainTreeNode> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final List<Integer> ONLY_ALIVE_EPOSTS	= Arrays.asList( 0 );
	private static final List<Integer> ALL_EPOSTS 			= Arrays.asList( 0, 1 );
	private static final List<Integer> ONLY_APPROVE_EPOSTS 	= Arrays.asList( 1 );

	private static final Logger log = LoggerFactory.getLogger( EPostDataProvider.class );
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private DivisionDataProvider ddp = null;
	private boolean visibleOverdueEPosts 	= false;
	private boolean visibleDeletedEPosts	= false;
	private boolean visibleDisapproveEPosts = true;
	private boolean divisionsBeforeEPost 	= true;
	private boolean visibleGroupEPost 		= true;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public EPostDataProvider( DivisionDataProvider ddp ) throws Exception {
		this( ddp, null );
	}

	public EPostDataProvider( DivisionDataProvider ddp, Timestamp timePoint ) throws NullPointerException {
		super( timePoint );
		if ( ddp == null ) {
			String errmsg = "EPostDataProvider() : ddp is null";
			if ( ( log != null) && log.isErrorEnabled() ) log.error( errmsg );
			throw new NullPointerException( errmsg );
		}
		this.ddp = ddp;		
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean isVisibleOverdueEPosts() {
		return visibleOverdueEPosts;
	}

	public void setVisibleOverdueEPosts( boolean val ) {
		visibleOverdueEPosts = val;
	}

	public boolean isVisibleDeletedEPosts() {
		return visibleDeletedEPosts;
	}

	public void setVisibleDeletedEPosts( boolean val ) {
		visibleDeletedEPosts = val;
	}

	public boolean isVisibleDisapproveEPosts() {
		return visibleDisapproveEPosts;
	}

	public void setVisibleDisapproveEPosts( boolean val ) {
		visibleDisapproveEPosts = val;
	}

	public boolean isCatalogBeforeEPost() {
		return divisionsBeforeEPost;
	}

	public void setCatalogBeforeEPost( boolean val ) {
		divisionsBeforeEPost = val;
	}

	public boolean isVisibleGroupEPost() {
		return visibleGroupEPost;
	}

	public void setVisibleGroupEPost( boolean val ) {
		visibleGroupEPost = val;
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void setCurrentTimePoint( Timestamp timePoint ) {
		super.setCurrentTimePoint( timePoint );
		ddp.setCurrentTimePoint( timePoint );
	}

	@Override
	public MainTreeNode getRoot() {
		return ddp.getRoot();
	}

	@Override
	public boolean isLeaf(MainTreeNode node) {
		return ( ( ( node == null ) || ( node.getUserObject() == null ) ) ? true 
				: ( ( ( node.getUserObject() instanceof EstablishedPost ) || 
						( node.getUserObject() instanceof EPostGroup ) ) ? true : false ) );
	}

	@Override
	public void loadChilds(MainTreeNode node, final ChildsListener<MainTreeNode> listener) {
		try {
			if ( ( node == null ) || ( node.getUserObject() == null ) 
					|| !( node.getUserObject() instanceof Division ) ) 
				throw new IllegalArgumentException(  "EPostDataProvider.loadChilds() : unknown node " + node );

			List<MainTreeNode> subDivisions = loadSubDivisions( node, listener );
			if ( subDivisions == null ) return;

			List<EstablishedPost> res = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
					ResourceKeeper.getQuery( QType.JPQL_LOAD_EPOSTS ), 
					EstablishedPost.class,
					new Pair<Object, Object>( "did", ( ( Division ) node.getUserObject() ).getId() ),
					new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
					new Pair<Object, Object>( "overdue", visibleOverdueEPosts ? getCurrentTimePoint() 
							: ResourceKeeper.getObject( OType.WAR ) ),
							new Pair<Object, Object>( "del", visibleDeletedEPosts ? ALL_EPOSTS : ONLY_ALIVE_EPOSTS ),
									new Pair<Object, Object>( "disaprv", visibleDisapproveEPosts ? ALL_EPOSTS 
											: ONLY_APPROVE_EPOSTS ) );

			List<MainTreeNode> lst = null;
			if ( ( res != null ) && ( res.size() > 0 ) ) {
				lst = joinLists( divisionsBeforeEPost ? subDivisions : convertList( !visibleGroupEPost ? res 
						: groupEstablishedPost( res ) ), 
						divisionsBeforeEPost ? convertList( !visibleGroupEPost ? res : groupEstablishedPost( res ) ) 
								: subDivisions );
			}
			listener.childsLoadCompleted( ( lst == null ) ? subDivisions : lst  );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "EPostDataProvider.loadChilds() : ", ex );
			listener.childsLoadFailed( ex ); 
		}	
	}


	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * load sub divisions
	 * @param node - existing MainTreeNode node , what contain Division entity
	 * @param listener - existing tree's listener
	 * @return 
	 */
	private List<MainTreeNode> loadSubDivisions( MainTreeNode node,
			final ChildsListener<MainTreeNode> listener ) {
		LocalChildsListener<MainTreeNode> lcl = new LocalChildsListener<>();
		ddp.loadChilds( node, lcl );
		if ( !lcl.isOperationComplete() ) listener.childsLoadFailed( lcl.getCause() );
		return lcl.getChilds();
	}

	/**
	 * compare two EstablishedPost entity
	 * @param val1 - fist existing EstablishedPost entity
	 * @param val2 - second existing EstablishedPost entity
	 * @return true if val1 equal val2, otherwise false
	 */
	private boolean equalEstablishedPost( EstablishedPost val1, EstablishedPost val2 ) {
		if ( ( val1 == null ) || ( val2 == null ) 
				|| ( val1.getBasisPost() == null ) 
				|| ( val2.getBasisPost() == null ) ) return false;
		if ( val1.getId() == val2.getId() ) return true;
		if ( ( val1.getBasisPost().getId() != val2.getBasisPost().getId() ) 
				|| ( val1.getFaset2() != val2.getFaset2() ) 
				|| ( val1.getFaset3() != val2.getFaset3() ) 
				|| ( val1.getFaset7() != val2.getFaset7() ) 
				|| ( val1.getFaset11() != val2.getFaset11() ) 
				|| ( val1.getFaset12() != val2.getFaset12() ) 
				|| ( val1.getFaset99() != val2.getFaset99() ) ) return false;
		return true;		
	}

	/**
	 * group someone EstablishedPost's entities in one element
	 * @param lst - list of EstablishedPost's entities
	 * @return list of EstablishedPost or EPostGroup objects
	 */
	private List<Object> groupEstablishedPost( List<EstablishedPost> lst ) {
		if ( ( lst == null ) || ( lst.size() == 0 ) ) return Collections.emptyList();

		List<Object> objs = new ArrayList<>();
		for ( EstablishedPost ep : lst ) {
			if ( objs.size() == 0 ) {
				objs.add( ep );
				continue;
			}			

			boolean findOK = false;
			for ( int i = 0; i < objs.size(); i ++ ) {
				Object obj = objs.get( i );
				if ( ( obj instanceof EstablishedPost ) && 
						equalEstablishedPost( ( EstablishedPost ) obj,  ep ) ) {
					EPostGroup epg = new EPostGroup( ( EstablishedPost ) obj,  ep );
					objs.set( i, epg ); // replace EstablishedPost to EPostGroup object
					findOK = true;
					break;
				}
				if ( ( obj instanceof EPostGroup ) && 
						equalEstablishedPost( ( ( EPostGroup ) obj ).getEstablishedPosts().get( 0 ) ,  ep ) ) {
					( ( EPostGroup ) obj ).addEstablishedPost( ep );
					findOK = true;
					break;
				}
			}				
			if ( !findOK ) objs.add( ep );
		}
		return objs;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	public static class EPostGroup {
		// =================================================================================================================
		// Fields
		// =================================================================================================================
		private List<EstablishedPost> establishedPosts = null;

		// =================================================================================================================
		// Constructors
		// =================================================================================================================
		public EPostGroup() { }

		public EPostGroup( EstablishedPost... lst ) { 
			if ( ( lst == null ) || ( lst.length == 0 ) ) return;
			establishedPosts = new ArrayList<>();
			for ( EstablishedPost ep : lst ) establishedPosts.add( ep );
		}

		public EPostGroup( List<EstablishedPost> lst ) { 
			this.establishedPosts = lst;
		}

		// =================================================================================================================
		// Getter & Setter
		// =================================================================================================================
		public List<EstablishedPost> getEstablishedPosts() {
			return establishedPosts;
		}

		public void setEstablishedPosts( List<EstablishedPost> lst ) {
			this.establishedPosts = lst;
		}
		// =================================================================================================================
		// Methods for/from SuperClass/Interfaces
		// =================================================================================================================
		@Override
		public String toString() {
			return "EstablishedPostGroup: " + getName() + "  <" + String.valueOf( establishedPosts == null ? 0 
					: establishedPosts.size() ) + ">";
		}

		// =================================================================================================================
		// Methods
		// =================================================================================================================
		public void addEstablishedPost( EstablishedPost val ) {
			if ( establishedPosts == null ) establishedPosts = new ArrayList<>();
			establishedPosts.add( val );
		}

		public void addEstablishedPosts( EstablishedPost... lst ) {
			if ( ( lst == null ) || ( lst.length == 0 ) ) return;
			if ( establishedPosts == null ) establishedPosts = new ArrayList<>();
			for ( EstablishedPost ep : lst ) establishedPosts.add( ep );
		}

		public String getName() {
			return ( !checkExist() ? " " : establishedPosts.get(0).getName() );		
		}

		public boolean checkExist() {
			return ( ( establishedPosts != null ) && ( establishedPosts.size() > 0 ) );
		}
	}
}