package dataModels.user;

import play.data.validation.Constraints;

/**
 * Created by jorda_000 on 2017-06-27.
 */
public class RecoverUser {


    @Constraints.Required(message = "Email field is required")
    @Constraints.Email(message = "You must provide a valid email address")
    @Constraints.MaxLength(value = 30, message = "Email must be no longer than 30 characters long")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
