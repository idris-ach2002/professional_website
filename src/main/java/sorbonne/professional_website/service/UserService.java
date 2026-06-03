package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.request.UserRequestDTO;
import sorbonne.professional_website.dto.response.UserResponseDTO;
import sorbonne.professional_website.entity.User;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.UserMapper;
import sorbonne.professional_website.repository.UserRepository;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository rpUser;

    public UserService(UserRepository rpUser) {
        this.rpUser = rpUser;
    }

    public void createUser(UserRequestDTO userRequestDTO) {
        User user = UserMapper.fromRequest(userRequestDTO);
        rpUser.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return rpUser.findAll()
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId) {
        User user = findUserById(userId);
        return UserMapper.toResponse(user);
    }

    public void updateUser(Long userId, UserRequestDTO userRequestDTO) {
        User user = findUserById(userId);
        UserMapper.updateEntityFromRequest(user, userRequestDTO);
        rpUser.save(user);
    }

    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        rpUser.delete(user);
    }

    private User findUserById(Long userId) {
        return rpUser.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }
}
