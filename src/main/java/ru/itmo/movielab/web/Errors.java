package ru.itmo.movielab.web;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;
import ru.itmo.movielab.service.AppException;

@Provider
public class Errors implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(Errors.class.getName());

    public Response toResponse(Exception e) {
        int status = 500;
        String message = "Ошибка сервера. Подробности в журнале WildFly.";
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof AppException a) {
                status = a.status;
                message = a.getMessage();
                break;
            }
            if (cause instanceof ConstraintViolationException c) {
                status = 400;
                message = c
                    .getConstraintViolations()
                    .stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Некорректные данные");
                break;
            }
            if (cause instanceof jakarta.json.bind.JsonbException) {
                status = 400;
                message =
                    "Некорректный тип поля: целые числа должны быть без дробной части и в диапазоне Integer/Long";
                break;
            }
            if (cause instanceof SQLException s && "P0001".equals(s.getSQLState())) {
                status = 400;
                message = s.getMessage().split("\n")[0];
                break;
            }
            if (
                cause instanceof SQLException s &&
                s.getSQLState() != null &&
                s.getSQLState().startsWith("23")
            ) {
                status = 400;
                message =
                    "Нарушено ограничение базы данных. Проверьте обязательные поля, диапазоны и связанные объекты.";
                break;
            }
            if (cause instanceof jakarta.persistence.OptimisticLockException) {
                status = 409;
                message = "Объект изменён другим пользователем. Откройте его заново.";
                break;
            }
        }
        if (e instanceof WebApplicationException web) {
            status = web.getResponse().getStatus();
            message =
                status == 400
                    ? "Некорректный JSON или тип поля: проверьте целые числа и их диапазоны"
                    : web.getMessage();
        }
        if (status == 500) {
            LOG.log(Level.SEVERE, "Request failed", e);
        }
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of("error", message))
            .build();
    }
}
