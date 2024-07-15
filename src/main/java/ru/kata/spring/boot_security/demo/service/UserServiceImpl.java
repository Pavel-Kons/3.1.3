package ru.kata.spring.boot_security.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kata.spring.boot_security.demo.dao.RoleRepository;
import ru.kata.spring.boot_security.demo.dao.UserRepository;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Set<String> getAllRolesNames() {
        return roleRepository
                .findAll()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    @Transactional
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("There is no %s user", email));
        }
        return new org.springframework.security.core.userdetails.User
                (user.getEmail(), user.getPassword(), mapRolesToAuthority(user.getRoles()));
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthority(Collection<Role> roles) {
        return roles
                .stream()
                .map(el -> new SimpleGrantedAuthority("ROLE_" + el.getName()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers(int count) {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void saveUser(User user, Set<String> roles) {
        if (roles == null) {
            user.setRoles(roleRepository.findAllByName("USER"));
        } else {
            user.setRoles(roleRepository.findAllByNameIn(roles));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUser(User user) {
        user.setRoles(roleRepository.findAllByName("USER"));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id).get();
    }
}
