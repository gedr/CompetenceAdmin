package minos.ui.models;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.text.WebTextField;

import ru.gedr.util.tuple.Pair;
import minos.data.services.ORMHelper;
import minos.entities.Actors;
import minos.entities.ActorsInfo;
import minos.entities.Profile;
import minos.resource.managers.IconResource;
import minos.resource.managers.IconResource.IconType;

public class ActorsTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;
	private static final String loadActorsInfo = "select entity from ActorsInfo entity where entity.journal.deleteMoment > CURRENT_TIMESTAMP";	
	private static final int TypePosition	= 4;
	private static final int ModePosition	= 5;
	private static final int LevelPosition	= 6;	
	
	private Class<?>[] columnClasses = new Class<?>[] { ImageIcon.class, Long.class, String.class, String.class, ActorsInfo.class, ActorsInfo.class, ActorsInfo.class, Profile.class };
	private String[] columnNames = new String[] { "Статус", "Код", "Эксперт", "Оцениваемый", "Резерв", "Вид", "Уровень", "Профиль" };
	private List<Actors> actorsList = null;
	private Map<Long, Pair<String, String>> cacheNames = new TreeMap<>();
	private TableColumnModel columnModel;
	private List<ActorsInfo> lai;
	private Map<Short, ImageIcon> icons; 

	public ActorsTableModel() {
		lai = ORMHelper.findByJpqQuery( loadActorsInfo, ActorsInfo.class );
		if ( lai == null ) lai = Collections.emptyList();
		icons = new TreeMap<>();
		icons.put( Actors.STATUS_BUILDING, IconResource.getInstance().getIcon( IconType.HAMMER, 16 ) );
		icons.put( Actors.STATUS_ACTIVE, IconResource.getInstance().getIcon( IconType.LIGHT_BULB, 16 ) );
		icons.put( Actors.STATUS_HISTORY, IconResource.getInstance().getIcon( IconType.BULB, 16 ) );
		icons.put( Actors.STATUS_DELETE, IconResource.getInstance().getIcon( IconType.DELETE, 16 ) );
	}
	
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {		
		return getActorsList() == null ? 0 : getActorsList().size();
	}

	@Override
	public Object getValueAt( int rowIndex, int columnIndex ) {		 
		Actors a = getActorsList().get( rowIndex );
		switch ( columnIndex ) {
		case 0: return icons.get( a.getStatus() );
				
		case 1: return Long.valueOf( a.getId() );
		
		case 2: return getActorsFullName(true, a);
		
		case 3: return getActorsFullName(false, a);

		case 4: return a.getReserveType().getName();

		case 5: return a.getTestMode().getName();

		case 6: return a.getReserveLevel().getName();

		case 7: return ( a.getProfile() == null ? "<не определен>" : a.getProfile().getProfilePattern().getName() );
		}		
		return null;
	}

	/**
	 * return person full name
	 * @param flag - if flag = true then return minos full name else return sinner full name
	 * @param a - actors
	 * @return person full name
	 */
	private String getActorsFullName(boolean flag, Actors a) {
		Pair<String, String> pss;
		if ( flag ) {
			if ( a.getMinos() == null ) return " ";
			pss = getNameFromCache( a.getId() );			
			if ( pss.getFirst() == null ) pss.setFirst( a.getMinos().getFullName() );
			return pss.getFirst();
		}		
		if ( a.getSinnerType() == Actors.SINNER_TYPE_ALIEN ) return ( a.getAlienSinner() == null ? " " : a.getAlienSinner().getName() );
		if ( a.getInternalSinner() == null ) return " ";
		pss = getNameFromCache( a.getId() );
		if ( pss.getSecond() == null ) pss.setSecond( a.getInternalSinner().getFullName() );
		return pss.getSecond();
	}
	
	/**
	 * return pair<Minos_Name, Sinner_Name> from cache for Actors' id
	 * @param id - identifier Actors' record
	 * @return Pair<> from cache or generate new Pair<>
	 */
	private Pair<String, String> getNameFromCache( Long id ) {
		Pair<String, String> pss = cacheNames.get( id );
		if ( pss == null ) {
			pss = new Pair<String, String>( null, null );
			cacheNames.put( id, pss );
		}		
		return pss;
	}

	@Override
	public Class<?> getColumnClass( int columnIndex ) {		
		return columnClasses[columnIndex];
	}

	@Override
	public String getColumnName( int columnIndex ) {
		return columnNames[columnIndex];
	}

	@Override
	public boolean isCellEditable( int rowIndex, int columnIndex ) {		
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		
	}

	public synchronized List<Actors> getActorsList() {
		return actorsList;
	}

	public synchronized void setActorsList( List<Actors> actorsList ) {
		this.actorsList = actorsList;		
	}

	public TableColumnModel getColumnModel() {
		return columnModel;
	}

	
	public void setColumnModel( TableColumnModel model ) {		
		columnModel = model;
		if ( model == null ) return;
		List<ActorsInfo> modes = new ArrayList<>();
		List<ActorsInfo> levels = new ArrayList<>();
		List<ActorsInfo> types = new ArrayList<>();

		for ( ActorsInfo ai : lai ) {
			if ( ( ai != null ) && ( ( ai.getVariety() == ActorsInfo.VARIETY_LEVEL ) || 
					( ai.getVariety() == ActorsInfo.VARIETY_SPEC ) ) ) levels.add( ai );
			if ( ( ai != null ) && ( ( ai.getVariety() == ActorsInfo.VARIETY_MODE ) || 
					( ai.getVariety() == ActorsInfo.VARIETY_SPEC ) ) ) modes.add( ai );
			if ( ( ai != null ) && ( ( ai.getVariety() == ActorsInfo.VARIETY_TYPE ) || 
					( ai.getVariety() == ActorsInfo.VARIETY_SPEC ) ) ) types.add( ai );		
		}
		
		for ( int i = 0; i < columnModel.getColumnCount(); i++ ) {
			TableColumn tc = columnModel.getColumn( i );
			if ( tc.getHeaderValue().equals( columnNames[ModePosition] ) ) {
				tc.setCellEditor( new DefaultCellEditor( makeComboBox( modes.toArray( new ActorsInfo[0] ) ) ) );
				continue;
			}
			if ( tc.getHeaderValue().equals( columnNames[LevelPosition] ) ) {
				tc.setCellEditor( new DefaultCellEditor( makeComboBox( levels.toArray( new ActorsInfo[0] ) ) ) );
				continue;
			}
			if ( tc.getHeaderValue().equals( columnNames[TypePosition] ) ) {
				tc.setCellEditor( new DefaultCellEditor( makeComboBox( types.toArray( new ActorsInfo[0] ) ) ) );
				continue;
			}
			
			if ( tc.getHeaderValue().equals( columnNames[1] ) ) {
				tc.setCellEditor( new DefaultCellEditor( makeTextField() ) );	
			}			
		}
	}
	
	private WebTextField makeTextField() {
		WebButton btn = new WebButton();
		btn.setFocusable( false );
		btn.setUndecorated( true );
		btn.setLeftRightSpacing( 0 );
		btn.setMoveIconOnPress( false );		
		btn.setCursor( Cursor.getDefaultCursor () );
		btn.setIcon( IconResource.getInstance().getIcon( IconType.SEARCH, 16 ) );
        btn.setPressedIcon ( IconResource.getInstance().getIcon( IconType.PSEARCH, 16 ) );
       
		WebTextField txt = new WebTextField();
		txt.setEditable( false );
		txt.setTrailingComponent( btn );
		return txt;		
	}
	
	/**
	 * make combo box component for edit cell in table
	 * @param elements - arrays ActorsInfo's elements for insert in element
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private WebComboBox makeComboBox(ActorsInfo[] elements) {
		WebComboBox wcb = new WebComboBox( elements );
		wcb.setRenderer( new ListCellRenderer<ActorsInfo>() {
			private WebLabel lbl;
			{
				lbl = new WebLabel();
				lbl.setOpaque( true );
			}
			
			@Override
			public Component getListCellRendererComponent( JList<? extends ActorsInfo> list, 
					ActorsInfo value, int index, boolean isSelected, boolean cellHasFocus) {
				lbl.setBackground( isSelected ? Color.LIGHT_GRAY : Color.WHITE );
				lbl.setText( value.getName() );
				return lbl;
			}
		});
		return wcb;		
	}
}