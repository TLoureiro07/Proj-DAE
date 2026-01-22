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
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.EmailBean;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.UserDTO;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.UserBean;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.PublicationDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.CommentDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.RatingDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.TagDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.CreateRatingDTO;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.PublicationHistoryDTO;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.PublicationHistory;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Rating;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.nio.charset.StandardCharsets;

@Path("publications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicationService {

    private static final Logger LOG = Logger.getLogger(PublicationService.class.getName());


    @EJB
    private PublicationBean publicationBean;

    @EJB
    private CommentBean commentBean;

    @EJB
    private UserBean userBean;

    @EJB
    private EmailBean emailBean;

    @EJB
    private RatingBean ratingBean;

    @EJB
    private TagBean tagBean;

    // Criar publicação sem ficheiro (opcional)
    @POST
    @Authenticated
    public Response createPublication(PublicationDTO dto, @Context SecurityContext sc) {
        try {
            String username = sc != null && sc.getUserPrincipal() != null ?
                sc.getUserPrincipal().getName() : null;
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            if (dto.title == null || dto.title.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Título é obrigatório"))
                    .build();
            }

            boolean isResponsible = sc.isUserInRole("Responsible") || sc.isUserInRole("Administrator");
            String visibility = "internal";
            
            if (dto.visibility != null && !dto.visibility.trim().isEmpty()) {
                if (isResponsible) {
                    visibility = dto.visibility;
                } else {
                    visibility = "internal";
                }
            }

            Publication p = new Publication();
            p.setTitle(dto.title.trim());
            p.setAuthors(dto.authors != null ? dto.authors : new java.util.ArrayList<>());
            p.setScientificArea(dto.scientificArea != null ? dto.scientificArea.trim() : null);
            p.setVisibility(visibility);
            p.setSummary(dto.summary != null ? dto.summary.trim() : null);

            Publication created = publicationBean.create(username, p);
            if (created == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Erro ao criar publicação"))
                    .build();
            }

            java.util.List<String> subscribedTags = new java.util.ArrayList<>();
            
            if (dto.tags != null && !dto.tags.isEmpty()) {
                for (TagDTO tagDto : dto.tags) {
                    if (tagDto != null && tagDto.id != null) {
                        Tag tag = tagBean.find(tagDto.id);
                        if (tag != null) {
                            boolean wasNewSubscription = publicationBean.wasOwnerSubscribedToTag(created.getId(), tag.getId());
                            publicationBean.addTag(created.getId(), tag.getId(), null);
                            if (wasNewSubscription) {
                                subscribedTags.add(tag.getName());
                                notifyUserSubscribedToTag(username, tag);
                            }
                        }
                    }
                }
            }

            created = publicationBean.findWithRelations(created.getId());
            if (created == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Erro ao recarregar publicação"))
                    .build();
            }

            return Response.status(Response.Status.CREATED)
                .entity(PublicationDTO.from(created))
                .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro interno: " + e.getMessage()))
                .build();
        }
    }

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
            String filename = filePart.getFileName();
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

            p = publicationBean.findWithRelations(p.getId());
            if (p == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Erro ao recarregar publicação"))
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
        Publication p = publicationBean.findWithRelations(id);
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

        User currentUser = userBean.find(editor);
        if (currentUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        boolean isOwner = p.getOwner() != null && p.getOwner().getUsername().equals(editor);
        boolean isResponsible = sc.isUserInRole("Responsible") || sc.isUserInRole("Administrator");

        if (!isOwner && !isResponsible) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", "Apenas o dono da publicação ou um Responsible/Administrator pode editar"))
                .build();
        }

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
            if (!isResponsible) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Apenas Responsible/Administrator pode alterar a visibilidade"))
                    .build();
            }
            p = publicationBean.updateVisibility(id, body.get("visibility").toString(), editor);
            updated = true;
        }

        if (body.containsKey("authors") && body.get("authors") != null) {
            Object authorsObj = body.get("authors");
            java.util.List<String> authors = new java.util.ArrayList<>();
            if (authorsObj instanceof java.util.List) {
                for (Object item : (java.util.List<?>) authorsObj) {
                    if (item != null) {
                        authors.add(item.toString());
                    }
                }
            }
            p = publicationBean.updateAuthors(id, authors, editor);
            updated = true;
        }

        if (!updated) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Nenhum campo válido para atualizar")).build();
        }

        p = publicationBean.findWithRelations(p.getId());
        if (p == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao recarregar publicação"))
                .build();
        }

        notifyPublicationEditedSubscribers(p, editor);

        return Response.ok(PublicationDTO.from(p)).build();
    }

    // EP04 - histórico de edições da publicação
    @GET
    @Path("{id}/history")
    @Authenticated
    public Response getHistory(@PathParam("id") Long id, @Context SecurityContext sc) {
        try {
            String username = sc != null && sc.getUserPrincipal() != null
                    ? sc.getUserPrincipal().getName()
                    : null;
            
            Publication p = publicationBean.find(id);
            if (p == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            
            User currentUser = userBean.find(username);
            if (currentUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            boolean isOwner = p.getOwner() != null && p.getOwner().getUsername().equals(username);
            boolean isResponsible = sc.isUserInRole("Responsible") || sc.isUserInRole("Administrator");
            
            if (!isOwner && !isResponsible) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Apenas o dono da publicação ou um Responsible/Administrator pode ver o histórico"))
                    .build();
            }
            
            List<PublicationHistory> history = publicationBean.getHistory(id);
            return Response.ok(Map.of("publicationId", id, "history", PublicationHistoryDTO.from(history))).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao obter histórico da publicação " + id, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao carregar histórico: " + e.getMessage()))
                .build();
        }
    }

    // EP07 - listar todas as publicações visíveis (com pesquisa e filtros)
    @GET
    @Authenticated
    public Response listVisible(@QueryParam("search") String search,
                                  @QueryParam("scientificArea") String scientificArea,
                                  @QueryParam("tag") String tagName,
                                  @QueryParam("sortBy") String sortBy,
                                  @QueryParam("order") String order) {
        try {
            List<Publication> list = publicationBean.findVisibleWithRelations(search, scientificArea, tagName, sortBy, order);
            List<PublicationDTO> out = list.stream()
                .map(PublicationDTO::from)
                .collect(Collectors.toList());
            return Response.ok(out).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao carregar publicações: " + e.getMessage()))
                .build();
        }
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

            Publication p = publicationBean.find(id);
            if (p == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            boolean isOwner = p.getOwner() != null && p.getOwner().getUsername().equals(username);
            boolean isResponsible = sc.isUserInRole("Responsible") || sc.isUserInRole("Administrator");

            if (!isOwner && !isResponsible) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Apenas o dono da publicação ou um Responsible/Administrator pode atualizar o ficheiro"))
                    .build();
            }

            java.util.List<InputPart> parts = form.getFormDataMap().get("file");
            if (parts == null || parts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Missing form field 'file'"))
                    .build();
            }
            InputPart filePart = parts.get(0);
            String filename = filePart.getFileName();
            InputStream inputStream = filePart.getBody(InputStream.class, null);

            p = publicationBean.updateFile(id, inputStream, filename, username);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();

            p = publicationBean.findWithRelations(p.getId());
            if (p == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Erro ao recarregar publicação"))
                    .build();
            }

            return Response.ok(PublicationDTO.from(p)).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("{id}/file")
    @Authenticated
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@PathParam("id") Long id,
                                 @Context SecurityContext sc) {
        try {
            String username = sc != null && sc.getUserPrincipal() != null ?
                sc.getUserPrincipal().getName() : null;
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            Publication p = publicationBean.find(id);
            if (p == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Publicação não encontrada")
                    .build();
            }

            boolean canAccess = p.getOwner() != null && p.getOwner().getUsername().equals(username);
            
            if (!canAccess) {
                if ("hidden".equals(p.getVisibility())) {
                    // Apenas Responsible/Administrator podem ver publicações ocultas
                    if (!sc.isUserInRole("Responsible") && !sc.isUserInRole("Administrator")) {
                        return Response.status(Response.Status.FORBIDDEN)
                            .entity("Não tem permissão para aceder a esta publicação")
                            .build();
                    }
                } else {
                    // Publicações públicas ou internas podem ser vistas por todos autenticados
                    canAccess = true;
                }
            }

            if (p.getFilePath() == null || p.getFilePath().isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Ficheiro não encontrado para esta publicação")
                    .build();
            }

            java.nio.file.Path filePath = java.nio.file.Paths.get(p.getFilePath());
            if (!java.nio.file.Files.exists(filePath) || !java.nio.file.Files.isRegularFile(filePath)) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Ficheiro não encontrado: " + filePath)
                    .build();
            }

            String filename = p.getFileName() != null ? p.getFileName() : "publication_file";

            return Response.ok(filePath.toFile())
                .header("Content-Disposition", "attachment;filename=\"" + filename + "\"")
                .build();
        } catch (Exception e) {
            return Response.serverError()
                .entity("Erro ao descarregar ficheiro: " + e.getMessage())
                .build();
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

        comment = commentBean.findWithRelations(comment.getId());
        if (comment == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao recarregar comentário"))
                .build();
        }

        Publication publication = publicationBean.findWithRelations(publicationId);
        notifyCommentSubscribers(publication, comment, username);

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
                                         CreateRatingDTO dto,
                                         @Context SecurityContext sc) {
        String username = sc != null && sc.getUserPrincipal() != null ?
            sc.getUserPrincipal().getName() : null;
        if (username == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (dto == null || dto.value == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Campo 'value' é obrigatório"))
                .build();
        }

        Integer value = dto.value;
        if (value < 1 || value > 5) {
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

        rating = ratingBean.findWithRelations(rating.getId());
        if (rating == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao recarregar rating"))
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
                                        @PathParam("tagId") Long tagId,
                                        @Context SecurityContext sc) {

        String username = sc != null && sc.getUserPrincipal() != null
                ? sc.getUserPrincipal().getName()
                : null;

        Publication p = publicationBean.find(publicationId);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Tag tag = tagBean.find(tagId);
        if (tag == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean wasNewSubscription = false;
        if (p.getOwner() != null && p.getOwner().getUsername().equals(username)) {
            wasNewSubscription = publicationBean.wasOwnerSubscribedToTag(publicationId, tagId);
        }

        p = publicationBean.addTag(publicationId, tagId, username);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        p = publicationBean.findWithRelations(p.getId());
        if (p == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao recarregar publicação"))
                .build();
        }

        if (username != null) {
            notifyTagAddedSubscribers(p, tag, username);
            
            if (wasNewSubscription) {
                notifyUserSubscribedToTag(username, tag);
            }
        }

        return Response.ok(PublicationDTO.from(p)).build();
    }

    // Desassociar tag de publicação (apenas Responsible/Administrator)
    @DELETE
    @Path("{id}/tags/{tagId}")
    @Authenticated
    @RolesAllowed({"Responsible", "Administrator"})
    public Response removeTagFromPublication(@PathParam("id") Long publicationId,
                                           @PathParam("tagId") Long tagId,
                                           @Context SecurityContext sc) {
        String username = sc != null && sc.getUserPrincipal() != null
                ? sc.getUserPrincipal().getName()
                : null;
        Publication p = publicationBean.removeTag(publicationId, tagId, username);
        if (p == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Recarregar com relações lazy inicializadas
        p = publicationBean.findWithRelations(p.getId());
        if (p == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Erro ao recarregar publicação"))
                .build();
        }

        return Response.ok(PublicationDTO.from(p)).build();
    }

    // ============================================
    // send notification email
    // ============================================

    private void notifyCommentSubscribers(Publication publication,
                                          Comment comment,
                                          String commenterUsername) {

        LOG.info("notifyCommentSubscribers called");

        if (publication == null) {
            LOG.warning("Publication is null, aborting email notification");
            return;
        }

        if (publication.getTags() == null) {
            LOG.warning("Publication has no tags, no notifications to send");
            return;
        }

        LOG.info("Publication ID: " + publication.getId() +
                ", Title: " + publication.getTitle());

        Set<User> recipients = new HashSet<>();

        for (Tag tag : publication.getTags()) {
            LOG.info("Processing tag: " + tag.getName() +
                    " (id=" + tag.getId() + ")");

            if (tag.getSubscribedUsers() == null) {
                LOG.warning("Tag " + tag.getName() + " has null subscribedUsers");
                continue;
            }

            LOG.info("Tag " + tag.getName() +
                    " has " + tag.getSubscribedUsers().size() +
                    " subscribed users");

            for (User user : tag.getSubscribedUsers()) {
                LOG.info("Found subscribed user: " + user.getUsername());

                if (!user.getUsername().equals(commenterUsername)) {
                    recipients.add(user);
                } else {
                    LOG.info("Skipping commenter himself: " + commenterUsername);
                }
            }
        }

        LOG.info("Total unique email recipients: " + recipients.size());

        for (User user : recipients) {
            String subject = "[Academics] Novo comentário numa publicação que segues";

            String body =
                    "Olá " + user.getName() + ",\n\n" +
                            "Foi adicionado um novo comentário à publicação:\n\n" +
                            "Título: " + publication.getTitle() + "\n" +
                            "Comentário: \"" + comment.getText() + "\"\n\n" +
                            "Autor do comentário: " + commenterUsername + "\n\n" +
                            "Recebeste este email porque subscreves uma das tags desta publicação.\n";

            LOG.info("Preparing email to: " + user.getEmail());
            LOG.fine("Email subject: " + subject);
            LOG.fine("Email body:\n" + body);

            try {
                emailBean.send(user.getEmail(), subject, body);
                LOG.info("Email successfully sent to " + user.getEmail());
            } catch (Exception e) {
                LOG.log(Level.SEVERE,
                        "Failed to send email to " + user.getEmail(),
                        e);
            }
        }
    }

    private void notifyTagAddedSubscribers(Publication publication, Tag tag, String actorUsername) {

        LOG.info("notifyTagAddedSubscribers called");

        if (publication == null || tag == null) {
            LOG.warning("Publication or Tag is null, aborting");
            return;
        }

        if (tag.getSubscribedUsers() == null || tag.getSubscribedUsers().isEmpty()) {
            LOG.info("No subscribed users for tag " + tag.getName());
            return;
        }

        LOG.info("Tag added: " + tag.getName() +
                " to publication: " + publication.getTitle());

        for (User user : tag.getSubscribedUsers()) {

            if (user.getUsername().equals(actorUsername)) {
                LOG.info("Skipping actor himself: " + actorUsername);
                continue;
            }

            String subject = "[Academics] Nova publicação associada a uma tag que segues";

            String body =
                    "Olá " + user.getName() + ",\n\n" +
                            "Uma publicação foi associada a uma tag que segues.\n\n" +
                            "Publicação: " + publication.getTitle() + "\n" +
                            "Tag: " + tag.getName() + "\n\n" +
                            "Ação realizada por: " + actorUsername + "\n\n";

            LOG.info("Sending tag-added email to " + user.getEmail());

            try {
                emailBean.send(user.getEmail(), subject, body);
                LOG.info("Email successfully sent to " + user.getEmail());
            } catch (Exception e) {
                LOG.log(Level.SEVERE,
                        "Failed to send email to " + user.getEmail(), e);
            }
        }
    }

    private void notifyPublicationEditedSubscribers(Publication publication, String editorUsername) {

        LOG.info("notifyPublicationEditedSubscribers called");

        if (publication == null || publication.getTags() == null) {
            LOG.warning("Publication or tags null, aborting");
            return;
        }

        Set<User> recipients = new HashSet<>();

        for (Tag tag : publication.getTags()) {
            if (tag.getSubscribedUsers() != null) {
                for (User user : tag.getSubscribedUsers()) {
                    if (!user.getUsername().equals(editorUsername)) {
                        recipients.add(user);
                    }
                }
            }
        }

        LOG.info("Total recipients for edit notification: " + recipients.size());

        for (User user : recipients) {

            String subject = "[Academics] Publicação atualizada";

            String body =
                    "Olá " + user.getName() + ",\n\n" +
                            "Uma publicação associada a uma tag que segues foi atualizada.\n\n" +
                            "Título: " + publication.getTitle() + "\n" +
                            "Editado por: " + editorUsername + "\n\n";

            LOG.info("Sending edit notification to " + user.getEmail());

            try {
                emailBean.send(user.getEmail(), subject, body);
                LOG.info("Email successfully sent to " + user.getEmail());
            } catch (Exception e) {
                LOG.log(Level.SEVERE,
                        "Failed to send email to " + user.getEmail(), e);
            }
        }
    }

    private void notifyUserSubscribedToTag(String username, Tag tag) {
        User user = userBean.find(username);
        if (user == null || tag == null) return;

        String subject = "[Academics] A tua publicação foi adicionada à plataforma";

        String body = "Olá " + user.getName() + ",\n\n" +
                "A tua publicação foi adicionada à plataforma.\n\n" +
                "Foste automaticamente subscrito na tag: " + tag.getName() + "\n\n" +
                "Receberás notificações sempre que houver novidades relacionadas com esta tag.";

        try {
            emailBean.send(user.getEmail(), subject, body);
            LOG.info("Subscription notification sent to " + user.getEmail());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to send subscription notification to " + user.getEmail(), e);
        }
    }

    @POST
    @Path("/import-csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Authenticated
    @RolesAllowed({"Administrator"})
    public Response importPublicationsCSV(MultipartFormDataInput input) {

        try {
            InputPart filePart = input.getFormDataMap().get("file").get(0);
            InputStream is = filePart.getBody(InputStream.class, null);

            int imported = publicationBean.importFromCSV(is);

            return Response.ok(
                    Map.of(
                            "message", "Publicações importadas com sucesso",
                            "count", imported
                    )
            ).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

}