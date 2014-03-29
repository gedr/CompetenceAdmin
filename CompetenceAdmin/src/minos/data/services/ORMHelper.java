package minos.data.services;

import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;

public class ORMHelper {
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
		EntityManager em = (EntityManager) tlem.get();
		if (em == null) {
			em = getFactory().createEntityManager();
			tlem.set(em);
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
		EntityTransaction tx = (EntityTransaction) tlet.get();
		if (tx == null) {
			tx = getCurrentManager().getTransaction();
			tx.begin();
			tlet.set(tx);
		}
	}

	public static void commitTransaction() {
		EntityTransaction tx = (EntityTransaction)tlet.get();
		if ( (tx != null) && tx.isActive() ) tx.commit();
		tlet.set(null);
	}

	public static void rollbackTransaction() {
		EntityTransaction tx = (EntityTransaction)tlet.get();
		tlet.set(null);
		if ( (tx != null) && tx.isActive() ) tx.rollback(); 
	}
	
	 public static void create( Serializable obj ) {
	      getCurrentManager().persist((Object) obj);	      
	 }

	 public static Object find(Class<?> clz, Object key) {
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
	 
	 
	 public static enum QueryType {JPQL, SQL, NAMED};
	 public static <T> List<T> executeQuery(QueryType qt, String statment, Class<T> clzT) {
		 ORMHelper.openManager();
		 try {
			 Query q = null;
			 switch(qt) {
			 case JPQL : 
				 q =  (clzT == null ? (Query) ORMHelper.getCurrentManager().createQuery( statment )
						 : (Query) ORMHelper.getCurrentManager().createQuery( statment, clzT ) );
				 break;
			case SQL:
				 q = (TypedQuery<T>) ( clzT == null ? ORMHelper.getCurrentManager().createNativeQuery( statment )
						 : ORMHelper.getCurrentManager().createNativeQuery( statment, clzT ) );
				 break;
			case NAMED:
				 q = (TypedQuery<T>) ORMHelper.getCurrentManager().createNamedQuery( statment, clzT );
				 break;
			 }			 
			 return ( q == null ? null : (List<T>) q.getResultList() );
		 } catch (RuntimeException ex) {
			 throw ex;
		 } finally {
			 ORMHelper.closeManager();
		 }
	 }
	 
	 /**
	  * This function create new record in DB's table from entity
	  * @param entity_ is new entity
	  * @return persist's entity
	  */
	 public static Object createEntity( Serializable entity ) {
		 ORMHelper.openManager();
		 try {
			 ORMHelper.beginTransaction();
			 ORMHelper.create(entity);
			 ORMHelper.commitTransaction();
			 return entity;
		 } catch (RuntimeException ex) {
			 ORMHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 ORMHelper.closeManager();
		 }
	 }

	 /**
	  * This function update existing record in DB's table
	  * @param entity_ is existing entity
	  * @return update entity
	  */
	 public static Object updateEntity( Serializable entity ) {
		 ORMHelper.openManager();
		 try {
			 ORMHelper.beginTransaction();
			 Object ret = ORMHelper.update(entity);
			 ORMHelper.commitTransaction();
			 return ret;
		 } catch (RuntimeException ex) {
			 ORMHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 ORMHelper.closeManager();
		 }
	 }

	 public static void refreshEntity( Serializable entity_ ) {
		 ORMHelper.openManager();
		 try {
			 ORMHelper.beginTransaction();
			 ORMHelper.refresh(entity_);
			 ORMHelper.commitTransaction();			 
		 } catch (RuntimeException ex) {
			 ORMHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 ORMHelper.closeManager();
		 }
	 }

	 public static Object findEntity( Class<?> clz, Object key, String... eagerFields ) {
		 ORMHelper.openManager();
		 try {
			 if(eagerFields != null) {
				 OpenJPAEntityManager kem = OpenJPAPersistence.cast(ORMHelper.getCurrentManager());
				 kem.getFetchPlan().addFields(clz, eagerFields);
			 }
			 ORMHelper.beginTransaction();
			 Object ret = ORMHelper.find(clz, key);
			 ORMHelper.commitTransaction();
			 return ret;
		 } catch (RuntimeException ex) {
			 ORMHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 ORMHelper.closeManager();
		 }
	 }

	 public static Object findEntity( Class<?> clz, Object key) {
		 return findEntity(clz, key, (String[]) null);
	 }
	 
	 public static void deleteEntity( Serializable entity ) {
		 ORMHelper.openManager();
		 try {
			 ORMHelper.beginTransaction();
			 ORMHelper.delete(entity);
			 ORMHelper.commitTransaction();
		 } catch (RuntimeException ex) {
			 ORMHelper.rollbackTransaction();
			 throw ex;
		 } finally {
			 ORMHelper.closeManager();
		 }
	 }
}