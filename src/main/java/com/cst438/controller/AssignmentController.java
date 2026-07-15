package com.cst438.controller;

import java.security.Principal;
import java.sql.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.GradeRepository;
import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.Term;
import com.cst438.domain.TermRepository;
import com.cst438.domain.UserRepository;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.RegistrarServiceProxy;

import jakarta.validation.Valid;

@RestController
public class AssignmentController {

    private final SectionRepository sectionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final RegistrarServiceProxy registrar;

    public AssignmentController(
            SectionRepository sectionRepository,
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository,
            RegistrarServiceProxy registrar
    ) {
        this.sectionRepository = sectionRepository;
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
        this.registrar = registrar;
    }

    // get Sections for an instructor
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year ,
            @RequestParam("semester") String semester,
            Principal principal)  {
        // return the Sections that have instructorEmail for the 
		// logged in instructor user for the given term.

        // Get instructor name and email
        String instructorEmail = principal.getName();
        String instructorName = userRepository.findByEmail(instructorEmail).getName();

        return sectionRepository
        .findByInstructorEmailAndYearAndSemester(instructorEmail, year, semester)
        .stream()
        .map(s ->
            new SectionDTO(
                s.getSectionNo(), // secNo
                s.getTerm().getYear(), // year
                s.getTerm().getSemester(), // semester
                s.getCourse().getCourseId(), // courseId
                s.getCourse().getTitle(), // title
                s.getSectionId(), // secId
                s.getBuilding(), // building
                s.getRoom(), // room
                s.getTimes(), // times
                instructorName, // instructorName,
                instructorEmail // instructorEmail
            )
        ).toList();
    }

    // instructor lists assignments for a section.
    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo,
            Principal principal) {

        // verify that user is the instructor for the section
        //  return list of assignments for the Section
        return null;
    }


    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) throws Exception {
        
        //  user must be the instructor for the Section
		//  check that assignment dueDate is between start date and 
		//  end date of the term
		//  create and save an Assignment entity
        //  return AssignmentDTO with database generated primary key

        
        // Instructor information
        String instructorEmail = principal.getName();
        
        // Get section from the database
        Section section = sectionRepository.findById(dto.secNo()).orElse(null);

        // Check validity of Assignment parameters
        // Check that section exists
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid section");
        }
        // Check that the user is the instructor for the section
        if (!instructorEmail.equals(section.getInstructorEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instructor email");
        }
        // Check that due date is valid
        // Get term
        Term term = section.getTerm();
        Date dueDate = Date.valueOf(dto.dueDate());
        int startCompare = term.getStartDate().compareTo(dueDate);
        int endCompare = term.getEndDate().compareTo(dueDate);
        if (startCompare > 0 || endCompare < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid due date");
        }

        // Create and save Assignment
        Assignment a = new Assignment();
        a.setTitle(dto.title());
        a.setDueDate(dueDate);
        a.setSection(section);
        assignmentRepository.save(a);

        // return AssignmentDTO
        return new AssignmentDTO(
            a.getAssignmentId(),
            a.getTitle(),
            a.getDueDate().toString(),
            section.getCourse().getCourseId(),
            section.getSectionId(),
            section.getSectionNo()
        );
    }


    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(@Valid @RequestBody AssignmentDTO dto, Principal principal) {
        //  update Assignment Entity.  only title and dueDate fields can be changed.
        //  user must be instructor of the Section
        
        return null;
    }


    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // verify that user is the instructor of the section
        // delete the Assignment entity
        
    }

    // student lists their assignments/grades  ordered by due date
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        //  return AssignmentStudentDTOs with scores of a 
		//  Grade entity exists.
		//  hint: use the GradeRepository findByStudentEmailAndAssignmentId
        //  If assignment has not been graded, return a null score.
        return null;
    }
}
