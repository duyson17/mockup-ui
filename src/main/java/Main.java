import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            // 1: Tải dữ liệu từ web và lưu vào Excel
            DownloadAndSaveToExcel.downloadAndSave();

            // 2: Đọc từ Excel và lưu vào bảng control
            ReadFromExcelAndSaveToMySQL.readAndSaveToMySQL("D://DataWarehouse/News.xlsx");

            // 3: Loading dữ liệu từ control vào bảng datawarehouse
            DataWarehouse.LoadDataWarehouse();

            System.out.println("Công việc đã hoàn thành.");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
