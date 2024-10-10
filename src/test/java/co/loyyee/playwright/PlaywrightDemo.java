package co.loyyee.playwright;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
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
import org.openqa.selenium.support.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.AriaRole;

public class PlaywrightDemo {
  private static Logger log = LoggerFactory.getLogger(PlaywrightDemo.class);

  private String url = "";
  private Page page;
  private BrowserContext context;
  private static Browser browser;
  private static Playwright playwright;
  private static Properties props;

  private Map<String, String> users = new HashMap<>();
  private String password = "";

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

  // @BeforeEach
  void wrong() {
    try (
        InputStream is = new FileInputStream("src/main/resources/application.properties");
        Playwright playwright = Playwright.create();) {
      Browser browser = playwright.chromium().launch(
          new BrowserType.LaunchOptions()
              .setHeadless(false)

      );
      context = browser.newContext();
      context.tracing().start(new Tracing.StartOptions()
          .setScreenshots(true)
          .setSnapshots(true)
          .setSources(true));
      page = context.newPage();

      Properties props = new Properties();
      props.load(is);
      url = props.getProperty("URL");
      page.navigate(url);

    } catch (IOException e) {
      log.error(e.getLocalizedMessage());
    }
  }

  @Test()
  void standardUserLogin() {
    String u = users.get("standard_user");

    page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username")).fill(u);
    page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password")).fill(password);

    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
    page.waitForTimeout(1000);

    String title = page.title();
    assertEquals(title, "Swag Labs");
  }

  @Test
  void lockedoutUser() {
    String u = users.get("locked_out_user");
    page.getByPlaceholder("Username").fill(u);
    page.getByPlaceholder("Password").fill(password);

    page.locator("input#login-button").click();

    var errCont = page.locator("h3[data-test='error']");
    String errMsg = errCont.textContent();
    assertEquals("Epic sadface: Sorry, this user has been locked out.", errMsg);

    var red = "#e2231a";
    String bg = (String) errCont.evaluate("""
        () => {
         const ct = document.querySelector('div.error-message-container.error');
         return window.getComputedStyle(ct, null).getPropertyValue("background-color");
        }
        """);
    String actual = Color.fromString(bg).asHex();

    assertEquals(red, actual);

    page.locator("button[data-test='error-button']").click();

  }

}
