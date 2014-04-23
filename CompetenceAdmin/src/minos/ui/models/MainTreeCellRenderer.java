package minos.ui.models;

import java.awt.Component;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

import minos.entities.*;
import minos.resource.managers.*;
import minos.resource.managers.IconResource.IconType;
import minos.utils.IconJoiner;

import com.alee.laf.label.WebLabel;

public class MainTreeCellRenderer implements TreeCellRenderer {
	private int sizeIcon;
	private WebLabel label = new WebLabel();
	private Map<Integer, ImageIcon> ppeIcons = null;
	private ImageIcon[] catalogIcon;
	private ImageIcon[] competenceIcon;
	private ImageIcon[] levelIcon;
	private ImageIcon indicatorIcon;
	private ImageIcon[] indicatorLevelIcon; 
	private ImageIcon profilePatternIcon;
	
	public MainTreeCellRenderer(int sizeIcon) {
		this.sizeIcon = sizeIcon;
		catalogIcon = new ImageIcon[] { 
				IconResource.getInstance().getIcon( IconType.CATALOG_SIMPLE, sizeIcon ), 
				IconResource.getInstance().getIcon( IconType.CATALOG_PROF, sizeIcon ),
				IconResource.getInstance().getIcon( IconType.CATALOG_PERS, sizeIcon ),
				IconResource.getInstance().getIcon( IconType.CATALOG_ADM, sizeIcon ) };
		competenceIcon = new ImageIcon[] { null, 
				IconResource.getInstance().getIcon( IconType.COMPETENCE_PROF, sizeIcon ),
				IconResource.getInstance().getIcon( IconType.COMPETENCE_PERS, sizeIcon ),
				IconResource.getInstance().getIcon( IconType.COMPETENCE_ADM, sizeIcon ) };
		levelIcon = new ImageIcon[] { 
				IconResource.getInstance().getIcon( IconType.LEVEL0, sizeIcon ), 
				IconResource.getInstance().getIcon( IconType.LEVEL1, sizeIcon ),
				IconResource.getInstance().getIcon( IconType.LEVEL2, sizeIcon ),
				IconResource.getInstance().getIcon( IconType.LEVEL3, sizeIcon ),
				IconResource.getInstance().getIcon( IconType.LEVEL4, sizeIcon ),
				IconResource.getInstance().getIcon( IconType.LEVEL5, sizeIcon ) };
		indicatorIcon = IconResource.getInstance().getIcon( IconType.INDICATOR, sizeIcon );
		indicatorLevelIcon = new ImageIcon[] { null, 
				IconJoiner.HJoiner(0, 0, 1, indicatorIcon, levelIcon[1] ),
				IconJoiner.HJoiner(0, 0, 1, indicatorIcon, levelIcon[2] ),
				IconJoiner.HJoiner(0, 0, 1, indicatorIcon, levelIcon[3] ),
				IconJoiner.HJoiner(0, 0, 1, indicatorIcon, levelIcon[4] ),
				IconJoiner.HJoiner(0, 0, 1, indicatorIcon, levelIcon[5] ) };
		profilePatternIcon = IconResource.getInstance().getIcon( IconType.PROFILE_PATTERN, sizeIcon );
	}
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		MainTreeNode tnode = ( MainTreeNode ) value;
		label.setText( null );
		if ( tnode.getUserObject() instanceof Catalog ) {
			Catalog c = ( Catalog ) tnode.getUserObject();
			label.setText( c.getName() );
			label.setIcon( catalogIcon[ c.getVariety() ] );					
			return label;
		}

		if ( tnode.getUserObject() instanceof Competence ) {
			Competence c = ( Competence ) tnode.getUserObject();
			label.setText( c.getName() );
			label.setIcon( competenceIcon[ c.getVariety() ] );					
			return label;
		}

		if ( tnode.getUserObject() instanceof Level ) {
			Level l = ( Level ) tnode.getUserObject();
			label.setText( l.getName() );
			label.setIcon( levelIcon[ l.getId() ] );					
			return label;
		}
		if ( tnode.getUserObject() instanceof Indicator ) {
			Indicator i = ( Indicator ) tnode.getUserObject();			
			label.setText( i.getName() );
			ImageIcon icon = indicatorIcon;
			if ( !( tnode.getParent().getUserObject() instanceof Level ) && 
					( i .getLevel() != null ) ) icon = indicatorLevelIcon[ i.getLevel().getId() ];
			label.setIcon( icon );					
			return label;
		}
		if ( tnode.getUserObject() instanceof ProfilePattern ) {
			ProfilePattern pp = ( ProfilePattern ) tnode.getUserObject();
			label.setText( pp.getName() );
			label.setIcon( profilePatternIcon );
			return label;
		}
		if ( tnode.getUserObject() instanceof ProfilePatternElement ) {			
			ProfilePatternElement ppe = ( ProfilePatternElement ) tnode.getUserObject();
			label.setText( ppe.getCompetence().getName() );
			if ( ppeIcons == null ) ppeIcons = new TreeMap<>();
			int kod = ( ppe.getMinLevel().getId() << 8 ) + ppe.getCompetence().getVariety();
			ImageIcon icon = ppeIcons.get( kod );
			if ( icon == null ) {
				icon = IconJoiner.HJoiner(0, 0, 2, 
						competenceIcon[ppe.getCompetence().getVariety()], 
						levelIcon[ppe.getMinLevel().getId()] );
				ppeIcons.put(kod, icon);
			}
			label.setIcon( icon );					
			return label;
		}

		
		return null;
	}

	public int getSizeIcon() {
		return sizeIcon;
	}
}