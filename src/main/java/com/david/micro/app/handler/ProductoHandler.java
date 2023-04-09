package com.david.micro.app.handler;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.david.micro.app.models.documents.Categoria;
import com.david.micro.app.models.documents.Producto;
import com.david.micro.app.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductoHandler {
	
	@Autowired
	private ProductoService service;
	@Value("{config.uploads.path}")
	private String path;
	
	@Autowired
	private Validator validar;
	
	public Mono<ServerResponse> upload(ServerRequest request){
		
		//Buscamos el id de la request
		String id = request.pathVariable("id");
		//retorna un mono, contiene los nombres dl parametro, obtenemos en nombre de la imagen
		return request.multipartData().map( multipart -> multipart.toSingleValueMap().get("file"))
				.cast(FilePart.class)
				.flatMap(file -> service.findById(id)
				.flatMap(p -> {
					 p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
					   .replace(" ", "")
					   .replace(":", "")
					   .replace("\\", ""));
					 return file.transferTo(new File(path + p.getFoto())).then(service.save(p));
				})).flatMap(p -> ServerResponse.created(URI.create("/api/v2/productos".concat(p.getId())))
					.contentType(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(p)))
				    .switchIfEmpty(ServerResponse.notFound().build());
				
	}
	
	public Mono<ServerResponse> crearconFoto(ServerRequest request){
		
		Mono<Producto> producto = request.multipartData().map(multipart -> {
			
			FormFieldPart nombre = (FormFieldPart) multipart.toSingleValueMap().get("nombre");
			FormFieldPart precio = (FormFieldPart) multipart.toSingleValueMap().get("precio");
			FormFieldPart catID = (FormFieldPart) multipart.toSingleValueMap().get("categoria.id");
			FormFieldPart catNombre = (FormFieldPart) multipart.toSingleValueMap().get("categoria.nombre");
			
			Categoria categoria = new Categoria(catNombre.value());
			categoria.setId(catID.value());
			return new Producto(nombre.value(),Double.parseDouble(precio.value()), categoria);
		});
		//retorna un mono, contiene los nombres dl parametro, obtenemos en nombre de la imagen
		return request.multipartData().map( multipart -> multipart.toSingleValueMap().get("file"))
				.cast(FilePart.class)
				.flatMap(file -> producto
				.flatMap(p -> {
					 p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
					   .replace(" ", "")
					   .replace(":", "")
					   .replace("\\", ""));
					 p.setCreateAt(new Date());
					 return file.transferTo(new File(path + p.getFoto())).then(service.save(p));
				})).flatMap(p -> ServerResponse.created(URI.create("/api/v2/productos".concat(p.getId())))
					.contentType(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(p)));
				
	}

	public Mono<ServerResponse> listar(ServerRequest request){
		
		return ServerResponse.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.findAll(),Producto.class);
	}
	
	public Mono<ServerResponse> ver(ServerRequest request){
		
		//Buscamos el id de la request
		String id = request.pathVariable("id");
		//Buscamos por id, y mediante flatMap como es un objeto reactivo hacemos una conversion de objeto y se lo pasamos al body
		//controlamos el error tambien
		return service.findById(id).flatMap(p -> ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(p)))
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> crear(ServerRequest request){
		
		//Extrae el objeto del body de la request
		Mono<Producto> producto = request.bodyToMono(Producto.class);
		
		
			
		
		//Con el flatMap reactivo añadimos la fecha y guardamos el objeto. Y los añadimos en la base de datos
		return producto.flatMap(p -> {
			//Validacion
			Errors errors = new BeanPropertyBindingResult(p, Producto.class.getName());
			//validamos errores
		    validar.validate(p, errors);
		    
		    if(errors.hasErrors()) {
		    	//Si hay errorres los recogemos en una lista y lo mostramos
		    	return Flux.fromIterable(errors.getFieldErrors())
		    			.map(fieldErrors -> "El campo " + fieldErrors.getDefaultMessage())
		    			.collectList()
		    			.flatMap(list -> ServerResponse.badRequest().body(BodyInserters.fromValue(list)));
		    }else {
		    	//Devolvemos el objeto
		    	if(p.getCreateAt() == null) {
					p.setCreateAt(new Date());
				}
				return service.save(p).flatMap(pdb -> ServerResponse
						.created(URI.create("/api/v2/productos".concat(pdb.getId())))
						.body(BodyInserters.fromValue(pdb)));
		    }
		    
		
		});
		
	}
	
	public Mono<ServerResponse> editar(ServerRequest request){
		
		//Extrae el objeto del body de la request
		Mono<Producto> producto = request.bodyToMono(Producto.class);
		//Buscamos el id de la request
		String id = request.pathVariable("id");
		
		//Objeto de la BBDD
		Mono<Producto> productoOB = service.findById(id);
		
		//Combinamos los dos objetos, el de la base de datos con el de la request.
		return productoOB.zipWith(producto,(db,req) ->{
			db.setNombre(req.getNombre());
			db.setPrecio(req.getPrecio());
			db.setCategoria(req.getCategoria());
			return db;
			//guardamos la modificacion en la base de datos y como Body acepta tipos reactivos le pasamos el objeto que se guarda y añadimos la clase
			
		}).flatMap(p -> ServerResponse
				.created(URI.create("/api/v2/productos".concat(p.getId())))
				.body(service.save(p), Producto.class)
				.switchIfEmpty(ServerResponse.notFound().build()));
				
		
	}
	
	public Mono<ServerResponse> eliminar(ServerRequest request){
		
		//Buscamos el id de la request
		String id = request.pathVariable("id");
		//Objeto de la BBDD
		Mono<Producto> productoOB = service.findById(id);
		
		//Se elimina y como no devuelve nada se tiene que hacer con el then y el no content que no manada nada
		// Controlamos el nulo que devuelve al haber borrado el articulo y que no nos de error.
		return productoOB.flatMap(p -> service.delete(p).then(ServerResponse.noContent().build()))
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	
}
