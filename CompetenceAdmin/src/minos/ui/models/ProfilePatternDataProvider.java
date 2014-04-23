package minos.ui.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import minos.data.services.ORMHelper;
import minos.entities.Catalog;
import minos.entities.ProfilePattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.tree.ChildsListener;


public class ProfilePatternDataProvider extends BasisDataProvider<MainTreeNode> {
	private static Logger log = LoggerFactory.getLogger( ProfilePatternDataProvider.class );

	private static final String jpqlLoadProfilePatterns = " select entity from ProfilePattern entity join fetch entity.name "
			+ " where ( ( :ts between entity.journal.editMoment and entity.journal.deleteMoment) %s) "
			+ " and entity.catalog.id = :cid order by entity.item ";
	private static final String jpqlLoadDeletedProfilePatterns = " or (entity.journal.deleteMoment < :ts and entity.status = 2) ";

	private String jpql;
	private BasisDataProvider<MainTreeNode> bdp 	= null;	
	private boolean visibleDeletedProfilePatterns  	= false;
	private boolean catalogBeforeProfilePattern  	= true;
	
	private void rebuildJpqlStatment() {
		jpql = String.format( jpqlLoadProfilePatterns,				
				( !visibleDeletedProfilePatterns ? " " : jpqlLoadDeletedProfilePatterns ) );
	}
	
	public ProfilePatternDataProvider( BasisDataProvider<MainTreeNode> bdp ) {		
		super();
		this.bdp = bdp;
		setCurrentTimePoint( null );
		rebuildJpqlStatment();
	}

	public ProfilePatternDataProvider(Timestamp timepoint, BasisDataProvider<MainTreeNode> bdp) {
		super();
		this.bdp = bdp;
		setCurrentTimePoint( timepoint );
		rebuildJpqlStatment();
	}

	@Override
	public MainTreeNode getRoot() {
		if ( ( bdp == null ) && log.isErrorEnabled() ) log.error( "ProfilePatternDataProvider.getRoot() : bdp is null" );
		return ( bdp == null ? null : bdp.getRoot() );
	}

	@Override
	public boolean isLeaf(MainTreeNode node) {
		if ( node.getUserObject() instanceof ProfilePattern ) return true;
		return false;
	}

	@Override
	public void loadChilds(MainTreeNode node, final ChildsListener<MainTreeNode> listener) {
		if ( !( node.getUserObject() instanceof Catalog ) ) {
			String errmsg = "ProfilePatternDataProvider.loadChilds() : unknown node type: " + node.getUserObject();
			if( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			listener.childsLoadFailed( new IllegalArgumentException( errmsg ) );
			return;
		}

		// load sub catalog
		@SuppressWarnings("unchecked")
		final List<MainTreeNode>[] siblings = ( List<MainTreeNode>[] ) new List[] { null };
		bdp.loadChilds( node, new ChildsListener<MainTreeNode>() {

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

		// load competence for catalog 
		ORMHelper.openManager();
		List<ProfilePattern> res = ORMHelper.getCurrentManager().createQuery( jpql, ProfilePattern.class ).
				setParameter( "ts", getCurrentTimePoint() ).
				setParameter( "cid", ( ( Catalog ) node.getUserObject() ).getId() ).getResultList();
		ORMHelper.closeManager();

		if ( (res == null) || (res.size() == 0) ) {
			listener.childsLoadCompleted( siblings[0] );
			return;
		}
		
		List<MainTreeNode> lst = new ArrayList<>();
		if ( catalogBeforeProfilePattern ) lst.addAll( siblings[0] );		
		for ( ProfilePattern pp : res ) lst.add( new MainTreeNode( pp ) );
		if ( !catalogBeforeProfilePattern ) lst.addAll( siblings[0] );		
		listener.childsLoadCompleted( lst );
	}

	@Override
	public void setCurrentTimePoint(Timestamp timePoint) {
		super.setCurrentTimePoint(timePoint);
		if ( bdp != null ) bdp.setCurrentTimePoint( timePoint );
	}

	public boolean isVisibleDeletedProfilePatterns() {
		return visibleDeletedProfilePatterns;
	}

	public void setVisibleDeletedProfilePatterns( boolean flag ) {
		this.visibleDeletedProfilePatterns = flag;
		rebuildJpqlStatment();
	}

	public boolean isCatalogBeforeProfilePattern() {
		return catalogBeforeProfilePattern;
	}

	public void setCatalogBeforeProfilePattern( boolean flag ) {
		this.catalogBeforeProfilePattern = flag;
	}
}