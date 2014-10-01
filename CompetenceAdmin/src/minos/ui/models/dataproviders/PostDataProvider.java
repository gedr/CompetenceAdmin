package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.alee.extended.tree.ChildsListener;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Division;
import minos.entities.OrgUnit;
import minos.entities.Post;
import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

public class PostDataProvider extends BasisDataProvider<MainTreeNode> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final Logger log = LoggerFactory.getLogger( EPostDataProvider.class );
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private DivisionDataProvider ddp = null;
	private boolean divisionsBeforeOrgUnit 	= true;
	private boolean visibleOverdueOrgUnit	= false;  
	private boolean visibleFired			= false;
	private boolean visibleTemporary		= false;
	private boolean visibleOverduePost		= false;
	private boolean visibleDeletePost		= false;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public PostDataProvider( DivisionDataProvider ddp ) throws Exception {
		super( null );
		if ( ddp == null ) {
			String errmsg = "OrgUnitDataProvider() : ddp is null";
			if ( ( log != null) && log.isErrorEnabled() ) log.error( errmsg );
			throw new NullPointerException( errmsg );
		}
		this.ddp = ddp;		
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean isDivisionsBeforeOrgUnit() {
		return divisionsBeforeOrgUnit;
	}

	public void setDivisionsBeforeOrgUnit( boolean val ) {
		this.divisionsBeforeOrgUnit = val;
	}

	public boolean isVisibleOverdueOrgUnit() {
		return visibleOverdueOrgUnit;
	}

	public void setVisibleOverdueOrgUnit( boolean val ) {
		this.visibleOverdueOrgUnit = val;
	}

	public boolean isVisibleFired() {
		return visibleFired;
	}

	public void setVisibleFired( boolean val ) {
		this.visibleFired = val;
	}

	public boolean isVisibleTemporary() {
		return visibleTemporary;
	}

	public void setVisibleTemporary( boolean val ) {
		this.visibleTemporary = val;
	}

	public boolean isVisibleOverduePost() {
		return visibleOverduePost;
	}

	public void setVisibleOverduePost( boolean val ) {
		this.visibleOverduePost = val;
	}

	public boolean isVisibleDeletePost() {
		return visibleDeletePost;
	}

	public void setVisibleDeletePost( boolean val ) {
		this.visibleDeletePost = val;
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
	public boolean isLeaf( MainTreeNode node ) {
		return ( ( ( node == null ) || ( node.getUserObject() == null ) ) ? true 
				: ( ( node.getUserObject() instanceof Post )  ? true : false )  );
	}

	@Override
	public void loadChilds( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		String jpql = " SELECT entity FROM OrgUnit entity "
				+ " INNER JOIN FETCH entity.post "
				+ " WHERE entity.division IN (:divisions) "
				+ "    AND entity.state IN (:states) "
				+ "    AND entity.typeTD IN (:types) "
				+ "    AND entity.post.isDeleted IN (:pdel) "
				+ "    AND ( ( CURRENT_TIMESTAMP BETWEEN entity.beginDate AND entity.endDate ) "
				+ "    OR ( :oudts > entity.endDate ) )"
				+ "    AND ( ( CURRENT_TIMESTAMP BETWEEN entity.post.beginDate AND entity.post.endDate )  "
				+ "    OR ( :pdts > entity.post.endDate ) )"
				;
		
		try {
			if ( ( node == null ) || ( node.getUserObject() == null ) ) {
				throw new IllegalArgumentException( "PostDataProvider.loadChilds() : node is illegal" );
			}
			if ( !( node.getUserObject() instanceof Division ) ) {
				ddp.loadChilds( node, listener );
				return;
			}
			List<MainTreeNode> subDivisions = loadSubDivisions( node, listener );
			if ( subDivisions == null ) return;

			Timestamp now = new Timestamp( System.currentTimeMillis() );
			List<OrgUnit> res = OrmHelper.findByQueryWithParam( QueryType.JPQL,
					jpql,
					OrgUnit.class,
					new Pair<Object, Object>( "divisions", node.getUserObject() ),
					new Pair<Object, Object>( "states", Arrays.asList( OrgUnit.STATE_ACTIVE, 
							visibleFired ? OrgUnit.STATE_FIRED : OrgUnit.STATE_ACTIVE ) ),
					new Pair<Object, Object>( "types", Arrays.asList( OrgUnit.TYPE_PERMANENTLY, 
							visibleTemporary ? OrgUnit.TYPE_TEMPORARY : OrgUnit.TYPE_PERMANENTLY ) ),
					new Pair<Object, Object>( "pdel", Arrays.asList( 0, 
							visibleDeletePost ? 1 : 0 ) ),
					new Pair<Object, Object>( "oudts", 
							visibleOverdueOrgUnit ? now : ResourceKeeper.getObject( OType.WAR ) ),
					new Pair<Object, Object>( "pdts", 
							visibleOverduePost ? now : ResourceKeeper.getObject( OType.WAR ) ) );
			
			List<MainTreeNode> lst = null;
			if ( ( res != null ) && ( res.size() > 0 ) ) {
				List<Post> lp = new ArrayList<>();
				for ( OrgUnit ou : res ) {
					if ( !lp.contains( ou.getPost() ) ) lp.add( ou.getPost() );
				}
				lst = joinLists( divisionsBeforeOrgUnit ? subDivisions : convertList( lp ), 
						divisionsBeforeOrgUnit ? convertList( lp ) : subDivisions );
			}
			listener.childsLoadCompleted( ( lst == null ) ? subDivisions : lst  );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "OrgUnitDataProvider.loadChilds() : ", ex );
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
}
