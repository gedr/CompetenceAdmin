package minos.ui.dialogs;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.alee.laf.rootpane.WebDialog;

public abstract class BasisDlg<T> extends WebDialog implements ActionListener {
	// ==========================================================================
	// Constants
	// ==========================================================================
	private static final long serialVersionUID = 1L;

	protected static final String CMD_OK 	= "OK";
	protected static final String CMD_CANCEL= "CANCEL";

	// ==========================================================================
	// Fields
	// ==========================================================================
	protected T source;
	protected T result;
	protected boolean readOnly;

	// ==========================================================================
	// Constructors
	// ==========================================================================
	public BasisDlg( Window owner, String title, T source, boolean readOnly ) {
		super( owner, title );
		this.source = source;
		this.readOnly = readOnly;
		initUI();				
	}

	public BasisDlg( Window owner, String title ) {
		super( owner, title );
	}

	// ==========================================================================
	// Getter & Setter
	// ==========================================================================
	public T getResult() {
		return result;
	}

	// ==========================================================================
	// Methods for/from SuperClass/Interfaces
	// ==========================================================================
	@Override
	public void actionPerformed( ActionEvent e ) {
		if ( CMD_OK.equals( e.getActionCommand() ) ) save();
		setVisible(false);
	}
	
	// ==========================================================================
	// Methods
	// ==========================================================================
	protected abstract void initUI();
	protected abstract void save();
}
