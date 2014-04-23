package minos.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

public class IconJoiner {	
	public static ImageIcon HJoiner(int hgap, int vgap, int space, ImageIcon... icons) {
		if ( ( icons == null ) || (icons.length == 0 ) ) return null;
		int width = 0;
		int height = 0;
		for ( ImageIcon i : icons ) {
			width += i.getIconWidth();
			if ( height < i.getIconHeight() ) height = i.getIconHeight();
		}
		width += ( hgap * 2 + space * ( icons.length - 1) );
		height += ( vgap * 2 );
		
		final BufferedImage compositeImage = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
		        
		final Graphics graphics = compositeImage.createGraphics();
		int x = hgap;
		int y = vgap;
		for ( ImageIcon i : icons ) {
			graphics.drawImage( i.getImage(), x, y, null );
			x += space + i.getIconWidth();
		}
		return new ImageIcon( compositeImage );
	}
	
	public static ImageIcon VJoiner(int hgap, int vgap, int space, ImageIcon... icons) {
		if ( ( icons == null ) || (icons.length == 0 ) ) return null;
		int width = 0;
		int height = 0;
		for ( ImageIcon i : icons ) {
			height += i.getIconHeight();
			if ( width < i.getIconWidth() ) width = i.getIconWidth();
		}
		width += ( hgap * 2 );
		height += ( vgap * 2 + space * ( icons.length - 1) );
		
		final BufferedImage compositeImage = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
		        
		final Graphics graphics = compositeImage.createGraphics();
		int x = hgap;
		int y = vgap;
		for ( ImageIcon i : icons ) {
			graphics.drawImage( i.getImage(), x, y, null );
			y += space + i.getIconHeight();
		}
		return new ImageIcon( compositeImage );
	}
}