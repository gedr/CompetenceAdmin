package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.List;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Catalog;
import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.alee.extended.tree.ChildsListener;

public class CatalogDataProvider extends BasisDataProvider<MainTreeNode> {	
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	public static final int ROOT_ID = 1;	

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( CatalogDataProvider.class );
	private boolean visibleDeleteCatalogs = false;	
	List<Short> enabledCatalogVariety = null;
	private MainTreeNode root = new MainTreeNode( ( Catalog ) OrmHelper.findEntity( Catalog.class, ROOT_ID ) );

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public CatalogDataProvider() {
		super( null );		
	}
	
	public CatalogDataProvider( Timestamp timePoint ) {
		super( timePoint );
	}

	public CatalogDataProvider( List<Short> enabledCatalogVariety ) {
		super( null );
		if ( ( enabledCatalogVariety == null ) || ( enabledCatalogVariety.size() == 0 ) ) {
			throw new IllegalArgumentException( "CatalogDataProvider() : enabledCatalogVariety is null or empty" ); 
		}
		this.enabledCatalogVariety = enabledCatalogVariety;		
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public void setVisibleDeleteCatalogs( boolean visibleDeleteCatalogs ) {
		this.visibleDeleteCatalogs = visibleDeleteCatalogs;
	}

	public boolean isVisibleDeleteCatalogs() {
		return visibleDeleteCatalogs;
	}

	public List<Short> getEnabledCatalogVariety() {
		return enabledCatalogVariety;
	}

	public void setEnabledCatalogVariety( List<Short> enabledCatalogVariety ) {
		this.enabledCatalogVariety = enabledCatalogVariety;
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void setCurrentTimePoint(Timestamp timePoint) {
		super.setCurrentTimePoint(timePoint);
	}

	@Override
	public MainTreeNode getRoot() {
		return root;
	}

	@Override
	public boolean isLeaf( MainTreeNode node ) {		
		return false;
	}

	@Override
	public void loadChilds( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		try {
			if ( !( node.getUserObject() instanceof Catalog ) ) {
				throw new IllegalArgumentException( "CatalogDataProvider.loadChilds() take wrong node :" + node );
			}
			
			List<Catalog> res = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
					ResourceKeeper.getQuery( QType.JPQL_LOAD_CATALOGS ), 
					Catalog.class, 
					new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
					new Pair<Object, Object>( "dts", visibleDeleteCatalogs ? getCurrentTimePoint() 
							: ResourceKeeper.getObject( OType.WAR ) ),
							new Pair<Object, Object>( "pcid", ( ( Catalog ) node.getUserObject() ).getId() ),
							new Pair<Object, Object>( "variety", enabledCatalogVariety) );
			listener.childsLoadCompleted( ( ( res == null ) || ( res.size() == 0 ) ) ? BASIS_EMPTY_LIST 
					: convertList( res ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() )  log.error( "CatalogDataProvider.loadChilds() : ", ex );
			listener.childsLoadFailed( ex );
		}		
	}
}