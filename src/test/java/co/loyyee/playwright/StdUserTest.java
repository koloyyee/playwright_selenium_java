package co.loyyee.playwright;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.AriaRole;

public class StdUserTest {

  private static Logger log = LoggerFactory.getLogger(StdUserTest.class);

  private String url = "";
  private String username = "";
  private String password = "";
  private Map<String, String> users = new HashMap<>();
  private final List<String> prods = List.of("Sauce Labs Backpack", "Sauce Labs Bike Light", "Sauce Labs Bolt T-Shirt",
      "Sauce Labs Fleece Jacket", "Sauce Labs Onesie", "Test.allTheThings() T-Shirt (Red)");

  private Page page;
  private BrowserContext context;

  private static Browser browser;
  private static Playwright playwright;
  private static Properties props;

  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions()
            .setHeadless(false));

    try (InputStream is = new FileInputStream("src/main/resources/application.properties")) {
      props = new Properties();
      props.load(is);

    } catch (IOException e) {
      log.error(e.getLocalizedMessage());
    }
  }

  @BeforeEach
  void createContext() {
    context = browser.newContext();
    context.tracing().start(new Tracing.StartOptions()
        .setScreenshots(true)
        .setSnapshots(true)
        .setSources(true));
    page = context.newPage();
  }

  @BeforeEach
  void setup() {
    url = props.getProperty("URL");
    page.navigate(url);

    /**
     * Interesting find: innerText vs textContent
     * innerText will contain newline \n
     * textContent doesn't
     */
    users = Arrays.stream(page.locator("div#login_credentials").innerText()
        .split("\n"))
        .filter(name -> name.contains("_"))
        .collect(Collectors.toMap(Function.identity(), Function.identity()));
    password = page.locator("div.login_password").innerText().split("\n")[1];
  }

  @AfterEach
  void closeContext() {
    context.tracing().stop(new Tracing.StopOptions()
        .setPath(Paths.get("src/main/report/tracing.zip")));
  }

  @AfterAll
  static void browserClose() {
    playwright.close();
  }

  @Test()
  void login() {
    String u = users.get("standard_user");

    page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")).fill(u);
    page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password")).fill(password);

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
    page.waitForTimeout(1000);

    String title = page.title();
    assertEquals(title, "Swag Labs");
  }

  @Test
  void shouldHaveCorrectList() {
    login();
    var actual = page.locator("div.inventory_item_name ").all().stream().map(el -> el.innerHTML()).toList();
    log.info("{}", actual);
    assertEquals(prods, actual);
  }

  @Test
  void testDescOrder() {
    login();
    page.locator("//select[@class='product_sort_container']").selectOption("za");
    var actual = page.locator("div.inventory_item_name ").all().stream().map(el -> el.innerHTML()).toList();
    log.info("{}", actual);
    List<String> copied = new ArrayList<>(prods);
    Collections.reverse(copied);
    assertEquals(copied, actual);
  }

  @Test
  void addFirstToCart() {
    login();
    var btn = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to cart")).first();
    assertEquals("Add to cart", btn.innerText());
    btn.click();

    var badge = page.locator("span.shopping_cart_badge");
    assertEquals(badge.isVisible(), true);
    assertEquals(badge.innerText(), "1");
  }

  @Test
  void addItemByName() {
    login();
    var el = page.locator("div.inventory_item").all()
        .stream()
        .filter(item -> {
          log.info("{}",item.textContent()) ;
          return item.locator("div.inventory_item_name").textContent().equals("Sauce Labs Backpack");
        })
        .findFirst()
        .orElseThrow();

    el.getByRole(AriaRole.BUTTON).click();

    var badge = page.locator("span.shopping_cart_badge");
    assertEquals(badge.isVisible(), true);
    assertEquals(badge.innerText(), "1");
  }
}
