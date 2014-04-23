package minos.ui.models;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import minos.data.services.ORMHelper;
import minos.entities.Division;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.tree.ChildsListener;

public class DivisionDataProvider extends BasisDataProvider<MainTreeNode> {
	private static Logger log = LoggerFactory.getLogger( CatalogDataProvider.class );
	private MainTreeNode root;

	private static final String jpqlLoadDivisions = " select entity from Division entity join fetch entity.fullName "
			+ " where ( entity.parentDivision.id = :pdid )"
			+ " and ( ( :ts between entity.beginDate and entity.endDate ) %s) "
			+ " and ( ( entity.isdelete = 0 ) %s )"
			+ " and ( ( entity.otizOk = 1 ) %s )"
			+ " order by entity.fullName";
	
	private static final String jpqlLoadOverdueDivisions = " or ( entity.endDate < :ts ) ";
	private static final String jpqlLoadDeletedDivisions = " or ( entity.isdelete = 1 ) ";
	private static final String jpqlLoadDisapproveDivisions = " or ( entity.otizOk = 0 ) ";
	
	private boolean visibleOverdueDivisions = false;
	private boolean visibleDeletedDivisions = false;
	private boolean visibleDisapproveDivisions = false;
	private String jpql;

	private void rebuildJpqlStatment() {
		jpql = String.format(jpqlLoadDivisions, 
				( !visibleOverdueDivisions ? " " : jpqlLoadOverdueDivisions ),
				( !visibleDeletedDivisions ? " " : jpqlLoadDeletedDivisions ),
				( !visibleDisapproveDivisions ? " " : jpqlLoadDisapproveDivisions ) );
	}
	
	public DivisionDataProvider( int rootID ) throws Exception {
		this( rootID, null );
	}

	
	public DivisionDataProvider( int rootID, Timestamp timePoint ) throws Exception {
		super( timePoint );
		Division d = ( Division ) ORMHelper.findEntity( Division.class, rootID );
		if ( d == null ) {
			Exception e = new IllegalArgumentException( " DivisionDataProvider.DivisionDataProvider() rootID is wrong " ); 
			if ( (log != null ) && log.isErrorEnabled() ) {
				log.error( "DivisionDataProvider.DivisionDataProvider() cannot find entity Division for rootID=" + rootID , e );
			}
			throw e;
		}		
		root = new MainTreeNode( d );
		rebuildJpqlStatment();
	}

	@Override
	public MainTreeNode getRoot() {		
		return root;
	}

	@Override
	public boolean isLeaf(MainTreeNode node) {
		return false;
	}

	@Override
	public void loadChilds(MainTreeNode node, ChildsListener<MainTreeNode> listener) {
		if ( !( node.getUserObject() instanceof Division ) ) {
			String errmsg = "DivisionDataProvider.loadChilds() take wrong node :" + node;
			if ( ( log != null ) && log.isErrorEnabled() )  log.error( errmsg );
			listener.childsLoadFailed( new IllegalArgumentException( errmsg ) );
		}
		
		ORMHelper.openManager();
		List<Division> res = ORMHelper.getCurrentManager().createQuery( jpql, Division.class ).
				setParameter( "ts", getCurrentTimePoint() ).
				setParameter( "pdid", ( ( Division ) node.getUserObject() ).getId() ).getResultList() ;		
		ORMHelper.closeManager();
		if ( (res == null) || (res.size() == 0) ) {
			listener.childsLoadCompleted( BASIS_EMPTY_LIST );
			return ;
		}		
		List<MainTreeNode> lst = new ArrayList<>();
		for ( Division d : res ) lst.add( new MainTreeNode( d ) );		
		listener.childsLoadCompleted( lst );		
	}

	public boolean isVisibleOverdueDivisions() {
		return visibleOverdueDivisions;
	}

	public void setVisibleOverdueDivisions(boolean visibleOverdueDivisions) {
		this.visibleOverdueDivisions = visibleOverdueDivisions;
		rebuildJpqlStatment();
	}

	public boolean isVisibleDeletedDivisions() {
		return visibleDeletedDivisions;
	}

	public void setVisibleDeletedDivisions(boolean visibleDeletedDivisions) {
		this.visibleDeletedDivisions = visibleDeletedDivisions;
		rebuildJpqlStatment();
	}

	public boolean isVisibleDisapproveDivisions() {
		return visibleDisapproveDivisions;
	}

	public void setVisibleDisapproveDivisions(boolean visibleDisapproveDivisions) {
		this.visibleDisapproveDivisions = visibleDisapproveDivisions;
		rebuildJpqlStatment();
	}
}