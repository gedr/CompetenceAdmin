package minos.ui.dialogs;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import minos.data.services.ORMHelper;
import minos.entities.Catalog;
import net.miginfocom.swing.MigLayout;

import com.alee.extended.panel.CollapsiblePaneListener;
import com.alee.extended.panel.WebCollapsiblePane;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.text.WebTextField;

public class CatalogDlg extends WebDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static final String OK_CMD = "1";
	private static final String CANCEL_CMD = "2";

	private WebTextField txtField = null;
	private Catalog catalog = null;
	private Catalog res = null;
	private boolean readOnly = true;

	public CatalogDlg(Window owner, String title, Catalog catalog, boolean readOnly) {
		super(owner, title);
		this.catalog = catalog;
		this.readOnly  = readOnly;
		initUI(null);		
	}
	
	public static Catalog showCatalogDlg(Window owner, String title, Catalog catalog, boolean readOnly) {
		CatalogDlg dlg = new CatalogDlg(owner, title, catalog, readOnly);
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);
		return dlg.getResult();		
	}	
	
	private void initUI(ImageIcon icon) {
		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" )); 

		add(new WebLabel(icon), "cell 0 0 1 3,gapx 0 10,gapy 0 10");
		add(new WebLabel("�������� ��������"), "cell 1 0,growx,aligny top");

		txtField = new WebTextField( 30 );
		txtField.setInputPrompt( "������� �������� �������� ..." );
		txtField.setInputPromptFont( txtField.getFont().deriveFont( Font.ITALIC ) );
		txtField.setEditable( !readOnly );
		add(txtField, "cell 1 1,growx,aligny top");
		if ( (catalog != null) && (catalog.getName() != null) ) txtField.setText( catalog.getName() );		
		
		if ( !readOnly ) {
			WebButton okBtn = new WebButton("OK");
			okBtn.setActionCommand(OK_CMD);		
			okBtn.addActionListener(this);
			add(okBtn, "flowx,cell 1 2,alignx right");
		}
		
		WebButton cancelBtn = new WebButton("������");
		cancelBtn.setActionCommand(CANCEL_CMD);
		cancelBtn.addActionListener(this);		
		add(cancelBtn, "cell 1 2,alignx right");
		addTechInfo();
		addHistoryInfo();
	}	
	
	private void addTechInfo() {
		if ( catalog == null ) return;
		
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "���: " + catalog.getId() + "\n" );
		txtArea.append( "������: " + catalog.getVersion() + "\n" );
		txtArea.append( "������: " + catalog.getStatus() + "\n" );
		txtArea.append( "������: " + catalog.getJournal().getCreateMoment() + "\n" );
		if ( catalog.getVersion() > 1 ) txtArea.append( "������������: " + catalog.getJournal().getEditMoment() + "\n" );
				
		WebScrollPane scrollPane = new WebScrollPane( txtArea, false );
        scrollPane.setPreferredSize( new Dimension( 100, 100 ) );

		WebCollapsiblePane pane = new WebCollapsiblePane( null, "����. ������." , scrollPane );
		pane.setExpanded( false );
		pane.addCollapsiblePaneListener(new CollapsiblePaneListener() {
			private int h = 100;
			
			@Override
			public void expanding(WebCollapsiblePane pane) {
				Dimension d = CatalogDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() + h ) ;
				CatalogDlg.this.setSize(d);
			}
			
			@Override
			public void expanded(WebCollapsiblePane pane) { }

			@Override
			public void collapsing(WebCollapsiblePane pane) { }
			
			@Override
			public void collapsed(WebCollapsiblePane pane) { 
				Dimension d = CatalogDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() - h ) ;
				CatalogDlg.this.setSize(d);
			}
		});
		add(pane, "cell 0 3, span, growx, wrap");
	}

	private void addHistoryInfo() {
		if ( ( catalog == null ) || ( catalog.getVersion() == 1 ) ) return;
		Catalog cat = (Catalog) ORMHelper.findEntity( Catalog.class, catalog.getId(), "ancestorCatalog", "historyCatalogs" );
		if ( cat.getHistoryCatalogs() == null ) return;
				
		WebList list = new WebList( cat.getHistoryCatalogs() );
		list.setEditable( false );
				
		WebScrollPane scrollPane = new WebScrollPane( list, false );
        scrollPane.setPreferredSize( new Dimension( 100, 100 ) );

		WebCollapsiblePane pane = new WebCollapsiblePane( null, "�������" , scrollPane );
		pane.setExpanded( false ); 
		pane.addCollapsiblePaneListener(new CollapsiblePaneListener() {
			private int h = 100;
			
			@Override
			public void expanding(WebCollapsiblePane pane) {
				Dimension d = CatalogDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() + h ) ;
				CatalogDlg.this.setSize(d);
			}
			
			@Override
			public void expanded(WebCollapsiblePane pane) { }

			@Override
			public void collapsing(WebCollapsiblePane pane) { }
			
			@Override
			public void collapsed(WebCollapsiblePane pane) { 
				Dimension d = CatalogDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() - h ) ;
				CatalogDlg.this.setSize(d);
			}
		});
		add(pane, "cell 0 4, span, growx, wrap");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if ( ( e.getActionCommand() == OK_CMD ) &&
				( (catalog == null) || 
						( (catalog != null) && !catalog.getName().equals( txtField.getText() ) ) ) ) {
				res = new Catalog();
				res.setName(txtField.getText());			
		}
		setVisible(false);
	}

	public Catalog getResult() {
		return res;
	}
}