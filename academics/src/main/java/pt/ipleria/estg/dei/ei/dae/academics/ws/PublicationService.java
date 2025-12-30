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
import pt.ipleria.estg.dei.ei.dae.academics.security.Authenticated;

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

    // EP01 - upload de publicação (PDF ou ZIP) - cria publicação diretamente (padrão Ficha 9)
    @POST
    @Path("upload")
    @Authenticated
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadPublication(MultipartFormDataInput form, 
                                      @Context SecurityContext sc) {
        try {
            var username = sc != null && sc.getUserPrincipal() != null ? 
                sc.getUserPrincipal().getName() : null;
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            var parts = form.getFormDataMap().get("file");
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

        if (body.containsKey("summary") && body.get("summary") != null) {
            p = publicationBean.updateSummary(id, body.get("summary").toString(), editor);
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

    // EP07 - listar todas as publicações visíveis
    @GET
    @Authenticated
    public Response listVisible(@QueryParam("search") String search, 
                                 @QueryParam("sortBy") String sortBy, 
                                 @QueryParam("order") String order) {
        List<Publication> list = publicationBean.findVisible(search, sortBy, order);
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
            var username = sc != null && sc.getUserPrincipal() != null ? 
                sc.getUserPrincipal().getName() : null;
            if (username == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            var parts = form.getFormDataMap().get("file");
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
    }
}