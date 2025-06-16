package com.example.crawling_sampling.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

public class SeleniumUtil {
    private static final String CHROME_DRIVER_PATH = "C:/tools/chromedriver.exe"; // 실제 경로로 변경하세요!

    // WebDriver 인스턴스는 한 번만 생성하고 재사용하는 것이 효율적입니다.
    // 하지만 각 요청마다 독립적인 드라이버가 필요하다면, initDriver()에서 새로 생성하고 finally 블록에서 quit()하는 방식이 맞습니다.
    // 여기서는 기존 Java 코드의 흐름을 따르기 위해 각 메소드 호출 시 드라이버를 생성하고 종료합니다.
    // 스프링 환경에서는 @Bean 또는 @Scope("prototype")을 고려할 수 있습니다.

    public static WebDriver initDriver() {
        System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless"); // 브라우저 창을 띄우지 않음 (백그라운드 실행)
        //options.addArguments("--disable-gpu");
        //options.addArguments("--no-sandbox");
        //options.addArguments("--disable-dev-shm-usage"); // Docker 환경에서 유용
        options.addArguments("--remote-allow-origins=*"); // CORS 문제 해결 (특정 상황에서 필요)

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5)); // 암시적 대기
        return driver;
    }

    public static void quitDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
        }
    }

}
