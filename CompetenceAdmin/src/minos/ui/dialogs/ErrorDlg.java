package minos.ui.dialogs;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.ImageIcon;

import net.miginfocom.swing.MigLayout;

import com.alee.extended.panel.WebCollapsiblePane;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;

public class ErrorDlg extends WebDialog implements ActionListener {
	private static final long serialVersionUID = -7490513327570966697L;
	private static final String OK_CMD 		= "1";

	private WebButton okBtn = null;
	private WebTextArea txtArea = null;
	private WebCollapsiblePane pane = null;
	
	public ErrorDlg( Window owner, String title, String content, Exception ex, ImageIcon icon ) {
		super(owner, title);	
		
        okBtn = new WebButton( "Закрыть" );
		okBtn.setActionCommand( OK_CMD );		
		okBtn.addActionListener( this );

		
		setLayout( new MigLayout( "", "[][grow]", "[][][grow][]" ));
		add(new WebLabel(icon) );
		add(new WebLabel( content ), "wrap");
		
		add( okBtn, "span, flowx,alignx right, wrap" );
		
		if ( ex != null ) {
			StringWriter sw = new StringWriter();
			ex.printStackTrace( new PrintWriter( sw ) );
			txtArea = new WebTextArea ();
			txtArea.setEditable( false );
			txtArea.append( sw.toString() );
			WebScrollPane scrollPane = new WebScrollPane( txtArea, false );
			scrollPane.setPreferredSize( new Dimension( 100, 100 ) );
			pane = new WebCollapsiblePane( null, "Детали" , scrollPane );
			pane.setExpanded( false );
			add( pane, "span, growx, growy, wrap");
			return;
		}
		setResizable( false );
	}
	
	public static void show( Window owner, String title, String content, Exception ex, ImageIcon icon ) {
		ErrorDlg dlg = new ErrorDlg(owner, title, content, ex, icon);
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);				
	}	
	
	@Override
	public void actionPerformed( ActionEvent e ) {
		switch ( e.getActionCommand() ) {
		case OK_CMD:
			setVisible( false );
			break;			
		}		
	}

}
