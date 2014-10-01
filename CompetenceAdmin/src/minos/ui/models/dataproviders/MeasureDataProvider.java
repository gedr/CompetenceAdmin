package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.services.FilialInfo;
import minos.entities.Division;
import minos.entities.Measure;
import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;
import ru.gedr.util.tuple.Pair;

import com.alee.extended.tree.ChildsListener;

public class MeasureDataProvider extends BasisDataProvider<MainTreeNode> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final byte ENTERPRISE_CODE = 0;
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( MeasureDataProvider.class );
	private MainTreeNode root = null;
	private boolean visibleDeletedMeasure = false;
	private boolean visibleOnlyCurrentFilial = false;
	private Pair<Timestamp, Timestamp> visibleInterval = null;
	private List<Measure> outer;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public MeasureDataProvider() {
		Calendar cal = Calendar.getInstance();		
		cal.set( cal.get( Calendar.YEAR ), Calendar.JANUARY, 1 );		
		visibleInterval = new Pair<>( new Timestamp( cal.getTimeInMillis() ), new Timestamp( System.currentTimeMillis() ) );
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public Pair<Timestamp, Timestamp> getVisibleInterval() {
		return visibleInterval;
	}

	public void setVisibleInterval( Pair<Timestamp, Timestamp> val ) {
		if ( ( val == null ) || ( val.getFirst() == null ) 
				|| ( val.getSecond() == null ) ) throw new IllegalArgumentException();
		if ( val.getFirst().after( val.getSecond() ) ) visibleInterval = new Pair<>( val.getSecond(), val.getFirst() );
		visibleInterval = val;
	} 

	public boolean isVisibleDeletedMeasure() {
		return visibleDeletedMeasure;
	}

	public void setVisibleDeletedMeasure( boolean visibleDeletedMeasure ) {
		this.visibleDeletedMeasure = visibleDeletedMeasure;
	}

	public boolean isVisibleOnlyCurrentFilial() {
		return visibleOnlyCurrentFilial;
	}

	public void setVisibleOnlyCurrentFilial( boolean flag ) {
		visibleOnlyCurrentFilial = flag;
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public MainTreeNode getRoot() {
		if ( root != null ) return root;
		Number num = ResourceKeeper.getObject( OType.DEFAULT_BRANCH_OFFICE_CODE );
		List<FilialInfo> lfi = ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO );
		if ( lfi == null) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasureDataProvider.getRoot() :"
					+ " resource BRANCH_OFFICES_INFO not found" );
			return null;
		}
		FilialInfo found = null;
		for ( FilialInfo fi : lfi ) {
			if ( fi.getCode() == ENTERPRISE_CODE ) found = fi;
			if ( num.equals( fi.getCode() ) ) visibleOnlyCurrentFilial = ( fi.getShift() > 1 ) ;
		}
		if ( found == null) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasureDataProvider.getRoot() :"
					+ " in resource BRANCH_OFFICES_INFO not found item with code=0" );				
			return null;
		}		
		root = new MainTreeNode( found );			
		return root;
	}

	@Override
	public boolean isLeaf( MainTreeNode node ) {
		return ( ( ( node == null ) || ( node.getUserObject() == null ) ) ? true 
				: ( node.getUserObject() instanceof Measure  ? true : false ) );
	}
	
	@Override
	public void loadChilds( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		try {
			if ( ( node == null ) || ( node.getUserObject() == null ) ) {
				throw new NullArgumentException( "MeasureDataProvider.loadChilds() : node is null" );
			}
			if ( node.getUserObject() instanceof FilialInfo ) { 
				loadBranchOffices( listener );
				return;
			}
			if ( node.getUserObject() instanceof Division ) {
				loadMeasure( node , listener );
				return;
			}
			throw new IllegalArgumentException( "MeasureDataProvider.loadChilds() take wrong node :" + node );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasureDataProvider.loadChilds() : ", ex );
			listener.childsLoadFailed( ex ); 
		}
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * add outer measure
	 * @param m is Measure object 
	 * @return true if collection changed
	 */
	public boolean addOuterMeasure( Measure m ) {
		if ( ( m == null ) || ( m.getBranchOffice() == null ) )return false;
		if ( ( outer == null ) || ( outer.size() == 0 ) ) outer = new ArrayList<>();
		return outer.add( m );
	}
	
	/**
	 * remove outer measure
	 * @param m - measure object
	 * @param useReferenceEquals -if true then use operation == else use function equals()
	 * @return true if remove successfully
	 */
	public boolean removeOuterMeasure( Measure m, boolean useReferenceEquals ) {
		if ( ( outer == null ) || ( m == null ) ) return false;
		for ( int i = 0; i < outer.size(); i++ ) {
			if ( useReferenceEquals && ( outer.get( i ) == m ) ) return outer.remove( i ) != null;
			if ( !useReferenceEquals && outer.get( i ).equals( m ) ) return outer.remove( i ) != null;
		}
		return false;
	}

	/**
	 * clear outer measure
	 * @return true if remove successfully
	 */
	public void clearOuterMeasure() {
		List<Measure> tmp = outer;
		outer = Collections.emptyList();
		if ( tmp !=null ) tmp.clear();
	}

	/**
	 * load branch offices
	 * @param listener 
	 */
	private void loadBranchOffices( ChildsListener<MainTreeNode> listener ) {
		List<Integer> li = makeBranchOfficeIds();		
		List<Division> res = OrmHelper.findByQueryWithParam(QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_DIVISIONS_BY_ID), 
				Division.class, 
				new Pair<Object, Object>( "ids", li ) );
		listener.childsLoadCompleted( ( ( res == null )  || ( res.size() == 0 ) ) ? BASIS_EMPTY_LIST 
				: convertList( res ) );		
	}

	/**
	 * make branch office IDs from FilialInfo's list
	 * @return list of branch office IDs
	 */
	private List<Integer> makeBranchOfficeIds() {
		List<FilialInfo> lfi = ResourceKeeper.getObject( ResourceKeeper.OType.BRANCH_OFFICES_INFO );
		if ( ( lfi == null ) || ( lfi.size() == 0 ) ) throw new NullPointerException( "MeasureDataProvider."
				+ "loadBranchOffices() : resource BRANCH_OFFICES_INFO not found " );

		List<Integer> li = new ArrayList<>();
		if ( !visibleOnlyCurrentFilial ) {			
			for ( FilialInfo fi : lfi ) {
				if ( ( fi.getCode() > 0 ) && ( fi.getCode() < 100 ) ) li.add( fi.getRootDivisionCode() );
			}
		} else {
			Byte cbo = ResourceKeeper.getObject( ResourceKeeper.OType.DEFAULT_BRANCH_OFFICE_CODE );			
			if ( cbo == null ) throw new NullPointerException( "MeasureDataProvider.loadBranchOffices() :"
					+ " resource DEFAULT_BRANCH_OFFICE_CODE not found" );
			FilialInfo found = null;
			for ( FilialInfo fii : lfi ) {
				if ( fii.getCode() == cbo ) {
					found = fii; 
					break;
				}
			}
			if ( found == null ) throw new NullPointerException( "MeasureDataProvider.loadBranchOffices() :"
					+ " not found DEFAULT_BRANCH_OFFICE_CODE in BRANCH_OFFICES_INFO" );
			if ( found.getCode() >= 100 ) {
				FilialInfo found2 = null;
				for ( FilialInfo fii : lfi ) {
					if ( ( fii.getShift() == found.getShift() ) && ( fii.getCode() > 0 ) && ( fii.getCode() <= 100 ) ) {
						found2 = fii; 
						break;
					}
				}
				if ( found2 == null ) throw new NullPointerException( "MeasureDataProvider.loadBranchOffices() :"
						+ " not found DEFAULT_BRANCH_OFFICE_CODE in BRANCH_OFFICES_INFO" );
				found = found2;
			}
			li.add( found.getRootDivisionCode() );	
		}
		return li;
	}
	
	/**
	 * load Measure entities 
	 * @param node - MainTreeNode node
	 * @param l - load listener
	 */
	private void loadMeasure( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		List<Measure> res = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_MEASURES ), 
				Measure.class, 
				new Pair<Object, Object>( "boffice", Arrays.asList( node.getUserObject() ) ),
				new Pair<Object, Object>( "intervalBegin", visibleInterval.getFirst() ),
				new Pair<Object, Object>( "intervalEnd", visibleInterval.getSecond() ),
				new Pair<Object, Object>( "dts", ( visibleDeletedMeasure ? new java.util.Date() :
					ResourceKeeper.getObject( OType.WAR ) ) ) );
		List<Measure> lm = joinLists( res, getOuterMeasure( ( Division ) node.getUserObject() ) );
		listener.childsLoadCompleted( ( ( lm == null ) || ( lm.size() == 0 ) ) ? BASIS_EMPTY_LIST : convertList( lm ) );
	}
	
	private List<Measure> getOuterMeasure( Division d ) {
		if ( outer == null ) return Collections.emptyList();
		List<Measure> lm = Collections.emptyList();
		for ( Measure m : outer ) {
			if ( m.getBranchOffice().equals( d ) ) {
				if ( lm.size() == 0 ) lm = new ArrayList<>();
				lm.add( m );
			}
		}
		return lm;
	}
}