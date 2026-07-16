package com.cst438.controller;

import java.security.Principal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.*;
import com.cst438.dto.*;

import jakarta.validation.Valid;

@RestController
public class AssignmentController {

    private final SectionRepository sectionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;

    public AssignmentController(
            SectionRepository sectionRepository,
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
    }

    // get Sections for an instructor
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal)  {
        // return the Sections that have instructorEmail for the 
		// logged in instructor user for the given term.

        // Get instructor name and email
        String instructorEmail = principal.getName();
        String instructorName = userRepository.findByEmail(instructorEmail).getName();

        // Return list of SectionDTOs
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

        // Instructor information
        String instructorEmail = principal.getName();
        
        // Get section from the database
        Section section = sectionRepository.findById(secNo).orElse(null);

        // Check query validity
        // Check that section exists
        if (section == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid section");
        }
        // Check that the user is the instructor for the section
        if (!instructorEmail.equals(section.getInstructorEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instructor email");
        }

        // Return list of assignments for the section
        return assignmentRepository.findBySectionNo(secNo)
        .stream()
        .map(a ->
            new AssignmentDTO(
                a.getAssignmentId(),
                a.getTitle(),
                a.getDueDate().toString(),
                section.getCourse().getCourseId(),
                section.getSectionId(),
                section.getSectionNo()
            )
        ).toList();
    }


    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {
        
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
        LocalDate dueDate = checkDueDate(dto.dueDate(), section.getTerm());
        if (dueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid due date");
        }

        // Create and save Assignment
        Assignment a = new Assignment();
        a.setTitle(dto.title());
        a.setDueDate(Date.valueOf(dueDate));
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
        
        // Instructor information
        String instructorEmail = principal.getName();

        // Get assignment from the database
        Assignment a = assignmentRepository.findById(dto.id()).orElse(null);
        // Check that assignment exists
        if (a == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid assignment");
        }

        // Get section
        Section section = a.getSection();

        // Check that the user is the instructor for the section
        if (!instructorEmail.equals(section.getInstructorEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instructor email");
        }

        // Check that due date is valid if a new one was provided
        if(dto.dueDate() != null && !dto.dueDate().isEmpty()) {
            LocalDate dueDate = checkDueDate(dto.dueDate(), section.getTerm());
            if (dueDate == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid due date");
            }
            // Set assignment due date
            a.setDueDate(Date.valueOf(dueDate));
        }

        // Set assignment title
        a.setTitle(dto.title());

        // Save assignment
        assignmentRepository.save(a);

        // Return AssignmentDTO
        return new AssignmentDTO(
            a.getAssignmentId(),
            a.getTitle(),
            a.getDueDate().toString(),
            section.getCourse().getCourseId(),
            section.getSectionId(),
            section.getSectionNo()
        );
    }


    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // verify that user is the instructor of the section
        // delete the Assignment entity
        
        // Instructor information
        String instructorEmail = principal.getName();

        // Get assignment from the database
        Assignment a = assignmentRepository.findById(assignmentId).orElse(null);
        // Check that assignment exists
        if (a == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid assignment");
        }

        // Get section
        Section section = a.getSection();

        // Check that the user is the instructor for the section
        if (!instructorEmail.equals(section.getInstructorEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instructor email");
        }

        // Delete assignment
        assignmentRepository.deleteById(assignmentId);
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

        // Student information
        String email = principal.getName();

        return assignmentRepository.findByStudentEmailAndYearAndSemester(email, year, semester)
            .stream()
            .map(a -> {
                // Get score
                Integer score = null;
                Grade grade = gradeRepository.findByStudentEmailAndAssignmentId(email, a.getAssignmentId());
                if(grade != null) {
                    score = grade.getScore();
                }

                // Create AssignmentStudentDTO
                return new AssignmentStudentDTO(
                    a.getAssignmentId(),
                    a.getTitle(),
                    a.getDueDate(),
                    a.getSection().getCourse().getCourseId(),
                    a.getSection().getSectionId(),
                    score
                );
            }).toList();
    }

    /**
     * Check if the due date is valid for the given term.
     * Converts due date and term start and end dates to LocalDate and compares them.
     * @param dueDateStr A String from the AssignmentDTO dueDate field.
     * @param term The Term entity for the section of the assignment.
     * @return A LocalDate instance if the due date is valid, or null if it is invalid.
     */
    private LocalDate checkDueDate(String dueDateStr, Term term) {
        LocalDate dueDate;
        try {
            // Get dates
            dueDate = LocalDate.parse(dueDateStr);
            LocalDate startDate = term.getStartDate().toLocalDate();
            LocalDate endCompare = term.getEndDate().toLocalDate();
            // Compare dates
            if (dueDate.isBefore(startDate) || dueDate.isAfter(endCompare)) {
                return null;
            }
        } catch (Exception e) {
            // Return null if due date is invalid (e.g. 2024-02-30)
            return null;
        }

        // Return due date if it is valid
        return dueDate;
    }
}
