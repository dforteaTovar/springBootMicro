package com.david.micro;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.david.micro.app.handler.ProductoHandler;
@Configuration
public class RouterFunctionConfig {
	
	
	
	//Es una anotacion que guarda el contenido en el contenedor via métoodo
	//RouterFunction no indica la funciones de un handler
	//Un response de http
	@Bean
	public RouterFunction<ServerResponse> routes(ProductoHandler handler){
			
		//se añaden mediante los predicates dos nuevas rutas para llamar desde POSTMAN, la hemos implementado con una function lambda, que hace referencia al método de la clase
		//ProductoHandler
		return route(GET("/api/v2/productos").or(GET("/api/v3/productos")),handler::listar)
				.andRoute(GET("/api/v2/productos/{id}").or(GET("/api/v3/productos/{id}")), handler::ver)
				.andRoute(POST("/api/v2/productos"), handler::crear)
		        .andRoute(PUT("/api/v2/productos/{id}"), handler::editar)
		        .andRoute(DELETE("/api/v2/productos/{id}"), handler::eliminar)
		        .andRoute(POST("/api/v2/productos/upload/{id}"), handler::upload)
		        .andRoute(POST("/api/v2/productos/crear"), handler::crearconFoto);
			
		}
	

}
	