package com.example.crawling_sampling.kbo;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KboService {

    private static final String DETAIL_URL =
            "https://www.koreabaseball.com/Record/Player/PitcherDetail/Basic.aspx?playerId=%s";
    private static final String LIST_URL =
            "https://www.koreabaseball.com/Record/Player/PitcherBasic/Basic1.aspx";

    /* ────────────────────────────────────────────────
       1) 투수 전체 목록 (playerId + name) 크롤링
     ──────────────────────────────────────────────── */
    public List<KboResponseDTO.PlayerInfo> crawlPitcherPlayerList() {

        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions()
                .addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(opt);

        List<KboResponseDTO.PlayerInfo> players = new ArrayList<>();

        try {
            driver.get(LIST_URL);
            Thread.sleep(1500);

            Document doc = Jsoup.parse(driver.getPageSource());
            for (Element a : doc.select("table.tData01.tt tbody a[href*='playerId=']")) {
                String name = a.text().trim();
                String playerId = a.attr("href")
                        .substring(a.attr("href").indexOf("playerId=") + 9);
                players.add(new KboResponseDTO.PlayerInfo(name, playerId));
            }

        } catch (Exception e) {
            log.error("📛 선수 목록 크롤링 실패", e);
        } finally {
            driver.quit();
        }
        return players;   // 필요하다면 여기서 DB 저장
    }

    /* ────────────────────────────────────────────────
       2) 특정 투수 한 명 상세 스탯 크롤링
     ──────────────────────────────────────────────── */
    public KboResponseDTO.PitcherStatsDTO crawlPitcherDetail(String playerId) {

        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions()
                .addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(opt);

        try {
            driver.get(DETAIL_URL.formatted(playerId));
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.tbl.tt")));

            Document doc = Jsoup.parse(driver.getPageSource());

            /* ── 첫 번째 테이블 (ERA, G, W, L) ── */
            Element table1 = doc.selectFirst("table.tbl.tt.mb5");
            if (table1 == null) throw new IllegalStateException("table1 not found");
            Elements t1 = table1.selectFirst("tbody tr").select("td");
            double era = parseD(t1.get(1).text());
            int g = parseI(t1.get(2).text());
            int w = parseI(t1.get(5).text());
            int l = parseI(t1.get(6).text());

            /* ── 두 번째 테이블 (WHIP, QS) ── */
            Element table2 = doc.select("table.tbl.tt").get(1);
            Elements t2 = table2.selectFirst("tbody tr").select("td");
            double whip = parseD(t2.get(10).text());
            int qs = parseI(t2.get(12).text());

            return new KboResponseDTO.PitcherStatsDTO(era, g, w, l, qs, whip);

        } catch (Exception e) {
            log.error("📛 크롤링 실패 playerId={}: {}", playerId, e.getMessage());
            return null;
        } finally {
            driver.quit();
        }
    }

    /* ────────────────────────────────────────────────
       3) 전체 투수 스탯 한꺼번에 크롤링
          - 반환 타입 / 변수 타입 모두 PitcherStatsDTO 로 통일
     ──────────────────────────────────────────────── */
    public List<KboResponseDTO.PitcherFullDTO> crawlAllPitchers() {
        List<KboResponseDTO.PlayerInfo> players = crawlPitcherPlayerList();
        List<KboResponseDTO.PitcherFullDTO> fullList = new ArrayList<>();

        for (KboResponseDTO.PlayerInfo p : players) {
            KboResponseDTO.PitcherStatsDTO stats = crawlPitcherDetail(p.getPlayerId());
            if (stats != null) {
                fullList.add(new KboResponseDTO.PitcherFullDTO(
                        p.getName(), p.getPlayerId(),
                        stats.getEra(), stats.getG(), stats.getW(),
                        stats.getL(), stats.getQs(), stats.getWhip()
                ));
            }
        }
        return fullList;
    }


    private static final String HITTER_LIST_URL =
            "https://www.koreabaseball.com/Record/Player/HitterBasic/Basic1.aspx";
    private static final String HITTER_DETAIL_URL =
            "https://www.koreabaseball.com/Record/Player/HitterDetail/Basic.aspx?playerId=%s";


    // 타자 목록 크롤링 (선수 이름 + ID 수집)
    public List<KboResponseDTO.PlayerInfo> crawlHitterPlayerList() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions()
                .addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(opt);

        List<KboResponseDTO.PlayerInfo> players = new ArrayList<>();

        try {
            driver.get(HITTER_LIST_URL);
            Thread.sleep(1500); // JS 로딩 대기

            Document doc = Jsoup.parse(driver.getPageSource());

            for (Element a : doc.select("table.tData01.tt tbody a[href*='playerId=']")) {
                String name = a.text().trim();
                String playerId = a.attr("href").split("playerId=")[1];
                players.add(new KboResponseDTO.PlayerInfo(name, playerId));
            }
        } catch (Exception e) {
            log.error("📛 타자 목록 크롤링 실패", e);
        } finally {
            driver.quit();
        }

        return players;
    }

    // 타자 상세 정보 크롤링 (타수, 안타, 타율, OPS)
    public KboResponseDTO.HitterStatsDTO crawlHitterDetail(String playerId) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions().addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(opt);

        try {
            String url = "https://www.koreabaseball.com/Record/Player/HitterDetail/Basic.aspx?playerId=" + playerId;
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table.tbl.tt")));

            Document doc = Jsoup.parse(driver.getPageSource());

            // 첫 번째 테이블 (AVG, AB, H)
            Element table1 = doc.selectFirst("table.tbl.tt.mb5");
            if (table1 == null) throw new IllegalStateException("📛 table1 없음");

            Elements t1 = table1.select("tbody tr").first().select("td");
            int ab = parseI(t1.get(6).text());   // AB
            int h = parseI(t1.get(9).text());   // H
            double avg = parseD(t1.get(3).text()); // AVG

            // 두 번째 테이블 (OPS)
            Element table2 = doc.select("table.tbl.tt").get(1);  // 두 번째 테이블
            Elements t2 = table2.select("tbody tr").first().select("td");
            double ops = parseD(t2.get(10).text());  // OPS

            return new KboResponseDTO.HitterStatsDTO(h, ab, avg, ops);

        } catch (Exception e) {
            log.error("📛 타자 크롤링 실패 playerId={}: {}", playerId, e.getMessage());
            return null;
        } finally {
            driver.quit();
        }
    }


    // 전체 타자 크롤링 (목록 + 상세정보 → DTO 통합)
    public List<KboResponseDTO.HitterStatsDTO> crawlAllHitters() {
        List<KboResponseDTO.PlayerInfo> players = crawlHitterPlayerList();
        List<KboResponseDTO.HitterStatsDTO> result = new ArrayList<>();

        for (KboResponseDTO.PlayerInfo p : players) {
            KboResponseDTO.HitterStatsDTO dto = crawlHitterDetail(p.getPlayerId());
            if (dto != null) {
                result.add(dto);
            }

            // ✅ 테스트 시 일부만 돌리기 (속도 문제 시 주석 해제)
            // if (result.size() >= 10) break;
        }

        return result;
    }

    /* ---------- 파싱 보조 ---------- */
    private double parseD(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int parseI(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }


}

