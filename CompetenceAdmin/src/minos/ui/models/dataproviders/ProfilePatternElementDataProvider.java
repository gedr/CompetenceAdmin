package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Competence;
import minos.entities.Profile;
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

public class ProfilePatternElementDataProvider extends BasisDataProvider<MainTreeNode> {
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( ProfilePatternElementDataProvider.class );
	private BasisDataProvider<MainTreeNode> bdp = null;
	private boolean visibleDeletedPPE = false;
	private List<Short> varieties = null;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ProfilePatternElementDataProvider( BasisDataProvider<MainTreeNode> bdp, List<Short> varieties ) {
		this( null, bdp, varieties );
	}

	public ProfilePatternElementDataProvider( Timestamp timepoint, BasisDataProvider<MainTreeNode> bdp, 
			List<Short> varieties ) {
		super( timepoint );
		if ( bdp == null ) throw new NullArgumentException( "ProfilePatternElementDataProvider() : bdp is null" );
		this.bdp = bdp;
		if ( (varieties == null ) || ( varieties.size() == 0 ) ) {
			throw new IllegalArgumentException( "ProfilePatternElementDataProvider() : parameter varieties is wrong" );
		}
		this.varieties = varieties;
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public boolean isVisibleDeletedPPE() {
		return visibleDeletedPPE;
	}

	public void setVisibleDeletedPPE( boolean flag ) {
		this.visibleDeletedPPE = flag;
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
	public boolean isLeaf(MainTreeNode node) {
		return ( ( ( node == null ) || ( node.getUserObject() == null ) ) ? true 
				: ( node.getUserObject() instanceof ProfilePatternElement  ? true : false ) );
	}

	@Override
	public void loadChilds( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		try {		
			if ( !( node.getUserObject() instanceof ProfilePattern ) && !( node.getUserObject() instanceof Profile ) ) {				
				bdp.loadChilds( node, listener );
				return;				
			}
			ProfilePattern pp = ( ProfilePattern ) ( node.getUserObject() instanceof ProfilePattern 
					? node.getUserObject() : ( ( Profile ) node.getUserObject() ).getProfilePattern() );
			List<Object[]> res = OrmHelper.findByQueryWithParam( QueryType.JPQL,
					ResourceKeeper.getQuery( QType.JPQL_LOAD_PPE_AND_COMPETENCE ), 
					Object[].class,
					new Pair<Object, Object>( "pps", Arrays.asList( pp ) ),
					new Pair<Object, Object>( "ts", getCurrentTimePoint() ),
					new Pair<Object, Object>( "vrt", varieties ),
					new Pair<Object, Object>( "dts", visibleDeletedPPE ? getCurrentTimePoint() 
							: ResourceKeeper.getObject( OType.WAR ) ) );
			if ( ( res == null ) || ( res.size() == 0 ) ) {
				listener.childsLoadCompleted( BASIS_EMPTY_LIST );
				return;
			}
			List<MainTreeNode> lmtn = new ArrayList<>();
			for ( Object[] objs : res ) {
				ProfilePatternElement ppe = ( ProfilePatternElement ) objs[0];
				Competence c = ( Competence ) objs[1];
				lmtn.add( ppe.getCompetence().equals( c ) ? new MainTreeNode( ppe ) : new MainTreeNode( ppe, c ) );
			}
			Collections.sort( lmtn, new Comparator<MainTreeNode>() {

				@Override
				public int compare( MainTreeNode arg0, MainTreeNode arg1 ) {
					Competence c0 = ( Competence ) ( arg0.getAddon() != null ? arg0.getAddon() 
							: ( ( ProfilePatternElement ) arg0.getUserObject() ).getCompetence() ) ;
					Competence c1 = ( Competence ) ( arg1.getAddon() != null ? arg1.getAddon() 
							: ( ( ProfilePatternElement ) arg1.getUserObject() ).getCompetence() ) ;
					if ( c0.getVariety() != c1.getVariety() ) return c0.getVariety() - c1.getVariety();

					return ( ( ProfilePatternElement ) arg0.getUserObject() ).getItem() 
							- ( ( ProfilePatternElement ) arg1.getUserObject() ).getItem();
				}
			} );
			listener.childsLoadCompleted( lmtn );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() )  log.error( "ProfilePatternElementDataProvider."
					+ "loadChilds() : ", ex );
			listener.childsLoadFailed( ex );
		}
	}
}