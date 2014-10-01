package minos.utils;

import java.awt.Rectangle;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ProgramConfig implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Logger log = LoggerFactory.getLogger( ProgramConfig.class );
	private Rectangle mainWindowBound;
	private String logConfigFile;
	private DBConnectionConfig dbconfig;
	private boolean showDBConnectionConfig;
	private int measureDivider;
	private int[] actorsColumnSize;
	private int[] personsColumnSize;

	public boolean saveConfig(String file) {		 
		Gson gson = new Gson();
		String str = gson.toJson(this);
		PrintWriter pw = null;
		try  {
			pw = new PrintWriter(file);
			pw.write(str);
			pw.close();
			return true;
		} catch(Exception e) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( " ProgramConfig.saveConfig() : ", e);
		} finally {
			if(pw != null)
				try {
					pw.close();
				} catch (Exception e) {					
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( " ProgramConfig.saveConfig() : ", e);
				}
		}
		return false;
	}

	public static ProgramConfig loadConfig(String filename) throws Exception{
		ProgramConfig pc = null;
		try {
			Gson gson = new Gson();
			pc = gson.fromJson(new FileReader(filename), ProgramConfig.class);
		} catch (Exception e) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( " ProgramConfig.loadConfig() : ", e);
			pc = null;
		}
		return pc;
	}	
	
	public ProgramConfig() {
		mainWindowBound = null;		
		dbconfig = null;
		showDBConnectionConfig = true;
		setMeasureDivider(250);
		setActorsColumnSize(null);
	}
	
	public DBConnectionConfig getDBConnectionConfig() {
		return this.dbconfig;
	}
	
	public void setDBConnectionConfig(DBConnectionConfig config) {
		this.dbconfig = config;
	}

	public boolean isShowDBConnectionConfig() {
		return showDBConnectionConfig;
	}

	public void setShowDBConnectionConfig(boolean showDBConnectionConfig) {
		this.showDBConnectionConfig = showDBConnectionConfig;
	}

	public Rectangle getMainWindowBound() {
		if ( mainWindowBound == null ) setMainWindowBound( new Rectangle(10, 10, 640, 480) );
		return mainWindowBound;
	}

	public void setMainWindowBound(Rectangle mainWindowBound) {
		this.mainWindowBound = mainWindowBound;
	}

	public String getLogConfigFile() {
		return logConfigFile;
	}

	public void setLogConfigFile(String logConfigFile) {
		this.logConfigFile = logConfigFile;
	}

	public int getMeasureDivider() {
		return measureDivider;
	}

	public void setMeasureDivider(int measureDivider) {
		this.measureDivider = measureDivider;
	}

	public int[] getActorsColumnSize() {
		return actorsColumnSize;
	}

	public void setActorsColumnSize(int[] actorsColumnSize) {
		this.actorsColumnSize = actorsColumnSize;
	}
	
	public int[] getPersonsColumnSize() {
		return personsColumnSize;
	}

	public void setPersonsColumnSize(int[] personsColumnSize) {
		this.personsColumnSize = personsColumnSize;
	}

}