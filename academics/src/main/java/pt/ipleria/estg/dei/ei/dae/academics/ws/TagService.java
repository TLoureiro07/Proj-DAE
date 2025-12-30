package pt.ipleria.estg.dei.ei.dae.academics.ws;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.TagDTO;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.TagBean;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleria.estg.dei.ei.dae.academics.security.Authenticated;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TagService {

    @EJB
    private TagBean tagBean;

    // Criar tag (apenas Responsible/Administrator)
    @POST
    @Authenticated
    @RolesAllowed({"Responsible", "Administrator"})
    public Response create(Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Nome da tag é obrigatório"))
                .build();
        }

        Tag tag = tagBean.create(name);
        if (tag == null) {
            return Response.status(Response.Status.CONFLICT)
                .entity(Map.of("error", "Tag já existe"))
                .build();
        }

        return Response.status(Response.Status.CREATED)
            .entity(TagDTO.from(tag))
            .build();
    }

    // Listar todas as tags
    @GET
    @Authenticated
    public Response list() {
        List<Tag> tags = tagBean.findAll();
        List<TagDTO> dtos = tags.stream()
            .map(TagDTO::from)
            .collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    // Obter tag específica
    @GET
    @Path("/{id}")
    @Authenticated
    public Response get(@PathParam("id") Long id) {
        Tag tag = tagBean.find(id);
        if (tag == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(TagDTO.from(tag)).build();
    }

    // Remover tag (apenas Responsible/Administrator)
    @DELETE
    @Path("/{id}")
    @Authenticated
    @RolesAllowed({"Responsible", "Administrator"})
    public Response delete(@PathParam("id") Long id) {
        Tag tag = tagBean.find(id);
        if (tag == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        tagBean.delete(id);
        return Response.noContent().build();
    }
}

