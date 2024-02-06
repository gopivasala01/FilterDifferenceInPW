package mainPackage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

public class RunnerClass {

    private static ChromeDriver driver;
    private static Actions actions;
    private static JavascriptExecutor js;
    private static WebDriverWait wait;
    private static String downloadFilePath;
    private static String failedReason;
    private static HSSFWorkbook workbook;
    private static HSSFSheet sheet; // Corrected to HSSFSheet
    private static int rowNum = 0;

    public static void main(String[] args) {
        initializeBrowser();
        if (signIn()) {
            navigateToBuildingForAllCompanies();
            createAndSaveExcelFile(AppConfig.excelFileLocation);
        } else {
            System.out.println("Sign-in failed");
        }
    }

    public static void initializeBrowser() {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<String, Object>();
        prefs.put("download.default_directory", downloadFilePath);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--remote-allow-origins=*");
        WebDriverManager.chromedriver().clearDriverCache().setup();
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    public static boolean signIn() {
        try {
            driver.get(AppConfig.URL);
            driver.findElement(Locators.userName).sendKeys(AppConfig.username);
            driver.findElement(Locators.password).sendKeys(AppConfig.password);
            driver.findElement(Locators.signMeIn).click();
            actions = new Actions(driver);
            js = (JavascriptExecutor) driver;
            driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
            wait = new WebDriverWait(driver, Duration.ofSeconds(2));
            try {
                if (driver.findElement(Locators.loginError).isDisplayed()) {
                    System.out.println("Login failed");
                    failedReason = ", Login failed";
                    return false;
                }
            } catch (Exception e) {
            }
            driver.manage().timeouts().implicitlyWait(100, TimeUnit.SECONDS);
            wait = new WebDriverWait(driver, Duration.ofSeconds(100));
            return true;
        } catch (Exception e) {
            System.out.println("Login failed");
            failedReason = ", Login failed";
            return false;
        }
    }

    public static void navigateToBuildingForAllCompanies() {
        workbook = new HSSFWorkbook();
        sheet = workbook.createSheet("Company Data");

        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Company");
        headerRow.createCell(1).setCellValue("Falls Within");
        headerRow.createCell(2).setCellValue("Cash or Accrual Accounting Basis");

        for (String company : AppConfig.companies) {
            if (navigateToBuilding(company)) {
                System.out.println("Navigation successful for company: " + company);
            } else {
                System.out.println("Navigation failed for company: " + company);
            }
        }
    }

    public static boolean navigateToBuilding(String company) {
        try {
            driver.get(AppConfig.homeURL);
            Thread.sleep(2000);
            driver.findElement(Locators.marketDropdown).click();
            String marketName = "HomeRiver Group - " + company;
            Select marketDropdownList = new Select(driver.findElement(Locators.marketDropdown));
            marketDropdownList.selectByVisibleText(marketName);
            Thread.sleep(3000);
            driver.findElement(By.linkText("Reports")).click();
            driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
            WebElement element = driver.findElement((By.xpath("//*[@a='Business Intelligence']")));
            Actions actions = new Actions(driver);
            actions.moveToElement(element).perform();
            actions.click().perform();
            Thread.sleep(2000);
            driver.findElement(By.xpath("//*[@a='-API']")).click();
            Thread.sleep(2000);
            WebElement element1 = driver.findElement(By.xpath("//span[contains(text(), '*Incremental - General Ledger (Last Month)')]"));
            Actions actions1 = new Actions(driver);
            actions1.moveToElement(element1).click().perform();
            Thread.sleep(2000);
            driver.findElement(By.xpath("//img[@alt='Edit filters']")).click();

            WebElement dropdownInput = driver.findElement(By.xpath("//input[@id='ext-comp-1031']"));
            String dropdownText = (String)((JavascriptExecutor)driver).executeScript("return arguments[0].value;", dropdownInput);
            System.out.println("Falls Within: " + dropdownText);

            WebElement dropdownInput1 = driver.findElement(By.xpath("//input[@id='ext-comp-1036']"));
            String dropdownText1 = (String)((JavascriptExecutor)driver).executeScript("return arguments[0].value;", dropdownInput1);
            System.out.println("Cash or Accrual Accounting Basis: " + dropdownText1);

            // Write data to Excel
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(company);
            row.createCell(1).setCellValue(dropdownText);
            row.createCell(2).setCellValue(dropdownText1);

            return true;
        } catch (Exception e) {
            System.out.println("Navigation to building failed");
            return false;
        }
    }

    public static void createAndSaveExcelFile(String fileName) {
        try {
            // Create a new workbook
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("Company Data");

            // Add header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Company");
            headerRow.createCell(1).setCellValue("Falls Within");
            headerRow.createCell(2).setCellValue("Cash or Accrual Accounting Basis");

            // Iterate through companies and populate data
            int rowNum = 1; // Start from the second row after header
            for (String company : AppConfig.companies) {
                if (navigateToBuilding(company)) {
                    System.out.println("Navigation successful for company: " + company);
                    rowNum++; // Move to the next row for the next company
                } else {
                    System.out.println("Navigation failed for company: " + company);
                }
            }

            // Save the workbook to the specified file
            try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
                workbook.write(outputStream);
                System.out.println("Excel file saved successfully.");
            } catch (IOException e) {
                System.out.println("Error saving Excel file: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error creating Excel file: " + e.getMessage());
        }
    }
}
