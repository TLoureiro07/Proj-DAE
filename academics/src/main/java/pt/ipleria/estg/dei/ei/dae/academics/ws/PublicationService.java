package pt.ipleria.estg.dei.ei.dae.academics.ws;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.PublicationBean;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.CommentBean;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.RatingBean;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.TagBean;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.PublicationDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.CommentDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.RatingDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.TagDTO;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.PublicationHistory;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Rating;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleria.estg.dei.ei.dae.academics.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

@Path("publications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicationService {
    @EJB
    private PublicationBean publicationBean;

    @EJB
    private CommentBean commentBean;

    @EJB
    private RatingBean ratingBean;

    @EJB
    private TagBean tagBean;

    // Criar publicação sem ficheiro (opcional)
    @POST
    @Authenticated
    public Response createPublication(PublicationDTO dto, @Context SecurityContext sc) {
        String username = sc != null && sc.getUserPrincipal() != null ? 
            sc.getUserPrincipal().getName() : null;
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Publication p = new Publication();
        p.setTitle(dto.title);
        p.setAuthors(dto.authors);
        p.setScientificArea(dto.scientificArea);
        p.setVisibility(dto.visibility != null ? dto.visibility : "internal");
        p.setSummary(dto.summary);

        // Associar tags se fornecidas
        if (dto.tags != null && !dto.tags.isEmpty()) {
            for (TagDTO tagDto : dto.tags) {
                Tag tag = tagBean.find(tagDto.id);
                if (tag != null) {
                    p.addTag(tag);
                }
            }
        }

        Publication created = publicationBean.create(username, p);
        if (created == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Erro ao criar publicação"))
                .build();
        }

        return Response.status(Response.Status.CREATED)
            .entity(PublicationDTO.from(created))
            .build();
    }

    // EP01 - upload de publicação (PDF ou ZIP) - cria publicação diretamente (padrão Ficha 9)
    @POST
    @Path("upload")
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadPublication(MultipartFormDataInput form, 
                                      @Context SecurityContext sc) {
        try {
            String username = sc != null && sc.getUserPrincipal() != null ? 
                sc.getUserPrincipal().getName() : null;
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            java.util.List<InputPart> parts = form.getFormDataMap().get("file");
            if (parts == null || parts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing form field 'file'"))
                    .build();
            }
            
            InputPart filePart = parts.get(0);
            String filename = filePart.getFileName(); // Padrão Ficha 9
            if (filename == null || filename.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Filename is required"))
                    .build();
            }
            
            InputStream inputStream = filePart.getBody(InputStream.class, null);
            
            Publication p = publicationBean.upload(username, filename, inputStream);
            if (p == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Erro ao criar publicação"))
                    .build();
            }
            
            return Response.status(Response.Status.CREATED)
                .entity(PublicationDTO.from(p))
                .build();
        } catch (Exception e) {
            return Response.serverError()
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }

    // EP02 - obter detalhe de uma publicação
    @GET
    @Path("{id}")
    @Authenticated
    public Response getPublication(@PathParam("id") Long id) {
        Publication p = publicationBean.find(id);
        if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
        PublicationDTO dto = PublicationDTO.from(p);
        return Response.ok(dto).build();
    }

    // EP03 - editar campos (ex: summary, visibility) - PATCH para atualizar campos parciais
    @PATCH
    @Path("{id}")
    @Authenticated
    public Response patchPublication(@PathParam("id") Long id, 
                                     Map<String, Object> body, 
                                     @Context SecurityContext sc) {
        String editor = sc != null && sc.getUserPrincipal() != null ? 
            sc.getUserPrincipal().getName() : null;
        if (editor == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Publication p = publicationBean.find(id);
        if (p == null) return Response.status(Response.Status.NOT_FOUND).build();

        boolean updated = false;

        if (body.containsKey("title") && body.get("title") != null) {
            p = publicationBean.updateTitle(id, body.get("title").toString(), editor);
            updated = true;
        }

        if (body.containsKey("summary") && body.get("summary") != null) {
            p = publicationBean.updateSummary(id, body.get("summary").toString(), editor);
            updated = true;
        }

        if (body.containsKey("scientificArea") && body.get("scientificArea") != null) {
            p = publicationBean.updateScientificArea(id, body.get("scientificArea").toString(), editor);
            updated = true;
        }

        if (body.containsKey("visibility") && body.get("visibility") != null) {
            p = publicationBean.updateVisibility(id, body.get("visibility").toString(), editor);
            updated = true;
        }

        if (!updated) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Nenhum campo válido para atualizar")).build();
        }

        return Response.ok(PublicationDTO.from(p)).build();
    }

    // EP04 - histórico de edições da publicação
    @GET
    @Path("{id}/history")
    @Authenticated
    public Response getHistory(@PathParam("id") Long id) {
        List<PublicationHistory> history = publicationBean.getHistory(id);
        return Response.ok(Map.of("publicationId", id, "history", history)).build();
    }

    // EP07 - listar todas as publicações visíveis (com pesquisa e filtros)
    @GET
    @Authenticated
    public Response listVisible(@QueryParam("search") String search,
                                 @QueryParam("scientificArea") String scientificArea,
                                 @QueryParam("tag") String tagName,
                                 @QueryParam("sortBy") String sortBy, 
                                 @QueryParam("order") String order) {
        List<Publication> list = publicationBean.findVisible(search, scientificArea, tagName, sortBy, order);
        List<PublicationDTO> out = list.stream()
            .map(PublicationDTO::from)
            .collect(Collectors.toList());
        return Response.ok(out).build();
    }

    // EP08 - atualizar ficheiro de publicação existente (opcional)
    @POST
    @Path("{id}/file")
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFile(@PathParam("id") Long id, 
                               MultipartFormDataInput form, 
                               @Context SecurityContext sc) {
        try {
            String username = sc != null && sc.getUserPrincipal() != null ? 
                sc.getUserPrincipal().getName() : null;
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            java.util.List<InputPart> parts = form.getFormDataMap().get("file");
            if (parts == null || parts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing form field 'file'"))
                    .build();
            }
            InputPart filePart = parts.get(0);
            String filename = filePart.getFileName(); // Padrão Ficha 9
            InputStream inputStream = filePart.getBody(InputStream.class, null);

            Publication p = publicationBean.updateFile(id, inputStream, filename, username);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(PublicationDTO.from(p)).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    // helper to parse filename from Content-Disposition header
    private String extractFileName(InputPart part) {
        try {
            java.util.Map<String, java.util.List<String>> headers = part.getHeaders();
            java.util.List<String> contentDispList = headers.get("Content-Disposition");
            if (contentDispList == null || contentDispList.isEmpty()) return null;
            String contentDisp = contentDispList.get(0);
            if (contentDisp == null) return null;
            for (String cd : contentDisp.split(";")) {
                cd = cd.trim();
                if (cd.startsWith("filename=")) {
                    String name = cd.substring(cd.indexOf('=') + 1).trim();
                    name = name.replaceAll("\"", "");
                    return name;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ============================================
    // COMENTÁRIOS
    // ============================================

    // Criar comentário
    @POST
    @Path("{id}/comments")
    @Authenticated
    public Response createComment(@PathParam("id") Long publicationId,
                                   Map<String, String> body,
                                   @Context SecurityContext sc) {
        String username = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String text = body.get("text");
        if (text == null || text.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Texto do comentário é obrigatório"))
                .build();
        }

        Comment comment = commentBean.create(publicationId, username, text.trim());
        if (comment == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Publicação não encontrada"))
                .build();
        }

        return Response.status(Response.Status.CREATED)
            .entity(CommentDTO.from(comment))
            .build();
    }

    // Listar comentários de uma publicação
    @GET
    @Path("{id}/comments")
    @Authenticated
    public Response listComments(@PathParam("id") Long publicationId,
                                  @Context SecurityContext sc) {
        String username = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Verificar se é Responsible ou Administrator (podem ver comentários ocultos)
        boolean includeHidden = false;
        try {
            if (sc.isUserInRole("Responsible") || sc.isUserInRole("Administrator")) {
                includeHidden = true;
            }
        } catch (Exception e) {
            // Se não conseguir verificar role, não inclui ocultos
        }

        List<Comment> comments = commentBean.findByPublication(publicationId, includeHidden);
        List<CommentDTO> dtos = comments.stream()
            .map(CommentDTO::from)
            .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }

    // Ocultar/mostrar comentário (apenas Responsible/Administrator)
    @PATCH
    @Path("{id}/comments/{commentId}/hidden")
    @Authenticated
    @RolesAllowed({"Responsible", "Administrator"})
    public Response toggleCommentHidden(@PathParam("id") Long publicationId,
                                        @PathParam("commentId") Long commentId,
                                        Map<String, Boolean> body) {
        Boolean hidden = body.get("hidden");
        if (hidden == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Campo 'hidden' é obrigatório"))
                .build();
        }

        commentBean.setHidden(commentId, hidden);
        return Response.ok(Map.of("message", "Comentário " + (hidden ? "ocultado" : "mostrado"))).build();
    }

    // Apagar comentário
    @DELETE
    @Path("{id}/comments/{commentId}")
    @Authenticated
    public Response deleteComment(@PathParam("id") Long publicationId,
                                  @PathParam("commentId") Long commentId,
                                  @Context SecurityContext sc) {
        String username = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Comment comment = commentBean.find(commentId);
        if (comment == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Só pode apagar o próprio comentário, ou se for Responsible/Administrator
        boolean canDelete = comment.getAuthor().getUsername().equals(username);
        try {
            if (sc.isUserInRole("Responsible") || sc.isUserInRole("Administrator")) {
                canDelete = true;
            }
        } catch (Exception e) {
            // Se não conseguir verificar role, só pode apagar o próprio
        }

        if (!canDelete) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", "Não tens permissão para apagar este comentário"))
                .build();
        }

        commentBean.delete(commentId);
        return Response.noContent().build();
    }

    // ============================================
    // RATINGS
    // ============================================

    // Criar ou atualizar rating
    @POST
    @Path("{id}/ratings")
    @Authenticated
    public Response createOrUpdateRating(@PathParam("id") Long publicationId,
                                         Map<String, Integer> body,
                                         @Context SecurityContext sc) {
        String username = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Integer value = body.get("value");
        if (value == null || value < 1 || value > 5) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Rating deve ser entre 1 e 5"))
                .build();
        }

        Rating rating = ratingBean.createOrUpdate(publicationId, username, value);
        if (rating == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Publicação não encontrada"))
                .build();
        }

        return Response.ok(RatingDTO.from(rating)).build();
    }

    // Listar ratings de uma publicação
    @GET
    @Path("{id}/ratings")
    @Authenticated
    public Response listRatings(@PathParam("id") Long publicationId) {
        List<Rating> ratings = ratingBean.findByPublication(publicationId);
        List<RatingDTO> dtos = ratings.stream()
            .map(RatingDTO::from)
            .collect(Collectors.toList());

        return Response.ok(dtos).build();
    }

    // Apagar rating
    @DELETE
    @Path("{id}/ratings/{ratingId}")
    @Authenticated
    public Response deleteRating(@PathParam("id") Long publicationId,
                                 @PathParam("ratingId") Long ratingId,
                                 @Context SecurityContext sc) {
        String username = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Rating rating = ratingBean.find(ratingId);
        if (rating == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Só pode apagar o próprio rating, ou se for Responsible/Administrator
        boolean canDelete = rating.getAuthor().getUsername().equals(username);
        try {
            if (sc.isUserInRole("Responsible") || sc.isUserInRole("Administrator")) {
                canDelete = true;
            }
        } catch (Exception e) {
            // Se não conseguir verificar role, só pode apagar o próprio
        }

        if (!canDelete) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", "Não tens permissão para apagar este rating"))
                .build();
        }

        ratingBean.delete(ratingId);
        return Response.noContent().build();
    }

    // ============================================
    // TAGS DE PUBLICAÇÃO (Responsible pode desassociar)
    // ============================================

    // Associar tag a publicação
    @POST
    @Path("{id}/tags/{tagId}")
    @Authenticated
    public Response addTagToPublication(@PathParam("id") Long publicationId,
                                        @PathParam("tagId") Long tagId) {
        Publication p = publicationBean.addTag(publicationId, tagId);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(PublicationDTO.from(p)).build();
    }

    // Desassociar tag de publicação (apenas Responsible/Administrator)
    @DELETE
    @Path("{id}/tags/{tagId}")
    @Authenticated
    @RolesAllowed({"Responsible", "Administrator"})
    public Response removeTagFromPublication(@PathParam("id") Long publicationId,
                                           @PathParam("tagId") Long tagId) {
        Publication p = publicationBean.removeTag(publicationId, tagId);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(PublicationDTO.from(p)).build();
    }
}