package dataModels.user;

import com.google.inject.Inject;
import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import javax.validation.Constraint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Created by jorda on 2017-06-07.
 */
public class CreateUser {

    public static final String ERROR_USER_EXISTS = "The email provided is already in use";
    public static final String ERROR_PASSWORD_MISMATCH = "The passwords do not match or not required length or user is not long enough FIX";
    public static final String USER_CREATE_SUCCESS = "User successfully created!";

    @Constraints.Required(message = "Email field is required")
    @Constraints.Email(message = "Please provide a valid email address")
    private String username;
    @Constraints.Required(message = "Password field is required")
    @Constraints.MinLength(value = 6, message = "Password must be at least 6 characters long")
    private String password;
    private String confirmPassword;

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<ValidationError>();
        if (!Objects.equals(password, confirmPassword))
            errors.add(new ValidationError("password", "The passwords do not match"));

        return errors.isEmpty() ? null : errors;
    }

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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

}
