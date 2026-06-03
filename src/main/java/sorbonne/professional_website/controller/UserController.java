package sorbonne.professional_website.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.request.ProfileRequestDTO;
import sorbonne.professional_website.dto.request.ProjectRequestDTO;
import sorbonne.professional_website.dto.request.TimelineRequestDTO;
import sorbonne.professional_website.dto.request.UserRequestDTO;
import sorbonne.professional_website.dto.response.UserResponseDTO;
import sorbonne.professional_website.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService srvUser;

    public UserController(UserService srvUser) {
        this.srvUser = srvUser;
    }

    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody UserRequestDTO userRequestDTO) {
        srvUser.createUser(userRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(srvUser.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(srvUser.getUserById(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateUser(
            @PathVariable Long userId,
            @RequestBody UserRequestDTO userRequestDTO
    ) {
        srvUser.updateUser(userId, userRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        srvUser.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/profile")
    public ResponseEntity<Void> createOrReplaceProfile(
            @PathVariable Long userId,
            @RequestBody ProfileRequestDTO profileRequestDTO
    ) {
        srvUser.createOrReplaceProfile(userId, profileRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<Void> updateProfile(
            @PathVariable Long userId,
            @RequestBody ProfileRequestDTO profileRequestDTO
    ) {
        srvUser.createOrReplaceProfile(userId, profileRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/timeline")
    public ResponseEntity<Void> createOrReplaceTimeline(
            @PathVariable Long userId,
            @RequestBody TimelineRequestDTO timelineRequestDTO
    ) {
        srvUser.createOrReplaceTimeline(userId, timelineRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{userId}/timeline")
    public ResponseEntity<Void> updateTimeline(
            @PathVariable Long userId,
            @RequestBody TimelineRequestDTO timelineRequestDTO
    ) {
        srvUser.createOrReplaceTimeline(userId, timelineRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/projects")
    public ResponseEntity<Void> addProjectToUser(
            @PathVariable Long userId,
            @RequestBody ProjectRequestDTO projectRequestDTO
    ) {
        srvUser.addProjectToUser(userId, projectRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
