package com.project4.TaskManager.services;


import com.cloudinary.Cloudinary;
import com.project4.TaskManager.exceptions.InformationExistException;
import com.project4.TaskManager.exceptions.InformationNotFoundException;
import com.project4.TaskManager.models.VerificationToken;
import com.project4.TaskManager.models.request.LoginRequest;
import com.project4.TaskManager.models.request.PasswordChangeRequest;
import com.project4.TaskManager.models.response.LoginResponse;
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


    @Transactional
    public void resetPasswordEmailSender(User user){
        User resetPassUser=userRepository.findByEmail(user.getEmail());
        if(resetPassUser != null) {

            VerificationToken verificationToken =
                    verificationTokenRepository.findByUser(resetPassUser)
                            .orElse(new VerificationToken());

            verificationToken.setToken(UUID.randomUUID().toString());
            verificationToken.setUser(resetPassUser);
            verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));

            verificationTokenRepository.save(verificationToken);
            sendMail(user.getEmail(), verificationToken.getToken());

        }
        else{
            throw new InformationExistException("User with email address " +resetPassUser.getEmail() + "does not exist");
        }
    }


    public void sendMail(String email,String token){
        String link = "http://localhost:8080/auth/users/password/reset/page?token=" + token;

        try{
            MimeMessage message =mailSender.createMimeMessage();
            MimeMessageHelper helper=new MimeMessageHelper(message,true,"UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(email);
            helper.setSubject("Password Reset Request");

            String html = """
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0; padding:0; background-color:#f4f4f7;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f7; height:100%; text-align:center;">
    <tr>
      <td align="center">
        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff; padding:30px; border-radius:12px; box-shadow:0 4px 15px rgba(0,0,0,0.1); text-align:center;">
          <tr>
            <td>
              <h1 style="color:#4F46E5; font-family:'Helvetica',Arial,sans-serif;">Welcome to Triple A Hospital</h1>
              <p style="font-size:16px; color:#333;">Hello,</p>
              <p style="font-size:16px; color:#333;">Click on the button to reset your password.</p>
              <a href="{link}" style="display:inline-block; padding:14px 25px; font-size:16px; font-weight:bold; color:#ffffff; background-color:#4F46E5; text-decoration:none; border-radius:8px;">Reset Password</a>
              <p style="margin-top:20px; font-size:12px; color:#999;">&copy; 2026 Hospital Management System</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
""";
            html = html.replace("{link}", link);
            helper.setText(html, true);
            mailSender.send(message);

        }
        catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    public ResponseEntity<String> resetPasswordPage(String token){
        String html = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>Reset Password</title>
    </head>
    <body style="text-align:center; margin-top:50px;">
      <h2>Reset Your Password</h2>
      <form action="/auth/users/password/reset/submit" method="post">
        <input type="hidden" name="token" value="%s" />
        <input type="password" name="newPassword" placeholder="New Password" required />
        <br><br>
        <button type="submit">Reset Password</button>
      </form>
    </body>
    </html>
    """.formatted(token);

        return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
    }

    public void resetPassword(String token,String newPass){
        Optional<VerificationToken> userToken= verificationTokenRepository.findByToken(token);
        if (userToken.isPresent() && userToken.get().getExpiryDate().isAfter(LocalDateTime.now()))
        {
            User user=userToken.get().getUser();
            user.setPassword(passwordEncoder.encode(newPass));
            userRepository.save(user);
            verificationTokenRepository.delete(userToken.get());
        }
    }


    public String ChangePassword(Authentication authentication, PasswordChangeRequest request) {
        String currentLoggedUserEmail = authentication.getName();
        User userLoggedIn=userRepository.findByEmail(currentLoggedUserEmail);

        if( passwordEncoder.matches(request.getCurrentPassword(),userLoggedIn.getPassword())){
            userLoggedIn.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(userLoggedIn);
            return "User password has been changed successfully";
        }else {
            throw new InformationExistException("The Current password is wrong");
        }

    }

    public User ImageUpdater (Authentication authentication, MultipartFile multipartFile) throws IOException {
        System.out.println("Service is calling createPerson");
        User user= userRepository.findByEmail(authentication.getName());
        String imageURL= cloudinary.uploader().upload(multipartFile.getBytes(),
                Map.of("public_id", UUID.randomUUID().toString())).get("url").toString();
        user.setProfileImage(imageURL);
        return this.userRepository.save(user);
    }

    public User softDeleteUser(Authentication authentication,Long userId){
        System.out.println("Service calling ==> deleteUser()");
        Optional<User> forginUser=userRepository.findById(userId);
        String currentLoggedUserEmail = authentication.getName();
        User userLoggedIn=userRepository.findByEmail(currentLoggedUserEmail);
        if(!userLoggedIn.getRole().getName().equals("ADMIN") && forginUser.isPresent()){
            throw new InformationExistException("User does not have permission or the user wanted to be deleted is not exist");
        }

        if(forginUser.get().getIsDeleted() ==true){
            forginUser.get().setIsDeleted(false);
        }else
        {
            forginUser.get().setIsDeleted(true);
        }

        return userRepository.save(forginUser.get());
    }

}
