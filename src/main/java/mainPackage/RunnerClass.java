package mainPackage;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;

public class RunnerClass {
	 static WebDriver driver;
	    static Actions actions;
	    static Connection conn;
	    static Statement stmt;
	    static ResultSet rs;
	    static final String CONNECTION_URL = "jdbc:sqlserver://azrsrv001.database.windows.net;databaseName=HomeRiverDB;user=service_sql02;password=xzqcoK7T;encrypt=true;trustServerCertificate=true;";
	    static StringBuilder stringBuilder = new StringBuilder();
	    
	    
	    String 	text1 = "";
	    public static void main(String[] args) {
	        try {
	            FileUtils.cleanDirectory(new File("C:\\SantoshMurthyP\\Lease Audit Automation\\ReportExcel\\reports.xlsx"));
	        } catch (Exception e) {}
	        
	        //----------------------- Update Last week Filter values to column PreviousFilterValueInPW---------------
	      String updatePreviousFilter = "UPDATE Staging.ReportProcess SET PreviousFilterValueInPw = FilterValueInPw";
	      updateTable(updatePreviousFilter);
	       
	       //----------------------Null the column FilterValueInPW for current run----------------------------------
	       String updateFilterValueInPW = "UPDATE Staging.ReportProcess SET FilterValueInPw = NULL";
	       updateTable(updateFilterValueInPW);
	        
	        
	        try {
	            initializeBrowser();
	            if (signIn()==true) {
	            	
	            	try {
	            		 String sqlSelect = 
	                     		
	            	            	//	"SELECT  ReportID, CompanyName, ReportName, ReportAliasName, ReportURL , FilterValidationThroughAutomation, FilterValueInPW FROM Staging.Reportprocess	 where	ReportAliasName ='*Bulk - Portfolios'";

	            	            		
	            	            		
	            	            		"SELECT ReportID, CompanyName, ReportName, ReportAliasName, ReportURL , FilterValidationThroughAutomation, FilterValueInPW\r\n"
	            	            		+ "	FROM Staging.Reportprocess \r\n"
	            	            		+ "	WHERE IsActive = 1 \r\n"
	            	            		+ "	  AND (FilterValidationThroughAutomation <> 1 OR FilterValidationThroughAutomation IS NULL) \r\n"
	            	            		+ "	AND ReportAliasName <>'*Incremental - General Ledger (Last Month)' AND ReportAliasName <> '*Incremental - General Ledger (Current Month)'\r\n"
	            	            		+ "	ORDER BY ReportAliasName, CompanyName;\r\n"
	            	            		+ "";
		               if( fetchDataFromDatabaseAndNavigate(sqlSelect) == true) {
		            	   String query = "SELECT  ReportID, CompanyName, ReportName, ReportAliasName, ReportURL , FilterValidationThroughAutomation, FilterValueInPW\r\n"
		            	   		+ "            		FROM Staging.Reportprocess WHERE IsActive = 1  and (FilterValueInPW = 'Failed to load the page' or FilterValueInPW is Null)\r\n"
		            	   		+ "            		ORDER BY ReportAliasName, CompanyName";
		            	   int i=0;
		            	   while(i<3) {
		            		   fetchDataFromDatabaseAndNavigate(query) ; 
		            		   i++;
		            	   }
		            		   try {
			            		   if(SampleText.differenceInFilters()==true) {
			            			   try {
			       	            		if(!SampleText.output.toString().isEmpty()) {
			       	            			SampleText.sendEmail(SampleText.output);
			       		            		OpenJira.jiraTicketCreation(SampleText.output);
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
		               
	            	
		               else {
		            		stringBuilder.append("Error while fetching data");
		            		SampleText.sendEmail(stringBuilder);
		               } 
	            	}
	            	catch (Exception e) {
	            		stringBuilder.append("Error while fetching data");
	            		SampleText.sendEmail(stringBuilder);
	            		 e.printStackTrace();
	     	            System.out.println("Error occurred While fetching Data: " + e.getMessage());
	            	}
	            
	            	
	            	
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            System.out.println("An error occurred: " + e.getMessage());
	        }
	        finally {
	        	driver.quit();
	        }
	       
           
	       
	        
	    }

    public static void initializeBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
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
            boolean unauthorizedPopupAppearts = false;
            try {   unauthorizedPopupAppearts = driver.findElements(By.xpath("//*[@class='toast toast-error']")).size() > 0;}
            catch (Exception e1) {}
              if(unauthorizedPopupAppearts) {
              	driver.findElement(By.xpath("//*[@class='toast-close-button']")).click();
              	driver.findElement(Locators.userName).sendKeys(AppConfig.username);
                  driver.findElement(Locators.password).sendKeys(AppConfig.password);
                  driver.findElement(Locators.signMeIn).click();
              	
              }
            return !isLoginErrorDisplayed();
        } catch (Exception e) {
            System.out.println("Login failed");
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

    public static boolean fetchDataFromDatabaseAndNavigate(String sqlSelect) throws IOException {
        try {
            conn = DriverManager.getConnection(CONNECTION_URL);
           
            		
            
            		
            		/*"	SELECT ReportID, CompanyName, ReportName, ReportAliasName, ReportURL , FilterValidationThroughAutomation, FilterValueInPW\r\n"
            		+ "	FROM Staging.Reportprocess \r\n"
            		+ "	WHERE IsActive = 1 \r\n"
            		+ "	  AND (FilterValidationThroughAutomation = 1 OR FilterValidationThroughAutomation IS NULL) \r\n"
            		+ "	AND ReportAliasName = '*Incremental - General Ledger (Last Month)'\r\n"
            		+ "	ORDER BY ReportAliasName, CompanyName;";*/

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
                   if( navigateToURL(finalURL, companyName, reportName, reportAliasName, reportURL, reportID)==true) {
                	   continue;
                   }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                System.out.println("---------------------------------------------");
            }
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

    public static boolean navigateToURL(String finalURL, String companyName, String reportName, String reportAliasName, String reportURL, String reportID) throws InterruptedException {
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
            	
            	try {
					String expiredURL = RunnerClass.driver.getCurrentUrl();
					if(expiredURL.contains("https://app.propertyware.com/pw/expired.jsp") || expiredURL.equalsIgnoreCase("https://app.propertyware.com/pw/expired.jsp?cookie") || expiredURL.contains(AppConfig.URL) || expiredURL.contains("https://app.propertyware.com/pw/login.jsp?unauthorized=true")) {
						
						RunnerClass.driver.navigate().to(AppConfig.URL);
						RunnerClass.driver.findElement(Locators.userName).sendKeys(AppConfig.username); 
					    RunnerClass.driver.findElement(Locators.password).sendKeys(AppConfig.password);
					    Thread.sleep(2000);
					    RunnerClass.driver.findElement(Locators.signMeIn).click();
					    Thread.sleep(3000);
					}
				}
				catch(Exception e) {}
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
            // Handle any exceptions or errors
            driver.quit();
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
            Thread.sleep(1000);
            driver.findElement(Locators.password).sendKeys(AppConfig.password);
            Thread.sleep(1000);
            driver.findElement(Locators.signMeIn).click();
            Thread.sleep(1000);
            boolean unauthorizedPopupAppearts = false;
          try {   unauthorizedPopupAppearts = driver.findElements(By.xpath("//*[@class='toast toast-error']")).size() > 0;}
          catch (Exception e1) {}
            if(unauthorizedPopupAppearts) {
            	Thread.sleep(1000);
            	driver.findElement(By.xpath("//*[@class='toast-close-button']")).click();
            	Thread.sleep(1000);
            	driver.findElement(Locators.userName).sendKeys(AppConfig.username);
            	Thread.sleep(1000);
                driver.findElement(Locators.password).sendKeys(AppConfig.password);
                Thread.sleep(1000);
                driver.findElement(Locators.signMeIn).click();
                Thread.sleep(1000);
            	
            }
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

    
    public static void updateTable(String query)
	 {
		    try (Connection conn = DriverManager.getConnection(CONNECTION_URL);
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
	}

