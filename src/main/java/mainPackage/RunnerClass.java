package mainPackage;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import io.github.bonigarcia.wdm.WebDriverManager;


public class RunnerClass {
	
    public static String[][] pendingLeases;
    public static String[][] failedLeases;
	public static WebDriverWait wait;
	public static boolean loggedOut = false;
    

    // Use ThreadLocal to store a separate ChromeDriver instance for each thread
    private static ThreadLocal<ChromeDriver> driverThreadLocal = new ThreadLocal<ChromeDriver>();
    private static ThreadLocal<String> failedReasonThreadLocal = new ThreadLocal<>();
    
    @BeforeSuite
    public static void DBChanges() {
        try {
            FileUtils.cleanDirectory(new File("C:\\SantoshMurthyP\\Lease Audit Automation\\ReportExcel\\reports.xlsx"));
        } catch (Exception e) {}
        
        //----------------------- Update Last week Filter values to column PreviousFilterValueInPW---------------
      String updatePreviousFilter = "UPDATE Staging.ReportProcess SET PreviousFilterValueInPw = FilterValueInPw";
      updateTable(updatePreviousFilter);
       
       //----------------------Null the column FilterValueInPW for current run----------------------------------
       String updateFilterValueInPW = "UPDATE Staging.ReportProcess SET FilterValueInPw = NULL";
       updateTable(updateFilterValueInPW);
    }

 

    @BeforeMethod
    public boolean setUp(){
        // Set up WebDriverManager to automatically download and set up ChromeDriver
    	//System.setProperty("webdriver.http.factory", "jdk-http-client");
    	try {
    			WebDriverManager.chromedriver().clearDriverCache().setup();
    		 	//WebDriverManager.chromedriver().setup();
    	       // RunnerClass.downloadFilePath = AppConfig.downloadFilePath;
    			Map<String, Object> prefs = new HashMap<String, Object>();
    		    // Use File.separator as it will work on any OS
    		   // prefs.put("download.default_directory",RunnerClass.downloadFilePath);
    	        ChromeOptions options = new ChromeOptions();
    	        options.addArguments("--remote-allow-origins=*");
    	        //options.addArguments("--headless");
    	        options.addArguments("--disable-gpu");  //GPU hardware acceleration isn't needed for headless
    	        options.addArguments("--no-sandbox");  //Disable the sandbox for all software features
    	        options.addArguments("--disable-dev-shm-usage");  //Overcome limited resource problems
    	        options.addArguments("--disable-extensions");  //Disabling extensions can save resources
    	        options.addArguments("--disable-plugins");
    	        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
    	        // Create a new ChromeDriver instance for each thread
    	        ChromeDriver driver = new ChromeDriver(options);
    	        driver.manage().window().maximize();
    	        //test = extent.createTest("Login Page");
    	        // Store the ChromeDriver instance in ThreadLocal
    	        driverThreadLocal.set(driver);
    	        driver.get(AppConfig.URL);
    	        driver.findElement(Locators.userName).sendKeys(AppConfig.username);
    	        driver.findElement(Locators.password).sendKeys(AppConfig.password);
    	        Thread.sleep(2000);
    	        driver.findElement(Locators.signMeIn).click();
    	        Thread.sleep(3000);
    	        wait = new WebDriverWait(driver, Duration.ofSeconds(2));
    	        
    	        try
    	        {
    	        if(driver.findElement(Locators.loginError).isDisplayed())
    	        {
    	        	System.out.println("Login failed");
    				return false;
    	        }
    	        }
    	        catch(Exception e) {}
    	       
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    		return false;
    	}
    	return true;
        
        
    }

    @Test(priority=1,dataProvider = "testData")
    public void testMethod(String reportID,String companyName, String reportName, String reportAliasName, String reportURL) throws Exception {
    	
    	String failedReason="";
    	
    	System.out.println("<-------- "+companyName+"-------"+reportName+" -------->");
    	// Retrieve the thread-specific ChromeDriver instance from ThreadLocal
        ChromeDriver driver = driverThreadLocal.get();
    
		try {
			String expiredURL = driver.getCurrentUrl();
			if(expiredURL.contains("https://app.propertyware.com/pw/expired.jsp") || expiredURL.equalsIgnoreCase("https://app.propertyware.com/pw/expired.jsp?cookie") || expiredURL.contains(AppConfig.URL)) {
				loggedOut = true;
				driver.navigate().to(AppConfig.URL);
				driver.findElement(Locators.userName).sendKeys(AppConfig.username); 
				driver.findElement(Locators.password).sendKeys(AppConfig.password);
			    Thread.sleep(2000);
			    driver.findElement(Locators.signMeIn).click();
			    Thread.sleep(3000);
			}
		}
		catch(Exception e) {}
		try {
			String number = extractNumberFromURL(reportURL);
	    	String finalURL = "https://app.propertyware.com/pw/reporting/reports.do?entityID=" + number;
	    	 try {
	             if( navigateToURL(driver,finalURL, companyName, reportName, reportAliasName, reportURL, reportID)==true) {
	      		   
	             }
	          } catch (Exception e) {
	              e.printStackTrace();
	          }
	          System.out.println("---------------------------------------------");
		}
		catch(Exception e) {}
		
		finally {
			setFailedReason(null);
			driver.quit();
		}
		
    }

    @Test(priority=2,dataProvider = "failedData")
    public void failedCasesRunMethod(String reportID,String companyName, String reportName, String reportAliasName, String reportURL) throws Exception {
    	
    	String failedReason="";
    	
    	System.out.println("Failed Cases <-------- "+companyName+" ------- "+reportName+" -------->");
    	// Retrieve the thread-specific ChromeDriver instance from ThreadLocal
        ChromeDriver driver = driverThreadLocal.get();
    
		try {
			String expiredURL = driver.getCurrentUrl();
			if(expiredURL.contains("https://app.propertyware.com/pw/expired.jsp") || expiredURL.equalsIgnoreCase("https://app.propertyware.com/pw/expired.jsp?cookie") || expiredURL.contains(AppConfig.URL)) {
				loggedOut = true;
				driver.navigate().to(AppConfig.URL);
				driver.findElement(Locators.userName).sendKeys(AppConfig.username); 
				driver.findElement(Locators.password).sendKeys(AppConfig.password);
			    Thread.sleep(2000);
			    driver.findElement(Locators.signMeIn).click();
			    Thread.sleep(3000);
			}
		}
		catch(Exception e) {}
		try {
			String number = extractNumberFromURL(reportURL);
	    	String finalURL = "https://app.propertyware.com/pw/reporting/reports.do?entityID=" + number;
	    	 try {
	             if( navigateToURL(driver,finalURL, companyName, reportName, reportAliasName, reportURL, reportID)==true) {
	      		   
	             }
	          } catch (Exception e) {
	              e.printStackTrace();
	          }
	          System.out.println("---------------------------------------------");
		}
		catch(Exception e) {}
		
		finally {
			setFailedReason(null);
			driver.quit();
		}
		
    }
    
    
    
    @AfterSuite
    public void executeCodeAfterSuite() {
    	
    	
    	StringBuilder stringBuilder = new StringBuilder();
    	
    	   try {
    		   if(SampleText.differenceInFilters()==true) {
    			   try {
	            		if(!SampleText.output.toString().isEmpty()) {
	            			SampleText.sendEmail(SampleText.output);
		            		//OpenJira.jiraTicketCreation(SampleText.output);
	            		}
	            		else {
	            			stringBuilder.append("No differences in the filters");
	            			SampleText.sendEmail(stringBuilder);
	            			
	            		}
	            		
	            	}
	            	catch (Exception e) {
	            		 e.printStackTrace();
	     	            System.out.println("Error Sending Email/Jira Creation: " + e.getMessage());
	            	}
    		   }
    		   else {
    			   stringBuilder.append("Unable to get the differences in the filters");
            		SampleText.sendEmail(stringBuilder);
    		   }
    	   }
    	   catch (Exception e) {
    		   stringBuilder.append("Unable to get the differences in the filters");
        		SampleText.sendEmail(stringBuilder);
        		 e.printStackTrace();
 	            System.out.println("Difference in filters fetching: " + e.getMessage());
        	}
    }
    
    
    public static String getFailedReason() {
    		 return failedReasonThreadLocal.get();
    }

    public static void setFailedReason(String failedReason) {
    	failedReasonThreadLocal.set(failedReason);
    }
    
        
    public static boolean fetchDataFromDatabaseAndNavigate(String sqlSelect) throws IOException {
    	Connection conn = null;
    	Statement stmt= null;
	    ResultSet rs= null;
	    final String CONNECTION_URL = "jdbc:sqlserver://azrsrv001.database.windows.net;databaseName=HomeRiverDB;user=service_sql02;password=xzqcoK7T;encrypt=true;trustServerCertificate=true;";
    	try {
        	
            conn = DriverManager.getConnection(CONNECTION_URL);
           
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sqlSelect);
            int rows =0;
            if (rs.last()) 
            {
            	rows = rs.getRow();
            	// Move to beginning
            	rs.beforeFirst();
            }
            System.out.println("No of Rows = "+rows);
            RunnerClass.pendingLeases = new String[rows][5];
            
            int  i=0;
            while(rs.next())
            {
            	String 	ReportID =  rs.getObject(1).toString();
            	String 	CompanyName =  (String) rs.getObject(2);
                String  ReportName = rs.getObject(3).toString();
                String ReportAliasName = rs.getObject(4).toString();
                String ReportURL = rs.getObject(5).toString(); 
                
               
    			//ID
                try 
                {
    				RunnerClass.pendingLeases[i][0] = ReportID;
                }
                catch(Exception e)
                {
                	RunnerClass.pendingLeases[i][0] = "";
                }
              //Company
                try 
                {
    				RunnerClass.pendingLeases[i][1] = CompanyName;
                }
                catch(Exception e)
                {
                	RunnerClass.pendingLeases[i][1] = "";
                }
              //leaseEntityID
                try 
                {
    				RunnerClass.pendingLeases[i][2] = ReportName;
                }
                catch(Exception e)
                {
                	RunnerClass.pendingLeases[i][2] = "";
                }
              //DataDifference between moveindate and today
                try 
                {
    				RunnerClass.pendingLeases[i][3] = ReportAliasName;
                }
                catch(Exception e)
                {
                	RunnerClass.pendingLeases[i][3] = "";
                }
              //moveINDate
                try 
                {
    				RunnerClass.pendingLeases[i][4] = ReportURL;
                }
                catch(Exception e)
                {
                	RunnerClass.pendingLeases[i][4] = "";
                }
              
    				i++;
            }	
            System.out.println("Total Pending Leases  = " +RunnerClass.pendingLeases.length);
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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
		return true;
    }   
    public static boolean navigateToURL(WebDriver driver,String finalURL, String companyName, String reportName, String reportAliasName, String reportURL, String reportID) throws InterruptedException {
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
                    return false;
                }
            }; 

            Thread.sleep(2000);
          
 
            driver.get(finalURL);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(500));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@class='ext-el-mask-msg x-mask-loading']")));
            intermittentPopUp(driver);
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Filters']")));
            Thread.sleep(5000);
            // Click on 'Filters'
            driver.findElement(By.xpath("//button[text()='Filters']")).click();
            
            Thread.sleep(5000);
           if( extractTextAndInputValuesFromPopup(driver, reportID)==true) {
        	   return true;
           }
           else {
        	   return false;
           }
         
        } catch (Exception e) {
        	String dropdownText1 = "Failed to load the page";
            updateDatabase(reportID, dropdownText1);
            e.printStackTrace();
            }
        
		return true;
    }
    public static boolean extractTextAndInputValuesFromPopup(WebDriver driver, String reportID) {
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
                boolean isFirstCustomFilterModeLabel = true;

                for (WebElement element : elements) {
                    if (element.isDisplayed()) {
                        String text = element.getText().trim();
                        String inputValue = element.getAttribute("value");

                        if ("label".equalsIgnoreCase(element.getTagName())) {
                            if ("Custom Filter Mode".equalsIgnoreCase(text)) {
                                if (isFirstCustomFilterModeLabel) {
                                    extractedValues.append(text).append(" : ").append("\n");
                                    isFirstCustomFilterModeLabel = false;
                                } else {
                                    extractedValues.append(text).append(" Duplicate : ");
                                }
                            } else {
                                extractedValues.append(text).append(" : ");
                            }
                        } else if ("input".equalsIgnoreCase(element.getTagName())) {
                            if ("radio".equalsIgnoreCase(element.getAttribute("type"))) {
                                if (element.isSelected()) {
                                    String labelText = element.findElement(By.xpath("following-sibling::label")).getText().trim();
                                    extractedValues.append("Checked: ").append(labelText).append("\n");
                                }
                            } else {
                                extractedValues.append(inputValue).append("\n");
                            }
                        }

                        if ("Custom Filter Mode".equalsIgnoreCase(text)) {
                            isInCustomFilterModeSection = true;
                            if (!customFilterModeLabelPrinted) {
                                extractedValues.append(text).append(" : ");
                                customFilterModeLabelPrinted = true;
                            }
                        } else if (isInCustomFilterModeSection && "table".equalsIgnoreCase(element.getTagName())) {
                            List<WebElement> rows = element.findElements(By.tagName("tr"));
                            for (WebElement row : rows) {
                                List<WebElement> cells = row.findElements(By.tagName("td"));
                                for (WebElement cell : cells) {
                                    extractedValues.append(cell.getText()).append("\t");
                                }
                                extractedValues.append("\n");
                            }
                        }
                    }
                }

                System.out.println("Final Extracted Values:");
                System.out.println(extractedValues.toString());
                updateDatabase(reportID, extractedValues.toString().trim());
            } else {
                System.out.println("Error: Script did not return a list of WebElements.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
		return true;
    }
    private static void updateDatabase(String reportID, String extractedValues) {
    	 try (Connection conn = DriverManager.getConnection(AppConfig.connectionUrl);
 		        Statement stmt = conn.createStatement();) 
 		    {
            String sqlUpdate = "UPDATE Staging.ReportProcess SET FilterValueInPW = ?, FilterValidationThroughAutomation = 1 WHERE ReportID = ?";
            PreparedStatement pstmt = conn.prepareStatement(sqlUpdate);
            pstmt.setString(1, extractedValues);
            pstmt.setString(2, reportID);
            int rowsAffected = pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public static void updateTable(String query)
	 {
		    try (Connection conn = DriverManager.getConnection(AppConfig.connectionUrl);
		        Statement stmt = conn.createStatement();) 
		    {
		      stmt.executeUpdate(query);
		     System.out.println("Record Updated");
		      stmt.close();
	            conn.close();
		    } catch (SQLException e) 
		    {
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
    public static void intermittentPopUp(WebDriver driver)
	{
		//Pop up after clicking lease name
				try
				{
					driver.manage().timeouts().implicitlyWait(3,TimeUnit.SECONDS);
			       WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
			        try {
			        	Thread.sleep(1000);
			        	driver.switchTo().frame(driver.findElement(Locators.scheduleMaintananceIFrame));
			        	if(driver.findElement(Locators.scheduleMaintanancePopUp2).isDisplayed()) {
			        		Thread.sleep(1000);
			        		driver.findElement(Locators.maintananceCloseButton).click();
			        	}
			        	driver.switchTo().defaultContent();
			        }
			        catch(Exception e) {}
			        try
			        {
					if(driver.findElement(Locators.popUpAfterClickingLeaseName).isDisplayed())
					{
						driver.findElement(Locators.popupClose).click();
					}
			        }
			        catch(Exception e) {}
			        try
			        {
					if(driver.findElement(Locators.scheduledMaintanancePopUp).isDisplayed())
					{
						driver.findElement(Locators.scheduledMaintanancePopUpOkButton).click();
					}
					 
			        }
			        catch(Exception e) {}
			        try
			        {
			        if(driver.findElement(Locators.scheduledMaintanancePopUpOkButton).isDisplayed()) {
			        	driver.findElement(Locators.scheduledMaintanancePopUpOkButton).click();
			        }
			     
			        } 
			        catch(Exception e) {}
			       
					driver.manage().timeouts().implicitlyWait(5,TimeUnit.SECONDS);
			        wait = new WebDriverWait(driver, Duration.ofSeconds(5));
				}
				catch(Exception e) {}
				 try {
		                WebElement container = driver.findElement(By.id("viewReportExpiryContainer"));
		                if (container.isDisplayed()) {
		                    WebElement inputElement = container.findElement(By.xpath("./div[2]/input"));
		                    inputElement.click();
		                }
		            } catch (NoSuchElementException e) {
		                
		            } catch (StaleElementReferenceException e) {
		                
		            } catch (Exception e) {
		                
		            }
				 try
				 {
					 WebElement unableToGetDatePopup = driver.findElement(By.id("//div[@class=' x-window x-window-plain x-window-dlg']"));
		                if (unableToGetDatePopup.isDisplayed()) {
		                    WebElement inputElement = unableToGetDatePopup.findElement(By.xpath("//button[text()=\"OK\"]"));
		                    inputElement.click();
		                }
				 } catch (NoSuchElementException e) {
		                
		            } catch (StaleElementReferenceException e) {
		                
		            } catch (Exception e) {
		                
		            }
				 }
	

	@DataProvider(name = "testData", parallel = true)
    public Object[][] testData() {
    	try {
    		fetchDataFromDatabaseAndNavigate(AppConfig.pendingLeasesQuery);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return pendingLeases;
    }
	
	@DataProvider(name = "failedData", parallel = true)
    public Object[][] failedData() {
    	try {
    		fetchDataFromDatabaseAndNavigate(AppConfig.failedLeasesQuery);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	 
        return pendingLeases;
    }
    
    
}