package pckg;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author tsanda
 */
public class PocitacApp {

    static String version = "0.1";
    public static String[] firmy;
    static Connection con = null;
    static Statement stmt = null;
    static ResultSet rs = null;

    static Logger logger;

    public static String[] getFirmy() {
        return firmy;
    }

    public static void connect() {
        String[] tempFirmy = new String[2000];

        try {
            Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");

            String driver = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};";
            String datab = "Data\\test.mdb;";

            // Pouziti cestiny pro JDBC-ODBC driver
            Properties prop = new Properties();
            prop.put("charSet", "cp1250");
            prop.put("user", "");
            prop.put("password", "");

            String url = driver + "DBQ=" + datab;

            // create connection to database using connection string
            con = DriverManager.getConnection(url, prop);
            // setup java.sql.Statement to run queries
            stmt = con.createStatement();
            System.out.println("Pripojeno do databaze " + datab);
            String SQL = "SELECT * from firmy";
            rs = stmt.executeQuery(SQL);
            int i = 0;

            while (rs.next()) {
                tempFirmy[i] = rs.getString("Firma");
                i++;
            }

            // prekopiroj docasne pole firem do finalniho
            firmy = new String[i];
            System.arraycopy(tempFirmy, 0, firmy, 0, firmy.length);

        } catch (ClassNotFoundException ex) {
            System.out.println("EX");
            Logger.getLogger(PocitacApp.class.getName()).log(Level.SEVERE, null, ex);
            logger.log(Level.WARNING, ex.getMessage());
        } catch (SQLException ex) {
            System.out.println("SQLE");
            Logger.getLogger(PocitacApp.class.getName()).log(Level.SEVERE, null, ex);
            logger.log(Level.WARNING, ex.toString());
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            logger = Logger.getLogger("MyLog");
            FileHandler fh;
            // This block configure the logger with handler and formatter
            fh = new FileHandler("c:\\MyLogFile.log", true);
            logger.addHandler(fh);
            logger.setLevel(Level.ALL);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            
            connect();

            new PocitacView();

        } catch (IOException ex) {
            Logger.getLogger(PocitacApp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(PocitacApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
