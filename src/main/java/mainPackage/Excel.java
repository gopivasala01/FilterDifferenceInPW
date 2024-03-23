package mainPackage;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Excel {

    public static void main(String[] args) {
        // Generate Excel with data
        Workbook workbook = writeDataToExcel();

        // Analyze dropdown values for all sheets and send email
        StringBuilder emailContent = new StringBuilder();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            analyzeDropdownValues(sheet, emailContent);
        }

        // Send email with all differences and attach Excel file
        sendEmail(emailContent.toString());
    }

    public static Workbook writeDataToExcel() {
        Workbook workbook = new XSSFWorkbook();
        final String CONNECTION_URL = "jdbc:sqlserver://azrsrv001.database.windows.net;databaseName=HomeRiverDB;user=service_sql02;password=xzqcoK7T;encrypt=true;trustServerCertificate=true;";

        try (Connection connection = DriverManager.getConnection(CONNECTION_URL)) {
            // Create summary sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            Row summaryHeaderRow = summarySheet.createRow(0);
            summaryHeaderRow.createCell(0).setCellValue("Sheet Name");
            summaryHeaderRow.createCell(1).setCellValue("Report Alias Name");

            // Fetch unique reportAliasName values from the database
           // String uniqueReportAliasNamesQuery = "SELECT DISTINCT ReportAliasName FROM Staging.Reportprocess WHERE IsActive = 1 AND (FilterValidationThroughAutomation = 1 OR FilterValidationThroughAutomation IS NULL) ORDER BY ReportAliasName";
            String uniqueReportAliasNamesQuery = "	SELECT ReportID, CompanyName, ReportName, ReportAliasName, ReportURL , FilterValidationThroughAutomation, FilterValueInPW\r\n"
            		+ "	FROM Staging.Reportprocess \r\n"
            		+ "	WHERE IsActive = 1 \r\n"
            		+ "	  AND (FilterValidationThroughAutomation = 1 OR FilterValidationThroughAutomation IS NULL) \r\n"
            		+ " AND ReportAliasName = '*Bulk - Prospects'	\r\n"
            		+ "	ORDER BY ReportAliasName, CompanyName;";

            try (PreparedStatement uniqueStatement = connection.prepareStatement(uniqueReportAliasNamesQuery);
                 ResultSet uniqueResultSet = uniqueStatement.executeQuery()) {

                int sheetIndex = 0;
                while (uniqueResultSet.next()) {
                    String reportAliasName = uniqueResultSet.getString("ReportAliasName");
                    String sheetName = "Sheet" + (sheetIndex + 1);
                    Sheet currentSheet = workbook.createSheet(sheetName);

                    // Create headers for the current sheet
                    Row headers = currentSheet.createRow(0);
                    headers.createCell(0).setCellValue("ReportID");
                    headers.createCell(1).setCellValue("CompanyName");
                    headers.createCell(2).setCellValue("ReportAliasName");
                    headers.createCell(3).setCellValue("ReportURL");
                    headers.createCell(4).setCellValue("FilterValidationThroughAutomation");
                    headers.createCell(5).setCellValue("FilterValueInPW");

                    // Fetch data for the current reportAliasName
                    String dataQuery = "SELECT ReportID, CompanyName, ReportAliasName, ReportName, ReportURL, FilterValidationThroughAutomation, FilterValueInPW FROM Staging.Reportprocess WHERE IsActive = 1 AND (FilterValidationThroughAutomation = 1 OR FilterValidationThroughAutomation IS NULL) AND ReportAliasName = ?";
                    try (PreparedStatement dataStatement = connection.prepareStatement(dataQuery)) {
                        dataStatement.setString(1, reportAliasName);
                        try (ResultSet resultSet = dataStatement.executeQuery()) {
                            // Write data to the current sheet
                            int rowNum = 1;
                            while (resultSet.next()) {
                                Row row = currentSheet.createRow(rowNum++);
                                row.createCell(0).setCellValue(resultSet.getString("ReportID"));
                                row.createCell(1).setCellValue(resultSet.getString("CompanyName"));
                                row.createCell(2).setCellValue(resultSet.getString("ReportAliasName"));
                                row.createCell(3).setCellValue(resultSet.getString("ReportURL"));
                                row.createCell(4).setCellValue(resultSet.getInt("FilterValidationThroughAutomation"));
                                row.createCell(5).setCellValue(resultSet.getString("FilterValueInPW"));
                            }
                        }
                    }

                    // Add entry to the summary sheet
                    Row summaryRow = summarySheet.createRow(sheetIndex + 1);
                    summaryRow.createCell(0).setCellValue(sheetName);
                    summaryRow.createCell(1).setCellValue(reportAliasName);

                    sheetIndex++;
                }
            }

            // Write the workbook to a file
            try (FileOutputStream fileOut = new FileOutputStream("C:\\SantoshMurthyP\\Lease Audit Automation\\report_data.xlsx")) {
                workbook.write(fileOut);
                System.out.println("Excel file generated successfully.");
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        return workbook;
    }

 
  
    public static void analyzeDropdownValues(Sheet sheet, StringBuilder emailContent) {
        if (sheet == null) {
            return;
        }

        Map<String, Integer> filterValueCounts = new HashMap<>();
        Map<String, Integer> previousFilterValueCounts = new HashMap<>();

        // Count occurrences of values in FilterValueInPW (5th column) and PreviousFilterValueInPW (6th column)
        countValueOccurrences(sheet, 5, filterValueCounts);
        countValueOccurrences(sheet, 6, previousFilterValueCounts);

        // Find minority values
        String minorityFilterValue = findMinorityValue(filterValueCounts);
        String minorityPreviousFilterValue = findMinorityValue(previousFilterValueCounts);

        // Iterate through each row to compare FilterValueInPW and PreviousFilterValueInPW for the minority values
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                String reportID = getStringValue(row.getCell(0)); // Assuming ReportID is at column index 0
                String filterValueInPW = getStringValue(row.getCell(5)); // Assuming FilterValueInPW is at column index 5
                String previousFilterValueInPW = getStringValue(row.getCell(6)); // Assuming PreviousFilterValueInPW is at column index 6

                if (filterValueInPW.equals(minorityFilterValue) && previousFilterValueInPW.equals(minorityPreviousFilterValue)) {
                    // Append details to emailContent
                    if (!filterValueInPW.equals(previousFilterValueInPW)) {
                        String companyName = getStringValue(row.getCell(1));
                        String reportAliasName = getStringValue(row.getCell(2));
                        appendToEmail(emailContent, reportAliasName, companyName, filterValueInPW);
                    }
                }
            }
        }
    }

    private static String getStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else {
            return "";
        }
    }

    private static void countValueOccurrences(Sheet sheet, int columnIndex, Map<String, Integer> valueCounts) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String value = cell.getStringCellValue();
                    valueCounts.put(value, valueCounts.getOrDefault(value, 0) + 1);
                }
            }
        }
    }

    private static String findMinorityValue(Map<String, Integer> valueCounts) {
        int minCount = Integer.MAX_VALUE;
        String minorityValue = "";
        for (Map.Entry<String, Integer> entry : valueCounts.entrySet()) {
            if (entry.getValue() < minCount) {
                minCount = entry.getValue();
                minorityValue = entry.getKey();
            }
        }
        return minorityValue;
    }

    private static void appendToEmail(StringBuilder emailContent, String reportAliasName, String companyName, String filterValueInPW) {
        emailContent.append("reportAliasName: ").append(reportAliasName).append("\n");
        emailContent.append("companyName: ").append(companyName).append("\n");
        emailContent.append("FilterValueInPW: ").append(filterValueInPW).append("\n");
        emailContent.append("........................................................................................................\n");
    }

    public static void sendEmail(String emailContent) {
        // Email configuration
        String smtpHost = "smtp.office365.com";
        String smtpPort = "587";
        String emailFrom = "santosh.p@beetlerim.com";
        String emailTo = "santosh.p@beetlerim.com";
        //String emailTo = "santosh.p@beetlerim.com , gopi.v@beetlerim.com , ratna@beetlerim.com , dahoffman@homeriver.com ";
        String emailSubject = "Different Value Detected";
        String emailBody = emailContent;

        // SMTP authentication information
        final String username = "santosh.p@beetlerim.com";
        final String password = "Welcome@123";

        // Set properties
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        // Create session
        Session session = null;
        try {
            session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
            message.setSubject(emailSubject);
            message.setText(emailBody);

            // Send message
            Transport.send(message);
            System.out.println("Email sent successfully!");
        } catch (AuthenticationFailedException e) {
            System.out.println("Authentication failed. Please check your username and password.");
            e.printStackTrace();
        } catch (MessagingException e) {
            System.out.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close the session
            if (session != null) {
                try {
                    session.getTransport().close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
