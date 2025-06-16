package com.example.crawling_sampling;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

public class SimpleSeleniumTest {
    public static void main(String[] args) {
        // !!! 중요: CHROME_DRIVER_PATH를 실제 chromedriver.exe 파일 경로로 수정해주세요 !!!
        // 예: "C:/tools/chromedriver.exe"
        String CHROME_DRIVER_PATH = "C:/tools/chromedriver.exe";

        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);

        ChromeOptions options = new ChromeOptions();
        // --remote-allow-origins=* 옵션은 유지하는 것이 일반적입니다.
        // 만약 이 테스트에서도 data:, 가 뜬다면, 이 라인도 주석 처리하고 테스트해보세요.
        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = null;
        try {
            System.out.println("Step 1: Initializing ChromeDriver...");
            driver = new ChromeDriver(options);
            System.out.println("Step 2: ChromeDriver initialized successfully.");

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            System.out.println("Step 3: Attempting to navigate to Google.com...");
            driver.get("https://www.google.com");
            System.out.println("Step 4: Navigation command sent. Current URL: " + driver.getCurrentUrl());

            System.out.println("Step 5: Waiting for 5 seconds to observe...");
            Thread.sleep(5000); // 페이지 로드 후 5초 대기

            System.out.println("Step 6: Page title: " + driver.getTitle());

        } catch (Exception e) {
            System.err.println("!!! ERROR during Selenium test !!!");
            e.printStackTrace();
        } finally {
            if (driver != null) {
                System.out.println("Step 7: Quitting ChromeDriver...");
                driver.quit(); // 브라우저 종료
                System.out.println("Step 8: ChromeDriver quit successfully.");
            }
        }
    }
}
