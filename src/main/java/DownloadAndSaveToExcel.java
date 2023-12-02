import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DownloadAndSaveToExcel {
    public static void downloadAndSave() {
        try {
            // Đường dẫn đến thư mục chứa tệp Excel
            String excelFolderPath = "D://DataWarehouse/";
            File excelFolder = new File(excelFolderPath);

            // Check if DataWarehouse folder exists
            if (!excelFolder.exists()) {
                // Create DataWarehouse folder
                excelFolder.mkdirs();
            }

            //  Read the content of the folder D://DataWarehouse/  and check if the DataWarehouse folder exists file News.xlsx
            String excelFilePath = excelFolderPath + "News.xlsx";
            File excelFile = new File(excelFilePath);
            Workbook workbook;

            if (excelFile.exists()) {
                FileInputStream inputStream = new FileInputStream(excelFilePath);
                workbook = new XSSFWorkbook(inputStream);
            } else {
                // If excel file is not exist ,
                //Create News.xlsx file whose sheet is data
                workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Data");

                // Create first row with fields Title, DateTime, LinkSource, Event, Source, Topic, Content, ImageURL
                Row headerRow = sheet.createRow(0);
                String[] columnHeaders = {"Title", "DateTime", "LinkSource", "Event", "Source", "Topic", "Content", "ImageURL"};
                for (int i = 0; i < columnHeaders.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columnHeaders[i]);
                }
            }

            // Get the next row to insert data according to the fields
            Sheet sheet = workbook.getSheet("Data");
            int startRow = sheet.getLastRowNum() + 1;

            // Get the link of the news page
            String url = "https://www.24h.com.vn/";

            // Use JSoup to connect and load website content
            Document document = Jsoup.connect(url).get();

            Elements divElements = document.select("div.col-4");

            // create for loop to get the div elements and get the content
            for (Element divElement : divElements) {
                // Lấy thẻ a trong div
                Element aElement = divElement.selectFirst("a");
                if (aElement != null) {
                    // Lấy href của thẻ a
                    String link = aElement.attr("href");

                    // Truy cập vào link và tải nội dung của trang con
                    Document topicDocument;
                    try {
                        // Thêm user-agent vào yêu cầu
                        topicDocument = Jsoup.connect(link)
                                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                .get();
                    } catch (HttpStatusException e) {
                        if (e.getStatusCode() == 503) {
                            // Xử lý trường hợp lỗi 503
                            System.out.println("Lỗi 503 - Tạm thời không khả dụng. Đang chờ...");
                            Thread.sleep(5000); // Chờ 5 giây
                            // Thử lại yêu cầu với user-agent
                            topicDocument = Jsoup.connect(link)
                                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                    .get();
                        } else {
                            throw e; // Ném lại nếu không phải là lỗi 503
                        }
                    }

                    // Lấy chủ đề từ trang con
                    String topic = topicDocument.selectFirst("title").text();

                    // Lấy thẻ span có class "tagsNameEvent" trong trang con
                    Element spanElement = topicDocument.selectFirst("time.cate-24h-foot-arti-deta-cre-post");
                    String eventText = "";
                    if (spanElement != null) {
                        // Lấy nội dung của thẻ a bên trong thẻ span này
                        eventText = spanElement.text();
                    }

                    // Lấy thông tin khác từ trang con
                    Element sourceElement = topicDocument.selectFirst("span#url_origin_cut");
                    String sourceText = sourceElement != null ? sourceElement.text() : "";

                    Element eventElement = topicDocument.selectFirst("div.cate-24h-foot-arti-deta-tags");
                    String event = eventElement != null ? eventElement.text() : "";

                    Element sourcesElement = topicDocument.selectFirst("div.nguontin");
                    String sources = sourcesElement != null ? sourcesElement.text() : "";

                    Element titleElement = topicDocument.selectFirst("a.active");
                    String title = titleElement != null ? titleElement.text() : "";

                    // Lấy các thẻ p từ trang con
                    Elements pElements = topicDocument.select("p");
                    StringBuilder pText = new StringBuilder();
                    for (Element pElement : pElements) {
                        pText.append(pElement.text()).append("\n");
                    }
                    if (pText.length() > 50) {
                        pText.setLength(50);
                    }
                    Element imgElement = topicDocument.selectFirst("img.news-image");
                    String imageUrl = imgElement != null ? imgElement.attr("src") : "";

                    // Tạo một hàng mới trong tệp Excel
                    Row row = sheet.createRow(startRow++);
                    // Insert data into columns in the News.xlsx file
                    Cell cellTopic = row.createCell(0);
                    cellTopic.setCellValue(topic);

                    Cell cellEvent = row.createCell(1);
                    cellEvent.setCellValue(eventText);

                    Cell cellSource = row.createCell(2);
                    cellSource.setCellValue(sourceText);

                    Cell cellSources = row.createCell(3);
                    cellSources.setCellValue(event);

                    Cell cell1 = row.createCell(4);
                    cell1.setCellValue(sources);

                    Cell cell2 = row.createCell(5);
                    cell2.setCellValue(title);

                    Cell cellContent = row.createCell(6);
                    cellContent.setCellValue(pText.toString());

                    Cell cellImage = row.createCell(7);
                    cellImage.setCellValue(imageUrl);


                }
            }

            // Save D://DataWarehouse/News
            FileOutputStream outputStream = new FileOutputStream(excelFilePath);
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();

            System.out.println("Dữ liệu đã được cập nhật vào tệp Excel thành công.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
