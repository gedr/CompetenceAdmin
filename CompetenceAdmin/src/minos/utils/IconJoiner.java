package minos.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class IconJoiner {
	public static Icon HJoiner(int hgap, int vgap, int space, Icon... icons) {
		if ( ( icons == null ) || (icons.length == 0 ) ) return null;
		int width = 0;
		int height = 0;
		for ( Icon i : icons ) {
			if ( i == null ) continue;
			width += i.getIconWidth();
			if ( height < i.getIconHeight() ) height = i.getIconHeight();
		}
		width += ( hgap * 2 + space * ( icons.length - 1) );
		height += ( vgap * 2 );
		
		final BufferedImage compositeImage = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
		        
		final Graphics graphics = compositeImage.createGraphics();
		int x = hgap;
		int y = vgap;
		for ( Icon i : icons ) {
			if ( i == null ) continue;
			i.paintIcon( null, graphics, x, y );
			x += space + i.getIconWidth();
		}
		return new ImageIcon( compositeImage );
	}
	
	public static Icon VJoiner(int hgap, int vgap, int space, Icon... icons) {
		if ( ( icons == null ) || (icons.length == 0 ) ) return null;
		int width = 0;
		int height = 0;
		for ( Icon i : icons ) {
			if ( i == null ) continue;
			height += i.getIconHeight();
			if ( width < i.getIconWidth() ) width = i.getIconWidth();
		}
		width += ( hgap * 2 );
		height += ( vgap * 2 + space * ( icons.length - 1) );
		
		final BufferedImage compositeImage = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
		        
		final Graphics graphics = compositeImage.createGraphics();
		int x = hgap;
		int y = vgap;
		for ( Icon i : icons ) {
			if ( i == null ) continue;
			i.paintIcon( null, graphics, x, y );
			y += space + i.getIconHeight();
		}
		return new ImageIcon( compositeImage );
	}
}