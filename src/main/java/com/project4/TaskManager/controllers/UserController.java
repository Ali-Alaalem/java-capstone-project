package com.project4.TaskManager.controllers;



import com.project4.TaskManager.models.User;
import com.project4.TaskManager.models.request.LoginRequest;
import com.project4.TaskManager.models.request.PasswordChangeRequest;
import com.project4.TaskManager.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping(path="/auth/users")
public class UserController {
private UserService userService;

@Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User createUser(@RequestBody User objectUser){
        System.out.println("Calling create user");
        return userService.createUser(objectUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest){
        System.out.println("Calling loginUser ==>");
        return userService.loginUser(loginRequest);

    }


    @PostMapping("/password/reset")
    public void resetPasswordEmailSender(@RequestBody User user){
        System.out.println("Calling resetPasswordEmailSender ==>");
        userService.resetPasswordEmailSender(user);
    }

    @GetMapping("/password/reset/page")
    public ResponseEntity<String> resetPasswordPage(@RequestParam("token") String token){
        System.out.println("Calling resetPasswordPage ==>");
        return userService.resetPasswordPage(token);
    }

    @PostMapping("/password/reset/submit")
    public ResponseEntity<String> resetPasswordSubmit(@RequestParam String token, @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok("<h3>Password reset successfully!</h3>");
    }

    @PutMapping("/change/password")
    public String ChangePassword(Authentication authentication, @RequestBody PasswordChangeRequest request){
        System.out.println("Controller calling ==> ChangePassword()");
        return userService.ChangePassword(authentication,request);
    }

    @PutMapping("/{userId}/user")
    @PreAuthorize("hasAuthority('ADMIN')")
    public User softDeleteUser(Authentication authentication,@PathVariable("userId") Long userId){
        System.out.println("Controller calling ==> softDeleteUser()");
        return userService.softDeleteUser(authentication,userId);
    }


    @PostMapping(
            value = "/imageUpdater",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAuthority('USER')")
    public User ImageUpdater(Authentication authentication,@RequestPart("image") MultipartFile image) throws IOException {
        return userService.ImageUpdater(authentication,image);
    }

    @PatchMapping("/UpdateUser")
    public Optional<User> updateUser(Authentication authentication, @RequestBody User user){
        System.out.println("Controller calling ==> updatePerson()");
        return this.userService.updateUser(authentication,user);
    }

    @DeleteMapping("/DeleteUser")
    public User deleteUser(Authentication authentication){
        System.out.println("Controller calling ==> updatePerson()");
        return this.userService.deleteUser(authentication);
    }

}
