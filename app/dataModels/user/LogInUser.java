package dataModels.user;

import play.data.validation.Constraints;

/**
 * Created by jorda on 2017-06-09.
 */
public class LogInUser {

    public static final String ERROR_USER_OR_PASSWORD_INVALID = "The username or password provided is invalid";

    @Constraints.MinLength(1)
    private String username;

    @Constraints.MinLength(1)
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
