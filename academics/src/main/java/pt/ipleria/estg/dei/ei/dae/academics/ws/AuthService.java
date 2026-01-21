package pt.ipleria.estg.dei.ei.dae.academics.ws;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.hibernate.Hibernate;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.*;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.EmailBean;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.PasswordResetBean;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleria.estg.dei.ei.dae.academics.entities.PasswordResetToken;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.security.Authenticated;
import pt.ipleria.estg.dei.ei.dae.academics.security.TokenIssuer;

import java.util.Map;
@Path("auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthService {

    @EJB
    private UserBean userBean;

    @EJB
    private PasswordResetBean passwordResetBean;

    @EJB
    private EmailBean emailBean;

    @Context
    private SecurityContext securityContext;

    // =========================
    // LOGIN
    // =========================
    @POST
    @Path("login")
    public Response login(LoginDTO dto) {

        if (dto == null || dto.username == null || dto.password == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!userBean.canLogin(dto.username, dto.password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "username ou palavra-passe incorretos"))
                    .build();
        }

        User user = userBean.find(dto.username);
        String role = Hibernate.getClass(user).getSimpleName();
        String token = TokenIssuer.issue(user.getUsername(), role);

        return Response.ok(Map.of("token", token)).build();
    }

    // =========================
    // RECOVER PASSWORD
    // =========================
    @POST
    @Path("recover-password")
    public Response recoverPassword(RecoverPasswordDTO dto) {

        if (dto == null || dto.email == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        User user = userBean.findByEmail(dto.email);

        // Não revelar se o email existe
        if (user == null)
            return Response.ok().build();

        PasswordResetToken token = passwordResetBean.createToken(user);

        String link = "http://localhost:3000/reset-password?token=" + token.getToken();

        try {
            emailBean.send(
                    user.getEmail(),
                    "Recuperação de Palavra-Passe",
                    "Clique no link para redefinir:\n" + link
            );
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Erro ao enviar email"))
                    .build();
        }

        return Response.ok().build();
    }

    // =========================
    // RESET PASSWORD
    // =========================
    @POST
    @Path("reset-password")
    public Response resetPassword(ResetPasswordDTO dto) {

        if (dto == null ||
                dto.token == null ||
                dto.newPassword == null ||
                dto.confirmPassword == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        if (!dto.newPassword.equals(dto.confirmPassword))
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Passwords não coincidem"))
                    .build();

        PasswordResetToken prt =
                passwordResetBean.findValidToken(dto.token);

        if (prt == null)
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Token inválido ou expirado"))
                    .build();

        userBean.changePassword(
                prt.getUser().getUsername(),
                dto.newPassword
        );

        passwordResetBean.markUsed(prt);

        return Response.ok(
                Map.of("message", "Password alterada com sucesso")
        ).build();
    }

    // =========================
    // CHANGE PASSWORD (LOGADO)
    // =========================
    @PATCH
    @Path("change-password")
    @Authenticated
    public Response changePassword(ChangePasswordDTO dto,
                                   @Context SecurityContext securityContext) {

        if (dto == null ||
                dto.old_password == null ||
                dto.new_password == null ||
                dto.confirm_password == null)
            return Response.status(Response.Status.BAD_REQUEST).build();

        if (!dto.new_password.equals(dto.confirm_password))
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Passwords não coincidem"))
                    .build();

        String username = securityContext.getUserPrincipal().getName();

        if (!userBean.canLogin(username, dto.old_password))
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Password atual incorreta"))
                    .build();

        userBean.changePassword(username, dto.new_password);

        return Response.ok(
                Map.of("message", "Password alterada com sucesso")
        ).build();
    }

    // =========================
    // USER INFO
    // =========================
    @GET
    @Path("user")
    @Authenticated
    public Response getAuthenticatedUser() {

        String username = securityContext.getUserPrincipal().getName();
        User user = userBean.find(username);

        if (user == null)
            return Response.status(Response.Status.NOT_FOUND).build();

        return Response.ok(UserDTO.from(user)).build();
    }
}

