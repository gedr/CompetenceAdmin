package minos.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

import com.alee.laf.button.WebButton;
import com.alee.laf.rootpane.WebDialog;

public class ComponentDlg extends WebDialog implements ActionListener, ComponentListener {
	// ==========================================================================
	// Constants
	// ==========================================================================
	private static final long serialVersionUID = 1L;
	
	protected static final String CMD_OK 	= "OK";
	protected static final String CMD_CANCEL= "CANCEL";

	// ==========================================================================
	// Fields
	// ==========================================================================
	private int result;
	private Dimension d;
	
	// ==========================================================================
	// Constructors
	// ==========================================================================
	public ComponentDlg( Window owner, String title, Component component, Dimension dim ) {
		super( owner, title );

		JPanel panel = new JPanel();
		panel.add( new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 24 ), CMD_OK, 
				"Сохранить", this, KeyEvent.VK_ENTER ) ), "cell 1 2,alignx right" );
		panel.add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CMD_CANCEL, 
				"Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), "cell 1 2,alignx right" );
		
		setLayout( new BorderLayout() );
		add( component, BorderLayout.CENTER );
		add( panel, BorderLayout.SOUTH );
		
		d = dim;
		if ( d != null )  {
			setPreferredSize( d );
		} else {
			pack();
			d = getSize();
		}
		addComponentListener( this );
		component.setVisible( true );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void actionPerformed( ActionEvent e ) {
		if ( e.getActionCommand().equals( CMD_OK ) ) result = JOptionPane.OK_OPTION;
		if ( e.getActionCommand().equals( CMD_CANCEL ) ) result = JOptionPane.CANCEL_OPTION;		
		setVisible( false );
	}

	@Override
	public void componentResized( ComponentEvent e ) { 
		d.setSize( e.getComponent().getWidth(), e.getComponent().getHeight() );
	}

	
	@Override
	public void componentShown( ComponentEvent e ) { }
	
	@Override
	public void componentMoved( ComponentEvent e ) { }
	
	@Override
	public void componentHidden( ComponentEvent e ) { }


	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * make Dialog what contain component in CENTRE
	 * @param owner - window owner
	 * @param title - Dialog title
	 * @param component - work component
	 * @return JOptionPane.OK_OPTION if user press OK button; JOptionPane.CANCEL_OPTION if user press CANCEL button
	 */
	public static int show( Window owner, String title, Component component, Dimension dim ) {
		ComponentDlg dlg = new ComponentDlg( owner, title, component, dim );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);	
		return dlg.getResult();		
	}	
	
	public int getResult() {
		return result;
	}

}