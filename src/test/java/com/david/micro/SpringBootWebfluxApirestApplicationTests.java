package com.david.micro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.david.micro.app.models.documents.Producto;

//clase test, vamos a probar nuestros servicios
//creamos un servidor que tenga una numeración ramdom para que no pise clq otro servidor.
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class SpringBootWebfluxApirestApplicationTests {
	
	@Autowired
	private WebTestClient client;

	 public void listarTest() {
		
		client.get()
		.uri("api/v3/productos")
		.accept(MediaType.APPLICATION_JSON_UTF8)
		.exchange()
        .expectStatus().isOk()
        .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody(Producto.class);
        
	}
}
