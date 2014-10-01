package minos.ui.panels;

import javax.swing.JLabel;

import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

import com.alee.laf.panel.WebPanel;

public class GlassPanel extends WebPanel {

	private static final long serialVersionUID = 1L;

	public GlassPanel( ) {
		JLabel lbl = new JLabel( ResourceKeeper.getIcon( IType.ANIMATED_GEARS, 0 ) );
		add( lbl );
		setOpaque( false );

	}
	

}
