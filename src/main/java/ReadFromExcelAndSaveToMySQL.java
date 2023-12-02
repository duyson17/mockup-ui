import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;

public class ReadFromExcelAndSaveToMySQL {

    public static void readAndSaveToMySQL(String excelFilePath) {
        try {
    //Connect database control with jdbcUrl = "jdbc:mysql://127.0.0.1:3306/control", jdbcUsername = "root", jdbcPassword ="123456"
            String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/control";
            String jdbcUsername = "root";
            String jdbcPassword = "123456";
            Connection connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);

            // Check if table control is exists
            if (!doesTableExist(connection, "control")) {
                // create table control
                createControlTable(connection);
            }

            // Backup control table into backup_control using funtion backupControlTable
            backupControlTable(connection);

            // Đường dẫn đến tệp Excel
            FileInputStream inputStream = new FileInputStream(excelFilePath);
            Workbook workbook = new XSSFWorkbook(inputStream);

            // Truncate control table
            truncateControlTable(connection);

            // Check whether the data in the backup table and the data in the excel file are duplicates or not
            truncateAndInsertData(connection, workbook);

            // Backup control table
            backupControlTable(connection);

            // Đóng kết nối đến cơ sở dữ liệu MySQL
            connection.close();

            System.out.println("Dữ liệu đã được cập nhật vào MySQL từ tệp Excel thành công.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void truncateAndInsertData(Connection connection, Workbook workbook) {
        try {
            //Reading data from d:/DataWarehouse/News.xlsx in the "data" sheet
            Sheet sheet = workbook.getSheet("Data");
            // create for loop get a line of data
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                String title = dataRow.getCell(0).getStringCellValue();

                // If there are no duplicates, Insert data into the control table
                if (!doesDataExistInBackup(connection, title)) {
                    String eventText = dataRow.getCell(1).getStringCellValue();
                    String sourceText = dataRow.getCell(2).getStringCellValue();
                    String event = dataRow.getCell(3).getStringCellValue();
                    String sources = dataRow.getCell(4).getStringCellValue();
                    String topic = dataRow.getCell(5).getStringCellValue();
                    String pText = dataRow.getCell(6).getStringCellValue();
                    String imageUrl = dataRow.getCell(7).getStringCellValue();

                    String insertQuery = "INSERT INTO control (Title, DateTime, LinkSource, Event, Source, Topic, Content, ImageURL) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
                    preparedStatement.setString(1, title);
                    preparedStatement.setString(2, eventText);
                    preparedStatement.setString(3, sourceText);
                    preparedStatement.setString(4, event);
                    preparedStatement.setString(5, sources);
                    preparedStatement.setString(6, topic);
                    preparedStatement.setString(7, pText);
                    preparedStatement.setString(8, imageUrl);

                    preparedStatement.executeUpdate();
                    preparedStatement.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void backupControlTable(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            String backupQuery = "INSERT INTO backup_control (Title, DateTime, LinkSource, Event, Source, Topic, Content, ImageURL) SELECT Title, DateTime, LinkSource, Event, Source, Topic, Content, ImageURL FROM control";
            statement.executeUpdate(backupQuery);
            statement.close();
            System.out.println("Dữ liệu từ bảng control đã được sao lưu.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean doesDataExistInBackup(Connection connection, String title) {
        try {
            String selectQuery = "SELECT Title FROM backup_control WHERE Title = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(selectQuery);
            preparedStatement.setString(1, title);
            ResultSet resultSet = preparedStatement.executeQuery();
            boolean dataExists = resultSet.next();
            resultSet.close();
            preparedStatement.close();
            return dataExists;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean doesTableExist(Connection connection, String tableName) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, null, tableName, null);
            return resultSet.next();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void createControlTable(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            String createTableQuery = "CREATE TABLE control (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "Title VARCHAR(255)," +
                    "DateTime VARCHAR(255)," +
                    "LinkSource VARCHAR(255)," +
                    "Event VARCHAR(255)," +
                    "Source VARCHAR(255)," +
                    "Topic VARCHAR(255)," +
                    "Content TEXT," +
                    "ImageURL VARCHAR(255)," +
                    "Timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            statement.executeUpdate(createTableQuery);
            statement.close();
            System.out.println("Bảng control đã được tạo thành công.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void truncateControlTable(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            String truncateQuery = "TRUNCATE TABLE control";
            statement.executeUpdate(truncateQuery);
            statement.close();
            System.out.println("Bảng control đã được truncate.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
