package pt.ipleria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Context;

import pt.ipleria.estg.dei.ei.dae.academics.dtos.CreateUserDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.UserDTO;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.PublicationBean;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.UserActivityBean;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.TagDTO;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.UserActivity;
import pt.ipleria.estg.dei.ei.dae.academics.security.Authenticated;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserService {

    @EJB
    private UserBean userBean;

    @EJB
    private PublicationBean publicationBean;

    @EJB
    private UserActivityBean userActivityBean;

    // EP01 — criar utilizador (admin)
    @POST
    @Authenticated
    @RolesAllowed({"Administrator"})
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
    @RolesAllowed({"Administrator", "Responsible"})
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
    @RolesAllowed({"Administrator"})
    public Response changeRole(@PathParam("username") String username,
                               CreateUserDTO dto) {

        userBean.changeRole(username, dto.role);
        return Response.ok("{\"message\":\"Role atualizado com sucesso\"}")
                .build();
    }

    // EP07 — ativar / suspender (apenas Administrator)
    @PATCH
    @Path("/{username}/active")
    @Authenticated
    @RolesAllowed({"Administrator"})
    public Response setActive(@PathParam("username") String username,
                              Map<String, Object> body) {
        if (body == null || !body.containsKey("active")) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Campo 'active' é obrigatório"))
                .build();
        }

        // Converter o valor para boolean (pode vir como Boolean, String, ou Number)
        Object activeObj = body.get("active");
        boolean active;
        if (activeObj instanceof Boolean) {
            active = (Boolean) activeObj;
        } else if (activeObj instanceof String) {
            active = Boolean.parseBoolean((String) activeObj);
        } else if (activeObj instanceof Number) {
            active = ((Number) activeObj).intValue() != 0;
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Campo 'active' deve ser um boolean"))
                .build();
        }

        userBean.setActive(username, active);
        User user = userBean.find(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(UserDTO.from(user)).build();
    }

    // Editar dados pessoais (nome e email) - próprio utilizador ou Administrator
    @PATCH
    @Path("/{username}")
    @Authenticated
    public Response updateProfile(@PathParam("username") String username,
                                  Map<String, Object> body,
                                  @Context SecurityContext sc) {
        String currentUser = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        
        // Só pode editar o próprio perfil, ou se for Administrator
        if (currentUser == null || 
            (!currentUser.equals(username) && !sc.isUserInRole("Administrator"))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User user = userBean.find(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean updated = false;
        if (body != null) {
            if (body.containsKey("name") && body.get("name") != null) {
                user.setName(body.get("name").toString());
                updated = true;
            }
            if (body.containsKey("email") && body.get("email") != null) {
                user.setEmail(body.get("email").toString());
                updated = true;
            }
        }

        if (!updated) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Nenhum campo válido para atualizar"))
                .build();
        }

        userBean.update(user);
        return Response.ok(UserDTO.from(user)).build();
    }

    // EP08 — listar publicações de um utilizador
    @GET
    @Path("/{username}/publications")
    @Authenticated
    public Response getUserPublications(@PathParam("username") String username,
                                         @Context SecurityContext sc) {
        User user = userBean.find(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Verificar se o utilizador pode ver estas publicações
        String currentUser = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        
        // Só pode ver as suas próprias publicações, ou se for Admin/Responsible
        if (currentUser == null || 
            (!currentUser.equals(username) && 
             !sc.isUserInRole("Administrator") && 
             !sc.isUserInRole("Responsible"))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            List<Publication> publications = publicationBean.findByOwnerWithRelations(username);
            List<Map<String, Object>> result = publications.stream()
                .map(p -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", p.getId());
                    map.put("title", p.getTitle() != null ? p.getTitle() : "");
                    map.put("visibility", p.getVisibility() != null ? p.getVisibility() : "");
                    map.put("uploadDate", p.getUploadDate() != null ? p.getUploadDate().toString() : "");
                    map.put("lastEdited", p.getLastEdited() != null ? p.getLastEdited().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());

            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao carregar publicações: " + e.getMessage()))
                .build();
        }
    }

    // EP09 — consultar histórico de atividade de um utilizador
    @GET
    @Path("/{username}/activity")
    @Authenticated
    public Response getUserActivity(@PathParam("username") String username,
                                     @Context SecurityContext sc) {
        User user = userBean.find(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Verificar se o utilizador pode ver este histórico
        String currentUser = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        
        // Só pode ver o seu próprio histórico, ou se for Administrator
        if (currentUser == null || 
            (!currentUser.equals(username) && !sc.isUserInRole("Administrator"))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<UserActivity> activities = userActivityBean.findByUser(username);
        List<Map<String, Object>> result = activities.stream()
            .map(a -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", a.getId());
                map.put("activityType", a.getActivityType());
                map.put("description", a.getDescription() != null ? a.getDescription() : "");
                map.put("activityDate", a.getActivityDate() != null ? a.getActivityDate().toString() : "");
                if (a.getPublication() != null) {
                    map.put("publicationId", a.getPublication().getId());
                    map.put("publicationTitle", a.getPublication().getTitle() != null ? 
                        a.getPublication().getTitle() : "");
                }
                return map;
            })
            .collect(Collectors.toList());

        return Response.ok(result).build();
    }

    // Subscrições de tags
    @GET
    @Path("/{username}/tags")
    @Authenticated
    public Response getSubscribedTags(@PathParam("username") String username,
                                      @Context SecurityContext sc) {
        User user = userBean.find(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Só pode ver as suas próprias subscrições, ou se for Administrator
        String currentUser = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        
        if (currentUser == null || 
            (!currentUser.equals(username) && !sc.isUserInRole("Administrator"))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            List<Tag> tags = userBean.getSubscribedTags(username);
            List<TagDTO> dtos = tags.stream()
                .map(TagDTO::from)
                .collect(Collectors.toList());

            return Response.ok(dtos).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao carregar subscrições: " + e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/{username}/tags/{tagId}")
    @Authenticated
    public Response subscribeToTag(@PathParam("username") String username,
                                   @PathParam("tagId") Long tagId,
                                   @Context SecurityContext sc) {
        String currentUser = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        
        if (currentUser == null || !currentUser.equals(username)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        userBean.subscribeToTag(username, tagId);
        
        // Registar atividade
        userActivityBean.recordActivity(username, null, "TAG_SUBSCRIPTION", 
            "Subscreveu tag ID: " + tagId);

        return Response.ok(Map.of("message", "Subscrição realizada com sucesso")).build();
    }

    @DELETE
    @Path("/{username}/tags/{tagId}")
    @Authenticated
    public Response unsubscribeFromTag(@PathParam("username") String username,
                                       @PathParam("tagId") Long tagId,
                                       @Context SecurityContext sc) {
        String currentUser = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        
        if (currentUser == null || !currentUser.equals(username)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        userBean.unsubscribeFromTag(username, tagId);
        return Response.noContent().build();
    }

    // Eliminar utilizador (apenas Administrator)
    @DELETE
    @Path("/{username}")
    @Authenticated
    @RolesAllowed({"Administrator"})
    public Response delete(@PathParam("username") String username) {
        User user = userBean.find(username);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Utilizador não encontrado"))
                .build();
        }

        boolean deleted = userBean.delete(username);
        if (!deleted) {
            return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", "Não é possível eliminar utilizador com publicações associadas"))
                .build();
        }

        return Response.noContent().build();
    }
}
