package pt.ipleria.estg.dei.ei.dae.academics.ws;

import pt.ipleria.estg.dei.ei.dae.academics.ejbs.StudentBean;
import pt.ipleria.estg.dei.ei.dae.academics.dtos.StudentDTO;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("students")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StudentService {
    @EJB
    private StudentBean studentBean;

    @GET
    @Path("/")
    public List<StudentDTO> getAllStudents() {
        return StudentDTO.from(studentBean.findAll());
    }
}
