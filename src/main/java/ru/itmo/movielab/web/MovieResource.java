package ru.itmo.movielab.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import ru.itmo.movielab.dto.MovieInput;
import ru.itmo.movielab.model.Movie;
import ru.itmo.movielab.service.MovieService;

@Path("movies")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MovieResource {

    @Inject
    MovieService service;

    @GET
    public Map<String, Object> list(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("10") int size,
        @QueryParam("column") @DefaultValue("name") String column,
        @QueryParam("q") @DefaultValue("") String q,
        @QueryParam("sort") @DefaultValue("id") String sort,
        @QueryParam("desc") @DefaultValue("false") boolean desc
    ) {
        return service.page(page, size, column, q, sort, desc);
    }

    @GET
    @Path("{id}")
    public Movie get(@PathParam("id") long id) {
        return service.get(id);
    }

    @POST
    public Movie create(MovieInput input) {
        return service.create(input);
    }

    @PUT
    @Path("{id}")
    public Movie update(@PathParam("id") long id, MovieInput input) {
        return service.update(id, input);
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") long id, @QueryParam("version") Long version) {
        service.delete(id, version);
    }

    @GET
    @Path("revision")
    public Map<String, Object> revision() {
        return Map.of("revision", service.revision());
    }
}
