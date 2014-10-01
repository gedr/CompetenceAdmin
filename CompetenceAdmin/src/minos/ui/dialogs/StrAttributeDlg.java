package minos.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import minos.entities.StringAttr;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.table.WebTable;

public class StrAttributeDlg extends WebDialog implements TableModel, ActionListener, KeyListener {
	//=====================================================================================================
	//=                                             Constants                                             =
	//=====================================================================================================
	private static final long serialVersionUID = 1L;
	
	private static final String KEY_COLUMN_NAME 	= "Ключ";
	private static final String VALUE_COLUMN_NAME 	= "Значение";
	private static final String OK_CMD				= "1";
	private static final String CANCEL_CMD 			= "2";
	private static final String DELETE_CMD 			= "3";
	
	public static enum Status { CREATE, READ, UPDATE, DELETE, EMPTY };

	//=====================================================================================================
	//=                                            Attributes                                             =
	//=====================================================================================================
	private static Logger log = LoggerFactory.getLogger( StrAttributeDlg.class );

	//private Window owner;
	private WebTable tbl = null;

	private int columnCount;
	private String[] columnName = null;
	private boolean visibleKey;
	private List<StringAttr> strAttrs;
	private List<StringAttr> removedAttr;
	private List<Pair<StringAttr, Status>> lpsab = null;
	private boolean enableUpdate;
	private boolean enableDelete;
	private List<Pair<StringAttr, Status>> result;

	//=====================================================================================================
	//=                                           Constructors                                            =
	//=====================================================================================================
	public StrAttributeDlg( Window owner, String title, boolean visibleKey, boolean visibleValue, 
			boolean enableCreate,  boolean enableUpdate, boolean enableDelete, List<StringAttr> strAttrs ) {
		super( owner, title );
		if ( !visibleKey && !visibleValue ) throw new IllegalArgumentException( "StrAttributePanel() : atributes will be invisible" );
		columnCount = ( ( ( !visibleKey || !visibleValue ) ) ? 1 : 2 );
		columnName = new String[columnCount];
		columnName[0] = visibleKey ? KEY_COLUMN_NAME : VALUE_COLUMN_NAME;
		if ( columnCount > 1 ) columnName[1] = VALUE_COLUMN_NAME;
		
		this.visibleKey = visibleKey;
		this.strAttrs = strAttrs;
		//this.owner = owner;

		this.enableUpdate = enableUpdate;
		this.enableDelete = enableDelete;
		if ( enableCreate || enableUpdate ) lpsab = initList( strAttrs, enableCreate );
		
		WebComboBox cb = new WebComboBox();
		cb.getEditor().getEditorComponent().addKeyListener( this );
		cb.setEditable( true );		
		cb.setDrawBorder( false );
		cb.setDrawFocus( false );

		tbl = new WebTable( this );
		tbl.getColumnModel().getColumn( 0 ).setCellEditor( new DefaultCellEditor( cb ) );
		tbl.getInputMap( JTable.WHEN_FOCUSED )
		.put( KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK ), 
				DELETE_CMD );
		tbl.getActionMap().put( DELETE_CMD, ActionAdapter.build( null, null, DELETE_CMD, null, this, null ) );

		WebPanel btnPanel = new WebPanel( new FlowLayout( FlowLayout.RIGHT ) );
		if ( enableCreate || enableUpdate )  btnPanel.add( new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 24 ), OK_CMD, 
				"Сохранить введеные данные", this, KeyEvent.VK_ENTER ) ) );
		btnPanel.add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CANCEL_CMD, 
				"Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ) );

		setLayout( new BorderLayout() );
		add( new WebScrollPane( tbl ), BorderLayout.CENTER );
		add( btnPanel, BorderLayout.SOUTH );
	}

	//=====================================================================================================
	//=                                                     Methods                                       =
	//=====================================================================================================
	public static List<Pair<StringAttr, Status>> show( Window owner, String title, boolean visibleKey, boolean visibleValue, 
			boolean enableCreate,  boolean enableUpdate,  boolean enableDelete, List<StringAttr> strAttrs ) {
		StrAttributeDlg dlg = new StrAttributeDlg( owner, title, visibleKey, visibleValue, enableCreate, enableUpdate, enableDelete, strAttrs );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.setSize( 450, 300 );
		dlg.setVisible(true);
		return dlg.getResult();
	}

	@Override
	public void addTableModelListener( TableModelListener l ) { }
	
	@Override
	public void removeTableModelListener( TableModelListener l ) { }

	@Override
	public boolean isCellEditable( int rowIndex, int columnIndex ) {
		return lpsab == null ? false : true; 
	}

	@Override
	public Class<?> getColumnClass( int columnIndex ) {
		return String.class;
	}

	@Override
	public int getColumnCount() {
		return columnCount;
	}

	@Override
	public String getColumnName( int columnIndex ) {
		return columnName[columnIndex];
	}

	@Override
	public int getRowCount() {		
		return lpsab != null ? lpsab.size() : ( strAttrs == null ? 0 : strAttrs.size() );
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex ) {
		if  ( lpsab == null ) return ( columnIndex == 0 
				? ( visibleKey ? strAttrs.get( rowIndex ).getKey() : strAttrs.get( rowIndex ).getValue() ) 
						: strAttrs.get( rowIndex ).getValue() );

		return( columnIndex == 0 ? ( visibleKey ? lpsab.get( rowIndex ).getFirst().getKey() : lpsab.get( rowIndex ).getFirst().getValue() ) 
				: lpsab.get( rowIndex ).getFirst().getValue() );
	}

	@Override
	public void setValueAt( Object value, int rowIndex, int columnIndex ) {
		String str = ( String ) value;
		Pair<StringAttr, Status> p = lpsab.get( rowIndex );
		
		if ( ( str == null ) || ( str.length() == 0 ) ) {
			if ( p.getSecond() == Status.EMPTY ) return;
			if ( p.getSecond() == Status.CREATE ) lpsab.remove( rowIndex );
			if ( ( ( p.getSecond() == Status.READ ) || ( p.getSecond() == Status.UPDATE ) ) 
					&& enableDelete ) {
				if ( removedAttr == null ) removedAttr = new ArrayList<>();
				removedAttr.add( p.getFirst() );
				lpsab.remove( rowIndex );
			}				
		}		

		if ( ( p.getSecond() == Status.CREATE ) || ( p.getSecond() == Status.UPDATE ) 
				|| ( p.getSecond() == Status.EMPTY )
				|| ( ( p.getSecond() == Status.READ ) && enableUpdate ) ) {
			if ( ( columnIndex == 0 ) && visibleKey ) p.getFirst().setKey( str );
			else p.getFirst().setValue( str );
			if ( p.getSecond() == Status.READ ) p.setSecond( Status.UPDATE );
			if ( p.getSecond() == Status.EMPTY ) {
				p.setSecond( Status.CREATE );
				lpsab.add( new Pair<>( newEmpty(), Status.EMPTY )  );
			}
		}		
	}	

	@Override
	public void actionPerformed( ActionEvent e ) {
		if ( e.getActionCommand().equals( DELETE_CMD ) ) {
			if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "remove row" );
			for ( int i : tbl.getSelectedRows() ) setValueAt( null, i, 0 );
			return;
		}
		if ( e.getActionCommand().equals( OK_CMD ) ) {			
			result = makeResult();
		}		
		setVisible( false );		
	}

	@Override
	public void keyPressed( KeyEvent e ) { }

	@Override
	public void keyReleased( KeyEvent e ) { } 

	@Override
	public void keyTyped( KeyEvent e ) {
		/*
		System.out.println( "EditingColumn=" + tbl.getEditingColumn() + "   EditingRow=" + tbl.getEditingRow() );
		JTextComponent tc = ( JTextComponent ) e.getSource();
		
		System.out.println( " keyTyped " + tc.getText() + e.getKeyChar() );
		
		List<StringAttr> lsa = ORMHelper.findByQueryWithParam( QueryType.SQL, 
				ResourceKeeper.getQuery( QType.SQL_LOAD_STRING_ATTR), 
				StringAttr.class, 
				new Pair<Object, Object>( Integer.valueOf( 1 ), Integer.valueOf( 1 ) ) );
*/
	}

	/**
	 * create list of StringAttr for edit
	 * @param lsa - current StringAttr may be null
	 * @param fCreate -flag of permission for create new StringAttr
	 * @return 
	 */
	private List<Pair<StringAttr, Status>> initList( List<StringAttr> lsa, boolean fCreate ) {
		List<Pair<StringAttr, Status>> l = new ArrayList<>();
		if ( ( lsa != null ) && ( lsa.size() > 0 ) ) {
			for ( StringAttr sa : lsa ) l.add( new Pair<>( sa, Status.READ ) );	
		}		
		if ( fCreate ) l.add( new Pair<>( newEmpty(), Status.EMPTY ) );
		return l;
	}

	/**
	 * make result Pair<list_live_element, list_delete_element>
	 * @return object of Pair<list_live_element, list_delete_element> ; otherwise null
	 */
	private List<Pair<StringAttr, Status>> makeResult() {
		if ( ( ( lpsab == null ) || ( lpsab.size() == 0 ) ) 
				&& ( ( removedAttr == null ) || ( removedAttr.size() == 0 ) ) ) return null;
		
		List<Pair<StringAttr, Status>> l = new ArrayList<>();
		if ( ( lpsab != null ) && ( lpsab.size() > 0 ) ) l.addAll( lpsab );
		if ( ( removedAttr != null ) && ( removedAttr.size() > 0 ) ) {
			for ( StringAttr sa : removedAttr ) {
				if ( sa != null ) l.add( new Pair<StringAttr, Status>( sa, Status.DELETE ) );
			}
		}
		return l;
	}

	/**
	 * create new empty StringAttr entity
	 * @return object of StringAttr  
	 */
	private StringAttr newEmpty() {
		return new StringAttr( ( visibleKey ? "" : null ), "", 1, 
				StringAttr.VARIETY_POFILE_PATTERN_ELEMENT_ATTRIBUTE, 
				null, null );
	}
	
	private List<Pair<StringAttr, Status>> getResult() {
		return result;		
	}
}
