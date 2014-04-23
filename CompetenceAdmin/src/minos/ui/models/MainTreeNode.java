package minos.ui.models;

import com.alee.extended.tree.AsyncUniqueNode;

public class MainTreeNode  extends AsyncUniqueNode {
	private static final long serialVersionUID = 1L;
	
	
	public MainTreeNode() {
		super();
		
	}
	public MainTreeNode( Object obj ) { 
		super( obj );
	}
	
	@Override
	public String toString() {
		return "TreeNode [object=" + getUserObject() + "]";
	}
	
}
