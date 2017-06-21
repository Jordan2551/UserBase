package dataModels.user;

import play.data.validation.Constraints;

/**
 * Created by jorda_000 on 2017-06-21.
 * Used for confirming if two passwords match.
 */
public class ConfirmPassword {

    @Constraints.Required
    @Constraints.MinLength(5)
    private String password;
    @Constraints.Required
    @Constraints.MinLength(5)
    private String confirmPassword;

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
