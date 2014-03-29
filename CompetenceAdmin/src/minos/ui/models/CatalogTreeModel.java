package minos.ui.models;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.data.services.ORMHelper;
import minos.entities.Catalog;

public class CatalogTreeModel extends BasisTreeModel {
	private static final Integer rootId = 1;
	private static Logger log = LoggerFactory.getLogger( CatalogTreeModel.class );
	private Catalog root = null;
	//private Set<Integer> leafNodeId;

	@Override
	public Object getRoot() {
		if ( root == null ) root = (Catalog) ORMHelper.findEntity( Catalog.class, rootId, "subCatalogs" );
		if ( (root == null) && (log != null) && log.isErrorEnabled() ) log.error("CatalogTreeModel.getRoor() : cannot find root node"); 
		return root;
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub	

	}

	@Override
	public boolean isLeaf(Object node) {
		if ( !checkObject(node, "isLeaf") ) return true;
		List<Catalog> catalogs = ( (Catalog) node ).getSubCatalogs();		
		return ( catalogs == null ? true : catalogs.size() == 0 );
	}

	@Override
	public int getChildCount(Object parent) {
		if ( !checkObject(parent, "getChildCount") ) return 0;
		List<Catalog> catalogs = ( (Catalog) parent ).getSubCatalogs();		
		return ( catalogs == null ? 0 : catalogs.size() );
	}

	@Override
	public Object getChild(Object parent, int index) {
		if ( !checkObject( parent, "getChild" ) ) return null;
		List<Catalog> catalogs = ( (Catalog) parent ).getSubCatalogs();
		if ( (catalogs == null) || (catalogs.size() ==0) 
				|| (index < 0) || (index >= catalogs.size()) ) return null;
		return catalogs.get( index );
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if ( !checkObject( parent, "getIndexOfChild" ) || !checkObject( child, "getIndexOfChild" )) return -1;
		List<Catalog> catalogs = ( (Catalog) parent ).getSubCatalogs();
		if ( (catalogs == null) || (catalogs.size() ==0) ) return -1;
		int ind = 0;
		for(Catalog cat : catalogs) {
			if ( cat.getId() == ( (Catalog) child ).getId() ) return ind;
			ind++;
		}		
		return -1;
	}
	
	/**
	 * This function check object
	 * @param obj 
	 * @return
	 */
	private boolean checkObject(Object obj, String funcName) {
		if ( obj == null ) {
			if( (log != null) && log.isErrorEnabled() ) log.error( "CatalogTreeModel." + funcName + "(): receive null argument" ); 
			return false;
		}
		if ( !(obj instanceof Catalog) ) {
			if( (log != null) && log.isErrorEnabled() ) log.error( "CatalogTreeModel.isLeaf(): receive illegal type argument" ); 
			return false;
		}
		return true;
	}
}