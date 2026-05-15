package com.project4.TaskManager.services;



import com.cloudinary.Cloudinary;
import com.project4.TaskManager.exceptions.InformationExistException;
import com.project4.TaskManager.exceptions.InformationNotFoundException;
import com.project4.TaskManager.models.VerificationToken;
import com.project4.TaskManager.repositories.RoleRepository;
import com.project4.TaskManager.repositories.UserRepository;
import com.project4.TaskManager.models.Role;
import com.project4.TaskManager.models.User;
import com.project4.TaskManager.repositories.VerificationTokenRepository;
import com.project4.TaskManager.security.JWTUtils;
import com.project4.TaskManager.security.MyUserDetails;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
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


    private final VerificationTokenRepository verificationTokenRepository;
    @Value("${sender.email}")
    private String senderEmail;

    public UserService(VerificationTokenRepository verificationTokenRepository, RoleRepository roleRepository, UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder,
                       JWTUtils jwtUtils, @Lazy AuthenticationManager authenticationManager, @Lazy MyUserDetails myUserDetails, JavaMailSender mailSender1, VerificationTokenRepository verificationTokenRepository1, JavaMailSender mailSender, Cloudinary cloudinary){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtUtils=jwtUtils;
        this.authenticationManager=authenticationManager;
        this.myUserDetails=myUserDetails;
        this.roleRepository=roleRepository;
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
            Optional<Role> role=roleRepository.findByName("User");
            objectUser.setRole(role.get());
            objectUser.setIsVerified(false);
            objectUser.setIsDeleted(false);
            objectUser.setProfileImage("http://res.cloudinary.com/dqqmgoftf/image/upload/v1775576374/f4c86146-3fbb-49a2-83aa-be503a5721ce.png");
            User user=userRepository.save(objectUser);

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

}
