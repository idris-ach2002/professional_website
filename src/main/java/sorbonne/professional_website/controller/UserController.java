package sorbonne.professional_website.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sorbonne.professional_website.dto.UserDTO;
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
    public ResponseEntity<Void> createUser(@RequestBody UserDTO userCreateDTO) {
        srvUser.createUser(userCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(srvUser.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(srvUser.getUserById(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateUser(
            @PathVariable Long userId,
            @RequestBody UserDTO userUpdateDTO
    ) {
        srvUser.updateUser(userId, userUpdateDTO);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        srvUser.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}