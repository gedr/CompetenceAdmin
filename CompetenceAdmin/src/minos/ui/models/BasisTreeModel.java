package minos.ui.models;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


public abstract class BasisTreeModel implements TreeModel{
	private EventListenerList listenerList = new EventListenerList();
	
	public BasisTreeModel() { }
	
	public abstract void refresh();	

	@Override
	public void addTreeModelListener(TreeModelListener arg) {
		listenerList.add(TreeModelListener.class, arg);		
	}

	@Override
    public void removeTreeModelListener(TreeModelListener arg) {
        listenerList.remove(TreeModelListener.class, arg);
    }

	@Override
	public void valueForPathChanged(TreePath tpath, Object newValue) { }		
}