package mainPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.util.*;

public class SummarySheet {

    public static void main(String[] args) {
        try {
            fetchDataFromDatabaseAndNavigate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fetchDataFromDatabaseAndNavigate() throws IOException {
        String CONNECTION_URL = "jdbc:sqlserver://azrsrv001.database.windows.net;databaseName=HomeRiverDB;user=service_sql02;password=xzqcoK7T;encrypt=true;trustServerCertificate=true;";

        File existingWorkbookFile = new File("C:\\SantoshMurthyP\\Lease Audit Automation\\reports123.xlsx");
        if (existingWorkbookFile.exists()) {
            existingWorkbookFile.delete();
            System.out.println("Existing workbook deleted successfully.");
        }

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        Workbook workbook = null;

        try {
            conn = DriverManager.getConnection(CONNECTION_URL);
            String sql = "SELECT CompanyName, ReportName, ReportAliasName, ReportURL FROM Staging.Reportprocess WHERE  IsActive = 1 ORDER BY ReportAliasName , CompanyName";
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);

            if (existingWorkbookFile.exists()) {
                FileInputStream inputStream = new FileInputStream(existingWorkbookFile);
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new XSSFWorkbook();
            }

            Set<String> reportAliasNames = new HashSet<>();
            while (rs.next()) {
                reportAliasNames.add(rs.getString("ReportAliasName").replaceAll("[*?/\\[\\]]", ""));
            }

            int sheetIndex = 0; // Initialize sheet index
            for (String aliasName : reportAliasNames) {
                Sheet summarySheet = workbook.createSheet("Sheet " + sheetIndex); // Create a new sheet
                Row headerRow = summarySheet.createRow(0); // Create header row
                headerRow.createCell(0).setCellValue("Company Name");
                headerRow.createCell(1).setCellValue("Report Name");
                headerRow.createCell(2).setCellValue("Report Alias Name");
                headerRow.createCell(3).setCellValue("Report URL");

                int rowNum = 1; // Start from row 1 for data

                // Iterate over ResultSet to fetch data
                rs.beforeFirst(); // Move cursor to before first row
                while (rs.next()) {
                    if (aliasName.equals(rs.getString("ReportAliasName").replaceAll("[*?/\\[\\]]", ""))) {
                        Row row = summarySheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(rs.getString("CompanyName"));
                        row.createCell(1).setCellValue(rs.getString("ReportName"));
                        row.createCell(2).setCellValue(rs.getString("ReportAliasName"));
                        row.createCell(3).setCellValue(rs.getString("ReportURL"));
                    }
                }

                sheetIndex++; // Increment sheet index
            }

            // Save the workbook after creating summary pages
            FileOutputStream outputStream = new FileOutputStream(existingWorkbookFile);
            workbook.write(outputStream);
            outputStream.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                    
                    
                        
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
