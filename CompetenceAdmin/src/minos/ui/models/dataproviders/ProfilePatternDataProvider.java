package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Catalog;
import minos.entities.ProfilePattern;
import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.alee.extended.tree.ChildsListener;


public class ProfilePatternDataProvider extends BasisDataProvider<MainTreeNode> {
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( ProfilePatternDataProvider.class );
	private BasisDataProvider<MainTreeNode> bdp 	= null;	
	private boolean visibleDeletedProfilePatterns  	= false;
	private boolean catalogBeforeProfilePattern  	= true;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ProfilePatternDataProvider( BasisDataProvider<MainTreeNode> bdp ) {
		this ( null, bdp );
	}

	public ProfilePatternDataProvider( Timestamp timepoint, BasisDataProvider<MainTreeNode> bdp ) {
		super( timepoint );
		if ( bdp == null ) throw new NullArgumentException( "ProfilePatternDataProvider() : bdp is null" );
		this.bdp = bdp;
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean isVisibleDeletedProfilePatterns() {
		return visibleDeletedProfilePatterns;
	}

	public void setVisibleDeletedProfilePatterns( boolean flag ) {
		this.visibleDeletedProfilePatterns = flag;
	}

	public boolean isCatalogBeforeProfilePattern() {
		return catalogBeforeProfilePattern;
	}

	public void setCatalogBeforeProfilePattern( boolean flag ) {
		this.catalogBeforeProfilePattern = flag;
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
		return bdp.getRoot();
	}

	@Override
	public boolean isLeaf(MainTreeNode node) {
		return ( ( ( node == null ) || ( node.getUserObject() == null ) ) ? true 
				: ( node.getUserObject() instanceof ProfilePattern  ? true : false ) );
	}

	@Override
	public void loadChilds( MainTreeNode node, final ChildsListener<MainTreeNode> listener ) {
		try {
			if ( ( node == null ) || ( node.getUserObject() == null ) ) {
				throw new IllegalArgumentException( "ProfilePatternDataProvider.loadChilds() : node is illegal" );
			}
			if ( !( node.getUserObject() instanceof Catalog ) ) {
				bdp.loadChilds( node, listener );
				return;
			}
			
			 List<MainTreeNode> subCatalogs = loadSubCatalogs( node, listener );
			 if ( subCatalogs == null ) return;

			List<ProfilePattern> res = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
					ResourceKeeper.getQuery( QType.JPQL_LOAD_PP ), 
					ProfilePattern.class, 
					new Pair<Object, Object>( "catalogs", Arrays.asList( ( Catalog ) node.getUserObject() ) ),
					new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
					new Pair<Object, Object>( "dts", visibleDeletedProfilePatterns ? getCurrentTimePoint() 
							: ResourceKeeper.getObject( OType.WAR ) ) );
			
			List<MainTreeNode> lst = null;
			if ( ( res != null ) && ( res.size() > 0 ) ) 
				lst = joinLists( ( catalogBeforeProfilePattern? subCatalogs : convertList( res ) ), 
						( catalogBeforeProfilePattern ? convertList( res ) : subCatalogs ) );				
			listener.childsLoadCompleted( ( lst == null ) ? subCatalogs : lst );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ProfilePatternDataProvider.loadChilds() : ", ex );
			listener.childsLoadFailed( ex );
		}
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * load sub catalogs
	 * @param node 
	 * @param listener
	 * @return
	 */
	private List<MainTreeNode> loadSubCatalogs( MainTreeNode node,
			final ChildsListener<MainTreeNode> listener ) {
		LocalChildsListener<MainTreeNode> lcl = new LocalChildsListener<>();
		bdp.loadChilds( node, lcl );
		if ( !lcl.isOperationComplete() ) listener.childsLoadFailed( lcl.getCause() );
		return lcl.getChilds();
	}
}