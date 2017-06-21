package controllers;

import com.google.inject.Inject;
import dataModels.user.ConfirmPassword;
import dataModels.user.CreateUser;
import dataModels.user.LogInUser;
import database.DB;
import emails.SendGridEmail;
import models.UsersEntity;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by jorda on 2017-06-07.
 */

enum tokenAuthorizeResult {
    authorized, tokenExpired, invalidToken, error
}

public class AccountController extends Controller {

    private final FormFactory formFactory;
    private Transaction tx = null;
    private UsersEntity loggedInUser;

    public UsersEntity getLoggedInUser() {
        return loggedInUser;
    }

    @Inject
    public AccountController(final FormFactory formFactory) {
        this.formFactory = formFactory;
    }

    public Result test() {
        return ok(views.html.test.render());
    }

    //TODO make sure only emails can be created
    public Result createAccount() {
        //Obtain the model data bound from the request
        Form<CreateUser> userForm = formFactory.form(CreateUser.class).bindFromRequest();

        //Check that password and confirm password match
        if (userForm.errors().size() == 0 && Objects.equals(userForm.get().getPassword(), userForm.get().getConfirmPassword())) {

            Session DBSession = DB.getSession();

            try {

                tx = DBSession.beginTransaction();

                List<UsersEntity> usersFromDB = DBSession.createCriteria(UsersEntity.class).list();

                for (UsersEntity userFromDB : usersFromDB) {
                    if (Objects.equals(userFromDB.getUsername().toLowerCase(), userForm.get().getUsername().toLowerCase())) {
                        return badRequest(views.html.index.render(CreateUser.ERROR_USER_EXISTS));
                    }
                }

                UsersEntity user = new UsersEntity();
                user.setUsername(userForm.get().getUsername());
                user.setActivated(false);//If not created through social media
                user.setRecovering(false);
                user.setLoginAttemptCount(0);
                user.setPassword(controllers.BCrypt.hashpw(userForm.get().getPassword(), controllers.BCrypt.gensalt()));
                user.setResetToken("");

                DBSession.save(user);
                DBSession.flush();
                tx.commit();


            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }
        } else {
            return badRequest(views.html.index.render(CreateUser.ERROR_PASSWORD_MISMATCH));
        }
        return ok(views.html.index.render(CreateUser.USER_CREATE_SUCCESS));

    }

    public Result logIn() {
        //If someone accesses the log in page after being logged in then log them out
        if (isLoggedIn()) {
            logOut();
        }
        return ok(views.html.login.render("Please provide your credentials"));
    }

    public void setLoggedInUser(UsersEntity loggedInUser) {
        session("id", String.valueOf(loggedInUser.getId()));
        session("username", String.valueOf(loggedInUser.getUsername()));
        session("isActivated", String.valueOf(loggedInUser.isActivated()));
        session("isRecovering", String.valueOf(loggedInUser.isRecovering()));
        session("loginAttemptCount", String.valueOf(loggedInUser.getLoginAttemptCount()));
    }

    public static boolean isLoggedIn() {return session().get("id") != null;}

    public void logOut() {
        //Discard the logged out user's session.
        session().clear();
    }

    public Result authenticate() {

        Form<LogInUser> userForm = formFactory.form(LogInUser.class).bindFromRequest();

        if (userForm.errors().size() == 0) {
            //Authenticate user and pw
            Session DBSession = DB.getSession();
            
            try {

                tx = DBSession.beginTransaction();

                List<UsersEntity> userFromDB = DBSession.createQuery("from UsersEntity where username = :username")
                        .setParameter("username", userForm.get().getUsername()).list();
                DBSession.flush();
                tx.commit();
                
                if (userFromDB.size() > 0) {
                    if (BCrypt.checkpw(userForm.get().getPassword(), userFromDB.get(0).getPassword())) {
                        //As the Session is just a Cookie, it is also just an HTTP header
                        //but Play provides a helper method to store a session value
                        setLoggedInUser(userFromDB.get(0));
                        return userSettings();
                    }
                }

            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }

        }
        return badRequest(views.html.login.render("Your username or password are incorrect"));
    }

    public Result userSettings() {

        //User persistence
        //TODO add more user properties
        String username = session().get("username");
        if (username != null) {
            return ok(views.html.user_settings.render(username));
        }
        return badRequest(views.html.unauthorized.render("You are unauthorized to access this page. Log in first"));
    }

    public Result passwordReset1() {
        // if (isLoggedIn())
        // return badRequest(views.html.unauthorized.render("You are already logged in"));
        return ok(views.html.request_reset.render());
    }

    public Result requestPasswordReset() {

        //TODO the email with password provided must result in a get request with the generated token. Set the routing for it and define the action
        //Save the token in the db for later confirmation
        Form<LogInUser> emailRequest = formFactory.form(LogInUser.class).bindFromRequest();

        if (emailRequest.errors().size() == 0) {

            Session DBSession = DB.getSession();

            try {

                tx = DBSession.beginTransaction();

                List<UsersEntity> userFromDB = DBSession.createQuery("from UsersEntity where username = :username")
                        .setParameter("username", emailRequest.get().getUsername()).list();
                DBSession.flush();
                tx.commit();

                if (userFromDB.size() != 0) {

                    //TODO: check that this is sufficient. How does this work?
                    //Generate a random token
                    SecureRandom random = new SecureRandom();
                    String token = new BigInteger(130, random).toString(32);

                    //Send the password request email.
                    //If it succeeds then save the password token & token life in db
                    if (SendGridEmail.sendPasswordRequestEmail(userFromDB.get(0).getUsername(), token, userFromDB.get(0).getId())) {

                        tx = DBSession.beginTransaction();
                        userFromDB.get(0).setResetToken(controllers.BCrypt.hashpw(token, controllers.BCrypt.gensalt()));
                        userFromDB.get(0).setResetTokenLife(new Timestamp(System.currentTimeMillis() + 21600000));
                        userFromDB.get(0).setRecovering(true);
                        DBSession.saveOrUpdate(userFromDB.get(0));
                        DBSession.flush();
                        tx.commit();

                    }
                } else {
                    return badRequest(views.html.unauthorized.render("User does not exist"));
                }

            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }
        }
        return badRequest(views.html.unauthorized.render("Password request sent! Check your email"));
    }

    //TODO is putting the id a good idea?
    //TODO through https?
    //TODO should the token expire as soon as I land on the reset page for the first time? Check this with other sites
    public Result passwordReset2(String token, long id) {

        //Authorize the token and information
        switch (checkToken(token, id)) {
            case authorized:
                return ok(views.html.reset.render(token, id));
            case tokenExpired:
                return badRequest(views.html.unauthorized.render("The token has expired. ---ADD LINK TO RESET PASSWORD PAGE AGAIN---"));
            case invalidToken:
                return badRequest(views.html.unauthorized.render("Invalid token, user doesn't exist or account is not recovering. ---CHANGE THIS ERROR IN THE FUTURE---"));
        }

        return badRequest(views.html.unauthorized.render("Error occurred"));

    }

    public Result resetPassword(String token, long id) {

        Form<ConfirmPassword> passwordReset = formFactory.form(ConfirmPassword.class).bindFromRequest();

        //Check that password and confirm password match
        if (passwordReset.errors().size() == 0 && Objects.equals(passwordReset.get().getPassword(), passwordReset.get().getConfirmPassword())) {

            //Authorize the token and information once again
            switch (checkToken(token, id)) {

                case authorized:

                    Session DBSession = DB.getSession();

                    try {

                        tx = DBSession.beginTransaction();

                        List<UsersEntity> userFromDB = DBSession.createQuery("from UsersEntity where id = :id")
                                .setParameter("id", id).list();

                        //Set the new password for the user, turn the recovery flag off, expire the reset token and delete the reset token
                        userFromDB.get(0).setPassword(BCrypt.hashpw(passwordReset.get().getPassword(), BCrypt.gensalt()));
                        userFromDB.get(0).setRecovering(false);
                        userFromDB.get(0).setResetToken("");
                        userFromDB.get(0).setResetTokenLife(new Timestamp(System.currentTimeMillis()));

                        DBSession.saveOrUpdate(userFromDB.get(0));

                        DBSession.flush();
                        tx.commit();

                        return ok(views.html.index.render("New password successfully set!"));

                    } catch (HibernateException e) {
                        if (tx != null) tx.rollback();
                        e.printStackTrace();
                    } finally {
                        DBSession.close();
                    }

                case tokenExpired:
                    return badRequest(views.html.unauthorized.render("The token has expired. ---ADD LINK TO RESET PASSWORD PAGE AGAIN---"));
                case invalidToken:
                    return badRequest(views.html.unauthorized.render("Invalid token, user doesn't exist or account is not recovering. ---CHANGE THIS ERROR IN THE FUTURE---"));
            }
        }

        //TODO add password mismatch warning
        return badRequest(views.html.unauthorized.render("Passwords do not match or there is an error with the password"));
        //TODO email setup. Send the password reset token to the requested email
    }

    //Checks if a password reset request is allowed
    private tokenAuthorizeResult checkToken(String token, long id) {

        if (token != null) {

            Session DBSession = DB.getSession();

            try {
                tx = DBSession.beginTransaction();

                List<UsersEntity> userFromDB = DBSession.createQuery("from UsersEntity where id = :id")
                        .setParameter("id", id).list();

                DBSession.flush();
                tx.commit();
                
                //User & token exist, user is recovering
                if (userFromDB.size() != 0 && BCrypt.checkpw(token, userFromDB.get(0).getResetToken()) && userFromDB.get(0).isRecovering()) {
                    //Token life valid
                    if (userFromDB.get(0).getResetTokenLife().after(new Timestamp(System.currentTimeMillis()))) {
                        return tokenAuthorizeResult.authorized;
                        //Token expired
                    } else {
                        return tokenAuthorizeResult.tokenExpired;
                    }
                    //Invalid user or token
                } else {
                    return tokenAuthorizeResult.invalidToken;
                }

            } catch (Exception e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }
        }
        return tokenAuthorizeResult.error;
    }

}