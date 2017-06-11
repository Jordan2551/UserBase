package controllers;

import com.google.inject.Inject;
import dataModels.user.CreateUser;
import dataModels.user.LogInUser;
import database.DB;
import models.UsersEntity;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by jorda on 2017-06-07.
 */
public class AccountController extends Controller {

    private final FormFactory formFactory;
    private Transaction tx = null;

    @Inject
    public AccountController(final FormFactory formFactory) {
        this.formFactory = formFactory;
    }

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

    public static boolean isLoggedIn() {
        //TODO test the isempty method
        if (session().get("user") != null) {
            return true;
        }
        return false;
    }

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
                        //TODO add more user properties
                        session("user", userFromDB.get(0).getUsername());
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
        String username = session().get("user");
        if (username != null) {
            return ok(views.html.user_settings.render(username));
        }
        return badRequest(views.html.unauthorized.render("You are unauthorized to access this page. Log in first"));
    }

    public Result recoverAccount() {
        if (isLoggedIn())
            return badRequest(views.html.unauthorized.render("You are already logged in"));

        return ok(views.html.recover.render());
    }

    public void requestPasswordReset(){

        String getGeneratedUsernamefromDB = "";

        UUID token = UUID.randomUUID();

        //Save the token in the db for later confirmation


        //TODO email setup. Send the password reset token to the requested email
    }


}