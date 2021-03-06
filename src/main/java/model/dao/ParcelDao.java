package model.dao;

import javax.persistence.EntityManager;

import db.DbException;
import model.entites.Parcel;

public class ParcelDao {
	
	EntityManager manager;
	
	public ParcelDao(EntityManager manager) {
		// He needs a Connection to acess the database
		this.manager = manager;
	}
	
	public void insert(Parcel parcel) throws DbException {
		if(manager == null) {
			throw new DbException("DB Connection not instantiated");
		}
		manager.getTransaction().begin();;
		manager.persist(parcel);
		manager.getTransaction().commit();
	}
	
	public void update(Parcel parcel) throws DbException {
		if(manager == null) {
			throw new DbException("DB Connection not instantiated");
		}
		manager.getTransaction().begin();;
		parcel = manager.merge(parcel);
		manager.getTransaction().commit();
	}
	
	public Parcel findById(Integer id) throws DbException {
		if(manager == null) {
			throw new DbException("DB Connection not instantiated");
		}
		return manager.find(Parcel.class, id);
	}
	
	public void delete(Parcel parcel) throws DbException {
		if(manager == null) {
			throw new DbException("DB Connection not instantiated");
		}
		manager.getTransaction().begin();
		parcel = manager.find(Parcel.class, parcel.getDocumentNumber());
		manager.remove(parcel);
		manager.getTransaction().commit();
	}
	
}
