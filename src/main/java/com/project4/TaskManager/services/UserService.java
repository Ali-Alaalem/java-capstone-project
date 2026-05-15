package com.project4.TaskManager.services;


import com.cloudinary.Cloudinary;
import com.project4.TaskManager.exceptions.InformationExistException;
import com.project4.TaskManager.exceptions.InformationNotFoundException;
import com.project4.TaskManager.models.VerificationToken;
import com.project4.TaskManager.models.request.LoginRequest;
import com.project4.TaskManager.models.response.LoginResponse;
import com.project4.TaskManager.repositories.RoleRepository;
import com.project4.TaskManager.repositories.UserRepository;
import com.project4.TaskManager.models.Role;
import com.project4.TaskManager.models.User;
import com.project4.TaskManager.repositories.VerificationTokenRepository;
import com.project4.TaskManager.security.JWTUtils;
import com.project4.TaskManager.security.MyUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private MyUserDetails myUserDetails;
    private RoleRepository roleRepository;
    private final JavaMailSender mailSender;
    private final Cloudinary cloudinary;
    private TokenService tokenService;

    private final VerificationTokenRepository verificationTokenRepository;
    @Value("${sender.email}")
    private String senderEmail;

    public UserService(VerificationTokenRepository verificationTokenRepository, TokenService tokenService, RoleRepository roleRepository, UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder,
                       JWTUtils jwtUtils, @Lazy AuthenticationManager authenticationManager, @Lazy MyUserDetails myUserDetails, JavaMailSender mailSender1, JavaMailSender mailSender, Cloudinary cloudinary){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtUtils=jwtUtils;
        this.authenticationManager=authenticationManager;
        this.myUserDetails=myUserDetails;
        this.roleRepository=roleRepository;
        this.tokenService=tokenService;
        this.verificationTokenRepository=verificationTokenRepository;
        this.mailSender = mailSender;
        this.cloudinary = cloudinary;
    }


    public Optional<User> findUserByEmail(String email)
    {
        return Optional.ofNullable(userRepository.findByEmail(email));
    }


    public User createUser(User objectUser){
        if(!userRepository.existsByEmail(objectUser.getEmail())){
            objectUser.setPassword(passwordEncoder.encode(objectUser.getPassword()));
            Optional<Role> role=roleRepository.findByName("CUSTOMER");
            objectUser.setRole(role.get());
            objectUser.setIsVerified(false);
            objectUser.setIsDeleted(false);
            objectUser.setProfileImage("http://res.cloudinary.com/dqqmgoftf/image/upload/v1775576374/f4c86146-3fbb-49a2-83aa-be503a5721ce.png");
            User user=userRepository.save(objectUser);


            VerificationToken verificationToken =
                    verificationTokenRepository.findByUser(user)
                            .orElse(new VerificationToken());

            verificationToken.setUser(user);
            verificationToken.setToken(UUID.randomUUID().toString());
            verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));

            verificationTokenRepository.save(verificationToken);
            tokenService.sendMail(user.getEmail(), verificationToken.getToken());

            return user;
        }else{
            throw new InformationExistException("User with email address " +objectUser.getEmail() + "already exist");
        }
    }


    public Optional<User> updateUser(Authentication authentication, User user) {

        Optional<User> existingUser = Optional.ofNullable(userRepository.findByEmail(authentication.getName()));

        if (user.getFirstName() != null) {
            existingUser.get().setFirstName(user.getFirstName());
        }

        if (user.getLastName() != null) {
            existingUser.get().setLastName(user.getLastName());
        }

        if (user.getEmail() != null) {
            existingUser.get().setEmail(user.getEmail());
        }

        if (user.getProfileImage() != null) {
            existingUser.get().setProfileImage(user.getProfileImage());
        }

        return Optional.of(userRepository.save(existingUser.get()));
    }

    public User deleteUser(Authentication authentication){
        System.out.println("Service calling ==> deleteUser()");
        Optional<User> user = Optional.ofNullable(this.userRepository.findByEmail(authentication.getName()));
        if(user.isPresent()){
            this.userRepository.delete(user.get());
            return user.get();
        }else{
            throw new InformationNotFoundException("An error happen during the delete process");
        }
    }


    public ResponseEntity<?> loginUser(LoginRequest loginRequest){

        UsernamePasswordAuthenticationToken authenticationToken=
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),loginRequest.getPassword());
        try {
            Authentication authentication=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                    loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            myUserDetails=(MyUserDetails) authentication.getPrincipal();
            if(myUserDetails.getUser().getIsVerified() && myUserDetails.getUser().getIsDeleted() !=true){
            final String JWT =jwtUtils.generateJwtToken(myUserDetails);
            return ResponseEntity.ok(new LoginResponse(JWT));
            }else{
                return ResponseEntity.ok(new LoginResponse("Your Account is not verified or deleted"));
            }
        }catch (Exception e){
            return ResponseEntity.ok(new LoginResponse("Error :User name of password is incorrect"));
        }

    }


}
