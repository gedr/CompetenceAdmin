package minos.utils;

import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

import minos.entities.Catalog;
import minos.entities.Competence;
import minos.utils.Permission.Block;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper.OType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.tree.WebTree;

public class AuxFunctions {
	private static Logger log = LoggerFactory.getLogger( AuxFunctions.class );

	/**
	 * check selection element in tree
	 * @param owner - parent window for error dialog
	 * @param tree - existing WebTree object
	 * @param emptySelection - flag for use empty select nodes 
	 * @param multiSelection - flag for use many select nodes
	 * @param clss - permission classes for selected nodes
	 * @return
	 */
	public static <T extends AsyncUniqueNode> boolean checkRightSelect( Window owner, 
			WebTree<T> tree, boolean emptySelection, boolean multiSelection, 
			String errmsg, Class<?>... clss ) {
		if ( emptySelection && ( tree.getSelectionCount() == 0 ) ) return true;

		if ( ( tree.getSelectionCount() == 1 ) && ( tree.getSelectedNode() != null ) 
				&& ( tree.getSelectedNode().getUserObject() != null ) ) {
			if ( clss == null ) return true; // node have any Class object
			for ( Class<?> clz : clss ) {
				if ( tree.getSelectedNode().getUserObject().getClass().equals( clz ) ) return true;
			}
		}

		if ( multiSelection && ( tree.getSelectionCount() > 1 ) ) {
			if ( clss == null ) return true; // nodes have any Class object
			for ( T t : tree.getSelectedNodes() ) {
				if ( ( t == null ) || ( t.getUserObject() == null ) ) continue;
				for ( Class<?> clz : clss ) {
					if ( t.getUserObject().getClass().equals( clz ) ) return true;
				}
			}
		}
		WebOptionPane.showMessageDialog( owner, errmsg, "Œ¯Ë·Í‡", WebOptionPane.ERROR_MESSAGE );
		return false;
	}

	/**
	 * initialize column width from array and/or default values
	 * @param model - existing TableColumnModel object
	 * @param widths - array of width
	 * @param def - default value if widths is null or elements in widths less when column count
	 */
	public static void initTableColumnWidth( TableColumnModel model, int[] widths, int def ) {
		if ( model == null ) return;
		for ( int i = 0; i < model.getColumnCount(); i++ ) {
			if ( ( widths == null ) || ( i >= widths.length ) ) {
				model.getColumn( i ).setPreferredWidth( def );
			} else {
				model.getColumn( i ).setPreferredWidth( widths[i] );
			}
		}
	}

	/**
	 * convert Set<T> to List<T>
	 * @param set existing Set<T> object
	 * @return List<T> object; if set is null or set is empty then return empty list; otherwise list of elements from Set collection
	 */
	public static <T> List<T> convertSetToList( Set<T> set ) {
		if ( ( set == null ) || ( set.size() == 0 ) ) return Collections.emptyList();
		List<T> lst = new ArrayList<>();
		for ( T t : set ) lst.add( t );
		return lst;		
	}
	
	/**
	 * execute tread in swing tread 
	 * @param doRun - executable object
	 * @param enableWait - if true then pass object in swing thread and wait ending; otherwise not waiting ending
	 */
	public static void invokeSwing( Runnable doRun, boolean enableWait ) {
		if ( doRun == null ) return;		
		if ( SwingUtilities.isEventDispatchThread() ) {
			doRun.run();
			return;
		}
		
		if ( !enableWait ) {
			SwingUtilities.invokeLater( doRun );
		} else {
			try {
				SwingUtilities.invokeAndWait( doRun );
			} catch ( Exception ex ) {
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( "AuxiliaryFunctions.invokeSwing() : ", ex );
			}
		}
	}

	/**
	 * simple version of invoke( Runnable doRun, boolean enableWait ) 
	 * @param doRun - executable object
	 */
	public static void invokeSwing( Runnable doRun ) {
		invokeSwing( doRun, false );
	}

	/**
	 * repaint component, can call from any thread
	 * @param c - component for repainting
	 */
	public static void repaintComponent( final Component c ) {
		invokeSwing( new Runnable() {		
			
			@Override
			public void run() {
				c.revalidate();
				c.repaint();
			}
		});
	}
	
	/**
	 * get permission for Block and Operation, use current configuration
	 * @param b - block for permission controlling
	 * @param op - executed operation
	 * @return true if permission granted
	 */
	public static boolean isPermission( Block b, Operation op ) {
		return Permission.combinePermission( ( Permission ) ResourceKeeper.getObject( OType.PERMISSION_CONSTRAINT ), 
				( Permission ) ResourceKeeper.getObject( OType.PERMISSION_CURRENT ), 
				( Permission ) ResourceKeeper.getObject( OType.PERMISSION_DEFAULT ), 
				b, op );
	}
	
	/**
	 * get allowed variety for read from db
	 * @param competenceOrCatalog - true if competence ; false - catalog 
	 * @return
	 */
	public static List<Short> getAllowedReadVariety( boolean competenceOrCatalog ) {
		List<Short> cmv = new ArrayList<>();
		if ( isPermission( competenceOrCatalog ? Block.ADMINISTRATIVE_COMPETENCE : Block.ADMINISTRATIVE_CATALOG, 
				Operation.READ ) ) {
			cmv.add( competenceOrCatalog ? Competence.ADMINISTRATIVE_COMPETENCE : Catalog.ADMINISTRATIVE_COMPETENCE ); 
		}				
		if ( isPermission( competenceOrCatalog ? Block.PERSONALITY_BUSINESS_COMPETENCE : Block.PERSONALITY_CATALOG, 
				Operation.READ ) ) { 
			cmv.add( competenceOrCatalog ? Competence.PERSONALITY_BUSINESS_COMPETENCE : Catalog.PERSONALITY_BUSINESS_COMPETENCE );
		}
		if ( AuxFunctions.isPermission( competenceOrCatalog ? Block.PROFESSIONAL_COMPETENCE : Block.PROFESSIONAL_CATALOG, 
				Operation.READ ) ) {
			cmv.add( competenceOrCatalog ? Competence.PROFESSIONAL_COMPETENCE : Catalog.PROFESSIONAL_COMPETENCE );
		}
		return cmv;
	}
	
	public static boolean equals( Object o1, Object o2, boolean nullEqualNull ) {
		if ( ( o1 == null ) && ( o2 == null ) ) return nullEqualNull;
		return ( o1 == null ? false : o1.equals( o2 ) );
	}
}
