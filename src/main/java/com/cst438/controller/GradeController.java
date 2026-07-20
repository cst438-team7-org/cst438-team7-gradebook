package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
public class GradeController {
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;

    public GradeController (
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
    }

    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeDTO> getAssignmentGrades(@PathVariable("assignmentId") int assignmentId, Principal principal) {
		// Check that the Section of the assignment belongs to the
		// logged in instructor
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);

        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid assignment");
        }

        Section section = assignment.getSection();

        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instructor email");
        }

        // return a list of GradeDTOs containing student scores for an assignment
        // if a Grade entity does not exist, then create the Grade entity
		// with a null score and return the gradeId.
        return section.getEnrollments()
                .stream()
                .map(enrollment -> {
                    Grade grade = gradeRepository.findByStudentEmailAndAssignmentId(
                            enrollment.getStudent().getEmail(),
                            assignmentId
                    );

                    if (grade == null) {
                        grade = new Grade();
                        grade.setAssignment(assignment);
                        grade.setEnrollment(enrollment);
                        grade.setScore(null);
                        gradeRepository.save(grade);
                    }

                    return new GradeDTO(
                            grade.getGradeId(),
                            enrollment.getStudent().getName(),
                            enrollment.getStudent().getEmail(),
                            assignment.getTitle(),
                            section.getCourse().getCourseId(),
                            section.getSectionId(),
                            grade.getScore()
                    );
                })
                .toList();
    }


    @PutMapping("/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateGrades(@Valid @RequestBody List<GradeDTO> dtoList, Principal principal) {
		// for each GradeDTO
        for (GradeDTO dto : dtoList) {
            Grade grade = gradeRepository.findById(dto.gradeId()).orElse(null);

            if (grade == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid grade");
            }

		    // check that the logged in instructor is the owner of the section
            Section section = grade.getAssignment().getSection();

            if (!section.getInstructorEmail().equals(principal.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid instructor email");
            }

            // update the assignment score
            grade.setScore(dto.score());
            gradeRepository.save(grade);
        }
    }
}