package ru.itmo.movielab.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@ApplicationScoped
public class AuthService {

    @PersistenceContext(unitName = "movies")
    EntityManager entityManager;

    @Transactional
    public boolean authenticate(String login, String password) {
        if (login == null || password == null || password.length() > 1024) {
            return false;
        }
        List<?> rows = entityManager
            .createNativeQuery("select salt,password_hash from app_users where login=:login")
            .setParameter("login", login)
            .getResultList();
        if (rows.isEmpty()) {
            return false;
        }
        Object[] row = (Object[]) rows.get(0);
        try {
            byte[] expectedHash = Base64.getDecoder().decode((String) row[1]);
            PBEKeySpec keySpec = new PBEKeySpec(
                password.toCharArray(),
                Base64.getDecoder().decode((String) row[0]),
                120000,
                256
            );
            byte[] actualHash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(keySpec)
                .getEncoded();
            keySpec.clearPassword();
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (Exception e) {
            throw new IllegalStateException("Password verification failed", e);
        }
    }
}
