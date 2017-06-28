package emails;

import com.sendgrid.*;
import java.io.IOException;

public class SendGridEmail {

    public static boolean sendAccountCreationEmail(String email, String token, long id) {

        Email from = new Email(System.getenv("FROM_EMAIL"));
        String subject = "Account Creation";
        Email to = new Email(email);
        Content content = new Content("text/plain", "Click this link to activate your account: http://localhost:9000/activate/" + token + "/" + id + "");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(System.getenv("SENDGRID_API_KEY"));

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            return true;
        } catch (IOException ex) {}

        return false;
    }

    public static boolean sendPasswordRequestEmail(String email, String token, long id) {

        Email from = new Email(System.getenv("FROM_EMAIL"));
        String subject = "Password Reset Request";
        Email to = new Email(email);
        Content content = new Content("text/plain", "Click this link to reset your password: http://localhost:9000/reset/" + token + "/" + id + "");
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(System.getenv("SENDGRID_API_KEY"));

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            return true;
        } catch (IOException ex) {}

        return false;
    }
}


