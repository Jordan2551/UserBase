package controllerTests;

import database.DB;
import models.UsersEntity;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;

/**
 * Created by jorda on 2017-06-08.
 */
public class AccountControllerTest {

    private Transaction tx = null;

    @Test
    public void testAccountCreation() {
        DB.getSession().close();
        Session session = DB.getSession();

        UsersEntity user = new UsersEntity();
        user.setUsername("jORDAN");
        user.setActivated(false);//If not created through social media
        user.setRecovering(false);
        user.setLoginAttemptCount(0);
        //TODO password encryption
        user.setPassword(controllers.BCrypt.hashpw("pizzapocket", controllers.BCrypt.gensalt()));

        try {

            session = DB.getSession();
            tx = session.beginTransaction();

            //Get id of newly saved restaurant
            session.save(user);

            tx.commit();

        } catch (HibernateException e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }


    }

}
