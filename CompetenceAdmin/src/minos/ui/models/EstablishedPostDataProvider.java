package minos.ui.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import minos.data.services.ORMHelper;
import minos.entities.Division;
import minos.entities.EstablishedPost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.tree.ChildsListener;

public class EstablishedPostDataProvider extends BasisDataProvider<MainTreeNode> {
	private static Logger log = LoggerFactory.getLogger( ProfilePatternDataProvider.class );
		
	private static final String jpqlLoadEstablishedPosts = " select entity from EstablishedPost entity join fetch entity.name join fetch entity.basisPost "
			+ " where ( entity.division.id = :did )"
			+ " and ( ( :ts between entity.beginDate and entity.endDate ) %s) "
			+ " and ( ( entity.isdelete = 0 ) %s )"
			+ " and ( ( entity.otizOk = 1 ) %s )"
			+ " order by entity.name ";
	
	private static final String jpqlLoadOverdueEstablishedPosts = " or ( entity.endDate < :ts ) ";
	private static final String jpqlLoadDeletedEstablishedPosts = " or ( entity.isdelete = 1 ) ";
	private static final String jpqlLoadDisapproveEstablishedPosts = " or ( entity.otizOk = 0 ) ";

	private boolean visibleOverdueEstablishedPosts = false;
	private boolean visibleDeletedEstablishedPosts = false;
	private boolean visibleDisapproveEstablishedPosts = true;
	private boolean catalogBeforeEstablishedPost = true;
	private boolean visibleGroupEstablishedPost = true;
	private String jpql; 

	private DivisionDataProvider ddp;

	private void rebuildJpqlStatment() {
		jpql = String.format(jpqlLoadEstablishedPosts, 
				( !visibleOverdueEstablishedPosts ? " " : jpqlLoadOverdueEstablishedPosts ),
				( !visibleDeletedEstablishedPosts ? " " : jpqlLoadDeletedEstablishedPosts ),
				( !visibleDisapproveEstablishedPosts ? " " : jpqlLoadDisapproveEstablishedPosts ) );
	}
	
	private boolean equalEstablishedPost( EstablishedPost val1, EstablishedPost val2 ) {
		if ( val1.getId() == val2.getId() ) return true;
		if ( ( val1.getBasisPost().getId() != val2.getBasisPost().getId() ) || ( val1.getFaset2() != val2.getFaset2() ) ||
				( val1.getFaset3() != val2.getFaset3() ) || ( val1.getFaset7() != val2.getFaset7() ) ||
				( val1.getFaset11() != val2.getFaset11() ) || ( val1.getFaset12() != val2.getFaset12() ) ||
				( val1.getFaset99() != val2.getFaset99() ) ) return false;
		return true;		
	}
	
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
					EstablishedPostGroup epg = new EstablishedPostGroup();
					epg.addEstablishedPost( ( EstablishedPost ) obj );
					epg.addEstablishedPost( ep );
					objs.set( i, epg );
					findOK = true;
					break;
				}
				if ( ( obj instanceof EstablishedPostGroup ) && 
						equalEstablishedPost( ( ( EstablishedPostGroup ) obj ).getEstablishedPosts().get( 0 ) ,  ep ) ) {
					( ( EstablishedPostGroup ) obj ).addEstablishedPost( ep );
					findOK = true;
					break;
				}
			}				
			if ( !findOK ) objs.add( ep );
		}
		return objs;
	}

	public static class EstablishedPostGroup {
		private List<EstablishedPost> establishedPosts = null;
		
		public EstablishedPostGroup() { }
		
		public EstablishedPostGroup(List<EstablishedPost> lst) { 
			this.establishedPosts = lst;
		}

		public List<EstablishedPost> getEstablishedPosts() {
			return establishedPosts;
		}

		public void setEstablishedPosts( List<EstablishedPost> lst ) {
			this.establishedPosts = lst;
		}
		
		public void addEstablishedPost( EstablishedPost val ) {
			if ( establishedPosts == null ) establishedPosts = new ArrayList<>();
			establishedPosts.add( val );
		}

		public String getName() {
			return ( ( ( establishedPosts == null ) || ( establishedPosts.size() == 0 ) ) ? " " : establishedPosts.get(0).getName() );		
		}

		public int getKpers() {
			return ( ( ( establishedPosts == null ) || ( establishedPosts.size() == 0 ) ) ? -1 : establishedPosts.get(0).getKpers() );		
		}

		@Override
		public String toString() {
			return "EstablishedPostGroup: " + getName() + "  <" + String.valueOf( establishedPosts == null ? 0 : establishedPosts.size() ) + ">";
		}
	}
	
	public EstablishedPostDataProvider( DivisionDataProvider ddp ) throws Exception {
		this( ddp, null );
	}
	
	public EstablishedPostDataProvider( DivisionDataProvider ddp, Timestamp timePoint ) throws NullPointerException {
		super( timePoint );
		if ( ddp == null ) {
			NullPointerException e = new NullPointerException( "EstablishedPostDataProvider.EstablishedPostDataProvider() : DivisionDataProvider is null" );
			if ( ( log != null) && log.isErrorEnabled() ) log.error( " EstablishedPostDataProvider.EstablishedPostDataProvider() : ", e );
			throw e;
		}
		this.ddp = ddp;		
		rebuildJpqlStatment();
	}

	@Override
	public MainTreeNode getRoot() {
		return ddp.getRoot();
	}

	@Override
	public boolean isLeaf(MainTreeNode node) {
		if ( ( node.getUserObject() instanceof EstablishedPost ) || 
				( node.getUserObject() instanceof EstablishedPostGroup ) ) return true;
		return false;
	}

	@Override
	public void loadChilds(MainTreeNode node, final ChildsListener<MainTreeNode> listener) {
		if ( !( node.getUserObject() instanceof Division ) ) {
			String errmsg = "EstablishedPostDataProvider.loadChilds() : unknown node type: " + node.getUserObject();
			if( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			listener.childsLoadFailed( new IllegalArgumentException( errmsg ) );
			return;
		}
		
		// load sub division
		@SuppressWarnings("unchecked")
		final List<MainTreeNode>[] siblings = ( List<MainTreeNode>[] ) new List[] { null };
		ddp.loadChilds( node, new ChildsListener<MainTreeNode>() {

			@Override
			public void childsLoadFailed(Throwable cause) {
				listener.childsLoadFailed( cause );
			}

			@Override
			public void childsLoadCompleted(List<MainTreeNode> childs) {
				siblings[0] = childs;				
			}
		} );		
		if ( siblings[0] == null ) return;

		// load established post for catalog 
		ORMHelper.openManager();
		List<EstablishedPost> res = ORMHelper.getCurrentManager().createQuery( jpql, EstablishedPost.class ).
				setParameter( "ts", getCurrentTimePoint() ).
				setParameter( "did", ( ( Division ) node.getUserObject() ).getId() ).getResultList();
		ORMHelper.closeManager();
		
		if ( ( res == null ) || ( res.size() == 0 ) ) {
			listener.childsLoadCompleted( siblings[0] );
			return;
		}
		
		List<Object> objs = null;
		if ( visibleGroupEstablishedPost ) objs = groupEstablishedPost( res );

		List<MainTreeNode> lst = new ArrayList<>();
		if ( catalogBeforeEstablishedPost ) lst.addAll( siblings[0] );		
		for ( Object o : ( visibleGroupEstablishedPost ? objs : res ) ) lst.add( new MainTreeNode( o ) );
		if ( !catalogBeforeEstablishedPost ) lst.addAll( siblings[0] );		
		listener.childsLoadCompleted( lst );
	}

	@Override
	public void setCurrentTimePoint( Timestamp timePoint ) {
		super.setCurrentTimePoint( timePoint );
		if ( ddp != null ) ddp.setCurrentTimePoint( timePoint );
	}
	
	public boolean isVisibleOverdueEstablishedPosts() {
		return visibleOverdueEstablishedPosts;
	}

	public void setVisibleOverdueEstablishedPosts( boolean val ) {
		if ( this.visibleOverdueEstablishedPosts != val ) {
			this.visibleOverdueEstablishedPosts = val;
			rebuildJpqlStatment();
		}		
	}

	public boolean isVisibleDeletedEstablishedPosts() {
		return visibleDeletedEstablishedPosts;
	}

	public void setVisibleDeletedEstablishedPosts( boolean val ) {
		if ( this.visibleDeletedEstablishedPosts != val ) {
			this.visibleDeletedEstablishedPosts = val;
			rebuildJpqlStatment();
		}		
	}

	public boolean isVisibleDisapproveEstablishedPosts() {
		return visibleDisapproveEstablishedPosts;
	}

	public void setVisibleDisapproveEstablishedPosts( boolean val ) {
		if ( this.visibleDisapproveEstablishedPosts != val ) {
			this.visibleDisapproveEstablishedPosts = val;
			rebuildJpqlStatment();
		}		
	}

	public boolean isCatalogBeforeEstablishedPost() {
		return catalogBeforeEstablishedPost;
	}

	public void setCatalogBeforeEstablishedPost( boolean val ) {
		if ( this.catalogBeforeEstablishedPost != val ) {
			this.catalogBeforeEstablishedPost = val;
			rebuildJpqlStatment();
		}		
	}

	public boolean isVisibleGroupEstablishedPost() {
		return visibleGroupEstablishedPost;
	}

	public void setVisibleGroupEstablishedPost( boolean val ) {
		if ( this.visibleGroupEstablishedPost != val ) {
			this.visibleGroupEstablishedPost = val;
			rebuildJpqlStatment();
		}		
	}
}