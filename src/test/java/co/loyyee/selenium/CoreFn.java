package co.loyyee.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CoreFn {

  public void handleLogin(WebDriver driver, String username, String password) {

    var nameInput = driver.findElement(By.cssSelector("input[name='user-name']"));
    var pwInput = driver.findElement(By.id("password"));

    nameInput.sendKeys(username);
    pwInput.sendKeys(password);
    driver.findElement(By.xpath("//input[@name='login-button']")).click();

  }
}