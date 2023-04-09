package com.david.micro.app.controllers;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.david.micro.app.models.documents.Producto;
import com.david.micro.app.services.ProductoService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
	
	@Autowired
	private ProductoService service;
	@Value("{config.uploads.path}")
	private String path;
	
	//Metodo que va a devolver una lista de producto que iran directamente en el response body del controlador de rest
//	@GetMapping
//	public Flux<Producto> lista(){
//		return service.findAll();
//	}
	
	//Devuelve un mono, al no ser reactivo la clase entity tenemos que utilizar un mono just para devolver el resultado
	@GetMapping
	public Mono<ResponseEntity<Flux<Producto>>> lista()
	{  
		//cambia el estado de la llamada a 200(ok) si la busqueda es correcta
//		Mono.just(ResponseEntity.ok(service.findAll()));
		
		//Esta otra forma lo que hace es cambiar el content type, lo pasa a UTF-8 y responde en el body manda el ok
		return 
				Mono.just(
				ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(service.findAll())
				);
		
	}
	
	//metodo detalle que nos devuelve un producto por su id, para ello el path variable que nos dice el id a buscar, utilizamos el map,
	//porque no es rectivo la clase Response.
	//controlamos el nulo con el default y creando una nueva cabecera que devuelva un 404
	@GetMapping("/{id}")
	public Mono<ResponseEntity<Producto>> ver(@PathVariable String id){
		return service.findById(id).map(p -> ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(p))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	//Método post que crea un producto y este producto vendrá en el cuerpo de la request, por eso la anotación.
	//Validamos los campos y mostramos una excepcion
	@PostMapping
	public Mono<ResponseEntity<Map<String, Object>>> crear(@Valid @RequestBody Mono<Producto> monoProducto){
		
		//Donde guardamos los mensajes de respuesta, de todo ok y de errores
		Map<String, Object> respuesta = new HashMap<String, Object>();
		
		return monoProducto.flatMap(producto -> {
			if(producto.getCreateAt()==null) {
				producto.setCreateAt(new Date());
			}
			//si todo a ido ok, guardamos los mensajes en el map de respuesta
			return service.save(producto).map(p-> {
				respuesta.put("producto", p);
				respuesta.put("mensaje", "Producto creado con éxito");
				respuesta.put("timestamp", new Date());
				return ResponseEntity
					.created(URI.create("/api/productos/".concat(p.getId())))
					.contentType(MediaType.APPLICATION_JSON)
					.body(respuesta);
				});
		//Recojemos el error, la excepcion para mostrarlaovera	
		}).onErrorResume(t -> {
			return Mono.just(t).cast(WebExchangeBindException.class)
					.flatMap(e -> Mono.just(e.getFieldErrors()))
					.flatMapMany(Flux::fromIterable)
					.map(fieldError -> "El campo "+fieldError.getField() + " " + fieldError.getDefaultMessage())
					.collectList()
					.flatMap(list -> {
						respuesta.put("errors", list);
						respuesta.put("timestamp", new Date());
						respuesta.put("status", HttpStatus.BAD_REQUEST.value());
						return Mono.just(ResponseEntity.badRequest().body(respuesta));
					});
							
		});
		

	}
	
	//Metodo que va a actualizar un producto, se actualiza mediante un id
	@PutMapping("{id}")
	public Mono<ResponseEntity<Producto>> editar (@RequestBody Producto producto, @PathVariable String id){
		
		return service.findById(id).flatMap(p -> {
			p.setNombre(producto.getNombre());
			p.setPrecio(producto.getPrecio());
			p.setCategoria(producto.getCategoria());
			return service.save(p);
		}).map(p -> ResponseEntity.created(URI.create("/api/productos".concat(p.getId())))
		  .contentType(MediaType.APPLICATION_JSON)
		  .body(p))
		  .defaultIfEmpty(ResponseEntity.notFound().build());		
	}
	
	
	//Metodo que borra un objeto de la BBDD si esta, sino esta se controla mediante el default y un http status
	@DeleteMapping("{id}")
	public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id){
		return service.findById(id).flatMap(p -> {
			return service.delete(p).then(Mono.just(new ResponseEntity<Void>(HttpStatus.NOT_FOUND)));
		}).defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
	}
	
   @PostMapping("/upload/{id}")
	public Mono<ResponseEntity<Producto>> upload(@PathVariable String id, @RequestPart FilePart file){
	   
	   return service.findById(id).flatMap(p -> {
		   p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
		   .replace(" ", "")
		   .replace(":", "")
		   .replace("\\", ""));
		   
		   return file.transferTo(new File(path + p.getFoto())).then(service.save(p));
	   }).map(p -> ResponseEntity.ok(p))
		 .defaultIfEmpty(ResponseEntity.notFound().build());	   
   }
   
   
   //Método post que crea un producto y y se añade foto.
   @PostMapping("/v2")
   public Mono<ResponseEntity<Producto>> crearconFoto(Producto producto, @RequestPart FilePart file){
   	
   	//Comprobamos que la fecha no es nula, si lo es le seteamos una fecha nueva
   	if(producto.getCreateAt() == null) {
   		producto.setCreateAt(new Date());
   	}
   	
   	
   	producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
	   .replace(" ", "")
	   .replace(":", "")
	   .replace("\\", ""));
   	
   	//retornamos un objeto producto por el cuerpo de la request y le asignamos la ruta donde se mostrara.
   	return file.transferTo(new File(path + producto.getFoto())).then(service.save(producto)).map(p -> 
   	ResponseEntity.created(
   			URI.create(
   					"/api/productos".concat(p.getId())))
   	.contentType(MediaType.APPLICATION_JSON)
   	.body(p));
   	
   }
   
}


