package minos.ui.models;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.alee.extended.tree.AsyncTreeDataProvider;
import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.ChildsListener;
import com.alee.utils.compare.Filter;

abstract public class BasisDataProvider<T extends AsyncUniqueNode> implements AsyncTreeDataProvider<T> {
	private Timestamp currentTimePoint = null;
	protected static final List<TreeElement> emptyList = Collections.emptyList();
	
	public static final Timestamp FUTURE_TIMEPOINT;
	static {
		Calendar cal = Calendar.getInstance();
		cal.set(3333, 3, 3);
		FUTURE_TIMEPOINT = new Timestamp( cal.getTimeInMillis() );		
	}
	
	public BasisDataProvider() {
		setCurrentTimePoint(null);
	}

	public BasisDataProvider(Timestamp timePoint) {
		setCurrentTimePoint(timePoint);
	}

	@Override
	abstract public T getRoot();

	@Override
	abstract public boolean isLeaf(T node);

	@Override
	abstract public void loadChilds(T node, ChildsListener<T> listener);

	@Override
	public Comparator<T> getChildsComparator(T node) {
		return null;
	}

	@Override
	public Filter<T> getChildsFilter(T node) {
		return null;
	}

	public Timestamp getCurrentTimePoint() {
		return currentTimePoint;
	}

	public void setCurrentTimePoint(Timestamp timePoint) {
		this.currentTimePoint = ( timePoint != null ? timePoint :  FUTURE_TIMEPOINT );
	}	
}