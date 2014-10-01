package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Competence;
import minos.entities.Indicator;
import minos.entities.Level;
import minos.entities.ProfilePattern;
import minos.entities.ProfilePatternElement;
import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.alee.extended.tree.ChildsListener;

public class IndicatorDataProvider extends BasisDataProvider<MainTreeNode> {
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( CompetenceDataProvider.class );
	private BasisDataProvider<MainTreeNode> bdp = null;	
	private boolean visibleDeletedIndicators = false;
	private boolean visibleLevels = false;
	private Map<Integer, List<Level>> levelMap; 

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public IndicatorDataProvider( BasisDataProvider<MainTreeNode> bdp ) {	
		this( null, bdp );
	}
	
	public IndicatorDataProvider( Timestamp timepoint, BasisDataProvider<MainTreeNode> bdp ) {
		super( timepoint );
		if ( bdp == null ) {
			String errmsg = "IndicatorDataProvider() : bdp is null";
			if ( ( log != null) && log.isErrorEnabled() ) log.error( errmsg );
			throw new NullPointerException( errmsg );
		}
		this.bdp = bdp;
		
		List<Level> levels = ResourceKeeper.getObject( OType.LEVELS_CACHE );
		if ( ( levels == null ) || ( levels.size() == 0 ) ) {
			String errmsg = "IndicatorDataProvider() : resourece LEVELS_CACHE not found" ;
			if ( ( log != null) && log.isErrorEnabled() ) log.error( errmsg );
			throw new NullPointerException( errmsg );
		}
		levelMap = new TreeMap<>();
		levelMap.put( 0, levels );
		for ( Level l : levels ) levelMap.put( l.getId(), Arrays.asList( l ) );
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean isVisibleDeletedIndicators() {
		return visibleDeletedIndicators;
	}
	
	public void setVisibleDeletedIndicators( boolean visibleDeletedIndicators ) {
		this.visibleDeletedIndicators = visibleDeletedIndicators;
	}

	public boolean isVisibleLevels() {
		return visibleLevels;
	}

	public void setVisibleLevels( boolean visibleLevels ) {
		this.visibleLevels = visibleLevels;
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void setCurrentTimePoint(Timestamp timePoint) {
		super.setCurrentTimePoint(timePoint);
		bdp.setCurrentTimePoint( timePoint );
	}

	@Override
	public MainTreeNode getRoot() {		
		return bdp.getRoot();
	}

	@Override
	public boolean isLeaf( MainTreeNode node ) {
		return ( ( ( node == null ) || ( node.getUserObject() == null ) ) ? true 
				: ( node.getUserObject() instanceof Indicator ? true : false ) );
	}

	@Override
	public void loadChilds( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		try {
			if ( ( node == null ) || ( node.getUserObject() == null ) ) {
				listener.childsLoadFailed( new NullArgumentException( "IndicatorDataProvider.loadChilds() : wrong node" ) );
			}
			if ( ( node.getUserObject() instanceof Competence )  || ( node.getUserObject() instanceof Level )
					|| ( ( node.getUserObject() instanceof ProfilePatternElement )
							&& ( ( ( ProfilePatternElement ) node.getUserObject() ).getCompetence() != null ) ) ) {
				loadIndicators( node, listener );
				return;
			}
			bdp.loadChilds( node, listener );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "IndicatorDataProvider.loadChilds() : ", ex );
			listener.childsLoadFailed( ex ); 
		}
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * load indicator's node in parent node
	 * @param mnode - parent node
	 * @param listener -event listener
	 */
	private void loadIndicators( MainTreeNode node, ChildsListener<MainTreeNode> listener ) throws Exception {
		Level level = null;
		Competence competence = null; 
		if ( node.getUserObject() instanceof Level ) {
			level = ( Level ) node.getUserObject();
			node = ( MainTreeNode ) node.getParent();
		}
		if ( node.getUserObject() instanceof  Competence ) competence = ( Competence ) node.getUserObject();
		if ( node.getUserObject() instanceof  ProfilePatternElement ) {
			ProfilePatternElement ppe = ( ProfilePatternElement ) node.getUserObject();
			competence = ppe.getCompetence();
			setCurrentTimePoint( ( ( ProfilePattern ) node.getParent().getUserObject() ).getTimePoint() );
			//setCurrentTimePoint( ppe.getProfilePattern().getTimePoint() );
		}
		if ( competence == null ) {
			throw new NullPointerException( "IndicatorDataProvider.loadIndicators() : not found parent competence" );
		}
		if ( visibleLevels && ( level == null ) ) {
			listener.childsLoadCompleted( makeLevels() );
			return;
		}
		List<Indicator> res = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_INDICATORS ), 
				Indicator.class, 
				new Pair<Object, Object>( "competences", Arrays.asList( competence ) ),
				new Pair<Object, Object>( "levels", ( level != null ? Arrays.asList( level ) 
						: ResourceKeeper.getObject( OType.LEVELS_CACHE ) ) ),
				new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
				new Pair<Object, Object>( "dts", visibleDeletedIndicators ? getCurrentTimePoint() 
						: ResourceKeeper.getObject( OType.WAR ) ) );
		listener.childsLoadCompleted( ( ( res == null ) || ( res.size() == 0 ) ) ? Collections.<MainTreeNode>emptyList() 
				: convertList( res ) );
 	}

	/**
	 * load levels' node to parent node 
	 * @param node - parent node
	 * @param listener - event listener
	 */
	private List<MainTreeNode> makeLevels() throws Exception {
		@SuppressWarnings("unchecked")
		List<Level> ll = ( List<Level> ) ResourceKeeper.getObject( OType.LEVELS_CACHE );
		if ( ( ll == null ) || ( ll.size() == 0 ) )  throw new NullPointerException( "IndicatorDataProvider."
				+ "loadLevels() : resource LEVELS_CACHE not found" );
		return convertList( ll );
	}
}