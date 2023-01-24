package com.piaskowy.urlshortenerbackend.user.service;

import com.piaskowy.urlshortenerbackend.auth.model.request.AuthenticationRequest;
import com.piaskowy.urlshortenerbackend.auth.model.request.RegisterRequest;
import com.piaskowy.urlshortenerbackend.auth.model.response.AuthenticationResponse;
import com.piaskowy.urlshortenerbackend.auth.service.JwtService;
import com.piaskowy.urlshortenerbackend.auth.token.model.entity.Token;
import com.piaskowy.urlshortenerbackend.user.model.CustomUserDetails;
import com.piaskowy.urlshortenerbackend.user.model.dto.UserDto;
import com.piaskowy.urlshortenerbackend.user.model.entity.User;
import com.piaskowy.urlshortenerbackend.user.model.mapper.UserModelMapper;
import com.piaskowy.urlshortenerbackend.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class UserService {
    private final UserRepository userRepository;
    private final UserRegistrationService userRegistrationService;
    private final UserEmailConfirmationService userEmailConfirmationService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserModelMapper mapper;

    UserService(final UserRepository userRepository, final UserRegistrationService userRegistrationService, final UserEmailConfirmationService userEmailConfirmationService, final AuthenticationManager authenticationManager, final JwtService jwtService, final UserModelMapper userModelMapper) {
        this.userRepository = userRepository;
        this.userRegistrationService = userRegistrationService;
        this.userEmailConfirmationService = userEmailConfirmationService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mapper = userModelMapper;
    }

    public UserDto registerUser(RegisterRequest registerRequest) throws MessagingException {
        log.info("User registration procedure started with given details: " + registerRequest.toString());
        User user = userRegistrationService.signUpNewUser(registerRequest);
        Token token = userEmailConfirmationService.generateAndSaveConfirmationToken(user);
        userEmailConfirmationService.sendAccountConfirmationEmail(token.getGeneratedToken(), user);
        return mapper.toDto(user);
    }

    public AuthenticationResponse authenticateUser(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword())
        );
        String jwtToken = userRepository
                .findByEmail(request.getEmail())
                .map(u -> jwtService.generateToken(new CustomUserDetails(u)))
                .orElseThrow(() -> new UsernameNotFoundException("User with email: " + request.getEmail() + " not found"));

        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    public UserDto getUserById(Long id) {
        return userRepository
                .findUserById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new UsernameNotFoundException("User with id: " + id + " not found"));
    }

    public UserDto getCurrentUser(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.user();
        return mapper.toDto(user);
    }

    @Transactional
    public void confirmUserEmail(String token) {
        log.info("Email confirmation procedure started with token: " + token);
        User user = userEmailConfirmationService.validateEmailConfirmationToken(token).getUser();
        userRepository.enableUserAccount(user.getEmail());
    }

}