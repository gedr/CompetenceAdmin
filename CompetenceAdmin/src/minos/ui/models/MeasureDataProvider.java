package minos.ui.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.data.services.ORMHelper;
import minos.entities.Measure;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;
import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Triplet;

import com.alee.extended.tree.ChildsListener;
import com.google.common.base.Joiner;

public class MeasureDataProvider extends BasisDataProvider<MainTreeNode> {
	private static Logger log = LoggerFactory.getLogger( ProfilePatternDataProvider.class );
	
	private static final String jpqlLoadMeasures = " select entity from Measure entity "
			+ " where MOD(entity.id / 16777216, 1) IN (%s)  " // operation ( entity.id/0x1000000 in (  ) ) - generate exception
			+ " and ( ( (entity.start between :intervalBegin and :intervalEnd) or "
			+ "(entity.stop between :intervalBegin and :intervalEnd) ) %s) ";
	private static final String jpqlLoadNotDeletedMeasures = " and (entity.journal.deleteMoment > CURRENT_TIMESTAMP) ";

	private MainTreeNode root = new MainTreeNode( new MeasureRoot() );
	
	private String buildJpqlStatment( Triplet<Integer, String, Byte[]> val ) {
		if ( ( val == null ) || ( val.getThird() == null ) || ( val.getThird().length == 0 ) ) {
			throw new IllegalArgumentException( "MeasureDataProvider.buildJpqlStatment() : val is null" );		
		}
		return String.format( jpqlLoadMeasures, 
				Joiner.on(", ").join( val.getThird() ).toString(), 
				( visibleDeletedMeasure ? " " : jpqlLoadNotDeletedMeasures ) );				
	}
	
	private boolean visibleDeletedMeasure = false;
	private boolean visibleOnlyCurrentFilial = false;
	private Pair<Timestamp, Timestamp> visibleInterval = null;
	private Triplet<Integer, String, Byte[]> currentFilial = null;

	private void loadMeasure( Triplet<Integer, String, Byte[]> t, ChildsListener<MainTreeNode> l ) {
		if ( l == null ) return;
		
		try {
			ORMHelper.openManager();
			List<Measure> res = ORMHelper.getCurrentManager()
					.createQuery( buildJpqlStatment( t ), Measure.class )
					.setParameter( "intervalBegin", visibleInterval.getFirst() )
					.setParameter( "intervalEnd", visibleInterval.getSecond() )
					.getResultList();
			ORMHelper.closeManager();
			if ( ( res == null ) || ( res.size() == 0 ) ) {
				l.childsLoadCompleted(BASIS_EMPTY_LIST);
				return;
			}
			List<MainTreeNode> lst = new ArrayList<>();
			for (Measure m : res) lst.add( new MainTreeNode( m ) );
			l.childsLoadCompleted(lst);
		} catch ( Exception e ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( " MeasureDataProvider.loadMeasure() : ", e );
			l.childsLoadFailed( e );
		}		
	}
	
	public static class MeasureRoot {
		@Override
		public String toString() {
			return "MeasureRoot must be invisible";
		}
	}	
	
	public MeasureDataProvider() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get( Calendar.YEAR );
		cal.set( year, Calendar.JANUARY, 1 );		
		visibleInterval = new Pair<>( new Timestamp( cal.getTimeInMillis() ), new Timestamp( System.currentTimeMillis() ) );
		setVisibleOnlyCurrentFilial( false );
	}

	@Override
	public MainTreeNode getRoot() {		
		return root;
	}

	@Override
	public boolean isLeaf( MainTreeNode node ) {
		return ( ( node.getUserObject() instanceof Measure ) ? true : false );
	}
	
	@Override
	public void loadChilds( MainTreeNode node, ChildsListener<MainTreeNode> listener ) {
		if ( node.getUserObject() instanceof MeasureRoot ) {
			if ( visibleOnlyCurrentFilial ) {
				loadMeasure( currentFilial,  listener );
				return;
			}
			
			@SuppressWarnings("unchecked")
			List<Triplet<Integer, String, Byte[]>> ltisb = 
					( List<Triplet<Integer, String, Byte[]>> ) Resources.getInstance().get( ResourcesConst.FILIALS_PREFIX );
			if ( ( ltisb == null ) || ( ltisb.size() == 0 ) ) {
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasureDataProvider.loadChilds() : cannot find resource FILIALS_PREFIX" );
				listener.childsLoadFailed( new IllegalArgumentException() );
				return;			
			}
			List<MainTreeNode> lmtn = new ArrayList<>();
			for ( Triplet<Integer, String, Byte[]> t : ltisb ) lmtn.add( new MainTreeNode( t ) );				
			listener.childsLoadCompleted( lmtn );
		}
		
		if ( node.getUserObject() instanceof Triplet<?, ?, ?> ) {
			@SuppressWarnings("unchecked")
			Triplet<Integer, String, Byte[]> t = ( Triplet<Integer, String, Byte[]> ) node.getUserObject();
			loadMeasure( t, listener );
		}		
	}

	public Pair<Timestamp, Timestamp> getVisibleInterval() {
		return visibleInterval;
	}

	public void setVisibleInterval( Pair<Timestamp, Timestamp> val ) {
		if ( ( val == null ) || ( val.getFirst() == null ) || ( val.getSecond() == null ) ) throw new IllegalArgumentException();
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
		if ( visibleOnlyCurrentFilial == flag ) return;

		visibleOnlyCurrentFilial = false;
		currentFilial = null;
		if ( !flag ) return;
		
		Integer cf = ( Integer ) Resources.getInstance().get( ResourcesConst.CURRENT_FILIAL );		
		if ( cf == null ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasureDataProvider.setVisibleOnlyCurrentFilial() : cannot find resource CURRENT_FILIAL" );
			return;			
		}
		
		@SuppressWarnings("unchecked")
		List<Triplet<Integer, String, Byte[]>> ltisb = 
				( List<Triplet<Integer, String, Byte[]>> ) Resources.getInstance().get( ResourcesConst.FILIALS_PREFIX );
		if ( ( ltisb == null ) || ( ltisb.size() == 0 ) ){
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasureDataProvider.setVisibleOnlyCurrentFilial() : cannot find resource FILIALS_PREFIX" );
			return;			
		}

		for ( Triplet<Integer, String, Byte[]> t : ltisb ) {
			if ( ( t != null ) && cf.equals( t.getFirst() ) ) {
				currentFilial = t;
				visibleOnlyCurrentFilial = true;
				return;
			}
		}
		if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MeasureDataProvider.setVisibleOnlyCurrentFilial() : cannot find CURRENT_FILIAL in FILIALS_PREFIX" );
	}
}