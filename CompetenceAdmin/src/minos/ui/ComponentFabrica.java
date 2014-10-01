package minos.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityNotFoundException;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;

import ru.gedr.util.tuple.Pair;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.ActorsInfo;
import minos.entities.Level;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

import com.alee.extended.panel.CollapsiblePaneListener;
import com.alee.extended.panel.WebCollapsiblePane;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.text.WebTextField;
import com.alee.managers.popup.PopupWay;
import com.alee.managers.popup.WebButtonPopup;

public class ComponentFabrica {
	public static final String NULL_ACTORS_INFO_NAME = "~~~~~~~";
	public static final int NULL_ACTORS_INFO_ID = -1;

	public static JTextComponent createOneLineTextEditor( int columns, boolean readOnly, String promt, String txt ) {
		WebTextField tf = new WebTextField( columns );
		tf.setInputPrompt( promt );
		tf.setInputPromptFont( tf.getFont().deriveFont( Font.ITALIC ) );
		tf.setEditable( !readOnly );
		tf.setText( txt );
		return tf;		
	}
	
	public static JTextComponent createMultiLineTextEditor( boolean readOnly, 
			String txt ) {
		WebTextArea ta = new WebTextArea( txt );
 		ta.setEditable( !readOnly );
 		ta.setLineWrap( true );
 		return ta;
	}	

	@SuppressWarnings("unchecked")
	public static JComboBox<Level> createLevelComboBox( Level level ) {		
		List<Level> levels = ResourceKeeper.getObject( OType.LEVELS_CACHE );
		WebComboBox cmb = new WebComboBox( levels.toArray( new Level[0] ) );
		cmb.setRenderer( new MinosCellRenderer<Level>( 24 ) );
		cmb.setSelectedItem( level != null ? level : levels.get( 0 ) ); 
		return cmb;
	}

	@SuppressWarnings("unchecked")
	public static JComboBox<ActorsInfo> createActorsInfoComboBox( boolean embedded, boolean nullEnable, 
			short... varieties ) {
		if ( ( varieties == null ) || ( varieties.length == 0 ) ) {
			throw new IllegalArgumentException( "ComponentFabrica.createActorsInfoComboBox() : param varieties is wrong" );
		}
		List<Short> ls = new ArrayList<>();
		for ( short s : varieties ) ls.add( s );
		List<ActorsInfo> lai = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_ACTORS_INFO ), 
				ActorsInfo.class, 
				new Pair<Object, Object>( "varieties", ls ),
				new Pair<Object, Object>( "ts", new Timestamp( System.currentTimeMillis() ) ),
				new Pair<Object, Object>( "dts", ResourceKeeper.getObject( OType.WAR )  ) );
		if ( ( lai == null ) || ( lai.size() == 0 ) ) {
			throw new EntityNotFoundException( "ComponentFabrica.createActorsInfoComboBox() : ActorsInfo not found" );
		}
		ActorsInfo[] arr;
		if ( !nullEnable ) {
			arr = lai.toArray( new ActorsInfo[0] ); 
		} else {
			arr = new ActorsInfo[lai.size() + 1];
			arr[0] = new ActorsInfo( NULL_ACTORS_INFO_NAME, ActorsInfo.VARIETY_SPEC, null );
			arr[0].setId( NULL_ACTORS_INFO_ID );
			int ind = 1;
			for ( ActorsInfo ai : lai ) arr[ind++] = ai;
		}
		DefaultComboBoxModel<ActorsInfo> model = new DefaultComboBoxModel<>( arr );
		WebComboBox cmb = new WebComboBox( model );
		cmb.setRenderer( new MinosCellRenderer<ActorsInfo>( 24 ) );	
		if ( embedded ){
			cmb.setDrawBorder( false );
			cmb.setDrawFocus( false );
		}
		return cmb;
	}

	public static WebCollapsiblePane createCollapsingPane( Icon icon, String title, Component content, Dimension preferedSize ) {
		WebScrollPane scrollPane = new WebScrollPane( content, false );		
		scrollPane.setPreferredSize( preferedSize );
		WebCollapsiblePane pane = new WebCollapsiblePane( icon, title, scrollPane );		
		pane.setExpanded( false ); 
		pane.addCollapsiblePaneListener( new CollapsiblePaneListener() {
			
			@Override
			public void expanding( WebCollapsiblePane pane ) {
				if ( ( pane == null ) ||  ( pane.getContent() == null ) 
						|| ( pane.getParent() == null ) ) return;
				double h = pane.getContent().getPreferredSize().getHeight();
				Dimension d = pane.getParent().getSize();
				d.setSize( d.getWidth(), d.getHeight() + h ) ;
				pane.getParent().setSize(d);		
				
			}
			
			@Override
			public void expanded( WebCollapsiblePane pane ) { }
			
			@Override
			public void collapsing( WebCollapsiblePane pane ) { }
			
			@Override
			public void collapsed( WebCollapsiblePane pane ) {
				if ( ( pane == null ) ||  ( pane.getContent() == null ) 
						|| ( pane.getParent() == null ) ) return;
				double h = pane.getContent().getPreferredSize().getHeight();
				Dimension d = pane.getParent().getSize();
				d.setSize( d.getWidth(), d.getHeight() + h ) ;
				pane.getParent().setSize(d);		
			}
		} );
		
		return pane;			
	}	
	
	public static <T extends TableModel> Component createTableFilter( final TableRowSorter<T> sorter, 
			int iconSize, String promt ) {
		if ( sorter == null ) return null;
		final WebTextField tf = ( WebTextField ) createOneLineTextEditor( 25, false, promt, null ); 
		WebButton filterButton = new WebButton( ActionAdapter.build( null, 
				ResourceKeeper.getIcon( IType.FILTER, iconSize ), 
				"Filter", "использовать фильтр", new ActionListener() {
					
					@Override
					public void actionPerformed( ActionEvent e ) {
						
						sorter.setRowFilter( RowFilter.regexFilter( tf.getText() ) );
					}
				}, 
				0 ) );

		tf.setTrailingComponent( filterButton );
		return tf;
	}
	
	public static JTextField createTableCellEditorForHandbook( Component content, Component focus, 
			PopupWay way , Action action ) {
		WebButton btn = new WebButton( action );
		btn.setFocusable( false );
		btn.setLeftRightSpacing( 0 );

		WebTextField txt = new WebTextField( false );	
		txt.setEditable( false );
		txt.setTrailingComponent( btn );
		txt.setDrawFocus( false );
		txt.setDrawShade( false );
		
		WebButtonPopup popup = new WebButtonPopup( btn, way );
		popup.setContent( content );
		popup.setDefaultFocusComponent( focus == null ? content : focus );

		return txt;
	}
	
	public static JTextField createTableCellEditorForDialog( Action action ) {
		WebButton btn = new WebButton( action );
		btn.setFocusable( false );
		btn.setLeftRightSpacing( 0 );

		WebTextField txt = new WebTextField( false );	
		txt.setEditable( false );
		txt.setTrailingComponent( btn );
		txt.setDrawFocus( false );
		txt.setDrawShade( false );
		return txt;
	}
	
}