  package minos.ui.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Unit;
import minos.entities.Catalog;
import minos.entities.ProfilePattern;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;
import net.miginfocom.swing.MigLayout;

import com.alee.extended.checkbox.CheckState;
import com.alee.extended.panel.CollapsiblePaneListener;
import com.alee.extended.panel.WebCollapsiblePane;
import com.alee.extended.tree.WebCheckBoxTree;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.text.WebTextField;

public class ProfilePatternDlg extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static final String OK_CMD 		= "1";
	private static final String CANCEL_CMD 	= "2";
	
	private WebTextField txtName = null;
	private WebTextArea txtDescr = null;
	private boolean readOnly;
	private List<DefaultMutableTreeNode> filialNodes = null;
	private List<DefaultMutableTreeNode> basePostNodes = null;
	private ProfilePattern profilePattern;
	private ProfilePattern res;
	private WebCheckBoxTree<DefaultMutableTreeNode> filterTree = null;	
	
	public ProfilePatternDlg(Window owner, String title, ProfilePattern profilePattern, boolean readOnly, ImageIcon icon ) {
		super(owner, title);
		this.profilePattern = profilePattern;
		this.readOnly  = readOnly;
		initUI( icon );		
	}
	
	public static ProfilePattern showProfilePatternDlg(Window owner, String title, ProfilePattern profilePattern, 
			boolean readOnly, ImageIcon icon ) {
		ProfilePatternDlg dlg = new ProfilePatternDlg(owner, title, profilePattern, readOnly, icon);
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);
		return dlg.getResult();		
	}	
	
	private void initUI(ImageIcon icon) {
		setLayout( new MigLayout( "", "[][grow]", "[][][][grow][][]" )); 

		add(new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10");
		add(new WebLabel("Название"), "cell 1 0,growx,aligny top");

		txtName = new WebTextField( 30 );
		txtName.setInputPrompt( "Введите название шаблона профиля ..." );
		txtName.setInputPromptFont( txtName.getFont().deriveFont( Font.ITALIC ) );
		txtName.setEditable( !readOnly );
		if ( profilePattern != null ) txtName.setText( profilePattern.getName() );
		add(txtName, "cell 1 1,growx,aligny top");
		
		add(new WebLabel( "Описание" ), "cell 1 2,growx,aligny top");

		txtDescr = new WebTextArea();
		txtDescr.setLineWrap( true );
		txtDescr.setEditable( !readOnly );
		if ( profilePattern != null ) txtDescr.setText( profilePattern.getDescription() );
		add(new WebScrollPane( txtDescr ), "cell 0 3 2 1,grow");		
		
		makeFilterTree();
		
		if ( !readOnly ) {
			WebButton okBtn = new WebButton("OK");
			okBtn.setActionCommand(OK_CMD);		
			okBtn.addActionListener(this);
			add(okBtn, "flowx,cell 1 5,alignx right");
		}
		
		WebButton cancelBtn = new WebButton("Отмена");
		cancelBtn.setActionCommand(CANCEL_CMD);
		cancelBtn.addActionListener(this);		
		add(cancelBtn, "cell 1 5,alignx right");
		// addTechInfo();
		// addHistoryInfo();
	}
	
	private DefaultMutableTreeNode initTreeNodes() {
		DefaultMutableTreeNode post = null;
		DefaultMutableTreeNode filial = null;
		DefaultMutableTreeNode cfo = null;
		
		if ( Resources.getInstance().containsResource( ResourcesConst.BASE_POST_LIST ) ) {
			@SuppressWarnings("unchecked")
			List<Pair<String, Byte>> posts = 
					(List<Pair<String, Byte>>) Resources.getInstance().get( ResourcesConst.BASE_POST_LIST );			
			post = new DefaultMutableTreeNode( "Базовые должности" );
			basePostNodes = new ArrayList<>();
			for( Pair<String, Byte> p : posts ) {
				DefaultMutableTreeNode node = new DefaultMutableTreeNode( p ); 
				basePostNodes.add( node );
				post.add( node );				
			}			
		}
		if ( Resources.getInstance().containsResource( ResourcesConst.FILIALS_LIST ) ) {
			@SuppressWarnings("unchecked")
			List<Pair<String, Long>> filials = 
					(List<Pair<String, Long>>) Resources.getInstance().get( ResourcesConst.FILIALS_LIST );			
			filial = new DefaultMutableTreeNode( "Филиалы" );
			filialNodes = new ArrayList<>();
			boolean first = true;
			for( Pair<String, Long> p : filials ) {
				if ( first ) { 
					first = false;
					continue;
				}
				DefaultMutableTreeNode node = new DefaultMutableTreeNode( p ); 
				filialNodes.add( node );
				filial.add( node );				
			}			
		}
		if ( Resources.getInstance().containsResource( ResourcesConst.CFO_LIST ) ) {			
			cfo = new DefaultMutableTreeNode( "ЦФО" );
		}
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode( "Фильтры" );
		if ( post != null ) root.add( post );
		if ( filial != null ) root.add( filial );
		if ( cfo != null ) root.add( cfo );
		return root;
	}
	
	private void makeFilterTree() {	
		DefaultMutableTreeNode root = initTreeNodes();
		filterTree = new WebCheckBoxTree<>( root );		
		filterTree.setCellRenderer( new TreeCellRenderer() {
			private WebLabel label = new WebLabel(); 
			
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value,
					boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
				Object obj = ( ( DefaultMutableTreeNode ) value).getUserObject();
				if ( obj instanceof Pair<?, ?> ) {
					@SuppressWarnings("unchecked")
					Unit<String> str = ( Unit<String> ) obj;
					label.setText( str.getFirst() );
					return label;
				}			
				label.setText( value.toString() );
				return label;
			}
		});		
		filterTree.setEditable( false );
		filterTree.setCheckingEnabled( !readOnly );
		filterTree.setRootVisible( false );
		filterTree.expandAll();

		if ( profilePattern != null ) {
			setSelectedFilialMask( profilePattern.getFilialMask() );
			setSelectedPostMask( profilePattern.getPostMask() );
		}
		
		WebScrollPane scrollPane = new WebScrollPane( filterTree, false );
        scrollPane.setPreferredSize( new Dimension( 100, 200 ) );

		WebCollapsiblePane pane = new WebCollapsiblePane( null, "Фильтры" , scrollPane );
		pane.setExpanded( false ); 
		pane.addCollapsiblePaneListener(new CollapsiblePaneListener() {
			private int h = 200;
			
			@Override
			public void expanding(WebCollapsiblePane pane) {
				Dimension d = ProfilePatternDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() + h ) ;
				ProfilePatternDlg.this.setSize(d);
			}
			
			@Override
			public void expanded(WebCollapsiblePane pane) { }

			@Override
			public void collapsing(WebCollapsiblePane pane) { }
			
			@Override
			public void collapsed(WebCollapsiblePane pane) { 
				Dimension d = ProfilePatternDlg.this.getSize();
				d.setSize(d.getWidth(), d.getHeight() - h ) ;
				ProfilePatternDlg.this.setSize(d);
			}
		});
		add( pane , "cell 0 4 2 1, span, growx, wrap" );
	}
	
	private void setSelectedPostMask( short val ) {
		for( DefaultMutableTreeNode node : basePostNodes ) {
			@SuppressWarnings("unchecked")
			Pair<String, Byte> p = (Pair<String, Byte>) node.getUserObject();
			if ( ( p.getSecond() & val ) != 0 ) filterTree.setChecked( node, true );
		}
	}
	
	private void setSelectedFilialMask( long val ) {
		for( DefaultMutableTreeNode node : filialNodes ) {
			@SuppressWarnings("unchecked")
			Pair<String, Long> p = ( Pair<String, Long> ) node.getUserObject();
			if ( ( p.getSecond() & val ) != 0 ) filterTree.setChecked( node, true );
		}
	}
	
	private short getSelectedPostMask() {
		short val = 0;
		for( DefaultMutableTreeNode node : basePostNodes ) {
			@SuppressWarnings("unchecked")
			Pair<String, Byte> p = (Pair<String, Byte>) node.getUserObject();
			if ( filterTree.getCheckState( node ) == CheckState.checked ) val |= p.getSecond();
		}
		return val;
	}
	
	private long getSelectedFilialMask() {
		long val = 0;
		for( DefaultMutableTreeNode node : filialNodes ) {
			@SuppressWarnings("unchecked")
			Pair<String, Long> p = ( Pair<String, Long> ) node.getUserObject();
			if ( filterTree.getCheckState( node ) == CheckState.checked ) val |= p.getSecond();			
		}
		return val;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if ( ( e.getActionCommand() == OK_CMD ) && 
				( ( profilePattern == null ) ||
						( ( profilePattern != null ) && 
								( !txtName.getText().equals( profilePattern.getName() ) || 
										!txtDescr.getText().equals( profilePattern.getDescription() ) ||
										( getSelectedPostMask() != profilePattern.getPostMask() ) ||
										( getSelectedFilialMask() != profilePattern.getFilialMask() ) ) ) ) ) {		
			res = new ProfilePattern( txtName.getText(), txtDescr.getText(), getSelectedFilialMask(), 0, 
					getSelectedPostMask(), (short) 0, ( Timestamp ) null, ( Catalog ) null);
		}
		setVisible( false );		
	}	
	
	public ProfilePattern getResult() {		
		return res;
	}
}