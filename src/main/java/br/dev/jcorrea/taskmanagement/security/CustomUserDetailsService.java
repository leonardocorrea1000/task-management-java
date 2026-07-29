package br.dev.jcorrea.taskmanagement.security;

import br.dev.jcorrea.taskmanagement.user.User;
import br.dev.jcorrea.taskmanagement.user.UserRepository;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmail(username.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return toUserDetails(user);
    }

    public UserDetails loadUserById(String id) {
        User user = userRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }
}
