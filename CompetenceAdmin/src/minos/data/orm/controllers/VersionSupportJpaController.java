package minos.data.orm.controllers;

import ru.gedr.util.tuple.Pair;

public abstract class VersionSupportJpaController<T> extends BasisJpaController<T> {
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public abstract Pair<T, T> newVersion( T oldEntity, T newEntity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception;
}