package co.loyyee.selenium;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SeleniumDemo {

  private String url;
  private WebDriver driver;
  private Logger log = LoggerFactory.getLogger(SeleniumDemo.class);
  private Map<String, String> users = new HashMap<>();
  private String password;
  private CoreFn fn = new CoreFn();

  @BeforeTest
  void setup() throws IOException {
    driver = new ChromeDriver();
    try (InputStream is = new FileInputStream("src/main/resources/application.properties")) {
      Properties props = new Properties();
      props.load(is);
      url = props.getProperty("URL");

      driver.get(url);

      WebElement names = driver.findElement(By.id("login_credentials"));
      users = Arrays.stream(names.getText().split("\n"))
          .filter(line -> line.contains("_"))
          .collect(Collectors.toMap(Function.identity(), Function.identity()));

      password = Arrays.stream(
          driver
              .findElement(By.cssSelector("div[data-test='login-password']"))
              .getText().split("\n"))
          .filter(s -> s.contains("_"))
          .findFirst()
          .get();
    }
  }

  @BeforeMethod
  void newDriver() {
    driver.get(url);
  }


  @AfterSuite
  void teardown() {
    driver.quit();
  }

  // @Test
  void gatherUsernamesPassword() {
    Assert.assertEquals(users.get("standard_user"), "standard_user");

    password = Arrays.stream(
        driver
            .findElement(By.cssSelector("div[data-test='login-password']"))
            .getText().split("\n"))
        .filter(s -> s.contains("_"))
        .findFirst()
        .get();
    Assert.assertEquals(password, "secret_sauce");
  }

  // @Test(dependsOnMethods = "gatherUsernamesPassword")
  @Test
  void standardUserLogin() {

    String u = users.get("standard_user");
    // Before Abstraction
    // var nameInput =
    // driver.findElement(By.cssSelector("input[name='user-name']"));
    // var pwInput = driver.findElement(By.id("password"));

    // nameInput.sendKeys(u);
    // pwInput.sendKeys(password);

    var loginBtn = driver.findElement(By.name("login-button"));
    var value = loginBtn.getAttribute("value");
    System.out.println(value);
    Assert.assertEquals(value, "Login");

    // loginBtn.click();

    fn.handleLogin(driver, u, password);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.className("inventory_item")));
    Assert.assertEquals(driver.getTitle(), "Swag Labs");
  }

  // @Test(dependsOnMethods = "gatherUsernamesPassword")
  @Test
  void lockedoutUser() {

    String u = users.get("locked_out_user");
    fn.handleLogin(driver, u, password);

    // expected to fail
    var red = "#e2231a";
    var errCont = driver.findElement(By.className("error-message-container"));
    String bg = errCont.getCssValue("background-color");
    String actual = Color.fromString(bg).asHex();
    log.info("bg: {} red: {}", bg, red);
    Assert.assertEquals(actual, red);

    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));

    var errMsg = errCont.findElement(By.tagName("h3")).getText();
    Assert.assertEquals(errMsg, "Epic sadface: Sorry, this user has been locked out.");
  }

}
