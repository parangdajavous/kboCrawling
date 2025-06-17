package com.example.crawling_sampling.utils;

import com.example.crawling_sampling.kbo.KboResponseDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

public class util {

    /* 페이지 네비게이션 클릭해서 끝까지 순회 */
    public static void scrapePagedTable(WebDriver driver, String url, Consumer<Document> consumer) throws InterruptedException {
        driver.get(url);

        while (true) {
            consumer.accept(Jsoup.parse(driver.getPageSource()));

            // XPath로 "다음 페이지" 버튼 찾기
            List<WebElement> nextBtns = driver.findElements(
                    By.xpath("//div[@class='paging']//a[contains(text(),'›')]"));

            if (nextBtns.isEmpty() || nextBtns.get(0).getAttribute("class").contains("disable")) break;

            nextBtns.get(0).click();
            Thread.sleep(700);  // 페이지 전환 대기
        }
    }


    /* 숫자 파싱 */
    public static int parseI(String s) {
        try { return parseInt(s.replace(",", "")); }
        catch (Exception e) { return 0; }
    }

    public static double parseD(String s) {
        try { return parseDouble(s.replace(",", "")); }
        catch (Exception e) { return 0.0; }
    }


    /* 맞대결 전적 크롤링 함수 */
//    public static KboResponseDTO.MatchupStatsDTO doCrawlMatchup(WebDriver driver, String pitcherTeam, String pitcher, String hitterTeam, String hitter) throws InterruptedException {
//        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//        driver.get("https://www.koreabaseball.com/Record/Etc/HitVsPit.aspx");
//
//        // 1. 투수팀 선택
//        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cphContents_cphContents_ddlPitcherTeam")));
//        new Select(driver.findElement(By.id("cphContents_cphContents_ddlPitcherTeam"))).selectByVisibleText(pitcherTeam);
//        Thread.sleep(1000); // 선수 드롭다운 갱신 대기
//
//        // 2. 투수 선택
//        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("cphContents_cphContents_ddlPitcher")));
//        new Select(driver.findElement(By.id("cphContents_cphContents_ddlPitcher"))).selectByVisibleText(pitcher);
//
//        // 3. 타자팀 선택
//        new Select(driver.findElement(By.id("cphContents_cphContents_ddlHitterTeam"))).selectByVisibleText(hitterTeam);
//        Thread.sleep(1000);
//
//        // 4. 타자 선택
//        new Select(driver.findElement(By.id("cphContents_cphContents_ddlHitter"))).selectByVisibleText(hitter);
//
//        // 5. 조회 버튼 클릭
//        driver.findElement(By.id("cphContents_cphContents_btnSearch")).click();
//        Thread.sleep(1000);
//
//        // 6. 결과 테이블 파싱
//        Document doc = Jsoup.parse(driver.getPageSource());
//        Element row = doc.selectFirst("table.tData.tt tbody tr");
//        if (row == null) return null;
//
//        Elements td = row.select("td");
//        if (td.size() < 10) return null;
//
//        int ab   = parseI(td.get(2).text());   // 타수
//        int h    = parseI(td.get(3).text());   // 안타
//        int hr   = parseI(td.get(6).text());   // 홈런
//        int bb   = parseI(td.get(9).text());   // 볼넷
//        double avg = parseD(td.get(0).text()); // 타율
//        double ops = parseD(td.get(13).text());// OPS
//
//
//        return new KboResponseDTO.MatchupStatsDTO(
//                pitcher,      // 투수
//                hitter,       // 타자
//                ab,           // 타수
//                h,            // 안타
//                hr,           // 홈런
//                bb,           // 볼넷
//                avg,          // 타율
//                ops           // OPS
//        );
//    }
}
