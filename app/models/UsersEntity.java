package models;

import play.data.validation.Constraints;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by jorda on 2017-06-07.
 */
@Entity
@Table(name = "users", schema = "userbase_db", catalog = "")
public class UsersEntity {
    private long id;
    private String username;
    private String password;
    private boolean isActivated;
    private boolean isRecovering;
    private int loginAttemptCount;
    private String resetToken;
    private Timestamp resetTokenLife;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Basic
    @Column(name = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Basic
    @Column(name = "password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Basic
    @Column(name = "isActivated")
    public boolean isActivated() {
        return isActivated;
    }

    public void setActivated(boolean activated) {
        isActivated = activated;
    }

    @Basic
    @Column(name = "isRecovering")
    public boolean isRecovering() {
        return isRecovering;
    }

    public void setRecovering(boolean recovering) {
        isRecovering = recovering;
    }

    @Basic
    @Column(name = "loginAttemptCount")
    public int getLoginAttemptCount() {
        return loginAttemptCount;
    }

    public void setLoginAttemptCount(int loginAttemptCount) {
        this.loginAttemptCount = loginAttemptCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UsersEntity that = (UsersEntity) o;

        if (id != that.id) return false;
        if (isActivated != that.isActivated) return false;
        if (isRecovering != that.isRecovering) return false;
        if (loginAttemptCount != that.loginAttemptCount) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (isActivated ? 1 : 0);
        result = 31 * result + (isRecovering ? 1 : 0);
        result = 31 * result + loginAttemptCount;
        return result;
    }

    @Basic
    @Column(name = "resetToken")
    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    @Basic
    @Column(name = "resetTokenLife")
    public Timestamp getResetTokenLife() {
        return resetTokenLife;
    }

    public void setResetTokenLife(Timestamp resetTokenLife) {
        this.resetTokenLife = resetTokenLife;
    }
}
