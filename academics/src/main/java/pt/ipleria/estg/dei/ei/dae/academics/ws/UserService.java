package pt.ipleria.estg.dei.ei.dae.academics.ws;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Context;

import pt.ipleria.estg.dei.ei.dae.academics.dtos.CreateUserDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.UserDTO;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.security.Authenticated;

import java.util.List;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserService {

    @EJB
    private UserBean userBean;

    // EP01 — criar utilizador (admin)
    @POST
    @Authenticated
    public Response create(CreateUserDTO dto) {

        User user = userBean.create(
                dto.username,
                dto.password,
                dto.name,
                dto.email,
                dto.role
        );

        if (user == null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Utilizador já existe\"}")
                    .build();
        }

        return Response.status(Response.Status.CREATED)
                .entity(UserDTO.from(user))
                .build();
    }

    // EP04 — consultar perfil
    @GET
    @Path("/{username}")
    @Authenticated
    public Response get(@PathParam("username") String username) {

        User user = userBean.find(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(UserDTO.from(user)).build();
    }

    // EP05 — listar utilizadores
    @GET
    @Authenticated
    public List<UserDTO> list() {
        return userBean.findAll()
                .stream()
                .map(UserDTO::from)
                .toList();
    }

    // EP06 — alterar role
    @PUT
    @Path("/{username}/role")
    @Authenticated
    public Response changeRole(@PathParam("username") String username,
                               CreateUserDTO dto) {

        userBean.changeRole(username, dto.role);
        return Response.ok("{\"message\":\"Role atualizado com sucesso\"}")
                .build();
    }

    // EP07 — ativar / suspender
    @PATCH
    @Path("/{username}")
    @Authenticated
    public Response setActive(@PathParam("username") String username,
                              String body) {

        boolean active = body.contains("active");
        userBean.setActive(username, active);

        return Response.ok("{\"username\":\"" + username + "\"}")
                .build();
    }
}
