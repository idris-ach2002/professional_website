package sorbonne.professional_website.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
}
