package minos.ui.panels;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import minos.utils.AuxFunctions;

import com.alee.laf.panel.WebPanel;

public class BasisPanel extends WebPanel implements AncestorListener, InternalFrameListener, WindowListener, 
HierarchyListener {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	// private static Logger log = LoggerFactory.getLogger( BasisPanel.class );
	private boolean useGlass;
	private boolean visibleGlass;
	private GlassPanel glass = null;
	private Component prevGlass = null;
	private Container base = null;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public BasisPanel( boolean useGlass ) {
		this.useGlass = useGlass;
		visibleGlass = false;
		addAncestorListener( this );
		addHierarchyListener( this );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void ancestorAdded( AncestorEvent a ) {
		if ( !useGlass ) return;
		glass = setGlass( a.getAncestor() );
		if ( visibleGlass ) setVisibleGlass( true );
	}

	@Override
	public void ancestorRemoved( AncestorEvent a ) {
		if ( !useGlass ) return;
		if ( glass != null ) setVisibleGlass( false );
		removeGlass();
		glass = null;
	}

	@Override
	public void hierarchyChanged( HierarchyEvent e ) {
		if ( !useGlass ) return;
		glass = setGlass( getParent() );
		if ( visibleGlass ) setVisibleGlass( true );
	}

	@Override
	public void ancestorMoved( AncestorEvent a ) { }

	@Override
	public void windowActivated( WindowEvent e ) { }

	@Override
	public void windowClosed( WindowEvent e ) { }

	@Override
	public void windowClosing( WindowEvent e ) { }

	@Override
	public void windowDeactivated( WindowEvent e ) { }

	@Override
	public void windowDeiconified( WindowEvent e ) { }

	@Override
	public void windowIconified( WindowEvent e ) { }

	@Override
	public void windowOpened( WindowEvent e ) { }

	@Override
	public void internalFrameActivated( InternalFrameEvent e ) { }

	@Override
	public void internalFrameClosed( InternalFrameEvent e ) { }

	@Override
	public void internalFrameClosing( InternalFrameEvent e ) { }

	@Override
	public void internalFrameDeactivated( InternalFrameEvent e ) { }

	@Override
	public void internalFrameDeiconified( InternalFrameEvent e ) { }

	@Override
	public void internalFrameIconified( InternalFrameEvent e ) { }

	@Override
	public void internalFrameOpened( InternalFrameEvent e ) { }
	
	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public void setVisibleGlass( boolean val ) {
		if ( !useGlass ) return;
		this.visibleGlass = val;
		if ( visibleGlass && ( glass == null ) ) glass = setGlass( getParent() );
		if ( glass != null ) AuxFunctions.invokeSwing( new Runnable() {

			@Override
			public void run() {				
				glass.setVisible( visibleGlass );
				if ( visibleGlass ) AuxFunctions.repaintComponent( glass );
			}
		});
	}
	
	public boolean isVisibleGlass() {
		return visibleGlass;
	}
	
	public void setUseGlass( boolean val ) {
		if ( useGlass == val ) return;
		useGlass = val;		
		if ( useGlass && ( glass == null ) ) glass = setGlass( getParent() );
		if ( !useGlass && ( glass != null ) ) {
			setVisibleGlass( false );
			removeGlass();
			glass = null;
		}
	}

	public boolean isUseGlass() {
		return useGlass;
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	private GlassPanel setGlass( Container c ) {
		while ( c != null ) {
			if ( c instanceof BasisPanel ) {
				return ( ( BasisPanel ) c ).glass;
			}
			if ( c instanceof JInternalFrame ) {
				base = c;
				prevGlass = ( ( JInternalFrame ) c ).getGlassPane();
				GlassPanel gls = new GlassPanel();
				( ( JInternalFrame ) c ).setGlassPane( gls );
				( ( JInternalFrame ) c ).addInternalFrameListener( this );
				return gls;
			}
			if ( c instanceof JDialog ) {
				base = c;
				prevGlass = ( ( JDialog ) c ).getGlassPane();
				GlassPanel gls = new GlassPanel();
				( ( JDialog ) c ).setGlassPane( gls );
				( ( JDialog ) c ).addWindowListener( this );
				return gls;
			} 
			if ( c instanceof JFrame ) {
				base = c;
				prevGlass = ( ( JFrame ) c ).getGlassPane();
				GlassPanel gls = new GlassPanel();
				( ( JFrame ) c ).setGlassPane( gls );
				( ( JFrame ) c ).addWindowListener( this );
				return gls;
			}
			c = c.getParent();
		}
		return null;
	}

	private void removeGlass() {
		glass = null;
		if ( base == null ) return;
		if ( base instanceof JInternalFrame ) {
			( ( JInternalFrame ) base ).setGlassPane( prevGlass );
			( ( JInternalFrame ) base ).removeInternalFrameListener( this );
		} else if ( base instanceof JDialog ) {
			( ( JDialog ) base ).setGlassPane( prevGlass );
			( ( JDialog ) base ).removeWindowListener( this );
		} else if ( base instanceof JFrame ) {
			( ( JFrame ) base ).setGlassPane( prevGlass );
			( ( JFrame ) base ).removeWindowListener( this );
		}
	}
}