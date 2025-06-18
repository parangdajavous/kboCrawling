package com.example.crawling_sampling.kbo;


import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.crawling_sampling.utils.util.*;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

@Slf4j
@Service
public class KboService {

    /* 투수 목록 크롤링 (선수 이름 + PlayerId / 스탯 수집) */
    public List<KboResponseDTO.PitcherFullDTO> crawlAllPitchers() {
        /* 이름 → DTO 매핑해서 이후 QS 채워 넣기 */
        Map<String, KboResponseDTO.PitcherFullDTO> map = new HashMap<>();

        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions()
                .addArguments("--headless=new", "--disable-gpu", "--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(opt);

        try {
            /* ── 1) Basic1 (ERA·WHIP 등) 페이징 순회 ───────────── */
            scrapePagedTable(driver,
                    "https://www.koreabaseball.com/Record/Player/PitcherBasic/Basic1.aspx",
                    doc -> {
                        for (Element row : doc.select("table.tData01.tt tbody tr")) {
                            Elements td = row.select("td");
                            if (td.size() < 14) continue;

                            String name = td.get(1).text();
                            String pid = td.get(1).selectFirst("a").attr("href")
                                    .replaceAll(".*playerId=", "");
                            double era = parseD(td.get(3).text());
                            int g = parseI(td.get(4).text());
                            int w = parseI(td.get(7).text());
                            int l = parseI(td.get(8).text());
                            double whip = parseD(td.get(13).text());

                            map.put(name,
                                    new KboResponseDTO.PitcherFullDTO(
                                            name, pid, era, g, w, l,
                                            0,          // QS → 나중에 채움
                                            whip));
                        }
                    });

            /* ── 2) Basic2 (QS) 페이징 순회 ──────────────────── */
            scrapePagedTable(driver,
                    "https://www.koreabaseball.com/Record/Player/PitcherBasic/Basic2.aspx",
                    doc -> {
                        for (Element row : doc.select("table.tData01.tt tbody tr")) {
                            Elements td = row.select("td");
                            if (td.size() < 13) continue;

                            String name = td.get(1).text();
                            int qs = parseI(td.get(12).text());

                            KboResponseDTO.PitcherFullDTO dto = map.get(name);
                            if (dto != null) {
                                dto.setQs(qs);      // ★ QS만 업데이트
                            }
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return new ArrayList<>(map.values());
    }


    /* 타자 목록 크롤링 (선수 이름 + PlayerId / 스탯 수집) */
    public List<KboResponseDTO.HitterFullDTO> crawlAllHitters() {
        List<KboResponseDTO.HitterFullDTO> result = new ArrayList<>();

        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions().addArguments("--headless", "--no-sandbox");
        WebDriver driver = new ChromeDriver(opt);

        try {
            driver.get("https://www.koreabaseball.com/Record/Player/HitterBasic/Basic1.aspx");

            // 전체 페이지 수 추출 (예: '1 2 >' 형태로 있으면 마지막 숫자 뽑기)
            Document firstPage = Jsoup.parse(driver.getPageSource());
            Elements pageLinks = firstPage.select("div.paging a");
            int lastPage = pageLinks.stream()
                    .map(Element::text)
                    .filter(t -> t.matches("\\d+"))
                    .mapToInt(Integer::parseInt)
                    .max().orElse(1);

            for (int page = 1; page <= lastPage; page++) {
                if (page > 1) {
                    // 페이지 버튼 클릭 (페이지가 1보다 크면 클릭 필요)
                    WebElement pageButton = driver.findElement(By.linkText(String.valueOf(page)));
                    pageButton.click();
                    Thread.sleep(1000); // 로딩 대기
                }

                // 현재 페이지 파싱
                Document doc = Jsoup.parse(driver.getPageSource());

                for (Element row : doc.select("table.tData01.tt tbody tr")) {
                    Elements td = row.select("td");
                    if (td.size() < 11) continue;

                    try {
                        String name = td.get(1).text();
                        String playerId = td.get(1).selectFirst("a").attr("href")
                                .replaceAll(".*playerId=", ""); // 필요하면 활성화
                        int ab = parseInt(td.get(6).text());
                        int h = parseInt(td.get(9).text());
                        double avg = parseDouble(td.get(3).text());
                        double ops = parseDouble(td.get(10).text());

                        result.add(new KboResponseDTO.HitterFullDTO(name, playerId, h, ab, avg, ops));
                    } catch (Exception e) {
                        System.out.println("⚠️ 파싱 실패: " + row.text());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return result;
    }


    public KboResponseDTO.MatchupStatsDTO crawlMatchup(String pitcherTeam, String pitcher, String hitterTeam, String hitter) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions()
                .addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://www.koreabaseball.com/Record/Etc/HitVsPit.aspx");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            /* 1️⃣ 드롭다운 순차 선택
             * Selenium으로 동적 요소 선택/조작*/
            new Select(driver.findElement(By.id(
                    "cphContents_cphContents_cphContents_ddlPitcherTeam")))
                    .selectByVisibleText(pitcherTeam);
            Thread.sleep(700);

            new Select(driver.findElement(By.id(
                    "cphContents_cphContents_cphContents_ddlPitcherPlayer")))
                    .selectByVisibleText(pitcher);
            Thread.sleep(700);

            new Select(driver.findElement(By.id(
                    "cphContents_cphContents_cphContents_ddlHitterTeam")))
                    .selectByVisibleText(hitterTeam);
            Thread.sleep(700);

            new Select(driver.findElement(By.id(
                    "cphContents_cphContents_cphContents_ddlHitterPlayer")))
                    .selectByVisibleText(hitter);
            Thread.sleep(700);

            /* 2️⃣ 조회 버튼 클릭 */
            driver.findElement(By.id("cphContents_cphContents_cphContents_btnSearch")).click();

    /* 3️⃣ 결과 테이블이 완전히 로드될 때까지 대기
          ─ td 개수가 14개 이상이면 OK                                               */
            wait.until(d -> {
                /* Selenium으로 전체 HTML 가져오기 */
                Document tmp = Jsoup.parse(d.getPageSource());
                Element r = tmp.selectFirst("table.tData.tt tbody tr");
                return r != null && r.select("td").size() >= 14;
            });

            /* 4️⃣ HTML 파싱
             * Jsoup으로 HTML 파싱*/
            Document doc = Jsoup.parse(driver.getPageSource());
            Element row = doc.selectFirst("table.tData.tt tbody tr");
            Elements td = row.select("td");        // 여기서 td.size() == 14 보장

            int ab = parseI(td.get(2).text());
            int h = parseI(td.get(3).text());
            int hr = parseI(td.get(6).text());
            int bb = parseI(td.get(8).text());
            double avg = parseD(td.get(0).text());
            double ops = parseD(td.get(13).text());

            return new KboResponseDTO.MatchupStatsDTO(
                    pitcher, hitter, ab, h, hr, bb, avg, ops);

        } catch (Exception e) {
            throw new RuntimeException("맞대결 크롤링 실패", e);
        } finally {
            driver.quit();
        }
    }


}

