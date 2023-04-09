package com.david.micro.app.models.dao;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.david.micro.app.models.documents.Categoria;


public interface CategoriaDao extends ReactiveMongoRepository<Categoria, String>{

}
