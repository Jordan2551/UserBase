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


    @Constraints.Required(message = "Email field is required")
    @Constraints.MaxLength(value = 30, message = "Email must be no longer than 18 characters long")
    @Constraints.Email(message = "You must provide a valid email address")
    private String username;

    @Constraints.Required(message = "Password field is required")
    @Constraints.MaxLength(value = 18, message = "Password must be no longer than 18 characters long")
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
