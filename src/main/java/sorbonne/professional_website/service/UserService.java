package sorbonne.professional_website.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.dto.UserDTO;
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

    public void createUser(UserDTO userCreateDTO) {
        User user = UserMapper.fromCreateDTO(userCreateDTO);
        rpUser.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return rpUser.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        User user = findUserById(userId);
        return UserMapper.toDTO(user);
    }

    public void updateUser(Long userId, UserDTO userUpdateDTO) {
        User user = findUserById(userId);

        UserMapper.updateEntityFromDTO(user, userUpdateDTO);

        rpUser.save(user);
    }

    public void deleteUser(Long userId) {
        User user = findUserById(userId);
        rpUser.delete(user);
    }

    private User findUserById(Long userId) {
        return rpUser.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User : " + userId));
    }
}