package ca.gb.comp3095.foodrecipe.controller.user;

import ca.gb.comp3095.foodrecipe.model.domain.User;
import ca.gb.comp3095.foodrecipe.model.service.UserService;
import ca.gb.comp3095.foodrecipe.view.AttributeTags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Optional;

@Controller
@RequestMapping(value = "/user", produces = {MediaType.APPLICATION_JSON_VALUE})
@Slf4j
public class UserViewController implements WebMvcConfigurer {
    @Autowired
    UserService userService;

    @GetMapping
    public String getUserDetails(Model model) {
        log.info("default user details");
        return "user/user";
    }


    @GetMapping(path = "/")
    public String getUserByName(@RequestParam("name") String userName, Model model) {
        try {
            UserDto userFound = userService.getUserByName(userName).map(UserConverter::fromDomain).orElseThrow(RuntimeException::new);
            model.addAttribute("user", userFound);
        } catch (Exception e) {
            log.warn("No user found for {}", userName, e);
            model.addAttribute("message", "User not found for name: " + userName);
        }
        return "user/user";
    }

    @GetMapping(path = "/id/{userId}")
    public String getUserById(@PathVariable("userId") Long userId, Model model) {
        try {
            UserDto userFound = userService.getUserById(userId).map(UserConverter::fromDomain).orElseThrow(RuntimeException::new);
            model.addAttribute("user", userFound);
        } catch (Exception e) {
            log.warn("No user found for id {}", userId, e);
            model.addAttribute("message", "User not found for id: " + userId);
        }
        return "user/user";
    }

    @GetMapping(path = "/create")
    public String showUserCreationForm(CreateUserCommand createUserCommand) {
        return "user/user-add";
    }

    @PostMapping(path = "/{userId}/change-password")
    public String changeUserPassword(@PathVariable("userId") Long userId, @Validated ChangeUserPasswordCommand changePasswordCommand, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            log.warn("Unable to change password, form has errors {}", bindingResult.getAllErrors());
            return "redirect:/user/id/" + userId;
        }

        if (!StringUtils.equals(changePasswordCommand.getPassword1(), changePasswordCommand.getPassword2())) {
            model.addAttribute(AttributeTags.ERROR, "Passwords do not match!");
            return "redirect:/user/id/" + userId;
        }

        Optional<User> userById = userService.getUserById(userId);
        if (userById.isEmpty()) {
            log.warn("No user found for id {}", userId);
            model.addAttribute(AttributeTags.ERROR, "Invalid user provided!");
            return "redirect:/";
        }

        try {
            User updatedUser = userService.changePassword(userId, changePasswordCommand.getPassword1());
            model.addAttribute(AttributeTags.USER, UserConverter.fromDomain(updatedUser));
            model.addAttribute(AttributeTags.SUCCESS, "Password changed successfully!");
        } catch (Exception e) {
            log.warn("unable to change password for user {} ", userById, e);
            model.addAttribute(AttributeTags.USER, UserConverter.fromDomain(userById.get()));
            model.addAttribute(AttributeTags.ERROR, "Unable to change password for user {}" + userById);
        }
        return "user/user";
    }

    @PostMapping(path = "/create")
    public String createNewUser(@Validated CreateUserCommand createUserCommand, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "user/signup";
        }
        try {
            User user = User.builder().name(createUserCommand.getUserName()).email(createUserCommand.getEmail()).password(createUserCommand.getPassword()).build();
            UserDto userDto = UserConverter.fromDomain(userService.createNewUser(user));
            log.info("successfully created user {}", userDto);
            model.addAttribute("message", String.format("User %s created successfully", userDto.getUserName()));
            model.addAttribute("user", userDto);
        } catch (Exception e) {
            log.error("Unable to create user {}", createUserCommand, e);
        }
        return "user/user";
    }

    @GetMapping(path = "/signup")
    public String signUp() {
        return "user/signup";
    }

}
