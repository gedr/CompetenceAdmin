package minos.ui.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import minos.data.services.ORMHelper;
import minos.entities.Division;
import minos.entities.EstablishedPost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.tree.ChildsListener;

public class EstablishedPostDataProvider extends BasisDataProvider<MainTreeNode> {
	private static Logger log = LoggerFactory.getLogger( ProfilePatternDataProvider.class );
		
	private static final String jpqlLoadEstablishedPosts = " select entity from EstablishedPost entity join fetch entity.name "
			+ " where ( entity.division.id = :did )"
			+ " and ( ( :ts between entity.beginDate and entity.endDate ) %s) "
			+ " and ( ( entity.isdelete = 0 ) %s )"
			+ " and ( ( entity.otizOk = 1 ) %s )"
			+ " order by entity.kpers";
	
	private static final String jpqlLoadOverdueEstablishedPosts = " or ( entity.endDate < :ts ) ";
	private static final String jpqlLoadDeletedEstablishedPosts = " or ( entity.isdelete = 1 ) ";
	private static final String jpqlLoadDisapproveEstablishedPosts = " or ( entity.otizOk = 0 ) ";

	private boolean visibleOverdueEstablishedPosts = false;
	private boolean visibleDeletedEstablishedPosts = false;
	private boolean visibleDisapproveEstablishedPosts = true;
	private boolean catalogBeforeEstablishedPost = true;
	private String jpql;

	private DivisionDataProvider ddp;

	private void rebuildJpqlStatment() {
		jpql = String.format(jpqlLoadEstablishedPosts, 
				( !visibleOverdueEstablishedPosts ? " " : jpqlLoadOverdueEstablishedPosts ),
				( !visibleDeletedEstablishedPosts ? " " : jpqlLoadDeletedEstablishedPosts ),
				( !visibleDisapproveEstablishedPosts ? " " : jpqlLoadDisapproveEstablishedPosts ) );
		System.out.println( jpql );
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
		
		// load sub catalog
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

		List<MainTreeNode> lst = new ArrayList<>();
		if ( catalogBeforeEstablishedPost ) lst.addAll( siblings[0] );		
		for ( EstablishedPost ep : res ) lst.add( new MainTreeNode( ep ) );
		if ( !catalogBeforeEstablishedPost ) lst.addAll( siblings[0] );		
		listener.childsLoadCompleted( lst );
	}

	@Override
	public void setCurrentTimePoint(Timestamp timePoint) {
		super.setCurrentTimePoint(timePoint);
		if ( ddp != null ) ddp.setCurrentTimePoint( timePoint );
	}
}