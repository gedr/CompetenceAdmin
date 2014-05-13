package minos.ui.dialogs;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import minos.data.services.ORMHelper;
import minos.entities.Competence;
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

public class CompetenceDlg extends WebDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static final String OK_CMD = "1";
	private static final String CANCEL_CMD = "2";

	private WebTextField nameField = null;
	private WebTextArea	 descrField = null;
	private Competence 	 competence = null;
	private Competence 	 res = null;
	private boolean 	 readOnly = true;
	
	public CompetenceDlg( Window owner, String title, Competence competence, boolean readOnly ) {
		super( owner, title );
		this.competence = competence;
		this.readOnly  = readOnly;
		initUI( null );
	}
		
	public static Competence showCompetenceDlg( Window owner, String title, Competence competence, boolean readOnly ) {
		CompetenceDlg dlg = new CompetenceDlg( owner, title, competence, readOnly );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal( true );
		dlg.pack();
		dlg.setVisible( true );
		return dlg.getResult();		
	}	
	
	private void initUI(ImageIcon icon) {
		setLayout(new MigLayout("", "[][grow]", "[][][][grow][][][]")); // [grow]

		add(new WebLabel(icon), "cell 0 0 1 3,gapx 0 10,gapy 0 10");

		add(new WebLabel("Название компетенции"), "cell 1 0,growx,aligny top");
		nameField = new WebTextField( 30 );
		nameField.setInputPrompt( "Введите название компетенции ..." );
		nameField.setInputPromptFont( nameField.getFont().deriveFont( Font.ITALIC ) );
		nameField.setEditable( !readOnly );
		add(nameField, "cell 1 1,growx,aligny top");
		
		add(new WebLabel("Описание компетенции"), "cell 1 2,growx,aligny bottom,gapy 5 0");
		descrField = new WebTextArea();
		descrField.setEditable( !readOnly );
		descrField.setLineWrap( true );
		add(new WebScrollPane( descrField ), "cell 0 3 2 1,grow");
		
		if ( competence != null ) {
			if( competence.getName() != null ) nameField.setText( competence.getName());
			if( competence.getDescription() != null ) descrField.setText( competence.getDescription());
		}
		
		if ( !readOnly ) {
			WebButton okBtn = new WebButton("OK");
			okBtn.setActionCommand(OK_CMD);		
			okBtn.addActionListener(this);
			add(okBtn, "flowx,cell 1 4,alignx right");
		}
		
		WebButton cancelBtn = new WebButton("Отмена");
		cancelBtn.setActionCommand(CANCEL_CMD);
		cancelBtn.addActionListener(this);		
		add(cancelBtn, "cell 1 4,alignx right");
		addTechInfo();
		addHistoryInfo();
	}	
	
	private void addTechInfo() {
		if ( competence == null ) return;		
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + competence.getId() + "\n" );
		txtArea.append( "Версия: " + competence.getVersion() + "\n" );
		txtArea.append( "Статус: " + competence.getStatus() + "\n" );
		txtArea.append( "Создан: " + competence.getJournal().getCreateMoment() + "\n" );
		if ( competence.getVersion() > 1 ) txtArea.append( "Редактирован: " + competence.getJournal().getEditMoment() + "\n" );
				
		WebScrollPane scrollPane = new WebScrollPane( txtArea, false );
        scrollPane.setPreferredSize( new Dimension( 100, 100 ) );

		WebCollapsiblePane pane = new WebCollapsiblePane( null, "техн. информ." , scrollPane );
		pane.setExpanded( false );
		pane.addCollapsiblePaneListener(new CollapsiblePaneListener() {
			private int h = 100;
			
			@Override
			public void expanding(WebCollapsiblePane pane) {
				Dimension d = CompetenceDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() + h ) ;
				CompetenceDlg.this.setSize(d);
			}
			
			@Override
			public void expanded(WebCollapsiblePane pane) { }

			@Override
			public void collapsing(WebCollapsiblePane pane) { }
			
			@Override
			public void collapsed(WebCollapsiblePane pane) { 
				Dimension d = CompetenceDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() - h ) ;
				CompetenceDlg.this.setSize(d);
			}
		});
		add(pane, "cell 0 5, span, growx, wrap");
	}

	private void addHistoryInfo() {
		if ( ( competence == null ) || ( competence.getVersion() == 1 ) ) return;
		Competence cmt = (Competence) ORMHelper.findEntity( Competence.class, competence.getId(), "ancestorCompetence", "historyCompetences" );
		if ( cmt.getHistoryList() == null ) return;
				
		WebList list = new WebList( cmt.getHistoryList() );
		list.setEditable( false );
				
		WebScrollPane scrollPane = new WebScrollPane( list, false );
        scrollPane.setPreferredSize( new Dimension( 100, 100 ) );

		WebCollapsiblePane pane = new WebCollapsiblePane( null, "История" , scrollPane );
		pane.setExpanded( false ); 
		pane.addCollapsiblePaneListener(new CollapsiblePaneListener() {
			private int h = 100;
			
			@Override
			public void expanding(WebCollapsiblePane pane) {
				Dimension d = CompetenceDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() + h ) ;
				CompetenceDlg.this.setSize(d);
			}
			
			@Override
			public void expanded(WebCollapsiblePane pane) { }

			@Override
			public void collapsing(WebCollapsiblePane pane) { }
			
			@Override
			public void collapsed(WebCollapsiblePane pane) { 
				Dimension d = CompetenceDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() - h ) ;
				CompetenceDlg.this.setSize(d);
			}
		});
		add(pane, "cell 0 6, span, growx, wrap");
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if ( ( e.getActionCommand() == OK_CMD ) &&
				( (competence == null) || 
						( (competence != null) && 
								( !competence.getName().equals( nameField.getText() ) || 
										!competence.getDescription().equals( descrField.getText() ) ) ) ) ) {
				res = new Competence();
				res.setName( nameField.getText() );
				res.setDescription( descrField.getText() );
		}
		setVisible(false);
	}

	public Competence getResult() {
		return res;
	}
}