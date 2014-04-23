package minos.ui.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.data.services.ORMHelper;
import minos.entities.*;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;

import com.alee.extended.tree.ChildsListener;

public class CompetenceDataProvider extends BasisDataProvider<MainTreeNode> {
	private static Logger log = LoggerFactory.getLogger( CompetenceDataProvider.class );
	private BasisDataProvider<MainTreeNode> bdp;	
	
	private static final String jpqlLoadCompetences = " select entity from Competence entity join fetch entity.name "
					+ " where ( ( :ts between entity.journal.editMoment and entity.journal.deleteMoment) %s) "
					+ " and entity.catalog.id = :cid order by entity.item ";
	private static final String jpqlLoadDeletedCompetences = " or (entity.journal.deleteMoment < :ts and entity.status = 2) ";
	
	private static final String jpqlLoadIndicators = " select entity from Indicator entity join fetch entity.name "
			+ " where ( ( :ts between entity.journal.editMoment and entity.journal.deleteMoment) %s)"
			+ " and entity.competence.id = :cid %s order by %s entity.item ";
	private static final String jpglConditionLoadIndicatorForLevel = " and entity.level.id = :lid ";
	private static final String jpqlOrderIndicatorByLevel = " entity.level, ";
	private static final String jpqlConditionLoadDeletedIndicators = " or (entity.journal.deleteMoment < :ts and entity.status = 2) ";
	
	private static final String jpqlLoadProfilePatternElements = " select entity from ProfilePatternElement entity join fetch entity.minLevel join fetch entity.competence "
			+ " where ( ( :ts between entity.journal.editMoment and entity.journal.deleteMoment) %s)"
			+ " and entity.profilePattern.id = :ppid order by entity.competence.variety, entity.item ";
	private static final String jpqlLoadDeletedProfilePatternElements = " or ( entity.journal.deleteMoment < :ts and entity.status = 2 ) ";
	
	private boolean visibleDeletedCompetens = false;
	private boolean visibleDeletedIndicators = false;
	private boolean visibleDeletedProfilePatternElements = false;
	private boolean visibleLevels = false;
	private boolean ñatalogBeforeCompetence = true;	
	private String jpqlCompetence;
	private String jpqlIndicator;
	private String jpqlPPE;
	
	private void rebuildJpqlCompetenceStatment() {
		jpqlCompetence = String.format( jpqlLoadCompetences, 
				( !visibleDeletedCompetens ? " " : jpqlLoadDeletedCompetences ) );
	}
	private void rebuildJpqlIndicatorStatment() {
		jpqlIndicator = String.format( jpqlLoadIndicators, 
				( !visibleDeletedIndicators ? " " : jpqlConditionLoadDeletedIndicators ),
				( !visibleLevels ? " " : jpglConditionLoadIndicatorForLevel ),
				( visibleLevels ? " " : jpqlOrderIndicatorByLevel ) );
	}

	private void rebuildJpqlPPEStatment() {
		jpqlPPE = String.format( jpqlLoadProfilePatternElements, 
				( !visibleDeletedProfilePatternElements ? " " : jpqlLoadDeletedProfilePatternElements ) );
	}

	public CompetenceDataProvider( BasisDataProvider<MainTreeNode> bdp ) {		
		super();
		this.bdp = bdp;
		setCurrentTimePoint( null );
		rebuildJpqlCompetenceStatment();
		rebuildJpqlIndicatorStatment();
		rebuildJpqlPPEStatment();
	}
	
	public CompetenceDataProvider( Timestamp timepoint, BasisDataProvider<MainTreeNode> bdp ) {
		super();
		this.bdp = bdp;
		setCurrentTimePoint( timepoint );
		rebuildJpqlCompetenceStatment();
		rebuildJpqlIndicatorStatment();
		rebuildJpqlPPEStatment();
	}
	
	@Override
	public MainTreeNode getRoot() {		
		if ( ( bdp == null ) && log.isErrorEnabled() ) log.error( "CompetenceDataProvider.getRoot() : bdp is null" );
		return ( bdp == null ? null : bdp.getRoot() );
	}

	@Override
	public boolean isLeaf(MainTreeNode node) {
		return ( ( node.getUserObject() instanceof Indicator ) ? true : false );
	}

	@Override
	public void loadChilds(MainTreeNode node, ChildsListener<MainTreeNode> listener) {
		if ( loadCompetences( node, listener ) ) return;
		if ( loadLevels( node, listener ) ) return;
		if ( loadIndicators( node, listener ) ) return;
		if ( loadProfilePatternElements( node, listener ) ) return;
		String errmsg = "CompetenceDataProvider.loadChilds() : unknown node: " + node;
		if( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
		listener.childsLoadFailed( new IllegalArgumentException( errmsg ));
	}

	@Override
	public void setCurrentTimePoint(Timestamp timePoint) {
		super.setCurrentTimePoint(timePoint);
		if ( bdp != null ) bdp.setCurrentTimePoint( timePoint );
	}
	
	private boolean loadProfilePatternElements( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		if ( !( node.getUserObject() instanceof ProfilePattern ) ) return false;

		ORMHelper.openManager();
		List<ProfilePatternElement> res = ORMHelper.getCurrentManager().createQuery( jpqlPPE, ProfilePatternElement.class ).
				setParameter( "ts", getCurrentTimePoint() ).
				setParameter( "ppid", ( ( ProfilePattern ) node.getUserObject() ).getId() ).getResultList();
		ORMHelper.closeManager();			
		if ( (res == null) || (res.size() == 0) ) {
			listener.childsLoadCompleted( BASIS_EMPTY_LIST );
			return true;
		}		
		List<MainTreeNode> lst = new ArrayList<>();
		for ( ProfilePatternElement ppe : res ) lst.add( new MainTreeNode( ppe ) );
		listener.childsLoadCompleted( lst );
		return true;
	}
	
	private boolean loadIndicators( MainTreeNode mnode, ChildsListener<MainTreeNode> listener ) {
		MainTreeNode node = mnode;
		if ( node.getUserObject() instanceof Level ) node = (MainTreeNode) node.getParent();
		
		if ( !( ( node.getUserObject() instanceof Competence ) || 
		( node.getUserObject() instanceof ProfilePatternElement ) ) ) return false;
				
		// load indicator for competence level
		int cid = ( node.getUserObject() instanceof  Competence ? 
				( ( Competence ) node.getUserObject() ).getId() :
					( ( ProfilePatternElement ) node.getUserObject() ).getCompetence().getId() );
		ORMHelper.openManager();
		TypedQuery<Indicator> q = ORMHelper.getCurrentManager().createQuery( jpqlIndicator, Indicator.class ).
				setParameter( "ts", getCurrentTimePoint() ).setParameter( "cid", cid );
		if ( visibleLevels ) q.setParameter( "lid", ( (Level) mnode.getUserObject() ).getId() );
		List<Indicator> res = q.getResultList();
		ORMHelper.closeManager();			
		if ( (res == null) || (res.size() == 0) ) {
			listener.childsLoadCompleted( BASIS_EMPTY_LIST );
			return true;
		}		
		List<MainTreeNode> lst = new ArrayList<>();
		for ( Indicator i : res ) lst.add( new MainTreeNode( i ) ); 		
		listener.childsLoadCompleted( lst );		
		return true;
	}
	
	private boolean loadLevels( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		if ( !( visibleLevels && 
				( ( node.getUserObject() instanceof Competence ) || 
						( node.getUserObject() instanceof ProfilePatternElement ) ) ) ) return false;
		
		@SuppressWarnings("unchecked")
		List<Level> llst =( List<Level> ) Resources.getInstance().get( ResourcesConst.LEVELS_CACHE );
		List<MainTreeNode> ltn = new ArrayList<>();
		for ( Level l : llst ) ltn.add( new MainTreeNode( l ) );
		listener.childsLoadCompleted( ltn );
		return true;
	}
	
	private boolean loadCompetences( MainTreeNode node, final ChildsListener<MainTreeNode> listener ) {
		if ( !( node.getUserObject() instanceof Catalog ) ) return false;
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
		if ( siblings[0] == null ) return false;

		// load competence for catalog 
		ORMHelper.openManager();
		List<Competence> res = ORMHelper.getCurrentManager().createQuery( jpqlCompetence, Competence.class ).
		setParameter( "ts", getCurrentTimePoint() ).setParameter( "cid", ( ( Catalog ) node.getUserObject() ).getId() ).
		getResultList();
		ORMHelper.closeManager();

		if ( (res == null) || (res.size() == 0) ) {
			listener.childsLoadCompleted( siblings[0] );
			return true;
		}

		List<MainTreeNode> lst = new ArrayList<>();
		if ( ñatalogBeforeCompetence ) lst.addAll( siblings[0] );		
		for ( Competence c : res ) lst.add( new MainTreeNode( c ) );
		if ( !ñatalogBeforeCompetence ) lst.addAll( siblings[0] );		
		listener.childsLoadCompleted( lst );
		return true;
	}
	
	public boolean isVisibleDeletedCompetens() {
		return visibleDeletedCompetens;
	}
	
	public void setVisibleDeletedCompetens( boolean visibleDeletedCompetens ) {
		this.visibleDeletedCompetens = visibleDeletedCompetens;
		rebuildJpqlCompetenceStatment();
	}

	public boolean isVisibleDeletedIndicators() {
		return visibleDeletedIndicators;
	}
	
	public void setVisibleDeletedIndicators( boolean visibleDeletedIndicators ) {
		this.visibleDeletedIndicators = visibleDeletedIndicators;
		rebuildJpqlIndicatorStatment();
	}

	public boolean isVisibleDeletedProfilePatternElements() {
		return visibleDeletedProfilePatternElements;
	}

	public void setVisibleDeletedProfilePatternElements( boolean visibleDeletedProfilePatternElements ) {
		this.visibleDeletedProfilePatternElements = visibleDeletedProfilePatternElements;
		rebuildJpqlPPEStatment();
	}

	public boolean isVisibleLevels() {
		return visibleLevels;
	}

	public void setVisibleLevels( boolean visibleLevels ) {
		this.visibleLevels = visibleLevels;
		rebuildJpqlIndicatorStatment();
	}
}