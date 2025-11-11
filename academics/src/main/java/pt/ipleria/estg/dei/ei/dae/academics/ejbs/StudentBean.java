package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Student;

import java.util.List;


@Stateless
public class StudentBean {
    @PersistenceContext
    private EntityManager entityManager;

    public void create(String username, String password, String name, String email) {
        Student student = new Student(username, password, name, email);
        entityManager.persist(student);
    }

    public List<Student> findAll() {
        return entityManager
                .createNamedQuery("getAllStudents", Student.class)
                .getResultList();
    }

}
