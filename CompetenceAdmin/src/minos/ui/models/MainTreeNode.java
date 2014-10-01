package minos.ui.models;

import com.alee.extended.tree.AsyncUniqueNode;

public class MainTreeNode  extends AsyncUniqueNode {
	private static final long serialVersionUID = 1L;
	
	private Object addon;
	
	public MainTreeNode() {
		super();

	}
	
	public MainTreeNode( Object obj ) { 
		super( obj );
	}

	public MainTreeNode( Object obj, Object addon ) { 
		super( obj );
		this.addon = addon;
	}

	@Override
	public String toString() {
		return "MainTreeNode [object=" + getUserObject() + "]";
	}

	public Object getAddon() {
		return addon;
	}

	public void setAddon( Object addon ) {
		this.addon = addon;
	}
	
}
