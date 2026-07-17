package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.*;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EnrollmentControllerUnitTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockitoBean
    RegistrarServiceProxy registrar;

    @Test
    public void getEnrollmentsSectionTest() throws Exception {
        // login as ted
        String instructorEmail = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> login = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(instructorEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login.getResponseBody().jwt();
        assertNotNull(jwt);

        // seed one enrollment for section 1
        int testEnrollmentId = 9999;

        Enrollment enrollment = new Enrollment();

        enrollment.setEnrollmentId(testEnrollmentId);
        enrollment.setGrade(null);

        User student = userRepository.findById(2).orElseThrow();

        Section section = sectionRepository.findById(1).orElseThrow();

        enrollment.setStudent(student);
        enrollment.setSection(section);

        enrollmentRepository.save(enrollment);

        try {
            // GET /sections/1/enrollments as the instructor
            EntityExchangeResult<List<EnrollmentDTO>> result = client.get()
                    .uri("/sections/1/enrollments")
                    .headers(headers -> headers.setBearerAuth(jwt))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBodyList(EnrollmentDTO.class).returnResult();

            List<EnrollmentDTO> enrollments = result.getResponseBody();
            assertNotNull(enrollments, "response body was null");

            assertFalse(enrollments.isEmpty(), "expected at least one enrollment in the roster");

            // find the seeded enrollment and verify DTO fields map correctly
            EnrollmentDTO dto = enrollments.stream().filter(e -> e.enrollmentId() == testEnrollmentId).findFirst().orElse(null);
            assertNotNull(dto, "seeded enrollment " + testEnrollmentId + " missing from response");

            // verify all 15 fields on the returned DTO
            assertEquals(testEnrollmentId, dto.enrollmentId(), "enrollmentId mismatch");
            assertNull(dto.grade(), "grade should be null before final grades are entered");
            assertEquals(2, dto.studentId(), "studentId mismatch");
            assertEquals("sam", dto.name(), "student name mismatch");
            assertEquals("sam@csumb.edu", dto.email(), "student email mismatch");
            assertEquals("cst489", dto.courseId(), "courseId mismatch");
            assertEquals("Software Engineering", dto.title(), "course title mismatch");
            assertEquals(1, dto.sectionId(), "sectionId mismatch");
            assertEquals(1, dto.sectionNo(), "sectionNo mismatch");
            assertEquals("90", dto.building(), "building mismatch");
            assertEquals("B104", dto.room(), "room mismatch");
            assertEquals("W F 10-11", dto.times(), "times mismatch");
            assertEquals(4, dto.credits(), "credits mismatch");
            assertEquals(2026, dto.year(), "year mismatch");
            assertEquals("Fall", dto.semester(), "semester mismatch");
        } finally {
            enrollmentRepository.deleteById(testEnrollmentId);
        }
    }

    @Test
    public void getEnrollmentsNotOwnerTest() throws Exception {
        // create a second instructor who does not own section 1
        int secondInstructorId = 9998;

        String secondEmail = "second-instructor@csumb.edu";
        String secondPassword = "secondpassword";

        User secondInstructor = new User();

        secondInstructor.setId(secondInstructorId);
        secondInstructor.setName("secondinstructor");
        secondInstructor.setEmail(secondEmail);
        secondInstructor.setPassword(passwordEncoder.encode(secondPassword));
        secondInstructor.setType("INSTRUCTOR");

        userRepository.save(secondInstructor);

        try {
            // login as the other instructor and obtain a JWT
            EntityExchangeResult<LoginDTO> login = client.get().uri("/login")
                    .headers(headers -> headers.setBasicAuth(secondEmail, secondPassword))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(LoginDTO.class).returnResult();

            String jwt = login.getResponseBody().jwt();
            assertNotNull(jwt);

            // attempt to view section 1's roster — should be rejected
            client.get().uri("/sections/1/enrollments")
                    .headers(headers -> headers.setBearerAuth(jwt))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isBadRequest();
        } finally {
            userRepository.deleteById(secondInstructorId);
        }
    }
}
