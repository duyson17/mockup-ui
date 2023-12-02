import java.sql.*;

public class DataWarehouse {
    //Connect database control with jdbcUrl = "jdbc:mysql://127.0.0.1:3306/control", jdbcUsername = "root", jdbcPassword ="123456"
    public static void LoadDataWarehouse() {
        String controlJdbcUrl = "jdbc:mysql://127.0.0.1:3306/control";
        String controlUsername = "root";
        String controlPassword = "123456";

        //Connect database datawarehouse with jdbcUrl = "jdbc:mysql://127.0.0.1:3306/control", jdbcUsername = "root", jdbcPassword ="123456"
        String dataWarehouseDbUrl = "jdbc:mysql://127.0.0.1:3306/datawarehouse";
        String datawarehouseUsername = "root";
        String datawarehouselPassword = "123456";

        try {
            Connection controlConnection = DriverManager.getConnection(controlJdbcUrl, controlUsername, controlPassword);
           // Get data in control table
            String selectControlQuery = "SELECT * FROM control";
            Statement controlStatement = controlConnection.createStatement();
            ResultSet controlResultSet = controlStatement.executeQuery(selectControlQuery);

            Connection dataWarehouseConnection = DriverManager.getConnection(dataWarehouseDbUrl, datawarehouseUsername, datawarehouselPassword);
//            String selectDatawarehouseQuery = "SELECT * FROM news";
//            Statement DatawarehouseStatement = dataWarehouseConnection.createStatement();
//            ResultSet DatawarehouseResultSet = DatawarehouseStatement.executeQuery(selectDatawarehouseQuery);

            // Insert data from control table to TopicsDetail, Date_dim, News, NewsTopics
            String insertDateDimQuery = "INSERT INTO date_dim (Year, Month, day_of_month, day_name, Hour, Minute, Period) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement dateDimStatement = dataWarehouseConnection.prepareStatement(insertDateDimQuery, Statement.RETURN_GENERATED_KEYS);

            String insertNewsQuery = "INSERT INTO news (Title, Datetime_ID, LinkSource, Event, Source, Content, LinkImage) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement newsStatement = dataWarehouseConnection.prepareStatement(insertNewsQuery, Statement.RETURN_GENERATED_KEYS);

            String insertNewstopicQuery = "INSERT INTO Newstopic (newID, Topic_ID, create_by, update_by) VALUES (?, ?, ?, ?)";
            PreparedStatement newstopicStatement = dataWarehouseConnection.prepareStatement(insertNewstopicQuery);

            String insertTopicDetailQuery = "INSERT INTO TopicDetail (Name, create_by, update_by) VALUES (?, ?, ?)";
            PreparedStatement topicDetailStatement = dataWarehouseConnection.prepareStatement(insertTopicDetailQuery, Statement.RETURN_GENERATED_KEYS);

            // Duyệt qua kết quả từ bảng control và insert vào bảng TopicDetail và News
            while (controlResultSet.next()) {
                String topic = controlResultSet.getString("Topic");

                // Kiểm tra xem chủ đề đã tồn tại trong TopicDetail chưa
                String checkTopicQuery = "SELECT ID FROM TopicDetail WHERE Name = ?";
                try (PreparedStatement checkTopicStatement = dataWarehouseConnection.prepareStatement(checkTopicQuery)) {
                    checkTopicStatement.setString(1, topic);
                    ResultSet existingTopicResultSet = checkTopicStatement.executeQuery();

                    int topicId;
                    if (existingTopicResultSet.next()) {
                        // Chủ đề đã tồn tại, lấy ID để sử dụng khi insert vào bảng news
                        topicId = existingTopicResultSet.getInt("ID");
                    } else {
                        // Chủ đề chưa tồn tại trong TopicDetail, chèn mới và lấy ID
                        topicDetailStatement.setString(1, topic);
                        topicDetailStatement.setString(2, null);  // create_by, set to null or fill with any desired value
                        topicDetailStatement.setString(3, null);  // update_by, set to null or fill with any desired value
                        topicDetailStatement.executeUpdate();

                        try (ResultSet generatedKeys = topicDetailStatement.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                topicId = generatedKeys.getInt(1);
                            } else {
                                throw new SQLException("Creating TopicDetail failed, no ID obtained.");
                            }
                        }
                    }

                    Timestamp timestamp = controlResultSet.getTimestamp("Timestamp");

                    dateDimStatement.setInt(1, timestamp.toLocalDateTime().getYear());
                    dateDimStatement.setInt(2, timestamp.toLocalDateTime().getMonthValue());
                    dateDimStatement.setInt(3, timestamp.toLocalDateTime().getDayOfMonth());
                    dateDimStatement.setString(4, timestamp.toLocalDateTime().getDayOfWeek().toString());
                    dateDimStatement.setInt(5, timestamp.toLocalDateTime().getHour());
                    dateDimStatement.setInt(6, timestamp.toLocalDateTime().getMinute());
                    dateDimStatement.setString(7, timestamp.toLocalDateTime().getHour() < 12 ? "AM" : "PM");

                    // Thực hiện insert và lấy id cho date_dim
                    int dateDimId;
                    dateDimStatement.executeUpdate();
                    try (ResultSet generatedKeys = dateDimStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            dateDimId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Creating date_dim failed, no ID obtained.");
                        }
                    }

                    String Title = controlResultSet.getString("Title");
                    String LinkSource = controlResultSet.getString("LinkSource");
                    String Event = controlResultSet.getString("Event");
                    String Source = controlResultSet.getString("Source");
                    String Content = controlResultSet.getString("Content");
                    String LinkImage = controlResultSet.getString("ImageURL");

                    // Thực hiện insert vào bảng news
                    newsStatement.setString(1, Title);
                    newsStatement.setInt(2, dateDimId);
                    newsStatement.setString(3, LinkSource);
                    newsStatement.setString(4, Event);
                    newsStatement.setString(5, Source);
                    newsStatement.setString(6, Content);
                    newsStatement.setString(7, LinkImage);
                    newsStatement.executeUpdate();

// Lấy ID của news vừa mới insert
                    int newId;
                    try (ResultSet generatedKeys = newsStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            newId = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("Creating news failed, no ID obtained.");
                        }
                    }
                    newstopicStatement.setInt(1,newId);
                    newstopicStatement.setInt(2,topicId);
                    newstopicStatement.setString(3,"Ly");
                    newstopicStatement.setString(4,"Ly");

                    newstopicStatement.executeUpdate();
                }
            }


            // Đóng tất cả các kết nối
            controlResultSet.close();
            controlStatement.close();
            topicDetailStatement.close();
            newsStatement.close();
            newstopicStatement.close();
            dateDimStatement.close();
            dataWarehouseConnection.close();

            System.out.println("Dữ liệu đã được chuyển từ control.db sang DataWarehouse.db bảng date_dim và news thành công.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
