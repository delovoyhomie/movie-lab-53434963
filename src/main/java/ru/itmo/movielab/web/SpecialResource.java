package ru.itmo.movielab.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
import ru.itmo.movielab.model.*;
import ru.itmo.movielab.service.SpecialService;

@Path("special")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SpecialResource {

    @Inject
    SpecialService service;

    public static class Input {

        public String text;
        public String source;
        public String target;
    }

    @POST
    @Path("delete-tagline")
    public Object deleteTagline(Input i) {
        return service.deleteTagline(i == null ? null : i.text);
    }

    @GET
    @Path("tagline")
    public List<Movie> contains(@QueryParam("text") String text) {
        return service.contains(text);
    }

    @GET
    @Path("genre-less")
    public List<Movie> less(@QueryParam("genre") String genre) {
        return service.less(genre);
    }

    @GET
    @Path("writers")
    public List<Person> writers() {
        return service.writers();
    }

    @POST
    @Path("redistribute")
    public Object redistribute(Input i) {
        return service.redistribute(i == null ? null : i.source, i == null ? null : i.target);
    }
}
