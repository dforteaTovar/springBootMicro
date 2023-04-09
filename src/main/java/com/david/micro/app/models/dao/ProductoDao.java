package com.david.micro.app.models.dao;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.david.micro.app.models.documents.Producto;


public interface ProductoDao extends ReactiveMongoRepository<Producto, String>{

}
