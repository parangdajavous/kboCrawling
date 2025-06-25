package com.example.crawling_sampling.kbo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrawlHitterLineupAndMachUp {


    // 선발투수
    public Map<String, String> startingPitchers() {
        WebDriverManager.chromedriver().setup();
        Map<String, String> pitcherMap = new HashMap<>();
        WebDriver driver = null;

        try {
            ChromeOptions opts = new ChromeOptions()
                    .addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");

            driver = new ChromeDriver(opts);

            String tomorrow = LocalDate.now()
                    .plusDays(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + tomorrow;
            driver.get(url);

            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));

            for (WebElement gameEl : driver.findElements(By.cssSelector("li.game-cont"))) {
                String gameId = gameEl.getAttribute("g_id");
                String awayTeamId = gameEl.getAttribute("away_id");
                String homeTeamId = gameEl.getAttribute("home_id");
                String awayPitId = gameEl.getAttribute("away_p_id");
                String homePitId = gameEl.getAttribute("home_p_id");
                String homeTeamName = gameEl.getAttribute("home_nm");
                String awayTeamName = gameEl.getAttribute("away_nm");

                if (awayPitId == null || homePitId == null || awayPitId.isBlank() || homePitId.isBlank())
                    continue;

                JsonObject api = callPitcherAnalysisApi(awayTeamId, awayPitId, homeTeamId, homePitId);
                if (api == null) continue;

                for (String teamType : List.of("away", "home")) {
                    int idx = teamType.equals("away") ? 0 : 1;
                    String teamName = teamType.equals("away") ? awayTeamName : homeTeamName;

                    JsonArray row = api.getAsJsonArray("rows").get(idx).getAsJsonObject().getAsJsonArray("row");
                    Document html = Jsoup.parse(row.get(0).getAsJsonObject().get("Text").getAsString());
                    String pitcherName = html.selectFirst("span.name").text().trim();

                    String key = gameId + "_" + teamName;
                    pitcherMap.put(key, pitcherName);

                    System.out.println("✔ " + key + " = " + pitcherName);
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 선발투수 크롤링 실패: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }

        return pitcherMap;
    }


    // 내부 전용 메서드
    private KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO crawlMatchup(
            String pitcherTeamNm, String pitcherNm,
            String hitterTeamNm, String hitterNm) {

        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            driver.get("https://www.koreabaseball.com/Record/Etc/HitVsPit.aspx");

            new Select(wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("cphContents_cphContents_cphContents_ddlPitcherTeam")))).selectByVisibleText(pitcherTeamNm);
            new Select(wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("cphContents_cphContents_cphContents_ddlPitcherPlayer")))).selectByVisibleText(pitcherNm);
            new Select(wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("cphContents_cphContents_cphContents_ddlHitterTeam")))).selectByVisibleText(hitterTeamNm);
            new Select(wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("cphContents_cphContents_cphContents_ddlHitterPlayer")))).selectByVisibleText(hitterNm);

            driver.findElement(By.id("cphContents_cphContents_cphContents_btnSearch")).click();

            wait.until(d -> Jsoup.parse(d.getPageSource())
                    .selectFirst("table.tData.tt tbody tr") != null);

            Document doc = Jsoup.parse(driver.getPageSource());
            Element row = doc.selectFirst("table.tData.tt tbody tr");
            if (row == null) return null;

            Elements td = row.select("td");
            Integer ab = parseIntSafe(td.get(2).text());
            Integer h = parseIntSafe(td.get(3).text());
            double avg = parseDoubleSafe(td.get(0).text());
            double ops = parseDoubleSafe(td.get(13).text());

            return new KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO(ab, h, avg, ops);

        } catch (Exception e) {
            System.out.println("❌ 전적 조회 실패: " + hitterNm + " vs " + pitcherNm);
            return null;
        } finally {
            driver.quit();
        }
    }



    // 타자 라인업
    public void crawlAllMatchups() {
        Map<String, String> pitcherMap = startingPitchers(); // gameId_teamName → pitcherName
        WebDriver driver = new ChromeDriver();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String url = "https://www.koreabaseball.com/Default.aspx";

        List<KboRequest.HitterSaveDTO> allGames = new ArrayList<>();

        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));

            List<WebElement> games = driver.findElements(By.cssSelector("li.game-cont"));

            for (WebElement game : games) {
                String gameId = game.getAttribute("g_id");
                String homeTeamName = game.getAttribute("home_nm").trim();
                String awayTeamName = game.getAttribute("away_nm").trim();

                String homePitcher = pitcherMap.get(gameId + "_" + homeTeamName);
                String awayPitcher = pitcherMap.get(gameId + "_" + awayTeamName);

                if (homePitcher == null || awayPitcher == null) {
                    System.out.println("❌ 선발투수 정보 없음: " + gameId);
                    continue;
                }

                System.out.printf("▶ %s vs %s / 선발: %s - %s\n",
                        awayTeamName, homeTeamName, awayPitcher, homePitcher);

                WebElement previewBtn = game.findElement(By.cssSelector("a#btnGame"));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", previewBtn);

                WebElement lineupTab = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//ul[@id='tabGame']//a[contains(text(),'\ub77c\uc778\uc5c5 \ubd84\uc11d')]")
                ));
                lineupTab.click();

                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("#tblAwayLineUp tbody tr, #tblHomeLineUp tbody tr")
                ));

                List<KboRequest.HitterSaveDTO.HitterInfo> allHitters = new ArrayList<>();

                List<WebElement> awayRows = driver.findElements(By.cssSelector("#tblAwayLineUp tbody tr"));
                for (int i = 0; i < awayRows.size(); i++) {
                    List<WebElement> cols = awayRows.get(i).findElements(By.tagName("td"));
                    if (cols.size() < 4) continue;

                    String hitterName = cols.get(2).getText().trim();
                    String position = cols.get(3).getText().trim();
                    Double seasonAVG = parseSeasonAVG(cols);

                    var matchup = crawlMatchup(homeTeamName, homePitcher, awayTeamName, hitterName);
                    List<KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO> matchupList =
                            matchup == null ? new ArrayList<>() : List.of(matchup);

                    allHitters.add(new KboRequest.HitterSaveDTO.HitterInfo(
                            awayTeamName, i + 1, hitterName, position, matchupList, seasonAVG,
                            homePitcher, homeTeamName
                    ));
                }

                List<WebElement> homeRows = driver.findElements(By.cssSelector("#tblHomeLineUp tbody tr"));
                for (int i = 0; i < homeRows.size(); i++) {
                    List<WebElement> cols = homeRows.get(i).findElements(By.tagName("td"));
                    if (cols.size() < 4) continue;

                    String hitterName = cols.get(2).getText().trim();
                    String position = cols.get(3).getText().trim();
                    Double seasonAVG = parseSeasonAVG(cols);

                    KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO matchup =
                            crawlMatchup(awayTeamName, awayPitcher, homeTeamName, hitterName);
                    List<KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO> matchupList =
                            matchup == null ? new ArrayList<>() : List.of(matchup);

                    allHitters.add(new KboRequest.HitterSaveDTO.HitterInfo(
                            homeTeamName, i + 1, hitterName, position, matchupList, seasonAVG,
                            awayPitcher, awayTeamName
                    ));
                }

                allGames.add(new KboRequest.HitterSaveDTO(gameId, allHitters, null));
                Thread.sleep(1000);
            }

            System.out.println("✅ 전체 경기 맞대게 수집 완료: " + allGames.size() + "경기");

        } catch (Exception e) {
            System.err.println("[ERROR] 전체 맞대게 수집 실패: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }


    // KBO 내부 API 호출 : 투수 시즌 스탯 JSON 얻기
    private JsonObject callPitcherAnalysisApi(String awayTeamId, String awayPitId, String homeTeamId, String homePitId) {
        try {
            Connection.Response response = Jsoup.connect("https://www.koreabaseball.com/ws/Schedule.asmx/GetPitcherRecordAnalysis")
                    .method(Connection.Method.POST)
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .ignoreContentType(true)
                    .data("leId", "1")
                    .data("srId", "0")
                    .data("seasonId", "2025")
                    .data("awayTeamId", awayTeamId)
                    .data("awayPitId", awayPitId)
                    .data("homeTeamId", homeTeamId)
                    .data("homePitId", homePitId)
                    .data("groupSc", "SEASON")
                    .execute();

            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (IOException e) {
            System.out.println("[ERROR] API 호출 실패");
            return null;
        }
    }



    /*------------------ Util함수로 뺄 예정 ------------------*/

    private Double parseSeasonAVG(List<WebElement> cols) {
        try {
            String avgText = cols.get(cols.size() - 1).getText().trim(); // 예: ".294"
            if (avgText.equals("-") || avgText.isEmpty()) return 0.0;
            return Double.parseDouble(avgText);
        } catch (Exception e) {
            return 0.0;
        }
    }




    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }


    public static void main(String[] args) {
        CrawlHitterLineupAndMachUp cah = new CrawlHitterLineupAndMachUp();
        cah.startingPitchers();
    }
}
