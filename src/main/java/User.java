import java.sql.Timestamp;
import org.apache.commons.validator.routines.EmailValidator;
import java.util.List;
import org.sql2o.*;


public class User extends Timestamped {

    // variables

    private String email;
    private String username;
    private String passwordHash;
    private String name;

    // constructors

    public User(String email, String username, String password, String name) {
        this.setEmail(email);
        this.setUsername(username);
        this.setPassword(password);
        this.setName(name);
    }

    public User(String name) {
        this.setEmail(name + "@example.com");
        this.setUsername(name);
        this.setPassword(name);
        this.setName(name);
    }

    // getters and setters

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        EmailValidator validator = EmailValidator.getInstance();
        if (validator.isValid(email)) {
            this.email = email;
        } else {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        if (username.matches("[A-Za-z0-9_.-]+")) {
            this.username = username;
        } else {
            throw new IllegalArgumentException("Invalid username");
        }
    }

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (!passwordHash.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")) {
            throw new IllegalArgumentException("Invalid password hash, regex fail");
        }
        if (passwordHash.length() != 44) {
            throw new IllegalArgumentException("Invalid password hash, not 44 length");
        }
        this.passwordHash = passwordHash;
    }

    public void setPassword(String password) {
        this.setPasswordHash(Utils.sha256Base64(password));
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // operators

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof User)) return false;
        User user = (User) obj;
        if (!this.email.equals(user.getEmail())) return false;
        if (!this.username.equals(user.getUsername())) return false;
        if (!this.passwordHash.equals(user.getPasswordHash())) return false;
        if (!this.name.equals(user.getName())) return false;
        if (this.id != user.getId()) return false;
        return true;
    }

    // methods

    public boolean checkPassword(String password) {
        String newHash = Utils.bytesToBase64(Utils.sha256(password));
        return newHash.equals(this.passwordHash);
    }

    public User save() {
        try (Connection con = DB.sql2o.open()) {
            String sql = "INSERT INTO users (email, username, passwordHash, name)"
                + "VALUES (:email, :username, :passwordHash, :name)";
            this.id = con.createQuery(sql, true)
                .bind(this)
                .executeUpdate()
                .getKey(int.class);
            return User.findById(this.id);
        }
    }

    public void delete() {
        try (Connection con = DB.sql2o.open()) {
            String sql = "DELETE FROM users WHERE id=:id";
            con.createQuery(sql)
                .addParameter("id", this.id)
                .executeUpdate();
            this.id = 0;
        }
    }

    // relations lookup

    public List<Question> getQuestions() {
        try (Connection con = DB.sql2o.open()) {
            String sql = "SELECT * FROM questions WHERE userId=:id";
            return con.createQuery(sql).bind(this).executeAndFetch(Question.class);
        }
    }

    public List<Set> getSets() {
        try (Connection con = DB.sql2o.open()) {
            String sql = "SELECT * FROM sets WHERE userId=:id";
            return con.createQuery(sql).bind(this).executeAndFetch(Set.class);
        }
    }

    // static methods

    public static List<User> all() {
        try (Connection con = DB.sql2o.open()) {
            String sql = "SELECT * FROM users";
            return con.createQuery(sql).executeAndFetch(User.class);
        }
    }

    public static User findById(int id) {
        try (Connection con = DB.sql2o.open()) {
            String sql = "SELECT * FROM users WHERE id=:id";
            return con.createQuery(sql)
                .addParameter("id", id)
                .executeAndFetchFirst(User.class);
        }
    }
}