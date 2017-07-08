package controllers;

import com.google.inject.Inject;
import controllers.*;
import controllers.routes;
import play.mvc.Http;
import play.mvc.Security;
import security.BCrypt;
import dataModels.user.*;
import database.DB;
import emails.SendGridEmail;
import models.UsersEntity;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.mvc.Controller;
import play.mvc.Result;
import security.Secured;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by jorda on 2017-06-07.
 */
//TODO SSL WITH MYSQL, HTTPS WHERE NEEDED
//TODO MYSQL CONNECTION POOL LIMIT?
//TODO should sessions expire?
//TODO should I only create the account after the code is verified? IS that even possibly given that the data has to exist somewhere. Maybe add a task that clears inactivated users after a certain timeframe OR the ability to resend a new token
//TODO make more elegant with flexible data binds

enum passwordTokenAuthorizeResult {
    authorized, tokenExpired, invalidToken, error
}

public class AccountController extends Controller {

    private final FormFactory formFactory;
    private Transaction tx = null;

    public Secured security = new Secured();

    @Inject
    public AccountController(final FormFactory formFactory) {
        this.formFactory = formFactory;
    }

    public Result test() {
        return ok(views.html.test.render());
    }

    public Result createAccount() {

        //Obtain the model data bound from the request
        Form<CreateUser> userForm = formFactory.form(CreateUser.class).bindFromRequest();

        //Check that password and confirm password match
        if (!userForm.hasErrors()) {

            Session DBSession = DB.getSession();

            try {

                tx = DBSession.beginTransaction();

                List<UsersEntity> usersFromDB = DBSession.createCriteria(UsersEntity.class).list();

                //Check that the email is not already being used in the database
                for (UsersEntity userFromDB : usersFromDB) {
                    if (Objects.equals(userFromDB.getUsername().toLowerCase(), userForm.get().getUsername().toLowerCase())) {
                        return badRequest(views.html.index.render(UserMessages.ERROR_EMAIL_EXISTS));
                    }
                }

                SecureRandom random = new SecureRandom();
                String token = new BigInteger(130, random).toString(32);

                UsersEntity user = new UsersEntity();
                user.setUsername(userForm.get().getUsername());
                user.setActivated(false);//If not created through social media
                user.setRecovering(false);
                user.setLoginAttemptCount(0);
                user.setPassword(BCrypt.hashpw(userForm.get().getPassword(), BCrypt.gensalt()));
                user.setResetToken("");
                user.setActivationToken(BCrypt.hashpw(token, BCrypt.gensalt()));

                DBSession.save(user);
                DBSession.flush();
                tx.commit();

                //Unfortunately I can only save the object first so I can get the id to later send in the email. The task implementation above is the solution incase the mailing service fails.
                SendGridEmail.sendAccountCreationEmail(user.getUsername(), token, user.getId());

            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }

        } else {
            return badRequest(views.html.index.render(printValidationErrors(userForm.errors())));
        }

        return ok(views.html.index.render(UserMessages.USER_CREATE_SUCCESS));

    }

    public Result activateAccount(String token, long id) {

        if (token != null) {

            Session DBSession = DB.getSession();

            try {

                tx = DBSession.beginTransaction();

                List<UsersEntity> userFromDB = DBSession.createQuery("from UsersEntity where id = :id")
                        .setParameter("id", id).list();

                if (userFromDB.size() > 0) {
                    if (BCrypt.checkpw(token, userFromDB.get(0).getActivationToken())) {
                        //If the activation token is valid then set the account to activated and delete the activation token.
                        userFromDB.get(0).setActivated(true);
                        userFromDB.get(0).setActivationToken("");
                        DBSession.saveOrUpdate(userFromDB.get(0));
                        DBSession.flush();
                        tx.commit();

                        return ok(views.html.index.render(UserMessages.USER_ACCOUNT_ACTIVATED_SUCCESS));

                    }
                } else {
                    return badRequest(views.html.index.render(UserMessages.ERROR_TOKEN_AUTH));
                }

            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }

        }
        return badRequest(views.html.index.render("Error"));
    }

    public Result logIn() {
        //If someone accesses the log in page after being logged in then log them out
        // request() = Returns the current HTTP request.
        if (request().username() != null) {
            logOut();
        }
        return ok(views.html.login.render());
    }

    public Result logOut() {
        //Discard the logged out user's session.
        session().clear();
        flash("success", "You've been logged out");
        return redirect(routes.AccountController.logIn());
    }

    //Determines user access
    public Result authenticate() {

        Form<LogInUser> userForm = formFactory.form(LogInUser.class).bindFromRequest();

        if (!userForm.hasErrors()) {

            Session DBSession = DB.getSession();

            try {

                tx = DBSession.beginTransaction();

                List<UsersEntity> userFromDB = DBSession.createQuery("from UsersEntity where username = :username")
                        .setParameter("username", userForm.get().getUsername()).list();
                DBSession.flush();
                tx.commit();

                if (userFromDB.size() > 0 && BCrypt.checkpw(userForm.get().getPassword(), userFromDB.get(0).getPassword())) {
                    if (userFromDB.get(0).isActivated()) {
                        session("email", userFromDB.get(0).getUsername());
                        return userSettings();
                    } else
                        flash("error", UserMessages.ERROR_ACCOUNT_NOT_ACTIVATED);
                    return redirect(routes.AccountController.logIn());
                } else {
                    flash("error", UserMessages.ERROR_USER_OR_PASSWORD_INVALID);
                    return redirect(routes.AccountController.logIn());
                }

            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }

        } else {
            flash("error", printValidationErrors(userForm.errors()));
            return redirect(routes.AccountController.logIn());
        }

        return badRequest(views.html.login.render());
    }

    //Attribute to assure a user is logged in before calling this request
    @Security.Authenticated(Secured.class)
    public Result userSettings() {
        return ok(views.html.user_settings.render(security.getUsername(Http.Context.current())));
    }

    public Result passwordReset1() {
        return ok(views.html.request_reset.render());
    }

    public Result requestPasswordReset() {

        //Save the token in the db for later confirmation
        Form<RecoverUser> emailRequest = formFactory.form(RecoverUser.class).bindFromRequest();

        if (!emailRequest.hasErrors()) {

            Session DBSession = DB.getSession();

            try {

                tx = DBSession.beginTransaction();

                List<UsersEntity> userFromDB = DBSession.createQuery("from UsersEntity where username = :username")
                        .setParameter("username", emailRequest.get().getUsername()).list();
                DBSession.flush();
                tx.commit();

                if (userFromDB.size() != 0) {

                    if (userFromDB.get(0).isActivated()) {
                        //TODO: check that this is sufficient. How does this work?
                        //Generate a random token
                        SecureRandom random = new SecureRandom();
                        String token = new BigInteger(130, random).toString(32);

                        //Send the password request email.
                        //If it succeeds then save the password token & token life in db
                        if (SendGridEmail.sendPasswordRequestEmail(userFromDB.get(0).getUsername(), token, userFromDB.get(0).getId())) {

                            tx = DBSession.beginTransaction();
                            userFromDB.get(0).setResetToken(BCrypt.hashpw(token, BCrypt.gensalt()));
                            userFromDB.get(0).setResetTokenLife(new Timestamp(System.currentTimeMillis() + 21600000));
                            userFromDB.get(0).setRecovering(true);
                            DBSession.saveOrUpdate(userFromDB.get(0));
                            DBSession.flush();
                            tx.commit();

                            return badRequest(views.html.unauthorized.render(UserMessages.USER_PASSWORD_RESET_REQUEST));

                        }
                    } else {
                        return badRequest(views.html.unauthorized.render(UserMessages.ERROR_ACCOUNT_NOT_ACTIVATED));
                    }

                } else {
                    return badRequest(views.html.unauthorized.render(UserMessages.ERROR_ACCOUNT_DOESNT_EXIST));
                }

            } catch (HibernateException e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }
        } else {
            return badRequest(views.html.unauthorized.render(printValidationErrors(emailRequest.errors())));
        }

        return badRequest(views.html.unauthorized.render("Error"));

    }

    public Result passwordReset2(String token, Long id) {

        //Authorize the token and information
        switch (checkToken(token, id)) {
            case authorized:
                return ok(views.html.reset.render(token, id));
            case tokenExpired:
                return badRequest(views.html.unauthorized.render(UserMessages.ERROR_TOKEN_EXPIRED));
            case invalidToken:
                return badRequest(views.html.unauthorized.render("Invalid token, user doesn't exist or account is not recovering. ---CHANGE THIS ERROR IN THE FUTURE---"));
        }

        return badRequest(views.html.unauthorized.render("Error occurred"));

    }

    public Result resetPassword(String token, long id) {

        Form<ConfirmPassword> passwordReset = formFactory.form(ConfirmPassword.class).bindFromRequest();

        //Check that password and confirm password match
        if ((!passwordReset.hasErrors()) && Objects.equals(passwordReset.get().getPassword(), passwordReset.get().getConfirmPassword())) {

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

                        return ok(views.html.index.render(UserMessages.USER_NEW_PASSWORD_SET_SUCCESS));

                    } catch (HibernateException e) {
                        if (tx != null) tx.rollback();
                        e.printStackTrace();
                    } finally {
                        DBSession.close();
                    }

                case tokenExpired:
                    return badRequest(views.html.unauthorized.render(UserMessages.ERROR_TOKEN_EXPIRED));
                case invalidToken:
                    return badRequest(views.html.unauthorized.render(UserMessages.ERROR_INVALID_TOKEN));
            }
        } else {
            return badRequest(views.html.unauthorized.render(printValidationErrors(passwordReset.errors())));
        }

        return badRequest(views.html.unauthorized.render("ERROR"));

    }

    //Checks if a password reset request is allowed
    private passwordTokenAuthorizeResult checkToken(String token, long id) {

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
                        return passwordTokenAuthorizeResult.authorized;
                        //Token expired
                    } else {
                        return passwordTokenAuthorizeResult.tokenExpired;
                    }
                    //Invalid user or token
                } else {
                    return passwordTokenAuthorizeResult.invalidToken;
                }

            } catch (Exception e) {
                if (tx != null) tx.rollback();
                e.printStackTrace();
            } finally {
                DBSession.close();
            }
        }
        return passwordTokenAuthorizeResult.error;
    }

    private String printValidationErrors(Map<String, List<ValidationError>> formToValidate) {

        String errorMsg = "";

        for (String field : formToValidate.keySet()) {
            for (ValidationError error : formToValidate.get(field)) {
                errorMsg += error.message() + ", ";
            }
        }
        return errorMsg;

    }


}