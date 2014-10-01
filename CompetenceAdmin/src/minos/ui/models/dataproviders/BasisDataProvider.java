package minos.ui.models.dataproviders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import minos.ui.models.MainTreeNode;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

import com.alee.extended.tree.AsyncTreeDataProvider;
import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.ChildsListener;
import com.alee.utils.compare.Filter;

abstract public class BasisDataProvider<T extends AsyncUniqueNode> implements AsyncTreeDataProvider<T> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	protected final List<T> BASIS_EMPTY_LIST = Collections.emptyList();

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private Timestamp currentTimePoint = null;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public BasisDataProvider() {
		this( null );
	}

	public BasisDataProvider( Timestamp timePoint ) {
		this.currentTimePoint = timePoint;
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public Timestamp getCurrentTimePoint() {
		return ( currentTimePoint != null ? currentTimePoint  
				: ( Timestamp ) ResourceKeeper.getObject( OType.DAMNED_FUTURE ) );
	}

	public void setCurrentTimePoint( Timestamp timePoint ) {
		this.currentTimePoint = timePoint;  
	}	

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	abstract public T getRoot();

	@Override
	abstract public boolean isLeaf( T node );

	@Override
	abstract public void loadChilds( T node, ChildsListener<T> listener );

	@Override
	public Comparator<T> getChildsComparator( T node ) {
		return null;
	}

	@Override
	public Filter<T> getChildsFilter( T node ) {
		return null;
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	protected <L> List<MainTreeNode> convertList( List<L> lst ) {
		List<MainTreeNode>  res = new ArrayList<>();
		for ( L c : lst ) res.add( new MainTreeNode( c ) );
		return res;
	}
	
	protected <K> List<K> joinLists( List<K> lst1, List<K> lst2 ) {
		List<K> res = new ArrayList<>();
		if ( ( lst1 != null ) && ( lst1.size() > 0 ) ) res.addAll( lst1 );
		if ( ( lst2 != null ) && ( lst2.size() > 0 ) ) res.addAll( lst2 );
		return res;
	}
}