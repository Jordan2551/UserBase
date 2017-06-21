package dataModels.user;

import play.data.validation.Constraints;



/**
 * Created by jorda on 2017-06-07.
 */
public class CreateUser {

    public static final String ERROR_USER_EXISTS = "The username provided already exists";
    public static final String ERROR_PASSWORD_MISMATCH = "The passwords do not match or not required length or user is not long enough FIX";
    public static final String USER_CREATE_SUCCESS = "User successfully created!";

    @Constraints.Required
    @Constraints.MinLength(5)
    private String username;
    @Constraints.Required
    @Constraints.MinLength(5)
    private String password;
    @Constraints.Required
    @Constraints.MinLength(5)
    private String confirmPassword;

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
