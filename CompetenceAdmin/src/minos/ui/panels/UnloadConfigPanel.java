package minos.ui.panels;

import java.awt.Component;
import java.awt.TextArea;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import minos.data.services.TablesInfo;
import minos.data.services.TablesInfo.Variety;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;
import net.miginfocom.swing.MigLayout;

import com.alee.extended.filechooser.WebPathField;
import com.alee.extended.list.CheckBoxCellData;
import com.alee.extended.list.CheckBoxListModel;
import com.alee.extended.list.WebCheckBoxList;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.utils.FileUtils;

public class UnloadConfigPanel extends WebPanel {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	
	//private static final Logger log = LoggerFactory.getLogger( UnloadConfigPanel.class );
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private WebPathField pathField;
	private WebCheckBoxList webCheckBoxList;
	private CheckBoxListModel model;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public UnloadConfigPanel() {
		pathField = new WebPathField( FileUtils.getDiskRoots()[0] );
		pathField.setPreferredWidth( 480 );
		model = createCheckBoxListModel();
		Component cmnt;
		if ( model == null ) {
			cmnt = new TextArea( "Нет таблиц" ); 
		} else {
			webCheckBoxList = new WebCheckBoxList( model );
			webCheckBoxList.setVisibleRowCount( 10 );
	        webCheckBoxList.setEditable( true );	
	        cmnt = webCheckBoxList;
		}
		
		setLayout( new MigLayout( "", "[][grow]", "[][][grow]" ) ); 
		add( new WebLabel( "Каталог для выгрузки" ), "cell 0 0" );
		add( pathField, "cell 1 0,growx" );
		add( new WebLabel( "Таблицы для выгрузки" ), "cell 0 1 2 1,growx" );
		add( new WebScrollPane( cmnt ), "cell 0 2 2 1, growx" );
    }
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public File getSaveDir() {
		return pathField.getSelectedPath();
	}
	
	public List<Integer> getTables() {
		TablesInfo ti = ResourceKeeper.getObject( OType.TABLES_INFO );
		List<Integer> li = new ArrayList<>();
		for ( int i : ti.getCodes() ) {
			if ( ti.getVarietyByCode( i ) == Variety.LOGGING_AND_TRANSPORT_ALWAYS ) li.add( i );
		}
		if ( ( model == null ) || ( model.getElements() == null ) || ( model.getElements().size() == 0 ) ) return li;
		for ( CheckBoxCellData cbcd : model.getElements() ) {
			if ( cbcd.isSelected() ) li.add( ti.getCodeByName( ( String ) cbcd.getUserObject() ) );
		}
		return li;
	}


    /**
     * Make  checkbox list for LOGGING_AND_TRANSPORT_SOMETIMES table
     * @return demo checkbox list model
     */
    private CheckBoxListModel createCheckBoxListModel() {
    	TablesInfo ti = ResourceKeeper.getObject( OType.TABLES_INFO );
    	String[] strs = ti.getNamesByVarieties( Variety.LOGGING_AND_TRANSPORT_SOMETIMES );
    	if ( ( strs == null ) || ( strs.length == 0 ) ) return null;
    	
    	CheckBoxListModel cblm = new CheckBoxListModel ();
    	for ( String s : strs ) {
    		cblm.addCheckBoxElement ( s, false );	
    	}
        return cblm;
	}

}
