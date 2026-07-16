package com.cst438.controller;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.cst438.domain.*;
import com.cst438.dto.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentControllerUnitTest {

    // Test data information
    static String testInstructorEmail = "instructor@csumb.edu";
    static String testPassword = "test";
    static String testStudentEmail = "student@csumb.edu";

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

        // Encode password for test users
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedTestPassword = passwordEncoder.encode(testPassword);

        // Add test instructor
        User instructor = new User();
        instructor.setId(4);
        instructor.setEmail(testInstructorEmail);
        instructor.setPassword(encodedTestPassword);
        instructor.setType("INSTRUCTOR");
        instructor.setName("Instructor");
        userRepository.save(instructor);

        // Add test student
        User student = new User();
        student.setId(5);
        student.setEmail(testStudentEmail);
        student.setPassword(encodedTestPassword);
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
    public void assignmentCreateEditDelete() {
        // Login as instructor and get the security token
        // Instructor information
        String instructorEmail = "ted@csumb.edu";
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

        // Create valid assignment
        // Create AssignmentDTO
        AssignmentDTO adto = new AssignmentDTO(
            0, 
            "Assignment", 
            "2026-09-30", 
            "cst489", 
            1, 
            1
        );
        // Try to create assignment with POST request
        EntityExchangeResult<AssignmentDTO> assignmentResponse = client.post()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(AssignmentDTO.class)
            .returnResult();
        AssignmentDTO assignment = assignmentResponse.getResponseBody();
        // Check that dto was returned
        assertNotNull(assignment);
        // Check that the assignment was created in the database
        Assignment actualAssignment = assignmentRepository.findById(assignment.id()).get();
        assertNotNull(actualAssignment);

        // Update assignment
        String newTitle = actualAssignment.getTitle() + " Putted";
        String newDueDate = "2026-08-30";
        // Create AssignmentDTO
        adto = new AssignmentDTO(
            assignment.id(),
            newTitle,
            newDueDate,
            "",
            0,
            0
        );
        // Try put request
        assignmentResponse = client.put()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isOk()
            .expectBody(AssignmentDTO.class)
            .returnResult();
        assignment = assignmentResponse.getResponseBody();
        // Check that dto was returned
        assertNotNull(assignment);
        // Check that the assignment was created in the database
        actualAssignment = assignmentRepository.findById(assignment.id()).get();
        assertNotNull(actualAssignment);
        // Check that actual assignment title and due date have been updated
        assertEquals(actualAssignment.getTitle(), newTitle);
        assertEquals(actualAssignment.getDueDate().toString(), newDueDate);

        // Delete assignment
        // Try delete request
        client.delete()
            .uri("/assignments/" + assignment.id())
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .returnResult();
        // Check that the assignment was deleted from the database
        actualAssignment = assignmentRepository.findById(assignment.id()).orElse(null);
        assertNull(actualAssignment);
    }

    @Test
    public void assignmentCreateInvalid() {
        // Login as instructor and get the security token
        // Instructor information
        String instructorEmail = "ted@csumb.edu";
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

        // Login as  test instructor and get the security token
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

        // Try to create assignment with invalid instructor email
        // Create AssignmentDTO
        AssignmentDTO adto = new AssignmentDTO(
            0, 
            "Assignment", 
            "1985-09-13", 
            "cst489", 
            1, 
            1
        );
        // Try to create assignment with POST request
        client.post()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(test_jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid instructor email')]").exists()
            .returnResult();

        // Try to create assignment with invalid section
        // Create AssignmentDTO
        adto = new AssignmentDTO(
            0, 
            "Assignment", 
            "1985-09-13", 
            "cst489", 
            1, 
            0
        );
        // Try to create assignment with POST request
        client.post()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid section')]").exists()
            .returnResult();

        // Try to create assignment with early date
        // Create AssignmentDTO
        adto = new AssignmentDTO(
            0, 
            "Assignment", 
            "1985-09-13", 
            "cst489", 
            1, 
            1
        );
        // Try to create assignment with POST request
        client.post()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid due date')]").exists()
            .returnResult();

        // Try to create assignment with late date
        // Create AssignmentDTO
        adto = new AssignmentDTO(
            0, 
            "Assignment", 
            "2077-11-30", 
            "cst489", 
            1, 
            1
        );
        // Try to create assignment with POST request
        client.post()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid due date')]").exists()
            .returnResult();
    }

    @Test
    public void assignmentDeleteInvalid() {
        // Login as  test instructor and get the security token
        // Get login response
        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(testInstructorEmail, testPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        // Get security token
        String test_jwt = login_dto.getResponseBody().jwt();
        assertNotNull(test_jwt);

        // Try to delete assignment with invalid assignment id
        client.delete()
            .uri("/assignments/0")
            .headers(headers -> headers.setBearerAuth(test_jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid assignment')]").exists()
            .returnResult();

        // Try to delete assignment with invalid instructor name
        client.delete()
            .uri("/assignments/6000")
            .headers(headers -> headers.setBearerAuth(test_jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid instructor email')]").exists()
            .returnResult();
    }

    @Test
    public void assignmentEditInvalid() {
        // Login as instructor and get the security token
        // Instructor information
        String instructorEmail = "ted@csumb.edu";
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

        // Login as  test instructor and get the security token
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

        // Get original assignment
        Assignment originalAssignment = assignmentRepository.findById(6000).get();

        // Try to update assignment with invalid assignment id
        // Create AssignmentDTO
        AssignmentDTO adto = new AssignmentDTO(
            0,
            "test",
            "",
            "",
            0,
            0
        );
        // Try put request
        client.put()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid assignment')]").exists()
            .returnResult();

        // Try to update assignment with invalid instructor name
        // Create AssignmentDTO
        adto = new AssignmentDTO(
            originalAssignment.getAssignmentId(),
            "test",
            "",
            "",
            0,
            0
        );
        // Try put request
        client.put()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(test_jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid instructor email')]").exists()
            .returnResult();

        // Try to update assignment with early due date
        // Create AssignmentDTO
        adto = new AssignmentDTO(
            originalAssignment.getAssignmentId(),
            "test",
            "1985-09-13",
            "",
            0,
            0
        );
        // Try put request
        client.put()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid due date')]").exists()
            .returnResult();

        // Try to update assignment with late due date
        // Create AssignmentDTO
        adto = new AssignmentDTO(
            originalAssignment.getAssignmentId(),
            "test",
            "2077-11-03",
            "",
            0,
            0
        );
        // Try put request
        client.put()
            .uri("/assignments")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(adto)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("$.errors[?(@=='invalid due date')]").exists()
            .returnResult();
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
        assertTrue(sections.size() > 0);
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
        assertTrue(assignments.size() > 0);

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

    @Test
    public void getAssignmentsforStudent() {
        // Login as Student Sam
        // Sam information
        String samEmail = "sam@csumb.edu";
        String samPassword = "sam2025";
        // Get login response
        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(samEmail, samPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        // Get security token
        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        // Get Assignments List for Sam
        EntityExchangeResult<List<AssignmentStudentDTO>> assignmentResponse = client.get()
            .uri("/assignments?year=2026&semester=Fall")
            .headers(headers -> headers.setBearerAuth(jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(AssignmentStudentDTO.class)
            .returnResult();
        List<AssignmentStudentDTO> assignments = assignmentResponse.getResponseBody();
        // Check that the assignments list has 1 assignment
        assertNotNull(assignments);
        assertTrue(assignments.size() > 0);
        // Check that the assignment has a grade of 100
        assertEquals(assignments.get(0).score(),  100);

        // Login as Test Student
        // Get login response
        login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(testStudentEmail, testPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        // Get security token
        String test_jwt = login_dto.getResponseBody().jwt();
        assertNotNull(test_jwt);
        
        // Get Assignments List for Test Student
        assignmentResponse = client.get()
            .uri("/assignments?year=2026&semester=Fall")
            .headers(headers -> headers.setBearerAuth(test_jwt))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(AssignmentStudentDTO.class)
            .returnResult();
        assignments = assignmentResponse.getResponseBody();
        // Check that the assignments list has 1 assignment
        assertNotNull(assignments);
        assertTrue(assignments.size() > 0);
        // Check that the assignment has a grade of 100
        assertNull(assignments.get(0).score());
    }
}
