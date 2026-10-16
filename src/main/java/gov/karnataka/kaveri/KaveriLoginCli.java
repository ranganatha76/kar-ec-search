package gov.karnataka.kaveri;

import java.io.Console;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;

public final class KaveriLoginCli {
    private static final String PORTAL_URL = "https://kaveri.karnataka.gov.in/";
    private static final Duration ELEMENT_TIMEOUT = Duration.ofSeconds(20);
    private static final int PREPARATION_WORKERS = 4;
    private static final List<String> PROPERTY_NAMES = List.of(
        "Satishchandra K R",
        "Satish chandra K R",
        "Ravichandra K R",
        "Ravi chandra K R",
        "ಸತೀಶ್ಚಂದ್ರ ಕೆ ಆರ್",
        "ಸತೀಶ್ ಚಂದ್ರ ಕೆ ಆರ್",
        "ಸತೀಶ ಚಂದ್ರ ಕೆ ಆರ್",
        "ಸತೀಶಚಂದ್ರ ಕೆ ಆರ್",
        "ರವಿಚಂದ್ರ ಕೆ ಆರ್",
        "ರವಿ ಚಂದ್ರ ಕೆ ಆರ್");

    private KaveriLoginCli() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            System.err.println("Run this program from a real terminal so the password can be entered securely.");
            System.exit(1);
        }

        try (WebDriver driver = new ChromeDriver(); Scanner scanner = new Scanner(System.in)) {
            driver.manage().window().maximize();
            driver.get(PORTAL_URL);

            System.out.println("Chrome has opened Kaveri Online Services.");
            System.out.println("Navigate to its login page, then press Enter when the username and password fields are visible.");
            scanner.nextLine();

            String username = console.readLine("Username: ");
            char[] password = console.readPassword("Password: ");
            try {
                fill(driver, "kaveri.usernameSelector", List.of(
                    "input[name='username']", "input[name='userName']", "input[name='userid']",
                    "input[name='userId']", "input[type='email']", "#username", "#userName", "#userid"), username);
                fill(driver, "kaveri.passwordSelector", List.of(
                    "input[type='password']", "input[name='password']", "#password"), new String(password));
            } finally {
                java.util.Arrays.fill(password, '\0');
            }

            System.out.println("Complete any CAPTCHA in Chrome. Press Enter to submit your credentials and request the OTP.");
            scanner.nextLine();
            click(driver, "kaveri.credentialSubmitSelector", List.of(
                "button[type='submit']", "input[type='submit']", "button[name='login']", "#login"));

            String otp = console.readLine("OTP: ");
            fill(driver, "kaveri.otpSelector", List.of(
                "input[name='otp']", "input[name='OTP']", "input[autocomplete='one-time-code']", "#otp", "#OTP"), otp);
            click(driver, "kaveri.otpSubmitSelector", List.of(
                "button[type='submit']", "input[type='submit']", "button[name='verify']", "#verify"));

            clickStartNewApplication(driver);
            clickOnlineEc(driver);
            clickEcPrerequisiteContinue(driver);
            clickAfter2004(driver);
            clickProceed(driver);
            searchTumkurPropertyNames(driver, scanner);
            System.out.println("The requested name searches have been submitted. Press Enter to close the browser.");
            scanner.nextLine();
        } catch (TimeoutException exception) {
            System.err.println("Could not find a required login control. See the README for selector overrides.");
            System.exit(2);
        }
    }

    private static void fill(WebDriver driver, String property, List<String> defaults, String value) {
        WebElement element = waitForFirst(driver, property, defaults);
        element.clear();
        element.sendKeys(value);
    }

    private static void click(WebDriver driver, String property, List<String> defaults) {
        waitForFirst(driver, property, defaults).click();
    }

    private static void searchTumkurPropertyNames(WebDriver driver, Scanner scanner) {
        clickVisibleText(driver, "kaveri.searchByPropertyNameSelector", "Search by Property Name");
        selectLocation(driver, "kaveri.districtSelector", "DISTRICT", List.of("Tumkur", "Tumakuru"));

        System.out.println("This will submit every supplied spelling for every Taluka, Hobli, and Village under Tumkur.");
        System.out.print("Type YES to continue: ");
        if (!"YES".equals(scanner.nextLine().trim())) {
            System.out.println("Location-wide search was not started.");
            return;
        }

        try (ExecutorService preparationPool = Executors.newFixedThreadPool(PREPARATION_WORKERS)) {
            for (String taluka : optionTexts(driver, "kaveri.talukaSelector", "TALUKA")) {
                selectLocation(driver, "kaveri.talukaSelector", "TALUKA", List.of(taluka));
                for (String hobli : optionTexts(driver, "kaveri.hobliSelector", "HOBLI/TOWN")) {
                    selectLocation(driver, "kaveri.hobliSelector", "HOBLI/TOWN", List.of(hobli));
                    for (String village : optionTexts(driver, "kaveri.villageSelector", "INDEX II: VILLAGE")) {
                        selectLocation(driver, "kaveri.villageSelector", "INDEX II: VILLAGE", List.of(village));
                        submitNames(driver, prepareNameJobs(preparationPool));
                    }
                }
            }
        }
    }

    private static List<Future<String>> prepareNameJobs(ExecutorService preparationPool) {
        return PROPERTY_NAMES.stream()
            .map(name -> preparationPool.submit(() -> name.trim().replaceAll("\\s+", " ")))
            .toList();
    }

    private static void submitNames(WebDriver driver, List<Future<String>> nameJobs) {
        for (Future<String> nameJob : nameJobs) {
            try {
                fill(driver, "kaveri.propertyNameSelector", List.of(
                    "input[name='propertyName']", "input[name='name']", "#propertyName", "#name"), nameJob.get());
                click(driver, "kaveri.propertySearchSelector", List.of(
                    "button[type='submit']", "input[type='submit']", "button[name='search']", "#search"));
            } catch (Exception exception) {
                throw new IllegalStateException("Could not prepare a property-name search", exception);
            }
        }
    }

    private static void selectLocation(WebDriver driver, String property, String label, List<String> values) {
        Select select = new Select(locationSelect(driver, property, label));
        for (String value : values) {
            for (WebElement option : select.getOptions()) {
                if (normalize(option.getText()).equals(normalize(value))) {
                    select.selectByVisibleText(option.getText());
                    return;
                }
            }
        }
        throw new TimeoutException("Could not select a value in " + label);
    }

    private static List<String> optionTexts(WebDriver driver, String property, String label) {
        WebDriverWait wait = new WebDriverWait(driver, ELEMENT_TIMEOUT);
        return wait.until(ignored -> {
            List<String> options = new Select(locationSelect(driver, property, label)).getOptions().stream()
                .map(WebElement::getText)
                .filter(text -> !text.isBlank())
                .filter(text -> !normalize(text).contains("select"))
                .toList();
            return options.isEmpty() ? null : options;
        });
    }

    private static WebElement locationSelect(WebDriver driver, String property, String label) {
        String selector = System.getProperty(property);
        if (selector != null) {
            return waitForFirst(driver, property, List.of(selector));
        }

        WebDriverWait wait = new WebDriverWait(driver, ELEMENT_TIMEOUT);
        return wait.until(ignored -> driver.findElements(By.xpath(
                "//*[normalize-space() = '" + label + "']/following::select[1]")).stream()
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElse(null));
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static void clickStartNewApplication(WebDriver driver) {
        String selector = System.getProperty("kaveri.startApplicationSelector");
        if (selector != null) {
            waitForFirst(driver, "kaveri.startApplicationSelector", List.of(selector)).click();
            return;
        }

        WebDriverWait wait = new WebDriverWait(driver, ELEMENT_TIMEOUT);
        wait.until(ignored -> driver.findElements(By.xpath(
                "//*[self::button or @role='button'][contains(normalize-space(), 'START A NEW APPLICATION')]")).stream()
            .filter(WebElement::isDisplayed)
            .findFirst()
            .map(element -> {
                element.click();
                return true;
            })
            .orElse(false));
    }

    private static void clickAfter2004(WebDriver driver) {
        clickVisibleText(driver, "kaveri.after2004Selector", "After 01/01/2004");
    }

    private static void clickProceed(WebDriver driver) {
        clickVisibleText(driver, "kaveri.proceedSelector", "Proceed");
    }

    private static void clickVisibleText(WebDriver driver, String property, String text) {
        String selector = System.getProperty(property);
        if (selector != null) {
            waitForFirst(driver, property, List.of(selector)).click();
            return;
        }

        WebDriverWait wait = new WebDriverWait(driver, ELEMENT_TIMEOUT);
        wait.until(ignored -> driver.findElements(By.xpath(
                "//*[self::button or self::label or @role='button' or @role='radio'][normalize-space() = '" + text + "']")).stream()
            .filter(WebElement::isDisplayed)
            .findFirst()
            .map(element -> {
                element.click();
                return true;
            })
            .orElse(false));
    }

    private static void clickEcPrerequisiteContinue(WebDriver driver) {
        String selector = System.getProperty("kaveri.ecPrerequisiteContinueSelector");
        if (selector != null) {
            waitForFirst(driver, "kaveri.ecPrerequisiteContinueSelector", List.of(selector)).click();
            return;
        }

        WebDriverWait wait = new WebDriverWait(driver, ELEMENT_TIMEOUT);
        wait.until(ignored -> driver.findElements(By.xpath(
                "//*[self::button or @role='button'][normalize-space() = 'Continue']")).stream()
            .filter(WebElement::isDisplayed)
            .findFirst()
            .map(element -> {
                element.click();
                return true;
            })
            .orElse(false));
    }

    private static void clickOnlineEc(WebDriver driver) {
        String selector = System.getProperty("kaveri.onlineEcSelector");
        if (selector != null) {
            waitForFirst(driver, "kaveri.onlineEcSelector", List.of(selector)).click();
            return;
        }

        WebDriverWait wait = new WebDriverWait(driver, ELEMENT_TIMEOUT);
        wait.until(ignored -> driver.findElements(By.xpath(
                "//*[contains(normalize-space(), 'ENCUMBERANCE CERTIFICATE -(ONLINE EC)')]")).stream()
            .filter(WebElement::isDisplayed)
            .findFirst()
            .map(element -> {
                element.click();
                return true;
            })
            .orElse(false));
    }

    private static WebElement waitForFirst(WebDriver driver, String property, List<String> defaults) {
        List<String> selectors = System.getProperty(property) == null
            ? defaults
            : List.of(System.getProperty(property));
        WebDriverWait wait = new WebDriverWait(driver, ELEMENT_TIMEOUT);
        return wait.until(ignored -> selectors.stream()
            .flatMap(selector -> driver.findElements(By.cssSelector(selector)).stream())
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElse(null));
    }
}