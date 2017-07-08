package dataModels.user;

import play.data.validation.Constraints;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by jorda_000 on 2017-06-21.
 * Used for confirming if two passwords match.
 */
public class ConfirmPassword {

    @Constraints.Required(message = "Password field is required")
    @Constraints.MaxLength(value = 18, message = "Password must be no longer than 18 characters long")
    private String password;
    @Constraints.Required(message = "Confirm password field is required")
    private String confirmPassword;

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<ValidationError>();
        if (!Objects.equals(password, confirmPassword))
            errors.add(new ValidationError("password", "The passwords do not match"));
        return errors.isEmpty() ? null : errors;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() { return confirmPassword; }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

}
