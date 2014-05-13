package minos.resource.managers;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import com.google.common.base.Joiner;

public class IconResource {
	private Map<Integer, ImageIcon> map;
	
	public static enum IconType { 
		ADD(1, "add.png"), EDIT(2, "edit.png"), DELETE(3, "delete.png"), 
		UP(4, "up.png"), DOWN(5, "down.png"), NEXT(6, "next.png"), 
		RELOAD(7, "reload.png"), CLOCK(8, "clock.png"), OK(9, "ok.png"), NO(10, "no.png"),	
		SEARCH(11, "search.png"), PSEARCH(12, "psearch.png"), FILTER(13, "filter.png"),
		DICE(14, "dice.png"), FLAG(15, "flag.png"), PENCIL(16, "pencil.png"),
		GEAR(17, "gear.png"), HAMMER(18, "hammer.png"), KEY(19, "key.png"),
		RULER(1000, "ruler.png"), LIGHT_BULB(1001, "light_bulb.png"), BULB(1002, "bulb.png"),		 
		USER_ADD(20, "users_add.png"), USER_DELETE(21, "users_delete.png"),
		BUSER_ADD(22, "business_user_add.png"),  BUSER_EDIT(23, "business_user_edit.png"), BUSER_DELETE(24, "business_user_delete.png"),		  
		LEVEL0(90, "level0.png"), LEVEL1(91, "level1.png"), LEVEL2(92, "level2.png"), 
		LEVEL3(93, "level3.png"), LEVEL4(94, "level4.png"), LEVEL5(95, "level5.png"),  
		CATALOG_ADD(100, "folder_add.png"),  CATALOG_EDIT(101, "folder_edit.png"), CATALOG_SIMPLE(102, "folder_yellow.png"),
		CATALOG_PROF(103, "folder_green.png"), CATALOG_PERS(104, "folder_yellow.png"), CATALOG_ADM(105, "folder_red.png"),
		COMPETENCE_ADD(110, "book_add.png"), COMPETENCE_PROF(111, "book_green.png"),
		COMPETENCE_PERS(112, "book_yellow.png"), COMPETENCE_ADM(113, "book_red.png"),
		INDICATOR_ADD(120, "page_add.png"), INDICATOR(121, "page.png"),
		PROFILE_PATTERN(130, "books.png"), PROFILE_PATTERN_ADD(131, "books_add.png"), PROFILE_PATTERN_EDIT(132, "books_edit.png")
		
		
		;		

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
		int ind = ( size << 16)  + icon.getIndex(); 
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