package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
import com.cst438.dto.LoginDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GradeControllerUnitTest {

    static int testAssignmentId;
    static int existingGradeId;
    static int createdGradeId;

    static final int firstStudentId = 997;
    static final int secondStudentId = 998;
    static final int otherInstructorId = 999;

    static final int firstEnrollmentId = 997;
    static final int secondEnrollmentId = 998;

    static final String instructorEmail = "ted@csumb.edu";
    static final String instructorPassword = "ted2025";

    static final String otherInstructorEmail = "grade-instructor@csumb.edu";
    static final String otherInstructorPassword = "test";

    static final String firstStudentEmail = "grade-student-one@csumb.edu";
    static final String secondStudentEmail = "grade-student-two@csumb.edu";

    @Autowired
    private WebTestClient client;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @BeforeAll
    public static void addTestData(
            @Autowired AssignmentRepository assignmentRepository,
            @Autowired EnrollmentRepository enrollmentRepository,
            @Autowired GradeRepository gradeRepository,
            @Autowired SectionRepository sectionRepository,
            @Autowired UserRepository userRepository
    ) {
        Section section = sectionRepository.findById(1).orElseThrow();

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // Add an instructor who does not own the test section
        User otherInstructor = new User();
        otherInstructor.setId(otherInstructorId);
        otherInstructor.setName("Grade Instructor");
        otherInstructor.setEmail(otherInstructorEmail);
        otherInstructor.setPassword(passwordEncoder.encode(otherInstructorPassword));
        otherInstructor.setType("INSTRUCTOR");
        userRepository.save(otherInstructor);

        // Add first test student
        User firstStudent = new User();
        firstStudent.setId(firstStudentId);
        firstStudent.setName("Grade Student One");
        firstStudent.setEmail(firstStudentEmail);
        firstStudent.setPassword(passwordEncoder.encode("test"));
        firstStudent.setType("STUDENT");
        userRepository.save(firstStudent);

        // Add second test student
        User secondStudent = new User();
        secondStudent.setId(secondStudentId);
        secondStudent.setName("Grade Student Two");
        secondStudent.setEmail(secondStudentEmail);
        secondStudent.setPassword(passwordEncoder.encode("test"));
        secondStudent.setType("STUDENT");
        userRepository.save(secondStudent);

        // Enroll both students in section 1
        Enrollment firstEnrollment = new Enrollment();
        firstEnrollment.setEnrollmentId(firstEnrollmentId);
        firstEnrollment.setSection(section);
        firstEnrollment.setStudent(firstStudent);
        enrollmentRepository.save(firstEnrollment);

        Enrollment secondEnrollment = new Enrollment();
        secondEnrollment.setEnrollmentId(secondEnrollmentId);
        secondEnrollment.setSection(section);
        secondEnrollment.setStudent(secondStudent);
        enrollmentRepository.save(secondEnrollment);

        // Add an assignment for the section
        Assignment assignment = new Assignment();
        assignment.setTitle("Grade Controller Test Assignment");
        assignment.setDueDate(Date.valueOf("2026-09-30"));
        assignment.setSection(section);
        assignmentRepository.save(assignment);
        testAssignmentId = assignment.getAssignmentId();

        // Give the first student an existing score
        Grade grade = new Grade();
        grade.setAssignment(assignment);
        grade.setEnrollment(firstEnrollment);
        grade.setScore(95);
        gradeRepository.save(grade);
        existingGradeId = grade.getGradeId();
    }

    @AfterAll
    public static void removeTestData(
            @Autowired AssignmentRepository assignmentRepository,
            @Autowired EnrollmentRepository enrollmentRepository,
            @Autowired GradeRepository gradeRepository,
            @Autowired UserRepository userRepository
    ) {
        if (createdGradeId != 0) {
            gradeRepository.deleteById(createdGradeId);
        }

        gradeRepository.deleteById(existingGradeId);
        assignmentRepository.deleteById(testAssignmentId);

        enrollmentRepository.deleteById(firstEnrollmentId);
        enrollmentRepository.deleteById(secondEnrollmentId);

        userRepository.deleteById(firstStudentId);
        userRepository.deleteById(secondStudentId);
        userRepository.deleteById(otherInstructorId);
    }

    @Test
    public void getAssignmentGradesTest() {
        String jwt = login(instructorEmail, instructorPassword);

        EntityExchangeResult<List<GradeDTO>> response = client.get()
                .uri("/assignments/" + testAssignmentId + "/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GradeDTO.class)
                .returnResult();

        List<GradeDTO> grades = response.getResponseBody();
        assertNotNull(grades);
        assertEquals(2, grades.size());

        GradeDTO firstStudentGrade = grades.stream()
                .filter(dto -> dto.studentEmail().equals(firstStudentEmail))
                .findFirst()
                .orElse(null);

        assertNotNull(firstStudentGrade);
        assertEquals(existingGradeId, firstStudentGrade.gradeId());
        assertEquals("Grade Student One", firstStudentGrade.studentName());
        assertEquals("Grade Controller Test Assignment", firstStudentGrade.assignmentTitle());
        assertEquals("cst489", firstStudentGrade.courseId());
        assertEquals(1, firstStudentGrade.sectionId());
        assertEquals(95, firstStudentGrade.score());

        GradeDTO secondStudentGrade = grades.stream()
                .filter(dto -> dto.studentEmail().equals(secondStudentEmail))
                .findFirst()
                .orElse(null);

        assertNotNull(secondStudentGrade);
        assertTrue(secondStudentGrade.gradeId() > 0);
        assertEquals("Grade Student Two", secondStudentGrade.studentName());
        assertNull(secondStudentGrade.score());

        Grade createdGrade = gradeRepository.findByStudentEmailAndAssignmentId(
                secondStudentEmail,
                testAssignmentId
        );

        assertNotNull(createdGrade);
        assertNull(createdGrade.getScore());
        createdGradeId = createdGrade.getGradeId();
    }

    @Test
    public void getAssignmentGradesInvalidAssignmentTest() {
        String jwt = login(instructorEmail, instructorPassword);

        client.get()
                .uri("/assignments/0/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@=='invalid assignment')]").exists();
    }

    @Test
    public void getAssignmentGradesNotOwnerTest() {
        String jwt = login(otherInstructorEmail, otherInstructorPassword);

        client.get()
                .uri("/assignments/" + testAssignmentId + "/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@=='invalid instructor email')]").exists();
    }

    @Test
    public void updateGradesTest() {
        String jwt = login(instructorEmail, instructorPassword);

        GradeDTO updatedGrade = new GradeDTO(
                existingGradeId,
                "Grade Student One",
                firstStudentEmail,
                "Grade Controller Test Assignment",
                "cst489",
                1,
                88
        );

        try {
            client.put()
                    .uri("/grades")
                    .headers(headers -> headers.setBearerAuth(jwt))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(List.of(updatedGrade))
                    .exchange()
                    .expectStatus().isOk();

            Grade grade = gradeRepository.findById(existingGradeId).orElseThrow();
            assertEquals(88, grade.getScore());
        } finally {
            Grade grade = gradeRepository.findById(existingGradeId).orElseThrow();
            grade.setScore(95);
            gradeRepository.save(grade);
        }
    }

    @Test
    public void updateGradesInvalidGradeTest() {
        String jwt = login(instructorEmail, instructorPassword);

        GradeDTO invalidGrade = new GradeDTO(
                0,
                "Grade Student One",
                firstStudentEmail,
                "Grade Controller Test Assignment",
                "cst489",
                1,
                88
        );

        client.put()
                .uri("/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(invalidGrade))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@=='invalid grade')]").exists();
    }

    @Test
    public void updateGradesNotOwnerTest() {
        String jwt = login(otherInstructorEmail, otherInstructorPassword);

        GradeDTO updatedGrade = new GradeDTO(
                existingGradeId,
                "Grade Student One",
                firstStudentEmail,
                "Grade Controller Test Assignment",
                "cst489",
                1,
                88
        );

        client.put()
                .uri("/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(updatedGrade))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors[?(@=='invalid instructor email')]").exists();

        Grade grade = gradeRepository.findById(existingGradeId).orElseThrow();
        assertEquals(95, grade.getScore());
    }

    private String login(String email, String password) {
        EntityExchangeResult<LoginDTO> response = client.get()
                .uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        LoginDTO loginDTO = response.getResponseBody();
        assertNotNull(loginDTO);
        assertNotNull(loginDTO.jwt());

        return loginDTO.jwt();
    }
}