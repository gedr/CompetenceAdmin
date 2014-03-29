package minos.resource.managers;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import com.google.common.base.Joiner;

public class IconResource {
	private Map<Integer, ImageIcon> map;
	
	public static enum IconType { 
		ADD(1, "add.png"), EDIT(2, "page_edit.png"), DELETE(3, "delete.png"), 
		UP(4, "up.png"), DOWN(5, "down.png"), NEXT(6, "next.png"), 
		REFRESH(7, "refresh.png"), 
		USER_ADD(8, "users_add.png"), USER_DELETE(9, "users_delete.png"),
		BUSER_ADD(11, "business_user_add.png"),  BUSER_EDIT(12, "business_user_edit.png"), BUSER_DELETE(13, "business_user_delete.png");		

		int ind;
		String name;
		IconType(int ind, String name) { 
			this.ind = ind;
			this.name = name;
		}
		public int getIndex() { return ind; }
		public String getName() { return name; }
	};
	
	
	private IconResource() { 
		map = new HashMap<Integer, ImageIcon>();
	}
	
	private static class Holder {
		private static final IconResource INSTANCE = new IconResource();
	}
	
	public static IconResource getInstance() {
		return Holder.INSTANCE;
	}
	
	public ImageIcon getIcon(IconType icon, int size) {
		if ( (icon == null) || (size > 1024) ) throw new IllegalArgumentException("IconType.getIcon() : " + (icon == null  ? "icon is null" : "big size") );
		int ind = size << 16 + icon.getIndex(); 
		ImageIcon img = map.get(Integer.valueOf(ind));
		if(img == null) {
			String res = Joiner.on("/").join("/img", String.valueOf(size), icon.getName()).toString();
			img = new ImageIcon(getClass().getResource(res));
			if(img != null) map.put(ind, img);			
		}
		return img;
	}

	public void clear() {
		map.clear();
	}
}

/*
ImageIcon icon1 = new ImageIcon(getClass().getResource("/img/32/page_edit.png"));
resources.put("icon.edit.32", icon1);

resources.put("icon.addFolder.32", new ImageIcon(getClass().getResource("/img/32/folder_add.png")));
resources.put("icon.loadFolder.32", new ImageIcon(getClass().getResource("/img/32/folder_down.png")));
resources.put("icon.addCompetence.32", new ImageIcon(getClass().getResource("/img/32/book_add.png")));
resources.put("icon.loadCompetence.32", new ImageIcon(getClass().getResource("/img/32/page_down.png")));
resources.put("icon.addIndicator.32", icon1);		
		

resources.put("icon.level3.32", new ImageIcon(getClass().getResource("/img/32/level3.png")));

*/