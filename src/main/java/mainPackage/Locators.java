package mainPackage;

import org.openqa.selenium.By;

public class Locators {
	public static By userName = By.id("loginEmail");
	public static By password = By.name("password");
	public static By signMeIn = By.xpath("//*[@value='Sign Me In']");
	public static By loginError = By.xpath("//*[@class='toast toast-error']");
	
	public static By marketDropdown = By.id("switchAccountSelect");

	 public static By popUpAfterClickingLeaseName = By.xpath("//*[@id='viewStickyNoteForm']");
	    public static By scheduledMaintanancePopUp = By.xpath("//*[text()='Scheduled Maintenance Notification']");
	    public static By scheduledMaintanancePopUpOkButton = By.id("alertDoNotShow");
	    public static By popupClose = By.xpath("//*[@id='editStickyBtnDiv']/input[2]");
	    public static By permissionDenied = By.xpath("//*[contains(text(),'Permission Denied')]");
	    public static By renewalPopup = By.id("viewStickyNoteForm");
	    public static By renewalPoupCloseButton = By.xpath("//*[@id='viewStickyNoteForm']/div/div[1]/input[2]");
	    public static By scheduleMaintananceIFrame = By.xpath("//iframe[@srcdoc='<meta name=\"referrer\" content=\"origin\" />']");
	    public static By scheduleMaintanancePopUp2 = By.xpath("//section[@role='dialog']");
	    public static By maintananceCloseButton = By.xpath("//a[@aria-label='Close modal']");
	    
}
