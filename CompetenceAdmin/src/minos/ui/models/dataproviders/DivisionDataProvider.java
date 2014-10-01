package minos.ui.models.dataproviders;


import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Division;
import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.alee.extended.tree.ChildsListener;

public class DivisionDataProvider extends BasisDataProvider<MainTreeNode> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final List<Integer> ALIVE_DIVISIONS 					= Arrays.asList( 0 );
	private static final List<Integer> ALIVE_AND_DELETED_DIVISIONS 		= Arrays.asList( 0, 1 );
	private static final List<Integer> APPROVE_DIVISIONS 				= Arrays.asList( 1 );
	private static final List<Integer> APPROVE_AND_DISAPPROVE_DIVISIONS = Arrays.asList( 1, 0 );

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( CatalogDataProvider.class );
	private MainTreeNode root;
	private boolean visibleOverdueDivisions = false;
	private boolean visibleDeletedDivisions = false;
	private boolean visibleDisapproveDivisions = false;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public DivisionDataProvider( int rootID ) throws Exception {
		this( rootID, null );
	}

	public DivisionDataProvider( int rootID, Timestamp timePoint ) throws Exception {
		super( timePoint );
		Division d = ( Division ) OrmHelper.findEntity( Division.class, rootID );
		if ( d == null ) {
			String errmsg = "DivisionDataProvider() : cannot find entity Division for rootID=" + rootID ;
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalArgumentException( errmsg );
		}		
		root = new MainTreeNode( d );
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean isVisibleOverdueDivisions() {
		return visibleOverdueDivisions;
	}

	public void setVisibleOverdueDivisions(boolean visibleOverdueDivisions) {
		this.visibleOverdueDivisions = visibleOverdueDivisions;
	}

	public boolean isVisibleDeletedDivisions() {
		return visibleDeletedDivisions;
	}

	public void setVisibleDeletedDivisions(boolean visibleDeletedDivisions) {
		this.visibleDeletedDivisions = visibleDeletedDivisions;
	}

	public boolean isVisibleDisapproveDivisions() {
		return visibleDisapproveDivisions;
	}

	public void setVisibleDisapproveDivisions(boolean visibleDisapproveDivisions) {
		this.visibleDisapproveDivisions = visibleDisapproveDivisions;
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public MainTreeNode getRoot() {		
		return root;
	}

	@Override
	public boolean isLeaf(MainTreeNode node) {
		return false;
	}

	@Override
	public void loadChilds( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		try {
			if ( ( node == null ) || ( node.getUserObject() == null ) 
					|| !( node.getUserObject() instanceof Division ) ) 
				throw new IllegalArgumentException( "DivisionDataProvider.loadChilds() take wrong node :" + node );

			List<Division> res = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
					ResourceKeeper.getQuery( QType.JPQL_LOAD_DIVISIONS ), 
					Division.class,
					new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
					new Pair<Object, Object>( "divisions", Arrays.asList( ( Division ) node.getUserObject() ) ),
					new Pair<Object, Object>( "overdue", visibleOverdueDivisions ? getCurrentTimePoint() 
							: ResourceKeeper.getObject( OType.WAR ) ),
					new Pair<Object, Object>( "del", visibleDeletedDivisions ? ALIVE_AND_DELETED_DIVISIONS 
							: ALIVE_DIVISIONS ),
					new Pair<Object, Object>( "disaprv", visibleDisapproveDivisions ? APPROVE_AND_DISAPPROVE_DIVISIONS 
							: APPROVE_DIVISIONS ) );
			listener.childsLoadCompleted( ( (res == null) || ( res.size() == 0 ) ) ? BASIS_EMPTY_LIST 
					: convertList( res ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "DivisionDataProvider.loadChilds() : ", ex );
			listener.childsLoadFailed( ex ); 
		}		
	}
}