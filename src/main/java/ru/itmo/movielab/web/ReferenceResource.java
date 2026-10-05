package ru.itmo.movielab.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;
import ru.itmo.movielab.dto.ReferenceInput;
import ru.itmo.movielab.service.ReferenceService;

@Path("references")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReferenceResource {

    @Inject
    ReferenceService service;

    @GET
    public Map<String, Object> options() {
        return service.options();
    }

    @GET
    @Path("{kind}")
    public List<?> list(@PathParam("kind") String kind) {
        return service.list(kind);
    }

    @GET
    @Path("{kind}/{id}")
    public Object get(@PathParam("kind") String kind, @PathParam("id") long id) {
        return service.get(kind, id);
    }

    @POST
    @Path("{kind}")
    public Object create(@PathParam("kind") String kind, ReferenceInput i) {
        return service.save(kind, null, i);
    }

    @PUT
    @Path("{kind}/{id}")
    public Object update(
        @PathParam("kind") String kind,
        @PathParam("id") long id,
        ReferenceInput i
    ) {
        return service.save(kind, id, i);
    }

    @DELETE
    @Path("{kind}/{id}")
    public void delete(
        @PathParam("kind") String kind,
        @PathParam("id") long id,
        @QueryParam("version") Long version
    ) {
        service.delete(kind, id, version);
    }
}
