package co.loyyee.selenium;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class StdUserTest {

  private WebDriver driver;
  private Logger log = LoggerFactory.getLogger(SeleniumDemo.class);
  private CoreFn fn = new CoreFn();

  private String url;
  private Map<String, String> users = new HashMap<>();
  private String password;

  private final List<String> prods = List.of("Sauce Labs Backpack", "Sauce Labs Bike Light", "Sauce Labs Bolt T-Shirt",
      "Sauce Labs Fleece Jacket", "Sauce Labs Onesie", "Test.allTheThings() T-Shirt (Red)");

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
  void resetUrl() {
    driver.get(url);
  }

  @AfterTest
  void teardown() {
    driver.quit();
  }

  @Test
  public void login() {

    String u = users.get("standard_user");
    fn.handleLogin(driver, u, password);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.className("inventory_item")));
    Assert.assertEquals(driver.getTitle(), "Swag Labs");
  }

  @Test(dependsOnMethods = { "login" })
  void shouldHaveCorrectList() {

    login();
    // get all the item names
    List<WebElement> els = driver.findElements(By.className("inventory_item_name"));
    List<String> actual = els.stream().map(el -> el.getText()).toList();
    Assert.assertTrue(prods.containsAll(actual));
    log.info("{}", actual);
    log.info("{}", prods);
    Assert.assertEquals(prods, actual, "should be the same order");

  }

  @Test(dependsOnMethods = { "login" })
  void testDescOrder() {

    login();
    var selectSort = driver.findElement(By.cssSelector("select.product_sort_container"));
    Select target = new Select(selectSort);
    target.selectByValue("za");

    List<String> els = driver.findElements(By.className("inventory_item_name")).stream().map(el -> el.getText())
        .toList();
    List<String> reverseProd = new ArrayList<>(prods);
    Collections.reverse(reverseProd);
    Assert.assertEquals(els, reverseProd, "should be in descending order.");
  }

  @Test
  void addFirstToCart() {
    login();
    var btn = driver.findElement(By.className("inventory_item")).findElement(By.tagName("button"));
    var beforeAdd = btn.getText();
    Assert.assertEquals(beforeAdd, "Add to cart");

    btn.click();
    // var afterAdd = btn.getText();
    // Assert.assertEquals(afterAdd, "Remove");

    var el = driver.findElement(By.cssSelector("span.shopping_cart_badge"));
    Assert.assertEquals(el.isDisplayed(), true);
    Assert.assertEquals(el.getText(), "1");
  }

  @Test
  void addItemByName() {
    login();
    var el = driver.findElements(By.className("inventory_item"))
        .stream()
        .filter(item -> item.findElement(By.className("inventory_item_name")).getText().equals("Sauce Labs Backpack"))
        .findFirst()
        .get();

    el.findElement(By.tagName("button")).click();

    var badge = driver.findElement(By.cssSelector("span.shopping_cart_badge"));
    Assert.assertEquals(badge.isDisplayed(), true);
    Assert.assertEquals(badge.getText(), "1");
  }
}
