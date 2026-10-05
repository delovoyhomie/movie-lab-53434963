package ru.itmo.movielab.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.*;
import ru.itmo.movielab.service.*;

@Path("auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Context
    HttpServletRequest request;

    @Inject
    AuthService auth;

    public static class Credentials {

        public String login;
        public String password;
    }

    @POST
    @Path("login")
    public Object login(Credentials c) {
        if (c == null || !auth.authenticate(c.login, c.password)) {
            throw new AppException(401, "Неверный логин или пароль");
        }
        var old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        var session = request.getSession(true);
        session.setAttribute("login", c.login);
        session.setAttribute("csrf", UUID.randomUUID().toString());
        return me();
    }

    @GET
    @Path("me")
    public Object me() {
        var s = request.getSession(false);
        if (s == null || s.getAttribute("login") == null) {
            throw new AppException(401, "Войдите в систему");
        }
        return Map.of("login", s.getAttribute("login"), "csrf", s.getAttribute("csrf"));
    }

    @POST
    @Path("logout")
    public void logout() {
        var s = request.getSession(false);
        if (s != null) {
            s.invalidate();
        }
    }
}
