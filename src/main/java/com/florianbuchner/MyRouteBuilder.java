package com.florianbuchner;

import com.google.gson.Gson;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class MyRouteBuilder extends RouteBuilder {

    private final String DATA_PATH = "src/data/";

    public void configure() {

        from("jetty:http://localhost:80/?matchOnUriPrefix=true")
                .routeId("resourceRoute")
                .onException(Exception.class)
                    .logStackTrace(true)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                    .setBody(constant("Internal server error"))
                    .removeHeaders("*")
                    .handled(true)
                .end()
                .filter(header(Exchange.HTTP_PATH).isEqualTo(""))
                    .setHeader(Exchange.HTTP_PATH, constant("index.html"))
                .end()
                .process(exchange -> {
                    final Message message = exchange.getIn();
                    try {
                        final Path path = Paths.get(DATA_PATH +
                                message.getHeader(Exchange.HTTP_PATH, String.class).replace("..",""));
                        message.setBody(Files.readAllBytes(path));
                    } catch (InvalidPathException | IOException exception) {
                        log.error("Error while reading file", exception);
                        message.setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
                    }
                })
                .removeHeaders("*", Exchange.HTTP_RESPONSE_CODE);

        from("jetty:http://localhost/interface?httpMethodRestrict=POST")
                .unmarshal().json(JsonLibrary.Gson, Map.class)
                .transform().simple("Hello ${body[firstName]} ${body[lastName]}")
                .marshal().json(JsonLibrary.Gson, Map.class);

    }

}
