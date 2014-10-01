package minos.data.orm;

public interface OrmCommand {
	public void execute( Object obj ) throws Exception;
}
