import java.nio.file.*;
import java.sql.*;

public class RunSql {

    public static void main(String[] args) throws Exception {
        try (
            var c = DriverManager.getConnection(
                System.getenv("DB_URL"),
                System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")
            )
        ) {
            for (String f : args)
                try (var s = c.createStatement()) {
                    s.execute(Files.readString(Path.of(f)));
                    System.out.println("OK " + f);
                }
        }
    }
}
