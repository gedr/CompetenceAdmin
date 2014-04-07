package minos.ui.dialogs;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.miginfocom.swing.MigLayout;
import minos.data.services.ORMHelper;
import minos.data.services.ORMHelper.QueryType;
import minos.entities.Indicator;
import minos.entities.Level;
import minos.ui.panels.CompetencePanel;

import com.alee.extended.panel.CollapsiblePaneListener;
import com.alee.extended.panel.WebCollapsiblePane;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.text.WebTextField;

public class IndicatorDlg extends WebDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static final String OK_CMD = "1";
	private static final String CANCEL_CMD = "2";
	
	private static Logger log = LoggerFactory.getLogger( CompetencePanel.class );

	private WebTextField txt = null;
	private  WebComboBox lvl = null; 
	
	private Indicator 	 indicator = null;
	private Indicator 	 res = null;
	private boolean 	 readOnly = true;
	private Level[]  	 levels = null;
	
	public IndicatorDlg( Window owner, String title, Indicator indicator, boolean readOnly ) {
		super( owner, title );
		this.indicator = indicator;
		this.readOnly  = readOnly;
		List<Level> lst = ORMHelper.executeQuery( QueryType.NAMED, "Level.findAll", Level.class );
		if ( ( lst == null ) || ( lst.size() == 0 ) ) {
			if ( log.isErrorEnabled() ) log.error( "IndicatorDlg.IndicatorDlg() :  levels not found" );
			return;
		}
		levels = new Level[ lst.size() ];
		int ind = 0;
		for( Level l : lst ) levels[ ind++ ] = l;
		initUI( null );
	}
		
	public static Indicator showIndicatorDlg( Window owner, String title, Indicator indicator, boolean readOnly ) {
		IndicatorDlg dlg = new IndicatorDlg( owner, title, indicator, readOnly );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal( true );
		dlg.pack();
		dlg.setVisible( true );
		return dlg.getResult();		
	}	

	private void initUI(ImageIcon icon) {
		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" + ( indicator == null ? "" : "[][]" ) ) ); // [grow]

		add( new WebLabel(icon), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );

		add( new WebLabel( "Название индикатора" ), "cell 1 0,growx,aligny top");
		txt = new WebTextField( 30 );
		txt.setInputPrompt( "Введите название индикатора ..." );
		txt.setInputPromptFont( txt.getFont().deriveFont( Font.ITALIC ) );
		txt.setEditable( !readOnly );
		add( txt, "cell 1 1,growx,aligny top" );
		
		add( new WebLabel( "Уровень индикатора" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );		
		lvl = new WebComboBox( levels );
		add( lvl, "cell 0 3 2 1,grow" );
		
		if ( indicator != null ) {
			if( indicator.getName() != null ) txt.setText( indicator.getName() );
			if( indicator.getLevel() != null ) lvl.setSelectedIndex( indicator.getLevel().getId() - 1 );;
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
		if ( indicator == null ) return;		
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + indicator.getId() + "\n" );
		txtArea.append( "Версия: " + indicator.getVersion() + "\n" );
		txtArea.append( "Статус: " + indicator.getStatus() + "\n" );
		txtArea.append( "Создан: " + indicator.getJournal().getCreateMoment() + "\n" );
		if ( indicator.getVersion() > 1 ) txtArea.append( "Редактирован: " + indicator.getJournal().getEditMoment() + "\n" );
				
		WebScrollPane scrollPane = new WebScrollPane( txtArea, false );
        scrollPane.setPreferredSize( new Dimension( 100, 100 ) );

		WebCollapsiblePane pane = new WebCollapsiblePane( null, "техн. информ." , scrollPane );
		pane.setExpanded( false );
		pane.addCollapsiblePaneListener(new CollapsiblePaneListener() {
			private int h = 100;
			
			@Override
			public void expanding(WebCollapsiblePane pane) {
				Dimension d = IndicatorDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() + h ) ;
				IndicatorDlg.this.setSize(d);
			}
			
			@Override
			public void expanded(WebCollapsiblePane pane) { }

			@Override
			public void collapsing(WebCollapsiblePane pane) { }
			
			@Override
			public void collapsed(WebCollapsiblePane pane) { 
				Dimension d = IndicatorDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() - h ) ;
				IndicatorDlg.this.setSize(d);
			}
		});
		add(pane, "cell 0 5, span, growx, wrap");
	}

	private void addHistoryInfo() {
		if ( ( indicator == null ) || ( indicator.getVersion() == 1 ) ) return;
		Indicator indc = (Indicator) ORMHelper.findEntity( Indicator.class, indicator.getId(), "ancestorIndicator", "historyIndicators" );		
		if ( indc.getHistoryIndicators() == null ) return;
				
		WebList list = new WebList( indc.getHistoryIndicators() );
		list.setEditable( false );
				
		WebScrollPane scrollPane = new WebScrollPane( list, false );
        scrollPane.setPreferredSize( new Dimension( 100, 100 ) );

		WebCollapsiblePane pane = new WebCollapsiblePane( null, "История" , scrollPane );
		pane.setExpanded( false ); 
		pane.addCollapsiblePaneListener(new CollapsiblePaneListener() {
			private int h = 100;
			
			@Override
			public void expanding(WebCollapsiblePane pane) {
				Dimension d = IndicatorDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() + h ) ;
				IndicatorDlg.this.setSize(d);
			}
			
			@Override
			public void expanded(WebCollapsiblePane pane) { }

			@Override
			public void collapsing(WebCollapsiblePane pane) { }
			
			@Override
			public void collapsed(WebCollapsiblePane pane) { 
				Dimension d = IndicatorDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() - h ) ;
				IndicatorDlg.this.setSize(d);
			}
		});
		add(pane, "cell 0 6, span, growx, wrap");
	}
			
	@Override
	public void actionPerformed(ActionEvent e) {
		if ( ( e.getActionCommand() == OK_CMD ) && ( lvl.getSelectedIndex() >= 0 ) &&
				( (indicator == null) || 
						( (indicator != null) && 
								( !indicator.getName().equals( txt.getText() ) || 
										( indicator.getLevel().equals( levels[ lvl.getSelectedIndex() ] ) ) ) ) ) ) {
				res = new Indicator();
				res.setName( txt.getText() );
				res.setLevel( levels[ lvl.getSelectedIndex() ] );
		}
		setVisible(false);
	}
	
	public Indicator getResult() {
		return res;
	}
}