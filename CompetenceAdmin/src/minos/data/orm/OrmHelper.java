package minos.data.orm;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;

import ru.gedr.util.tuple.Pair;

public class OrmHelper {
	private static EntityManagerFactory factory;
	private static final ThreadLocal<EntityManager> tlem = new ThreadLocal<>();
	private static final ThreadLocal<EntityTransaction> tlet = new ThreadLocal<>();
	
	public static EntityManagerFactory getFactory() {
		return factory;
	}
	
	public static void setFactory(EntityManagerFactory emf) {
		factory = emf;
	}

	public static void openManager() {
		EntityManager em = ( EntityManager ) tlem.get();
		if (em == null) {
			em = getFactory().createEntityManager();
			tlem.set( em );
		}
	}  

	public static EntityManager getCurrentManager() {
		return (EntityManager) tlem.get();
	}

	public static void closeManager() {
		EntityManager em = (EntityManager) tlem.get();
		tlem.set(null);
		if ( (em != null) && em.isOpen() ) em.close();
	}

	public static void beginTransaction() {
		EntityTransaction tx = ( EntityTransaction ) tlet.get();
		if ( tx == null ) {
			tx = getCurrentManager().getTransaction();
			tx.begin();
			tlet.set( tx );
		}
	}

	public static void commitTransaction() {
		EntityTransaction tx = ( EntityTransaction )tlet.get();
		tlet.set( null );
		if ( ( tx != null ) && tx.isActive() ) tx.commit();		
	}

	public static void rollbackTransaction() {
		EntityTransaction tx = ( EntityTransaction )tlet.get();
		tlet.set( null );
		if ( ( tx != null ) && tx.isActive() ) tx.rollback(); 
	}
	
	 public static Object create( Serializable obj ) {
	      getCurrentManager().persist( ( Object ) obj);	
	      return obj;
	 }

	 public static Object find( Class<?> clz, Object key ) {
		 return getCurrentManager().find( clz, key );
	 }

	 public static Object update( Serializable obj ) {
		 return getCurrentManager().merge((Object) obj);
	 } 

	 public static void refresh( Serializable obj ) {
		 getCurrentManager().refresh((Object) obj);
	 } 

	 public static void delete( Serializable obj ) {
		 getCurrentManager().remove( (Object) obj);
	 } 
	 
	 public static <T>void executeAsTransaction( OrmCommand cmd, T cmdParam ) throws Exception {
		 if ( cmd == null ) return;
		 openManager();
		 try {
			 beginTransaction();
			 cmd.execute( cmdParam );
			 commitTransaction();
		 } catch ( Exception ex ) {
			 rollbackTransaction();
			 throw ex;
		 } finally {
			 closeManager();
		 }		 
	 }
	 
	 public static enum QueryType {JPQL, SQL, NAMED};
	 public static Object executeQuery( QueryType qtype, String statment ) {
		 OrmHelper.openManager();
		 try {
			 switch( qtype ) {
			 case JPQL: return OrmHelper.getCurrentManager().createQuery( statment ).getSingleResult();
			 case SQL: return OrmHelper.getCurrentManager().createNativeQuery( statment ).getSingleResult();
			 case NAMED: return OrmHelper.getCurrentManager().createNamedQuery( statment ).getSingleResult();
			 }
		 } catch (RuntimeException ex) {
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }	
		 return null;
	 }

	 @SafeVarargs
	 public static Object executeQueryWithParam( QueryType qtype, String statment, Pair<Object, Object>... params ) {
		 OrmHelper.openManager();
		 try {
			 Query q = null; 
			 switch( qtype ) {
			 case JPQL: 
				 q = getCurrentManager().createQuery( statment );				 
				 break;
			 case SQL: 
				 q = getCurrentManager().createNativeQuery( statment );
				 break;
			 case NAMED: 
				 q = getCurrentManager().createNamedQuery( statment );
				 break;
			 }
			 if ( ( params != null ) && ( params.length > 0 ) && ( q != null ) ) {
				 for ( Pair<Object, Object> p : params ) {
					 if ( (  p.getFirst() instanceof Integer ) 
							 && ( ( qtype == QueryType.JPQL ) || ( qtype == QueryType.SQL ) 
									 || ( qtype == QueryType.NAMED ) ) ) {
						 q.setParameter( ( Integer ) p.getFirst(), p.getSecond() );
						 continue;
					 }
					 if ( ( p.getFirst() instanceof String ) 
							 && ( ( qtype == QueryType.JPQL ) || ( qtype == QueryType.NAMED ) ) ) {
						 q.setParameter( ( String ) p.getFirst(), p.getSecond() );
						 continue;
					 }
					 throw new IllegalArgumentException( "illegal parameter : " + p.toString() );
				 }				 
			 }
			 return q.getSingleResult();
		 } catch (RuntimeException ex) {
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }	
	 }
	 
	 /**
	  * This function create new record in DB's table from entity
	  * @param entity_ is new entity
	  * @return persist's entity
	  */
	 public static Object createEntity( Serializable entity ) {
		 OrmHelper.openManager();
		 try {
			 OrmHelper.beginTransaction();
			 OrmHelper.create(entity);
			 OrmHelper.commitTransaction();
			 return entity;
		 } catch (RuntimeException ex) {
			 OrmHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }
	 }

	 /**
	  * This function update existing record in DB's table
	  * @param entity_ is existing entity
	  * @return update entity
	  */
	 public static Object updateEntity( Serializable entity ) {
		 OrmHelper.openManager();
		 try {
			 OrmHelper.beginTransaction();
			 Object ret = OrmHelper.update(entity);
			 OrmHelper.commitTransaction();
			 return ret;
		 } catch (RuntimeException ex) {
			 OrmHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }
	 }

	 public static void refreshEntity( Serializable entity_ ) {
		 OrmHelper.openManager();
		 try {			 
			 OrmHelper.refresh(entity_);			 			 
		 } catch (RuntimeException ex) {
			 OrmHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }
	 }

	 public static void deleteEntity( Serializable entity ) {
		 OrmHelper.openManager();
		 try {
			 OrmHelper.beginTransaction();
			 OrmHelper.delete(entity);
			 OrmHelper.commitTransaction();
		 } catch (RuntimeException ex) {
			 OrmHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }
	 }

	 public static Object findEntity( Class<?> clz, Object key) {
		 return findEntity(clz, key, (String[]) null);
	 }

	 public static Object findEntity( Class<?> clz, Object key, String... eagerFields ) {
		 OrmHelper.openManager();
		 try {
			 if(eagerFields != null) {
				 OpenJPAEntityManager kem = OpenJPAPersistence.cast(OrmHelper.getCurrentManager());
				 kem.getFetchPlan().addFields(clz, eagerFields);
			 }
			 OrmHelper.beginTransaction();
			 Object ret = OrmHelper.find(clz, key);
			 OrmHelper.commitTransaction();
			 return ret;
		 } catch (RuntimeException ex) {
			 OrmHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }
	 }

	 /**
	  * find rows in DB and make list of entities
	  * @param qtype - query type
	  * @param statment - query statement
	  * @param clzT - entities class 
	  * @return list of entities
	  */
	 public static <T> List<T> findByQuery( QueryType qt, String statment, Class<T> clzT ) {
		 OrmHelper.openManager();
		 try {
			 Query q = null;
			 switch(qt) {
			 case JPQL : 
				 q =  (clzT == null ? (Query) OrmHelper.getCurrentManager().createQuery( statment )
						 : (Query) OrmHelper.getCurrentManager().createQuery( statment, clzT ) );
				 break;
			case SQL:
				 q = ( clzT == null ? OrmHelper.getCurrentManager().createNativeQuery( statment )
						 : OrmHelper.getCurrentManager().createNativeQuery( statment, clzT ) );
				 break;
			case NAMED:
				 q = ( clzT == null ? OrmHelper.getCurrentManager().createNamedQuery( statment ) :
					 OrmHelper.getCurrentManager().createNamedQuery( statment, clzT ) );
				 break;
			 }
			 @SuppressWarnings("unchecked")
			List<T> tmp = q.getResultList();
			 return tmp;
		 } catch (RuntimeException ex) {
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }
	 }

	 /**
	  * find rows in DB and make list of entities
	  * @param qtype - query type
	  * @param statment - query statement
	  * @param clzT - entities class 
	  * @param params - parameters for query (Pair<String, Object> - for named parameter (only JPQL and NAMED type), 
	  * 									Pair<Integer, Object> - for numerical parameter )
	  * @return list of entities
	  */
	 @SafeVarargs
	 public static <T> List<T> findByQueryWithParam( QueryType qtype, String statment, Class<T> clzT, Pair<Object, Object>... params  ) {
		 OrmHelper.openManager();
		 try {
			 Query q = null;
			 switch( qtype ) {
			 case JPQL : 
				 q =  (clzT == null ? (Query) OrmHelper.getCurrentManager().createQuery( statment )
						 : (Query) OrmHelper.getCurrentManager().createQuery( statment, clzT ) );
				 break;
			case SQL:
				 q = ( clzT == null ? OrmHelper.getCurrentManager().createNativeQuery( statment )
						 : OrmHelper.getCurrentManager().createNativeQuery( statment, clzT ) );
				 break;
			case NAMED:
				 q = ( clzT == null ? OrmHelper.getCurrentManager().createNamedQuery( statment ) :
					 OrmHelper.getCurrentManager().createNamedQuery( statment, clzT ) );
				 break;
			 }
			 if ( ( params != null ) && ( params.length > 0 ) && ( q != null ) ) {
				 for ( Pair<Object, Object> p : params ) {
					 if ( p == null ) continue;
					 if ( (  p.getFirst() instanceof Integer ) 
							 && ( ( qtype == QueryType.JPQL ) || ( qtype == QueryType.SQL ) 
									 || ( qtype == QueryType.NAMED ) ) ) {
						 q.setParameter( ( Integer ) p.getFirst(), p.getSecond() );
						 continue;
					 }
					 if ( ( p.getFirst() instanceof String ) 
							 && ( ( qtype == QueryType.JPQL ) || ( qtype == QueryType.NAMED ) ) ) {
						 q.setParameter( ( String ) p.getFirst(), p.getSecond() );
						 continue;
					 }
					 throw new IllegalArgumentException( "illegal parameter : " + p.toString() );
				 }				 
			 }
			 @SuppressWarnings("unchecked")
			List<T> tmp = q.getResultList();
			 return tmp;
		 } catch (RuntimeException ex) {
			 throw ex;
		 } finally {
			 OrmHelper.closeManager();
		 }
	 }
}