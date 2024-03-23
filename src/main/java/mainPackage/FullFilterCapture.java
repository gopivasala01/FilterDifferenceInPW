package mainPackage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

public class FullFilterCapture {

    static WebDriver driver;
    static Actions actions;
    static Connection conn;
    static Statement stmt;
    static ResultSet rs;
    static final String CONNECTION_URL = "jdbc:sqlserver://azrsrv001.database.windows.net;databaseName=HomeRiverDB;user=service_sql02;password=xzqcoK7T;encrypt=true;trustServerCertificate=true;";

    public static void main(String[] args) {
        try {
            FileUtils.cleanDirectory(new File("C:\\SantoshMurthyP\\Lease Audit Automation\\ReportExcel\\reports.xlsx"));
        } catch (Exception e) {}

        try {
            initializeBrowser();
            if (signIn()) {
                fetchDataFromDatabaseAndNavigate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    public static void initializeBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--incognito");
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.setPageLoadTimeout(Duration.ofSeconds(500));
        WebDriverManager.chromedriver().clearDriverCache().setup();
        driver = new ChromeDriver(options);
        actions = new Actions(driver);
        driver.manage().window().maximize();
    }

    public static boolean signIn() {
        try {
            driver.get(AppConfig.URL);
            driver.findElement(Locators.userName).sendKeys(AppConfig.username);
            driver.findElement(Locators.password).sendKeys(AppConfig.password);
            driver.findElement(Locators.signMeIn).click();
            return !isLoginErrorDisplayed();
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean isLoginErrorDisplayed() {
        try {
            return driver.findElement(Locators.loginError).isDisplayed(); 
        } catch (Exception e) {
            return false;
        }
    }

    public static void fetchDataFromDatabaseAndNavigate() throws IOException {
        try {
            conn = DriverManager.getConnection(CONNECTION_URL);
            String sqlSelect = "SELECT ReportID, CompanyName, ReportName, ReportAliasName, ReportURL, FilterValidationThroughAutomation, FilterValueInPW FROM Staging.Reportprocess WHERE IsActive = 1 AND ((FilterValidationThroughAutomation <> 1 OR FilterValidationThroughAutomation IS NULL) AND ReportAliasName <> '*Incremental - General Ledger (Current Month)' AND ReportAliasName <> '*Incremental - General Ledger (Last Month)') ORDER BY ReportAliasName, CompanyName";
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sqlSelect);
            List<String[]> resultSetData = new ArrayList<>();
            resultSetData.clear();
            rs.beforeFirst();
            while (rs.next()) {
                String[] rowData = {
                        rs.getString("ReportID"),
                        rs.getString("CompanyName"),
                        rs.getString("ReportName"),
                        rs.getString("ReportAliasName"),
                        rs.getString("ReportURL")
                };
                resultSetData.add(rowData);
            }

            for (String[] rowData : resultSetData) {
                String reportID = rowData[0];
                String companyName = rowData[1];
                String reportName = rowData[2];
                String reportAliasName = rowData[3];
                String reportURL = rowData[4];
                String number = extractNumberFromURL(reportURL);
                String finalURL = "https://app.propertyware.com/pw/reporting/reports.do?entityID=" + number;

                System.out.println("CompanyName: " + companyName);
                System.out.println("ReportName: " + reportName);
                System.out.println("ReportAliasName: " + reportAliasName);
                System.out.println("ReportURL: " + reportURL);
                System.out.println("Final URL with extracted number: " + finalURL);

                try {
                    navigateToURL(finalURL, companyName, reportName, reportAliasName, reportURL, reportID);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                System.out.println("---------------------------------------------");
            }
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void navigateToURL(String finalURL, String companyName, String reportName, String reportAliasName, String reportURL, String reportID) throws InterruptedException {
        try {
            driver.get(AppConfig.homeURL);
            switch (companyName) {
                case "California PFW":
                    companyName = "California pfw";
                    break;
                case "Institutional Accounts (IAG)":
                    companyName = "Institutional Accounts";
                    break;
                case "Chicago PFW":
                    companyName = "Chicago pfw";
                    break;
                case "Hawaii Kona":
                    companyName = "Kona (Legacy Hawaiian Dream)";
                    break;
                default:
                    break;
            }

            try {
                driver.findElement(Locators.marketDropdown).click();
                String marketName = "HomeRiver Group - " + companyName;
                Select marketDropdownList = new Select(driver.findElement(Locators.marketDropdown));
                marketDropdownList.selectByVisibleText(marketName);
            } catch (Exception e) {
                try {
                    driver.findElement(Locators.marketDropdown).click();
                    String marketName = "z.Legacy - HomeRiver Group - " + companyName;
                    Select marketDropdownList = new Select(driver.findElement(Locators.marketDropdown));
                    marketDropdownList.selectByVisibleText(marketName);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

            Thread.sleep(2000);
            actions.sendKeys(Keys.ESCAPE).build().perform();

            driver.get(finalURL);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(500));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@class='ext-el-mask-msg x-mask-loading']")));
            intermittentPopUp(driver);
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Filters']")));
            Thread.sleep(5000);
            // Click on 'Filters'
            driver.findElement(By.xpath("//button[text()='Filters']")).click();
            
            Thread.sleep(5000);
            extractTextAndInputValuesFromPopup(driver, reportID);
         
        } catch (Exception e) {
        	String dropdownText1 = "Failed to load the page";
            updateDatabase(reportID, dropdownText1);
            e.printStackTrace();
            // Handle any exceptions or errors
            // driver.quit();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--incognito"); // Open Chrome in incognito mode
            WebDriverManager.chromedriver().clearDriverCache().setup();
            driver = new ChromeDriver(options);
            options.setPageLoadStrategy(PageLoadStrategy.NORMAL); // Or PageLoadStrategy.EAGER if needed
            options.setPageLoadTimeout(Duration.ofSeconds(500));

            driver.manage().window().maximize();
            driver.get(AppConfig.URL);
            driver.findElement(Locators.userName).sendKeys(AppConfig.username);
            driver.findElement(Locators.password).sendKeys(AppConfig.password);
            driver.findElement(Locators.signMeIn).click();
        }
    }

    public static void extractTextAndInputValuesFromPopup(WebDriver driver, String reportID) {
        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        try {
            Thread.sleep(3000);
            Object result = jsExecutor.executeScript("return document.querySelectorAll('.x-panel-body.x-panel-body-noheader.x-panel-body-noborder.x-form *');");
            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<WebElement> elements = (List<WebElement>) result;
                boolean isInCustomFilterModeSection = false;
                boolean customFilterModeLabelPrinted = false;
                StringBuilder extractedValues = new StringBuilder();
                String checkedRadioLabel = null; // Store the label text for the checked radio button
                for (WebElement element : elements) {
                    if (element.isDisplayed()) {
                        String text = element.getText().trim();
                        String inputValue = element.getAttribute("value");
                        if ("label".equalsIgnoreCase(element.getTagName())) {
                            System.out.println("Label: " + text);
                            extractedValues.append("Label: ").append(text).append("\n");
                        } else if ("input".equalsIgnoreCase(element.getTagName())) {
                            if ("checkbox".equalsIgnoreCase(element.getAttribute("type"))) {
                                if (element.isSelected()) {
                                    System.out.println("Toggle Button: " + text + " (Checked)");
                                    extractedValues.append("Toggle Button: ").append(text).append(" (Checked)\n");
                                } else {
                                    System.out.println("Toggle Button: " + text + " (Unchecked)");
                                    extractedValues.append("Toggle Button: ").append(text).append(" (Unchecked)\n");
                                }
                            } else if ("radio".equalsIgnoreCase(element.getAttribute("type"))) {
                                if (element.isSelected()) {
                                    checkedRadioLabel = text; // Store the label text for the checked radio button
                                    System.out.println("Radio Button: " + text + " (Checked)");
                                    extractedValues.append("Radio Button: ").append(text).append(" (Checked)\n");
                                }
                            } else {
                                System.out.println("Input: " + inputValue);
                                extractedValues.append("Input: ").append(inputValue).append("\n");
                            }
                        }
                        if ("Custom Filter Mode".equalsIgnoreCase(text)) {
                            isInCustomFilterModeSection = true;
                            if (!customFilterModeLabelPrinted) {
                                System.out.println("Label: Custom Filter Mode");
                                extractedValues.append("Label: Custom Filter Mode\n");
                                customFilterModeLabelPrinted = true;
                            }
                        } else if (isInCustomFilterModeSection && "table".equalsIgnoreCase(element.getTagName())) {
                            List<WebElement> rows = element.findElements(By.tagName("tr"));
                            extractedValues.append("Custom Filter Mode Table:\n");
                            for (WebElement row : rows) {
                                List<WebElement> cells = row.findElements(By.tagName("td"));
                                for (WebElement cell : cells) {
                                    System.out.print(cell.getText() + "\t");
                                    extractedValues.append(cell.getText()).append("\t");
                                }
                                System.out.println();
                                extractedValues.append("\n");
                            }
                        }
                    }
                }
                // Print the label of the checked radio button along with its status
               
                updateDatabase(reportID, extractedValues.toString());
            } else {
                System.out.println("Error: Script did not return a list of WebElements.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void updateDatabase(String reportID, String extractedValues) {
        try {
            String sqlUpdate = "UPDATE Staging.ReportProcess SET FilterValueInPW = ?, FilterValidationThroughAutomation = 1 WHERE ReportID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sqlUpdate);
            pstmt.setString(1, extractedValues);
            pstmt.setString(2, reportID);
            int rowsAffected = pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static String extractNumberFromURL(String url) {
        Pattern pattern = Pattern.compile("/(\\d+)/");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static void intermittentPopUp(WebDriver driver) {
        try {
            driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
            WebElement container = driver.findElement(By.id("viewReportExpiryContainer"));
            if (container.isDisplayed()) {
                WebElement inputElement = container.findElement(By.xpath("./div[2]/input"));
                inputElement.click();
            }
            WebElement unableToGetDatePopup = driver.findElement(By.xpath("//div[@class=' x-window x-window-plain x-window-dlg']"));
            if (unableToGetDatePopup.isDisplayed()) {
                WebElement inputElement = unableToGetDatePopup.findElement(By.xpath("//button[text()='OK']"));
                inputElement.click();
            }
        } catch (Exception ignored) {
        }
    }
}
