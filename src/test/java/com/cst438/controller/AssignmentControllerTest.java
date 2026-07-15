package com.cst438.controller;

import java.sql.Date;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.Grade;
import com.cst438.domain.GradeRepository;
import com.cst438.domain.Assignment;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.User;
import com.cst438.domain.UserRepository;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.RegistrarServiceProxy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentControllerTest {

    String testInstructorEmail = "instructor@csumb.edu";
    String testPassword = "admin";
    String testStudentEmail = "student@csumb.edu";

    @Autowired
    private WebTestClient client;
    
    @Autowired
    AssignmentRepository assignmentRepository;
    
    @Autowired
    EnrollmentRepository enrollmentRepository;
    
    @Autowired
    GradeRepository gradeRepository;

    @Autowired
    SectionRepository sectionRepository;
    
    @Autowired
    UserRepository userRepository;

    @MockitoBean
    RegistrarServiceProxy registrarService;

    @BeforeAll
    public static void addTestData(
        @Autowired AssignmentRepository assignmentRepository,
        @Autowired SectionRepository sectionRepository,
        @Autowired UserRepository userRepository,
        @Autowired EnrollmentRepository enrollmentRepository,
        @Autowired GradeRepository gradeRepository) {
        // Get Section
        Section section = sectionRepository.findById(1).get();

        // Get Sam
        User sam = userRepository.findByEmail("sam@csumb.edu");

        // Add test instructor
        User instructor = new User();
        instructor.setId(4);
        instructor.setEmail("instructor@csumb.edu");
        instructor.setPassword("$2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP9vpMu0ssKi8GW");
        instructor.setType("INSTRUCTOR");
        instructor.setName("Instructor");
        userRepository.save(instructor);

        // Add test student
        User student = new User();
        student.setId(5);
        student.setEmail("student@csumb.edu");
        student.setPassword("$2a$10$8cjz47bjbR4Mn8GMg9IZx.vyjhLXR/SKKMSZ9.mP9vpMu0ssKi8GW");
        student.setType("STUDENT");
        student.setName("Student");
        userRepository.save(student);

        // Add test assignment
        Assignment assignment = new Assignment();
        assignment.setTitle("Test Assignment");
        assignment.setDueDate(Date.valueOf("2026-09-30"));
        assignment.setSection(section);
        assignmentRepository.save(assignment);

        // Add test enrollments
        // Test enrollment for Sam
        Enrollment enrollmentSam = new Enrollment();
        enrollmentSam.setEnrollmentId(0);
        enrollmentSam.setSection(section);
        enrollmentSam.setStudent(sam);
        enrollmentRepository.save(enrollmentSam);
        // Test enrollment for Student
        Enrollment enrollmentStudent = new Enrollment();
        enrollmentStudent.setEnrollmentId(1);
        enrollmentStudent.setSection(section);
        enrollmentStudent.setStudent(student);
        enrollmentRepository.save(enrollmentStudent);

        // Add test grade for Sam
        Grade grade = new Grade();
        grade.setAssignment(assignment);
        grade.setEnrollment(enrollmentSam);
        grade.setScore(100);
        gradeRepository.save(grade);
    }

    @Test
    public void getSectionsForInstructorTest() throws Exception {
        // Login as instructor and get the security token
        // Instructor information
        String instructorEmail = "ted@csumb.edu";
        String instructorName = "ted";
        String password = "ted2025";
        // Get login response
        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(instructorEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        // Get security token
        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        // Check result from valid inputs
        // Get sections list for instructor
        EntityExchangeResult<List<SectionDTO>> sectionResponse = client.get()
            .uri("/sections?year=2026&semester=Fall")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(SectionDTO.class)
            .returnResult();
        List<SectionDTO> sections = sectionResponse.getResponseBody();
        // Check that the sections list has 1 element (the test section in the database)
        assertNotNull(sections);
        assertEquals(sections.size(), 1);
        // Get SectionDTO
        SectionDTO section = sections.get(0);

        // Get expected values from the database
        Section actualSection = sectionRepository
            .findByInstructorEmailAndYearAndSemester(instructorEmail, 2026, "Fall")
            .get(0);

        // Compare sectionDTO and expected values
        assertEquals(section.secNo(), actualSection.getSectionNo()); // secNo
        assertEquals(section.year(), actualSection.getTerm().getYear()); // year
        assertEquals(section.semester(), actualSection.getTerm().getSemester()); // semester
        assertEquals(section.courseId(), actualSection.getCourse().getCourseId()); // courseId
        assertEquals(section.title(), actualSection.getCourse().getTitle()); // title
        assertEquals(section.secId(), actualSection.getSectionId()); // secId
        assertEquals(section.building(), actualSection.getBuilding()); // building
        assertEquals(section.room(), actualSection.getRoom()); // room
        assertEquals(section.times(), actualSection.getTimes()); // times
        assertEquals(section.instructorName(), instructorName); // instructorName
        assertEquals(section.instructorEmail(), instructorEmail); // instructorEmail

        // Get bad queries
        // Get query for invalid year
        sectionResponse = client.get()
            .uri("/sections?year=1999&semester=Fall")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(SectionDTO.class)
            .returnResult();
        sections = sectionResponse.getResponseBody();
        assertNotNull(sections);
        assertEquals(sections.size(), 0);

        // Get query for invalid semester
        sectionResponse = client.get()
            .uri("/sections?year=2026&semester=Spring")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(SectionDTO.class)
            .returnResult();
        sections = sectionResponse.getResponseBody();
        assertNotNull(sections);
        assertEquals(sections.size(), 0);

        // Get query for instructor with no sections
        // Get login response
        login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(testInstructorEmail, testPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        // Get security token
        String test_jwt = login_dto.getResponseBody().jwt();
        assertNotNull(test_jwt);
        // Get sections list for instructor
        sectionResponse = client.get()
            .uri("/sections?year=2026&semester=Fall")
            .headers(headers -> headers.setBearerAuth(test_jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(SectionDTO.class)
            .returnResult();
        sections = sectionResponse.getResponseBody();
        // Check that the sections list has 0 elements
        assertNotNull(sections);
        assertEquals(sections.size(), 0);
    }

    @Test
    public void getAssignmentsForInstructor() {
        // Login as instructor and get the security token
        // Instructor information
        String instructorEmail = "ted@csumb.edu";
        String instructorName = "ted";
        String password = "ted2025";
        // Get login response
        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(instructorEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        // Get security token
        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        // Get assignments list for instructor
        EntityExchangeResult<List<AssignmentDTO>> assignmentResponse = client.get()
            .uri("/sections/1/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(AssignmentDTO.class)
            .returnResult();
        List<AssignmentDTO> assignments = assignmentResponse.getResponseBody();
        // Check that the assignments list has 1 element (the test assignment in the database)
        assertNotNull(assignments);
        assertEquals(assignments.size(), 1);

        // Check that the AssignmentDTO has the expected values
        // Get AssignmentDTO
        AssignmentDTO assignmentDTO = assignments.get(0);
        Assignment assignment = assignmentRepository.findById(assignmentDTO.id()).get();
        // Compare assignmentDTO and expected values
        assertEquals(assignmentDTO.id(), assignment.getAssignmentId()); // assignmentId
        assertEquals(assignmentDTO.title(), assignment.getTitle()); // title
        assertEquals(assignmentDTO.dueDate(), assignment.getDueDate().toString()); // dueDate
        assertEquals(assignmentDTO.secId(), assignment.getSection().getSectionId()); // secId
        assertEquals(assignmentDTO.secNo(), assignment.getSection().getSectionNo()); // secNo
        assertEquals(assignmentDTO.courseId(), assignment.getSection().getCourse().getCourseId()); // courseId

        // Check that we get an error message for a invalid section
        client.get()
            .uri("/sections/0/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid section')]").exists()
            .returnResult();
        
        // Check that we get an error message for invalid instructor
        // Login as test instructor
        login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(testInstructorEmail, testPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        // Get security token
        String test_jwt = login_dto.getResponseBody().jwt();
        assertNotNull(test_jwt);
        // Try to get section, make sure that we get error message
        client.get()
            .uri("/sections/1/assignments")
            .headers(headers -> headers.setBearerAuth(test_jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid instructor email')]").exists()
            .returnResult();
    }
}
