package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import pt.ipleria.estg.dei.ei.dae.academics.entities.*;

@Stateless
public class PasswordResetBean {

    @PersistenceContext
    private EntityManager em;

    public PasswordResetToken createToken(User user) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        PasswordResetToken resetToken =
                new PasswordResetToken(token, user, expiresAt);

        em.persist(resetToken);
        return resetToken;
    }

    public PasswordResetToken findValidToken(String token) {
        TypedQuery<PasswordResetToken> q = em.createQuery(
                "SELECT t FROM PasswordResetToken t WHERE t.token = :token",
                PasswordResetToken.class
        );

        PasswordResetToken prt = q
                .setParameter("token", token)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (prt == null) return null;
        if (prt.isUsed()) return null;
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) return null;

        return prt;
    }

    public void markUsed(PasswordResetToken token) {
        token.setUsed(true);
        em.merge(token);
    }
}
