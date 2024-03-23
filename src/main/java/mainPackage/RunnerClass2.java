package mainPackage;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import io.github.bonigarcia.wdm.WebDriverManager;

public class RunnerClass2 {
	

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
        options.addArguments("--incognito"); // Open Chrome in incognito mode
        WebDriverManager.chromedriver().clearDriverCache().setup();
        driver = new ChromeDriver(options);
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL); // Or PageLoadStrategy.EAGER if needed
        options.setPageLoadTimeout(Duration.ofSeconds(500));
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

            // Query to fetch data
            String sqlSelect = "SELECT DISTINCT TOP 100\r\n"
            		+ "    ReportID,\r\n"
            		+ "    CompanyName,\r\n"
            		+ "    ReportName,\r\n"
            		+ "    ReportAliasName,\r\n"
            		+ "    ReportURL,\r\n"
            		+ "    FilterValidationThroughAutomation,\r\n"
            		+ "    FilterValueInPW\r\n"
            		+ "FROM\r\n"
            		+ "    Staging.Reportprocess\r\n"
            		+ "WHERE\r\n"
            		+ "    IsActive = 1\r\n"
            		+ "    AND (\r\n"
            		+ "        (FilterValidationThroughAutomation = 1 OR FilterValidationThroughAutomation IS NULL)\r\n"
            		+ "        AND ReportAliasName <> '*Incremental - General Ledger (Current Month)'\r\n"
            		+ "        AND ReportAliasName <> '*Incremental - General Ledger (Last Month)'\r\n"
            		+ "    )\r\n"
            		+ "    AND ReportID IN (1463,1364,1709,1371,1505,1270,1354,1374,1376,1312,1710,1421,1712,1369,1365,1379,1711,1373,1366,1378,1375,1250,1362,1370,1253,1377,1368,1372,1255,1367,1713)\r\n"
            		+ "ORDER BY\r\n"
            		+ "    ReportAliasName,\r\n"
            		+ "    CompanyName;\r\n"
            		+ "";

            // Execute query
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sqlSelect);

            // Populate resultSetData list
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

            // Process fetched data
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

                // Update FilterValidationThroughAutomation flag in the database

                System.out.println("---------------------------------------------");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Close resources
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



    public static void navigateToURL(String finalURL, String companyName, String reportName, String reportAliasName, String reportURL, String reportID) throws InterruptedException, IOException {

        try {
           
            driver.get(AppConfig.homeURL);
            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.ESCAPE).build().perform();

            // Adjust companyName if needed
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
                // Select the market dropdown
                driver.findElement(Locators.marketDropdown).click();
                String marketName = "HomeRiver Group - " + companyName;
                Select marketDropdownList = new Select(driver.findElement(Locators.marketDropdown));
                marketDropdownList.selectByVisibleText(marketName);
            } catch (Exception e) {
                // If the market dropdown selection fails, try alternative naming convention
                try {
                    driver.findElement(Locators.marketDropdown).click();
                    String marketName = "z.Legacy - HomeRiver Group - " + companyName;
                    Select marketDropdownList = new Select(driver.findElement(Locators.marketDropdown));
                    marketDropdownList.selectByVisibleText(marketName);
                } catch (Exception e1) {
                    // Handle any exceptions or errors during market dropdown selection
                    e1.printStackTrace();
                }
            }

            Thread.sleep(2000);
            actions.sendKeys(Keys.ESCAPE).build().perform(); // Handle pop-ups again
            intermittentPopUp(driver);

            // Access the final URL
            driver.get(finalURL);
            Thread.sleep(2000);

            String dropdownText1 = "";
            String displayClearFundDetails = "";
            String dropdownText2 = "";
            String dropdownText3 = "";
            String searchByAccountValue= "";

            // Check if permission denied message is displayed
            boolean permissionDenied = !driver.findElements(By.xpath("//h2[contains(text(),'You do not have permission to access the requested')]")).isEmpty();
            if (permissionDenied) {
                WebElement permissionDeniedElement = driver.findElement(By.xpath("//h2[contains(text(),'You do not have permission to access the requested')]"));
                if (permissionDeniedElement.isDisplayed()) {
                    String permissionDeniedText = permissionDeniedElement.getText();
                    System.out.println(permissionDeniedText);
                    dropdownText1 = "Wrong URL";
                    updateDatabase(reportID, dropdownText1);
                }
            } else {
                // Wait for elements to be visible
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(500));
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@class='ext-el-mask-msg x-mask-loading']")));
                intermittentPopUp(driver);
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Filters']")));

                // Click on 'Filters'
                driver.findElement(By.xpath("//button[text()='Filters']")).click();

                // Check if 'Falls Within' dropdown is present
                StringBuilder extractedValues = new StringBuilder(); // Initialize a StringBuilder to concatenate values
                boolean fallsWithinPresent = driver.findElements(By.xpath("(//div[@class='x-form-item '])/label[text()='Falls Within']")).size() > 0;
                boolean wherePresent = driver.findElements(By.xpath("(//div[@class='x-form-item '])/label[text()='Where']")).size() > 0;
                boolean calculateBalanceData = driver.findElements(By.xpath("//*[text()='Calculate Balances As Of']")).size() > 0;
                boolean searchByAccount = driver.findElements(By.xpath("//*[text()='Search for Accounts']")).size() > 0;
                boolean reportOptions = driver.findElements(By.xpath("//*[text()='Report Options']")).size() > 0;
                boolean displayClearFundDetailsappear = driver.findElements(By.xpath("//*[text()='Display Clear Funds Detail']")).size() > 0;
                boolean consolidatedappear = driver.findElements(By.xpath("//*[text()='Consolidated']")).size() > 0;
                boolean accountTypeFilterappear = driver.findElements(By.xpath("//*[text()='Account Type Filter']")).size() > 0;
                boolean propertyFilter = driver.findElements(By.xpath("//*[text()='Property Filters']")).size() > 0;
                boolean cashorAccrualAccountingBasisappears  = driver.findElements(By.xpath("//*[text()='Cash or Accrual Accounting Basis']")).size() > 0;
                boolean prePaymentsappears = driver.findElements(By.xpath("//*[text()='Prepayments']")).size() > 0;

                boolean daysVacantCalculationappears = driver.findElements(By.xpath("//*[text()='Days Vacant Calculation']/following::input[2]")).size() > 0;
                

                if (calculateBalanceData) {
                    try {
                        dropdownText3 = driver.findElement(By.xpath("//*[text()='Date ']/following::input[1]")).getAttribute("value");
                        System.out.println(dropdownText3);
                        extractedValues.append("Calculate Balance Data: ").append(dropdownText3).append("\n"); // Append to StringBuilder
                    } catch (Exception e) {
                        // Handle any exceptions or errors
                        e.printStackTrace();
                    }
                }

                if (fallsWithinPresent) {
                    try {
                        dropdownText1 = driver.findElement(By.xpath("//*[text()='Falls Within']/following::input[2]")).getAttribute("value");
                        System.out.println(dropdownText1);
                        if (dropdownText1.equals("Custom Date Range") || dropdownText1.equals("Yesterday")) {
                            dropdownText1 = "";
                        }
                        extractedValues.append("Falls Within: ").append(dropdownText1).append("\n"); // Append to StringBuilder
                    } catch (Exception e) {
                        // Handle any exceptions or errors
                        e.printStackTrace();
                    }
                }

                if (wherePresent) {
                    try {
                        dropdownText2 = driver.findElement(By.xpath("//*[text()='Where']/following::input[2]")).getAttribute("value");
                        System.out.println(dropdownText2);
                        extractedValues.append("Where: ").append(dropdownText2).append("\n"); // Append to StringBuilder
                    } catch (Exception e) {
                        // Handle any exceptions or errors
                        e.printStackTrace();
                    }
                }

                if (propertyFilter) {
                    if (cashorAccrualAccountingBasisappears) {
                        try {
                            String cashorAccrualAccountingBasis = driver.findElement(By.xpath("//*[text()='Cash or Accrual Accounting Basis']/following::input[2]")).getAttribute("value");
                            if (cashorAccrualAccountingBasis == null || cashorAccrualAccountingBasis.isEmpty()) {
                                cashorAccrualAccountingBasis = "No Value";
                            }
                            System.out.println(cashorAccrualAccountingBasis);
                            extractedValues.append("Cash or Accrual Accounting Basis: ").append(cashorAccrualAccountingBasis).append("\n"); // Append to StringBuilder
                        } catch (Exception e) {
                            // Handle any exceptions or errors
                            e.printStackTrace();
                        }
                    }

                    if (prePaymentsappears) {
                        try {
                            String prePayments = driver.findElement(By.xpath("//*[text()='Prepayments']/following::input[2]")).getAttribute("value");
                            if (prePayments == null || prePayments.isEmpty()) {
                                prePayments = "No Value";
                            }
                            System.out.println(prePayments);
                            extractedValues.append("Prepayments: ").append(prePayments).append("\n"); // Append to StringBuilder
                        } catch (Exception e) {
                            // Handle any exceptions or errors
                            e.printStackTrace();
                        }
                    }
                }


                try {
                    List<WebElement> divElements = driver.findElements(By.xpath("//*[@id='propertyFiltersRadio']/div/div/div/div[1]/div"));

                    for (int i = 0; i < divElements.size(); i++) {
                        WebElement divElement = divElements.get(i);
                        WebElement radioButton = divElement.findElement(By.tagName("input"));
                        if (radioButton.getAttribute("checked") != null) {
                            String selectedValue = radioButton.getAttribute("value");
                            System.out.println("Selected value: " + selectedValue);

                            // Construct the XPath dynamically with the index i
                            String labelXPath = String.format("//*[@id='propertyFiltersRadio']/div/div/div/div[1]/div[%d]/label", i + 1);

                            // Find the label associated with the checked radio button using the constructed XPath
                            WebElement labelElement = driver.findElement(By.xpath(labelXPath));
                            String labelText = labelElement.getText();
                            System.out.println("Label text: " + labelText);

                            //extractedValues.append("Selected value: ").append(selectedValue).append("\n"); // Append to StringBuilder
                            extractedValues.append("Property Filters: ").append(labelText).append("\n\n"); // Append to StringBuilder

                            break; // Exit the loop after finding the selected radio button and its label
                        }
                    }
                } catch (Exception e) {
                    // Handle any exceptions or errors
                    e.printStackTrace();
                }

                try {
                    if (searchByAccount) {
                        searchByAccountValue = driver.findElement(By.xpath("//*[text()='Search for Accounts']/following::input[1]")).getAttribute("value");
                        System.out.println(searchByAccountValue);
                        extractedValues.append("Search By Account Value: ").append(searchByAccountValue).append("\n"); // Append to StringBuilder
                    }
                } catch (Exception e) {
                    // Handle any exceptions or errors
                    e.printStackTrace();
                }

                try {
                    if (reportOptions) {
                        /*WebElement parentElement = driver.findElement(By.xpath("(//*[@class='x-fieldset-body'])[7]"));

                        // Check if parentElement is found and displayed
                        if (parentElement != null && parentElement.isDisplayed()) {
                            // Find all child elements within the parent element
                            List<WebElement> elements = parentElement.findElements(By.xpath(".//*"));

                            // Iterate through each element
                            for (WebElement element : elements) {
                                // Check if the element is displayed
                                if (element.isDisplayed()||element.isEnabled()||element.isSelected()) {
                                    // Get the text content of the element
                                    String elementText = element.getText().trim();

                                    // Print the text content
                                    System.out.println("Text: " + elementText);
                                }
                            }

                        }*/

                        if (displayClearFundDetailsappear) {
                            displayClearFundDetails = driver.findElement(By.xpath("//*[text()='Display Clear Funds Detail']/following::input[1]")).getAttribute("value");
                            if (displayClearFundDetails.equals("") || (displayClearFundDetails.equals(null))) {
                                displayClearFundDetails = "No Value";
                            }
                            System.out.println(displayClearFundDetails);
                            extractedValues.append("Display ClearFund Details: ").append(displayClearFundDetails).append("\n"); // Append to StringBuilder

                            String printed = driver.findElement(By.xpath("//*[text()='Printed']/following::input[2]")).getAttribute("value");
                            if (printed.equals("") || (printed.equals(null))) {
                                printed = "No Value";
                            }
                            System.out.println(printed);
                            extractedValues.append("Printed: ").append(printed).append("\n"); // Append to StringBuilder

                            String cleared = driver.findElement(By.xpath("//*[text()='Cleared']/following::input[2]")).getAttribute("value");
                            if (cleared.equals("") || (cleared.equals(null))) {
                                cleared = "No Value";
                            }
                            System.out.println(cleared);
                            extractedValues.append("Cleared: ").append(cleared).append("\n"); // Append to StringBuilder

                            String includeVideoedChecks = driver.findElement(By.xpath("//*[text()='Include Voided Checks']/following::input[2]")).getAttribute("value");
                            if (includeVideoedChecks.equals("") || (includeVideoedChecks.equals(null))) {
                                includeVideoedChecks = "No Value";
                            }
                            System.out.println(includeVideoedChecks);
                            extractedValues.append("Include Videoed Checks: ").append(includeVideoedChecks).append("\n"); // Append to StringBuilder

                            String payeeName = driver.findElement(By.xpath("//*[text()='Payee Name']/following::input[1]")).getAttribute("value");
                            if (payeeName.equals("") || (payeeName.equals(null))) {
                                payeeName = "No Value";
                            }
                            System.out.println(payeeName);
                            extractedValues.append("Payee Name: ").append(payeeName).append("\n"); // Append to StringBuilder

                            String payeeType = driver.findElement(By.xpath("//*[text()='Payee Type']/following::input[2]")).getAttribute("value");
                            if (payeeType.equals("") || (payeeType.equals(null))) {
                                payeeType = "No Value";
                            }
                            System.out.println(payeeType);
                            extractedValues.append("Payee Type: ").append(payeeType).append("\n"); // Append to StringBuilder

                            String checkNumberFrom = driver.findElement(By.xpath("//*[text()='Check Number']/following::input[1]")).getAttribute("value");
                            if (checkNumberFrom.equals("") || (checkNumberFrom.equals(null))) {
                                checkNumberFrom = "No Value";
                            }
                            System.out.println(checkNumberFrom);
                            extractedValues.append("Check Number From: ").append(checkNumberFrom).append("\n"); // Append to StringBuilder

                            String checkNumberTo = driver.findElement(By.xpath("//*[text()='Check Number']/following::input[2]")).getAttribute("value");
                            if (checkNumberTo.equals("") || (checkNumberTo.equals(null))) {
                                checkNumberTo = "No Value";
                            }
                            System.out.println(checkNumberTo);
                            extractedValues.append("Check Number To: ").append(checkNumberTo).append("\n"); // Append to StringBuilder

                            String checkAmountrFrom = driver.findElement(By.xpath("//*[text()='Check Amount']/following::input[1]")).getAttribute("value");
                            if (checkAmountrFrom.equals("") || (checkAmountrFrom.equals(null))) {
                                checkAmountrFrom = "No Value";
                            }
                            System.out.println(checkAmountrFrom);
                            extractedValues.append("Check Amount From: ").append(checkAmountrFrom).append("\n"); // Append to StringBuilder

                            String checkAmountrTo = driver.findElement(By.xpath("//*[text()='Check Amount']/following::input[2]")).getAttribute("value");
                            if (checkAmountrTo.equals("") || (checkAmountrTo.equals(null))) {
                                checkAmountrTo = "No Value";
                            }
                            System.out.println(checkAmountrTo);
                            extractedValues.append("Check Amount To: ").append(checkAmountrTo).append("\n"); // Append to StringBuilder

                            String printedOnFrom = driver.findElement(By.xpath("//*[text()='Printed On']/following::input[1]")).getAttribute("value");
                            if (printedOnFrom.equals("") || (printedOnFrom.equals(null))) {
                                printedOnFrom = "No Value";
                            }
                            System.out.println(printedOnFrom);
                            extractedValues.append("Printed On From: ").append(printedOnFrom).append("\n"); // Append to StringBuilder

                            String printedOnTo = driver.findElement(By.xpath("//*[text()='Printed On']/following::input[2]")).getAttribute("value");
                            if (printedOnTo.equals("") || (printedOnTo.equals(null))) {
                                printedOnTo = "No Value";
                            }
                            System.out.println(printedOnTo);
                            extractedValues.append("Printed On To: ").append(printedOnTo).append("\n"); // Append to StringBuilder*/

                        } else if (consolidatedappear) {
                            String consolidated = driver.findElement(By.xpath("//*[text()='Consolidated']/following::input[2]")).getAttribute("value");
                            if (consolidated.equals("") || (consolidated.equals(null))) {
                                consolidated = "No Value";
                            }
                            System.out.println(consolidated);
                            extractedValues.append("Consolidated: ").append(consolidated).append("\n"); // Append to StringBuilder*/

                            String accountswithnoactivity = driver.findElement(By.xpath("//*[text()='Hide $0 Accounts with no activity']/following::input[2]")).getAttribute("value");
                            if (accountswithnoactivity.equals("") || (accountswithnoactivity.equals(null))) {
                                accountswithnoactivity = "No Value";
                            }
                            System.out.println(accountswithnoactivity);
                            extractedValues.append("Hide $0 Accounts with no activity: ").append(accountswithnoactivity).append("\n"); // Append to StringBuilder*/

                            String summaryRow = driver.findElement(By.xpath("//*[text()='Hide $0 Accounts with no activity']/following::input[2]")).getAttribute("value");
                            if (summaryRow.equals("") || (summaryRow.equals(null))) {
                                summaryRow = "No Value";
                            }
                            System.out.println(summaryRow);
                            extractedValues.append("Hide $0 Summary Row: ").append(summaryRow).append("\n"); // Append to StringBuilder*/
                        }
                        else if(accountTypeFilterappear) {
                        	String accountTypeFilter = driver.findElement(By.xpath("//*[text()='Account Type Filter']/following::input[2]")).getAttribute("value");
                            if (accountTypeFilter.equals("") || (accountTypeFilter.equals(null))) {
                            	accountTypeFilter = "No Value";
                            }
                            System.out.println(accountTypeFilter);
                            extractedValues.append("Account Type Filter: ").append(accountTypeFilter).append("\n"); // Append to StringBuilder*/
                        	
                        }
                        else if(daysVacantCalculationappears) {
                        	String daysVacantCalculation = driver.findElement(By.xpath("//*[text()='Days Vacant Calculation']/following::input[2]")).getAttribute("value");
                            if (daysVacantCalculation.equals("") || (daysVacantCalculation.equals(null))) {
                            	daysVacantCalculation = "No Value";
                            }
                            System.out.println(daysVacantCalculation);
                            extractedValues.append("Days Vacant Calculation: ").append(daysVacantCalculation).append("\n"); // Append to StringBuilder*/
                        	
                        }
                        else {
                        	String excludeDeactivatedBuildings = driver.findElement(By.xpath("//*[text()='Exclude Deactivated Buildings']/following::input[2]")).getAttribute("value");
                            if (excludeDeactivatedBuildings.equals("") || (excludeDeactivatedBuildings.equals(null))) {
                            	excludeDeactivatedBuildings = "No Value";
                            }
                            System.out.println(excludeDeactivatedBuildings);
                            extractedValues.append("Exclude Deactivated Buildings: ").append(excludeDeactivatedBuildings).append("\n"); // Append to StringBuilder*/

                        }

                    }
                } catch (Exception e) {
                    // Handle any exceptions or errors
                    e.printStackTrace();
                }

                try {
                    List<WebElement> elements1 = driver.findElements(By.xpath("(//*[@class='x-table-layout-cell']/div/div/div[2]/div/div/div[2]/div/div)"));

                    // Print custom filter indication
                    System.out.println("Custom Filter:");

                    // Append custom filter indication
                    extractedValues.append("Custom Filter:\n");

                    for (WebElement row : elements1) {
                        // Find all cells (td) within the row
                        List<WebElement> cells = row.findElements(By.xpath(".//td"));

                        // Print content of each cell
                        for (WebElement cell : cells) {
                            System.out.print(cell.getText() + "\t"); // Print cell content
                            extractedValues.append(cell.getText()).append("\t"); // Append to StringBuilder
                        }
                        System.out.println(); // Move to the next line after printing the entire row
                        extractedValues.append("\n"); // Move to the next line after printing the entire row
                    }
                } catch (Exception e) {
                    // Handle any exceptions or errors
                    e.printStackTrace();
                }


                // Update database with the concatenated string
                updateDatabase(reportID, extractedValues.toString());
            }

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





    private static void updateDatabase(String reportID, String extractedValues) {

        try {
            String sqlUpdate = "UPDATE Staging.ReportProcess SET FilterValueInPW = ?, FilterValidationThroughAutomation = 1 WHERE ReportID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sqlUpdate);
            pstmt.setString(1, extractedValues);
            pstmt.setString(2, reportID);
            int rowsAffected = pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace(); // Or log the exception
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
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            try {
                WebElement container = driver.findElement(By.id("viewReportExpiryContainer"));
                if (container.isDisplayed()) {
                    WebElement inputElement = container.findElement(By.xpath("./div[2]/input"));
                    inputElement.click();
                }
            } catch (Exception ignored) {
            }
            try {
                WebElement unableToGetDatePopup = driver.findElement(By.id("//div[@class=' x-window x-window-plain x-window-dlg']"));
                if (unableToGetDatePopup.isDisplayed()) {
                    WebElement inputElement = unableToGetDatePopup.findElement(By.xpath("//button[text()=\"OK\"]"));
                    inputElement.click();
                }
            } catch (Exception ignored) {
            }
            
        } catch (Exception ignored) {
        }
        
    }
}
