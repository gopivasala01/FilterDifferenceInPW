package mainPackage;

import org.openqa.selenium.By;

public class Locators {
	public static By userName = By.id("loginEmail");
	public static By password = By.name("password");
	public static By signMeIn = By.xpath("//*[@value='Sign Me In']");
	public static By loginError = By.xpath("//*[@class='toast toast-error']");
	
	public static By marketDropdown = By.id("switchAccountSelect");

}
