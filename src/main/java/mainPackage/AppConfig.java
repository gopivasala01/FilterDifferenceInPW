package mainPackage;

import java.util.Arrays;
import java.util.List;

public class AppConfig {
	  public static String URL ="https://app.propertyware.com/pw/login.jsp";
	   public static String username ="mds0418@gmail.com";
	   public static String password ="KRm#V39fecMDGg#";
	   public static String homeURL = "https://app.propertyware.com/pw/home/home.do";
	   public static String excelFileLocation = "C:\\SantoshMurthyP\\Lease Audit Automation\\ReportExcelNew";
	   
	   public static String connectionUrl = "jdbc:sqlserver://azrsrv001.database.windows.net;databaseName=HomeRiverDB;user=service_sql02;password=xzqcoK7T;encrypt=true;trustServerCertificate=true;";
	   
	   public static String pendingLeasesQuery = 
        		
           	//	"SELECT  ReportID, CompanyName, ReportName, ReportAliasName, ReportURL , FilterValidationThroughAutomation, FilterValueInPW FROM Staging.Reportprocess	 where	ReportAliasName ='*Bulk - Portfolios'";

           		
           		
				"SELECT ReportID, CompanyName, ReportName, ReportAliasName, ReportURL , FilterValidationThroughAutomation, FilterValueInPW\r\n"
           		+ "	FROM Staging.Reportprocess \r\n"
           		+ "	WHERE IsActive = 1 \r\n"
           		+ "	  AND (FilterValidationThroughAutomation <> 1 OR FilterValidationThroughAutomation IS NULL) \r\n"
           		+ "	AND ReportAliasName <>'*Incremental - General Ledger (Last Month)' AND ReportAliasName <> '*Incremental - General Ledger (Current Month)'\r\n"
           		+ "	ORDER BY ReportAliasName, CompanyName;\r\n"
           		+ "";

	  // public static final List<String> companies = Arrays.asList("Chicago pfw" ,"Triad");
	  public static final List<String> companies = Arrays.asList(
		        "Alabama",
		        "Arizona",
		        "Arkansas",
		        "Austin",
		        "Boise",
		        "California",
		        "California pfw",
		        "Chattanooga",
		        "Chicago",
		        "Chicago pfw",
		        "Columbia - St Louis",
		        "Colorado Springs",
		        "Dallas/Fort Worth",
		        "Delaware",
		        "Florida",
		        "Georgia",
		        "Hawaii",
		        "Houston",
		        "Idaho Falls",
		        "Indiana",
		        "Institutional Accounts",
		        "Kansas City",
		        "Lake Havasu",
		        "Little Rock",
		        "Maine",
		        "Maryland",
		        "Montana",
		        "New Jersey",
		        "New Mexico",
		        "North Carolina",
		        "Ohio",
		        "OKC",
		        "Pennsylvania",
		        "San Antonio",
		        "Savannah",
		        "South Carolina",
		        "Spokane",
		        "Tennessee",
		        "Tulsa",
		        "Utah",
		        "Virginia",
		        "Washington DC"
		    );

}
