package minos.ui.models;

import com.alee.extended.tree.AsyncUniqueNode;

public class TreeElement extends AsyncUniqueNode {
	private static final long serialVersionUID = 1L;

	public enum TreeElementType { UNKNOWN, CATALOG, COMPETENCE, LEVEL, INDICATOR };
	private TreeElementType type;
	private int current;
	private int ancestor;
	private String name;
	private short variety;
	
	public TreeElement() {
		super();
		type = TreeElementType.UNKNOWN;
		current = 0;
		ancestor = 0;
		name = null;
		variety = 0;				
	}
	
	public TreeElement(TreeElementType type, int current, int ancestor, short variety, String name) {
		super();
		this.type = type;
		this.current = current;
		this.ancestor = ancestor;
		this.variety = variety;
		this.name = name;
	}
	
	public int getAncestor() {
		return ancestor;
	}
	
	public void setAncestor(int ancestor) {
		this.ancestor = ancestor;
	}
	
	public int getCurrent() {
		return current;
	}
	
	public void setCurrent(int current) {
		this.current = current;
	}
	
	public TreeElementType getType() {
		return type;
	}
	
	public void setType(TreeElementType type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(obj == this) return true;
		if( !(obj instanceof TreeElement) ) return false;
		TreeElement other = (TreeElement) obj;
		return ( (other.type == this.type) && (other.ancestor == this.ancestor) && (other.current == this.current) &&
				(other.variety == this.variety) && (other.name.equals(this.name) ) );
	}

	@Override
	public String toString() {
		return "[TreeElement: " + name + "] type:" + type.toString() + "   current:" + String.valueOf(current) + "   ancestor:" + String.valueOf(ancestor);
	}

	public short getVariety() {
		return variety;
	}

	public void setVariety(short variety) {
		this.variety = variety;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}