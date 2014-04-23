package minos.ui.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import minos.data.services.ORMHelper;
import minos.entities.Catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.tree.ChildsListener;

public class CatalogDataProvider extends BasisDataProvider<MainTreeNode> {	
	private static final int rootId = 1;	
	
	private static final String jpqlLoadCatalogs = " select entity from Catalog entity join fetch entity.name "
			+ " where entity.variety %s "
			+ " and ( (:ts between entity.journal.editMoment and entity.journal.deleteMoment) %s) "
			+ " and entity.parentCatalog.id = :pcid order by entity.item";

	private static final String jpqlLoadDeletedCatalogs = " or ( entity.journal.deleteMoment < :ts and entity.status = 2) ";
	private static Logger log = LoggerFactory.getLogger( CatalogDataProvider.class );
	
	public static enum CatalogType { 
		COMPETENCE_CATALOG( " in (1, 2, 3)" ), PROFILE_PATTERN_CATALOG( "= 0" );
		String value;
		CatalogType(String value) { this.value = value; }
		public String getValue() { return value; }
	};	
	
	private String jpql;
	private CatalogType ctype = CatalogType.COMPETENCE_CATALOG;
	private boolean visibleDeleteCatalogs = false;	
	private MainTreeNode root = new MainTreeNode( ( Catalog ) ORMHelper.findEntity( Catalog.class, rootId ) );
	
	private void rebuildJpqlStatment() {
		jpql = String.format(jpqlLoadCatalogs, ctype.getValue(),
				( !visibleDeleteCatalogs ? " " : jpqlLoadDeletedCatalogs ) );
	}

	public CatalogDataProvider() {
		super();
		setCurrentTimePoint( null );
		rebuildJpqlStatment();
	}
	
	public CatalogDataProvider(Timestamp timePoint) {
		super();
		setCurrentTimePoint( timePoint );	
		rebuildJpqlStatment();
	}

	public CatalogDataProvider(CatalogType ctype) {
		super();
		setCurrentTimePoint( null );
		setCatalogType( ctype );
		rebuildJpqlStatment();
	}
	
	@Override
	public MainTreeNode getRoot() {
		return root;
	}

	@Override
	public void loadChilds(MainTreeNode node, ChildsListener<MainTreeNode> listener) {
		if ( !( node.getUserObject() instanceof Catalog ) ) {
			String errmsg = "CatalogDataProvider.loadChilds() take wrong node :" + node;
			if ( ( log != null ) && log.isErrorEnabled() )  log.error( errmsg );
			listener.childsLoadFailed( new IllegalArgumentException( errmsg ) );
		}		
		ORMHelper.openManager();
		List<Catalog> res = ORMHelper.getCurrentManager().createQuery( jpql, Catalog.class ).
				setParameter( "ts", getCurrentTimePoint() ).
				setParameter( "pcid", ( ( Catalog ) node.getUserObject() ).getId() ).getResultList() ;		
		ORMHelper.closeManager();
		if ( (res == null) || (res.size() == 0) ) {
			listener.childsLoadCompleted( BASIS_EMPTY_LIST );
			return ;
		}		
		List<MainTreeNode> lst = new ArrayList<>();
		for ( Catalog c : res ) lst.add( new MainTreeNode( c ) );		
		listener.childsLoadCompleted( lst );		
	}

	@Override
	public boolean isLeaf(MainTreeNode node) {		
		return false;
	}

	public void setVisibleDeleteCatalogs(boolean visibleDeleteCatalogs) {
		this.visibleDeleteCatalogs = visibleDeleteCatalogs;
		rebuildJpqlStatment();
	}

	public boolean isVisibleDeleteCatalogs() {
		return visibleDeleteCatalogs;
	}

	public void setCatalogType(CatalogType ctype) {
		this.ctype = ctype;
		rebuildJpqlStatment();
	}

	public CatalogType getCatalogType() {
		return ctype;
	}
}