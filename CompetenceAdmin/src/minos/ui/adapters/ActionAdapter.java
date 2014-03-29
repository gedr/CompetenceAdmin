package minos.ui.adapters;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

public class ActionAdapter extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private ActionListener handler;
		
	public ActionAdapter(String text, ImageIcon icon, String actionCmd, String toolTip, 
			ActionListener handler, Integer mnemonic) {				
		super(text,  icon);
		this.handler = handler;
		if(toolTip != null) putValue(SHORT_DESCRIPTION, toolTip);
		if(mnemonic != null) putValue(MNEMONIC_KEY, mnemonic);
		if(actionCmd != null) putValue(ACTION_COMMAND_KEY, actionCmd);       
	}
	
	public static ActionAdapter build(String text, ImageIcon icon, String actionCmd, 
			String toolTip, ActionListener handler, Integer mnemonic) {		
		return new ActionAdapter(text, icon, actionCmd, toolTip, handler, mnemonic);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(handler != null)
			handler.actionPerformed(e);	    	
	}
}
