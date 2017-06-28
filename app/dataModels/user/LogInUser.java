package dataModels.user;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by jorda on 2017-06-09.
 */
public class LogInUser {

    public static final String ERROR_USER_OR_PASSWORD_INVALID = "The username or password provided is invalid";

    @Constraints.Required(message = "Email field is required")
    @Constraints.Email(message = "You must provide a valid email address")
    private String username;

    @Constraints.Required(message = "Password field is required")
    private String password;


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
