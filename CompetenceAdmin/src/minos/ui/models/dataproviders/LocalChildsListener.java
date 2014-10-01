package minos.ui.models.dataproviders;

import java.util.List;

import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.ChildsListener;

public class LocalChildsListener<T  extends AsyncUniqueNode > implements ChildsListener<T> {
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private Throwable cause = null;
	private List<T> childs = null;
	private boolean operationComplete = false;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public LocalChildsListener() { }

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public Throwable getCause() {
		return cause;
	}

	public List<T> getChilds() {
		return childs;
	}

	public boolean isOperationComplete() {
		return operationComplete;
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void childsLoadCompleted( List<T> childs ) {
		this.childs = childs;
		operationComplete = true;		
	}

	@Override
	public void childsLoadFailed( Throwable cause ) {
		this.cause = cause;		
		operationComplete = false;
	}
}