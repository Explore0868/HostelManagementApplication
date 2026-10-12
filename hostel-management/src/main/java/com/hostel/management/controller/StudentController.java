
package com.hostel.management.controller;

import com.hostel.management.dto.StudentRequest;
import com.hostel.management.dto.StudentUpdateRequest;

import com.hostel.management.dto.StudentResponse;
import com.hostel.management.service.StudentService;

import jakarta.validation.Valid;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentResponse> registerStudent(
            @Valid @RequestBody StudentRequest request) {

        StudentResponse response =
                studentService.registerStudent(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                studentService.getStudentById(id));
    }
    

	@PutMapping("/{id}")
	public ResponseEntity<StudentResponse> updateStudent(
	        @PathVariable Long id,
	        @Valid @RequestBody StudentUpdateRequest request) {
	
	    return ResponseEntity.ok(
	            studentService.updateStudent(id, request));
	}
	
	@PatchMapping("/{id}/deactivate")
	public ResponseEntity<StudentResponse> deactivateStudent(
	        @PathVariable Long id) {
	
	    return ResponseEntity.ok(
	            studentService.deactivateStudent(id));
	}
	
	@PatchMapping("/{id}/reactivate")
	public ResponseEntity<StudentResponse> reactivateStudent(
	        @PathVariable Long id) {
	
	    return ResponseEntity.ok(
	            studentService.reactivateStudent(id));
	}
	

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
	    studentService.deleteStudent(id);
	    return ResponseEntity.noContent().build();
	}


}
