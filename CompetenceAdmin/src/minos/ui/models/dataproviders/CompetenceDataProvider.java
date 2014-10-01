package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Catalog;
import minos.entities.Competence;
import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import com.alee.extended.tree.ChildsListener;

public class CompetenceDataProvider extends BasisDataProvider<MainTreeNode> {
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( CompetenceDataProvider.class );
	private BasisDataProvider<MainTreeNode> bdp = null;
	List<Short> enabledCatalogVariety = null;
	private boolean visibleDeletedCompetens = false;
	private boolean ñatalogBeforeCompetence = true;	

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public CompetenceDataProvider( BasisDataProvider<MainTreeNode> bdp, List<Short> enabledCatalogVariety ) {
		this( null, bdp, enabledCatalogVariety );	
	}

	public CompetenceDataProvider( Timestamp timepoint, BasisDataProvider<MainTreeNode> bdp, 
			List<Short> enabledCatalogVariety  ) {
		super( timepoint );
		if ( bdp == null ) {
			String errmsg = "CompetenceDataProvider() : bdp is null";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new NullPointerException( errmsg ); 
		}		
		this.bdp = bdp;
		if ( ( enabledCatalogVariety == null ) || ( enabledCatalogVariety.size() == 0 ) ) {
			String errmsg = "CompetenceDataProvider() : enabledCatalogVariety is null or empty";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new NullPointerException( errmsg ); 
		}
		this.enabledCatalogVariety = enabledCatalogVariety;
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean isVisibleDeletedCompetens() {
		return visibleDeletedCompetens;
	}
	
	public void setVisibleDeletedCompetens( boolean visibleDeletedCompetens ) {
		this.visibleDeletedCompetens = visibleDeletedCompetens;
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void setCurrentTimePoint( Timestamp timePoint ) {
		super.setCurrentTimePoint( timePoint );
		bdp.setCurrentTimePoint( timePoint );
	}

	@Override
	public MainTreeNode getRoot() {		
		return bdp.getRoot();
	}

	@Override
	public boolean isLeaf( MainTreeNode node ) {
		return ( ( ( node == null ) || ( node.getUserObject() == null ) ) ? true 
				: ( node.getUserObject() instanceof Competence  ? true : false ) );
	}

	@Override
	public void loadChilds( MainTreeNode node, final ChildsListener<MainTreeNode> listener ) {
		try {
			if ( ( node == null ) || ( node.getUserObject() == null ) ) {
				throw new IllegalArgumentException( "CompetenceDataProvider.loadChilds() : node is illegal" );
			}
			if ( !( node.getUserObject() instanceof Catalog ) ) bdp.loadChilds( node, listener ); 

			List<MainTreeNode> subCatalogs = loadSubCatalogs( node, listener );
			if ( subCatalogs == null ) return;			 
			
			List<Competence> res = null;
			if ( enabledCatalogVariety.contains( ( ( Catalog ) node.getUserObject() ).getVariety() ) ) {
				res = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
						ResourceKeeper.getQuery( QType.JPQL_LOAD_COMPETENCES ), 
						Competence.class, 
						new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
						new Pair<Object, Object>( "dts", visibleDeletedCompetens ? getCurrentTimePoint() 
								: ResourceKeeper.getObject( OType.WAR ) ),
						new Pair<Object, Object>( "catalogs", Arrays.asList( node.getUserObject() ) ) );
			}
			
			List<MainTreeNode> lst = null;
			if ( ( res != null ) && ( res.size() > 0 ) ) 
				lst = joinLists( ( ñatalogBeforeCompetence ? subCatalogs : convertList( res ) ), 
						( ñatalogBeforeCompetence ? convertList( res ) : subCatalogs ) );				
			listener.childsLoadCompleted( ( lst == null ) ? subCatalogs : lst );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "CompetenceDataProvider.loadChilds() : ", ex );
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