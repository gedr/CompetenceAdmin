package minos.ui.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import minos.data.services.ORMHelper;
import minos.ui.models.TreeElement.TreeElementType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.tree.ChildsListener;

public class CatalogDataProvider extends BasisDataProvider<TreeElement> {	
	private static final int rootId = 1;	
	private static Logger log = LoggerFactory.getLogger( CatalogDataProvider.class );		
	
	private TreeElement root = new TreeElement(TreeElementType.CATALOG, rootId, rootId, (short)0, "must be invisible" );

	
	public CatalogDataProvider() {
		super();
		setCurrentTimePoint(null);
	}
	
	public CatalogDataProvider(Timestamp timePoint) {
		super();
		setCurrentTimePoint(timePoint);		
	}	
	
	@Override
	public TreeElement getRoot() {
		return root;
	}

	@Override
	public void loadChilds(TreeElement node, ChildsListener<TreeElement> listener) {
		if( (log != null) && log.isDebugEnabled() ) log.debug( "CatalogTreeModel.loadChilds()" );
		
		ORMHelper.openManager();
		Query q = ORMHelper.getCurrentManager().createQuery("select c.id, c.ancestorCatalog, c.variety, c.name from Catalog c "
				+ " where ( (c.journal.editMoment <= :ts and :ts < c.journal.deleteMoment) or (c.journal.deleteMoment < :ts and c.status = 2) )"
				+ " and c.parentCatalog.id = :pid order by c.item");
		q.setParameter( "ts", getCurrentTimePoint() );
		q.setParameter( "pid", node.getCurrent() );
		@SuppressWarnings("unchecked")
		List<Object[]> res = q.getResultList();
		ORMHelper.closeManager();
		if ( (res == null) || (res.size() == 0) ) {
			listener.childsLoadCompleted( emptyList );
			return ;
		}		
		List<TreeElement> lst = new ArrayList<>();
		for ( Object[] objs : res ) { 
			lst.add( new TreeElement( TreeElementType.CATALOG, (Integer) objs[0], (Integer) ( objs[1] == null ? objs[0] : objs[1] ), 
					(Short) objs[2], (String) objs[3] ) ) ;
		}
		listener.childsLoadCompleted( lst );		
	}

	@Override
	public boolean isLeaf(TreeElement node) {	
		if ( !checkObject( node, "isLeaf" ) ) return true;
		return false;
	}	
	
	private boolean checkObject(TreeElement obj, String funcName) {
		if ( obj == null ) {
			if( (log != null) && log.isErrorEnabled() ) log.error( "CatalogTreeModel." + funcName + "(): receive null argument" ); 
			return false;
		}
		if ( obj.getType() != TreeElement.TreeElementType.CATALOG ) {
			if( (log != null) && log.isErrorEnabled() ) log.error( "CatalogTreeModel.isLeaf(): receive illegal value " ); 
			return false;
		}
		return true;
	}
}