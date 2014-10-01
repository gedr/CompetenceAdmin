package minos.ui.dialogs;

import java.awt.Component;
import java.awt.Font;
import java.awt.Window;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.miginfocom.swing.MigLayout;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import ru.gedr.util.tuple.Triplet;

import com.alee.extended.button.WebSwitch;
import com.alee.extended.window.WebPopOver;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.separator.WebSeparator;


public class SwitchOptionDlg extends WebPopOver implements ActionListener {
	//=====================================================================================================
	//=                                             Constants                                             =
	//=====================================================================================================
	private static final long serialVersionUID = 1L;
	private static final String OK_CMD = "1";
	public static final String OPTIONS_CHANGE = "OptionsChange";
	

	//=====================================================================================================
	//=                                            Attributes                                             =
	//=====================================================================================================
	private Map<Integer, WebSwitch> map = null;
	private PropertyChangeSupport pcs = null;
	private List<Triplet<String, String, Boolean>> options = null;

	//=====================================================================================================
	//=                                           Constructors                                            =
	//=====================================================================================================
	/**
	 * @param owner - Window owner
	 * @param options - list options Triplet<Group_name, option_name, option_state>
	 * 					Group_name may be null
	 * 					option_name not be null and empty
	 * 					option_state - is on or off state
	 */
	public SwitchOptionDlg( Window owner, List<Triplet<String, String, Boolean>> options ) {
		super( owner );
		this.options = options;		
			  
	    setMargin( 10 );
	    setMovable( false );
	    pcs = new PropertyChangeSupport(this);
	    setLayout( new MigLayout( "", "[][]", "[][]" ));
	    
	    Font font = null;
	    List<String> groups = getUniqueGroup( options );
	    for ( String s : groups ) {
		    WebLabel lbl = ( s == null ? null : new WebLabel( s ) );
		    if ( font == null  ) font = lbl.getFont().deriveFont( Font.BOLD );
		    lbl.setFont( font );
		    if ( lbl != null ) add( lbl, "split 2, span" );
		    add( new WebSeparator(), ( lbl != null ? "growx, wrap" : "split 2, span, growx, wrap" ) );
		    
		    int ind = -1;
		    for ( Triplet<String, String, Boolean> t : options ) {
		    	ind++;
		    	if ( ( t == null ) || ( t.getSecond() == null ) || t.getSecond().isEmpty() ) continue;
		    	if ( ( ( s == null ) && ( t.getFirst() == null ) ) 
		    		|| ( ( s != null ) && s.equals( t.getFirst() ) ) ) {
		    		WebSwitch ws = makeSwitchBtn( t.getThird() );
		    		if ( map == null ) map = new TreeMap<Integer, WebSwitch>();
		    		map.put( ind, ws );
		    	    add( new WebLabel( t.getSecond() ) );
		    	    add( ws, "wrap" );
		    	}		    	
		    }
	    }
	    
	    add( new WebSeparator(), "split 2, span, growx, wrap" );	    
	    add( new WebButton( ActionAdapter.build( "Применить", ResourceKeeper.getIcon( IType.OK, 24 ), 
	    		OK_CMD, "Сохранить введеные данные", this, KeyEvent.VK_ENTER ) ), "growx, span" );
	}

	//=====================================================================================================
	//=                                                     Methods                                       =
	//=====================================================================================================
	public static void show( Window owner, List<Triplet<String, String, Boolean>> options,
			Component host, PropertyChangeListener listener ) {
		SwitchOptionDlg dlg = new SwitchOptionDlg( owner, options );
		dlg.setCloseOnFocusLoss( true );
		dlg.addPropertyChangeListener( listener );
		dlg.show( host );
	}
	
	
	@Override
	public void actionPerformed( ActionEvent e ) {
		if ( e.getActionCommand().equals( OK_CMD ) ) {
			if ( saveResult() ) pcs.firePropertyChange( OPTIONS_CHANGE, null, options );
		}
    	setVisible( false );
    	dispose();
	}
	
	public void addPropertyChangeListener( PropertyChangeListener listener ) {
		pcs.addPropertyChangeListener( listener );
	}

	public void removePropertyChangeListener( PropertyChangeListener listener ) {
		pcs.removePropertyChangeListener( listener );
	}
    
	/**
	 * make list of name's group
	 * @param options user options
	 * @return
	 */
	private List<String> getUniqueGroup( List<Triplet<String, String, Boolean>> options ) {
		if ( ( options == null ) || ( options.size() == 0 ) ) return Collections.emptyList();
		List<String> ls = new ArrayList<>();
		int ind = 0;
		int nullInd = -1;
		for ( Triplet<String, String, Boolean> t : options ) {
			if ( t == null ) continue;
			if ( t.getFirst() == null ) {
				if ( nullInd == -1 ) nullInd = ind++;
				continue;
			}
			if ( !ls.contains( t.getFirst() ) ) {
				ls.add( t.getFirst() );
				ind++;
			}
		}
		if ( nullInd != -1 ) ls.add( nullInd, null );
		return ls;
	}

	private WebSwitch makeSwitchBtn( Boolean selected ) {		
        WebSwitch ws = new WebSwitch();
        ws.setRound ( 10 );
        WebLabel lbl = new WebLabel( ResourceKeeper.getIcon( ResourceKeeper.IType.OK, 16 ), WebLabel.CENTER );
        lbl.setMargin(2, 2, 2, 2);
        ws.setLeftComponent ( lbl );
        lbl = new WebLabel( ResourceKeeper.getIcon( ResourceKeeper.IType.CANCEL, 16 ), WebLabel.CENTER );
        lbl.setMargin(2, 2, 2, 2);
        ws.setRightComponent ( lbl );
        ws.setEnabled( selected == null ? false : true );
        ws.setSelected( selected == null ? false : selected );
        return ws;
	}
	
	private boolean saveResult() {
		if ( ( options == null ) || ( options.size() == 0 ) ) return false;	
		boolean change = false;
		for ( int i = 0; i < options.size(); i++ ) {
			if ( !map.containsKey( i ) ) continue;
			Boolean bb = options.get( i ).getThird();
			boolean bs = map.get( i ).isSelected();
			if ( ( bb != null ) && ( bb != bs ) ) {
				options.get( i ).setThird( bs );
				change = true;
			}
		}
		return change;
	}
}
