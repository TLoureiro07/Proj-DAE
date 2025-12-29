package pt.ipleria.estg.dei.ei.dae.academics.ws;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.PublicationBean;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.PublicationDTO;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.PublicationHistory;

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
/*
    @EJB
    private PublicationBean publicationBean;

    // EP01 - criar nova publicação (owner obtido do SecurityContext)
    @POST
    public Response createPublication(PublicationDTO dto, @Context SecurityContext sc) {
        String owner = null;
        if (sc != null && sc.getUserPrincipal() != null) owner = sc.getUserPrincipal().getName();
        Publication p = new Publication();
        p.setTitle(dto.title);
        p.setAuthors(dto.authors);
        p.setScientificArea(dto.scientificArea);
        p.setTags(dto.tags);
        p.setVisibility(dto.visibility == null ? "internal" : dto.visibility);
        p = publicationBean.create(owner, p);
        return Response.status(Response.Status.CREATED).entity(PublicationDTO.from(p)).build();
    }

    // EP02 - detalhe
    @GET
    @Path("{id}")
    public Response getPublication(@PathParam("id") Long id) {
        Publication p = publicationBean.find(id);
        if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
        PublicationDTO dto = PublicationDTO.from(p);
        // resumo, rating, comments -> aqui usar campos existentes ou calcular
        return Response.ok(dto).build();
    }

    // EP03 - editar campos (ex: summary) - PATCH simplificado como POST/PUT
    @PATCH
    @Path("{id}")
    public Response patchPublication(@PathParam("id") Long id, PublicationDTO dto, @QueryParam("editor") String editor) {
        if (dto.summary != null) {
            Publication p = publicationBean.updateSummary(id, dto.summary, editor);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(java.util.Map.of("message", "Publicacao atualizada com sucesso")).build();
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    // EP04 - histórico
    @GET
    @Path("{id}/history")
    public Response getHistory(@PathParam("id") Long id) {
        List<PublicationHistory> history = publicationBean.getHistory(id);
        return Response.ok(java.util.Map.of("publicationId", id, "history", history)).build();
    }

    // EP05 - alterar visibilidade
    @PATCH
    @Path("{id}/visibility")
    public Response changeVisibility(@PathParam("id") Long id, java.util.Map<String,String> body, @QueryParam("editor") String editor) {
        String visibility = body.get("visibility");
        if (visibility == null) return Response.status(Response.Status.BAD_REQUEST).build();
        Publication p = publicationBean.changeVisibility(id, visibility, editor);
        if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(java.util.Map.of("id", p.getId(), "title", p.getTitle(), "visibility", p.getVisibility())).build();
    }

    // EP06 - minhas publicações
    @GET
    @Path("mine")
    public Response myPublications(@QueryParam("owner") String owner) {
        List<Publication> list = publicationBean.findByOwner(owner);
        List<Object> out = list.stream().map(p -> java.util.Map.of(
                "id", p.getId(),
                "title", p.getTitle(),
                "visibility", p.getVisibility(),
                "lastEdited", p.getLastEdited()
        )).collect(Collectors.toList());
        return Response.ok(out).build();
    }

    // EP07 - listar visíveis (simples)
    @GET
    public Response listVisible(@QueryParam("search") String search, @QueryParam("sortBy") String sortBy, @QueryParam("order") String order) {
        List<Publication> list = publicationBean.findVisible(search, sortBy, order);
        List<Object> out = list.stream().map(p -> java.util.Map.of(
                "id", p.getId(),
                "title", p.getTitle(),
                "author", p.getAuthors() != null && !p.getAuthors().isEmpty() ? p.getAuthors().get(0) : null,
                "visibility", p.getVisibility(),
                "tags", p.getTags(),
                "ratingAvg", p.getRatingAvg()
        )).collect(Collectors.toList());
        return Response.ok(out).build();
    }

    // EP08 - upload ficheiro adapted: accepts multipart/form-data with field 'file' and uses SecurityContext
    @POST
    @Path("{id}/file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(@PathParam("id") Long id, MultipartFormDataInput form, @Context SecurityContext sc) {
        try {
            var username = sc != null && sc.getUserPrincipal() != null ? sc.getUserPrincipal().getName() : null;
            if (username == null) username = "unknown";

            // get the first part named 'file'
            var parts = form.getFormDataMap().get("file");
            if (parts == null || parts.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "Missing form field 'file'"))
                        .build();
            }
            InputPart filePart = parts.get(0);
            String filename = extractFileName(filePart);
            InputStream inputStream = filePart.getBody(InputStream.class, null);

            Publication p = publicationBean.saveFile(id, inputStream, filename, username);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(Map.of("id", p.getId(), "file", p.getFileName())).build();
        } catch (Exception e) {
            return Response.serverError().entity(java.util.Map.of("error", e.getMessage())).build();
        }
    }

    // helper to parse filename from Content-Disposition header
    private String extractFileName(InputPart part) {
        try {
            var headers = part.getHeaders();
            var contentDisp = headers.getFirst("Content-Disposition");
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
    }*/
}