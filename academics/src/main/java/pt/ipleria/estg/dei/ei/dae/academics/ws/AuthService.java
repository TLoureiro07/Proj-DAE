package pt.ipleria.estg.dei.ei.dae.academics.ws;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Context;

import org.hibernate.Hibernate;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.LoginDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.ChangePasswordDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.UserDTO;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.security.Authenticated;
import pt.ipleria.estg.dei.ei.dae.academics.security.TokenIssuer;

import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthService {

    @EJB
    private UserBean userBean;

    @Context
    private SecurityContext securityContext;

    @POST
    @Path("/login")
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

        String role = org.hibernate.Hibernate.getClass(user).getSimpleName();
        String token = TokenIssuer.issue(
                user.getUsername(),
                role
        );

        return Response.ok(Map.of("token", token)).build();
    }

    @PATCH
    @Path("/change-password")
    @Authenticated
    public Response changePassword(ChangePasswordDTO dto,
                                   @Context SecurityContext securityContext) {

        if (dto == null ||
                dto.old_password == null ||
                dto.new_password == null ||
                dto.confirm_password == null) {

            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!dto.new_password.equals(dto.confirm_password)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Passwords não coincidem"))
                    .build();
        }

        String username = securityContext
                .getUserPrincipal()
                .getName();

        if (!userBean.canLogin(username, dto.old_password)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Palavra-passe atual incorreta"))
                    .build();
        }

        userBean.changePassword(username, dto.new_password);

        return Response.ok(
                Map.of("message", "Palavra-passe alterada com sucesso")
        ).build();
    }

    @GET
    @Path("/user")
    @Authenticated
    public Response getAuthenticatedUser() {
        String username = securityContext.getUserPrincipal().getName();
        User user = userBean.find(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(UserDTO.from(user)).build();
    }
}
