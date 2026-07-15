package com.cst438.controller;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.dto.LoginDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.RegistrarServiceProxy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentControllerTest {

    @Autowired
    private WebTestClient client;
    @Autowired
    SectionRepository sectionRepository;

    @MockitoBean
    RegistrarServiceProxy registrarService;

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
    }
}
