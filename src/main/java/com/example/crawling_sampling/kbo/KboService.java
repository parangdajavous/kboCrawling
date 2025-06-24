package com.example.crawling_sampling.kbo;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class KboService {


    // 맞대결 전적
//    public KboResponseDTO.MatchupStatsDTO crawlMatchup(String pitcherTeam, String pitcher, String hitterTeam, String hitter) {
//        WebDriverManager.chromedriver().setup();
//        ChromeOptions options = new ChromeOptions()
//                .addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
//        WebDriver driver = new ChromeDriver(options);
//
//        try {
//            driver.get("https://www.koreabaseball.com/Record/Etc/HitVsPit.aspx");
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//
//            /* 1️⃣ 드롭다운 순차 선택
//             * Selenium으로 동적 요소 선택/조작*/
//            new Select(driver.findElement(By.id(
//                    "cphContents_cphContents_cphContents_ddlPitcherTeam")))
//                    .selectByVisibleText(pitcherTeam);
//            Thread.sleep(700);
//
//            new Select(driver.findElement(By.id(
//                    "cphContents_cphContents_cphContents_ddlPitcherPlayer")))
//                    .selectByVisibleText(pitcher);
//            Thread.sleep(700);
//
//            new Select(driver.findElement(By.id(
//                    "cphContents_cphContents_cphContents_ddlHitterTeam")))
//                    .selectByVisibleText(hitterTeam);
//            Thread.sleep(700);
//
//            new Select(driver.findElement(By.id(
//                    "cphContents_cphContents_cphContents_ddlHitterPlayer")))
//                    .selectByVisibleText(hitter);
//            Thread.sleep(700);
//
//            /* 2️⃣ 조회 버튼 클릭 */
//            driver.findElement(By.id("cphContents_cphContents_cphContents_btnSearch")).click();
//
//    /* 3️⃣ 결과 테이블이 완전히 로드될 때까지 대기
//          ─ td 개수가 14개 이상이면 OK                                               */
//            wait.until(d -> {
//                /* Selenium으로 전체 HTML 가져오기 */
//                Document tmp = Jsoup.parse(d.getPageSource());
//                Element r = tmp.selectFirst("table.tData.tt tbody tr");
//                return r != null && r.select("td").size() >= 14;
//            });
//
//            /* 4️⃣ HTML 파싱
//             * Jsoup으로 HTML 파싱*/
//            Document doc = Jsoup.parse(driver.getPageSource());
//            Element row = doc.selectFirst("table.tData.tt tbody tr");
//            Elements td = row.select("td");        // 여기서 td.size() == 14 보장
//
//            int ab = parseI(td.get(2).text());
//            int h = parseI(td.get(3).text());
//            int hr = parseI(td.get(6).text());
//            int bb = parseI(td.get(8).text());
//            double avg = parseD(td.get(0).text());
//            double ops = parseD(td.get(13).text());
//
//            return new KboResponseDTO.MatchupStatsDTO(
//                    pitcher, hitter, ab, h, hr, bb, avg, ops);
//
//        } catch (Exception e) {
//            throw new RuntimeException("맞대결 크롤링 실패", e);
//        } finally {
//            driver.quit();
//        }
//    }

    // 상대 선발투수 라인업
    // 선택한 팀이 홈팀일 경우 원정팀 선발투수를 보여주고, 선택한 팀이 원정팀일 경우 홈팀의 선발투수를 보여준다
//    public KboResponseDTO.StartingPitcherFullDTO crawlStartingPitcher(String gameId, String teamType) {
//        WebDriver driver = null;
//        try {
//            String targetDate = gameId.substring(0, 8);
//
//            ChromeOptions options = new ChromeOptions();
//            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
//            driver = new ChromeDriver(options);
//
//            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + targetDate;
//            driver.get(url);
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));
//
//            WebElement gameEl = driver.findElement(By.cssSelector("li.game-cont[g_id='" + gameId + "']"));
//            if (gameEl == null) {
//                log.warn("[WARN] gameId={} 해당 경기 요소를 찾을 수 없습니다", gameId);
//                return null;
//            }
//
//            String awayTeamId = gameEl.getAttribute("away_id");
//            String homeTeamId = gameEl.getAttribute("home_id");
//            String awayPitId = gameEl.getAttribute("away_p_id");
//            String homePitId = gameEl.getAttribute("home_p_id");
//
//            // ✅ 선발투수 발표 전 (둘 중 하나라도 없음)
//            if (awayPitId.isEmpty() || homePitId.isEmpty()) {
//                return new KboResponseDTO.StartingPitcherFullDTO(
//                        "", "", 0.0, 0, "없음", 0, 0.0,
//                        teamType, gameId, "라인업 발표 전입니다"
//                );
//            }
//
//            // ✅ 전력분석 API 요청
//            String apiUrl = "https://www.koreabaseball.com/ws/Schedule.asmx/GetPitcherRecordAnalysis";
//            Connection.Response response = Jsoup.connect(apiUrl)
//                    .method(Connection.Method.POST)
//                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
//                    .ignoreContentType(true)
//                    .data("leId", "1")
//                    .data("srId", "0")
//                    .data("seasonId", "2025")
//                    .data("awayTeamId", awayTeamId)
//                    .data("awayPitId", awayPitId)
//                    .data("homeTeamId", homeTeamId)
//                    .data("homePitId", homePitId)
//                    .data("groupSc", "SEASON")
//                    .execute();
//
//            JsonObject jsonObj = JsonParser.parseString(response.body()).getAsJsonObject();
//            JsonObject pitcherObj = jsonObj.getAsJsonArray("rows")
//                    .get(teamType.equals("away") ? 0 : 1)
//                    .getAsJsonObject();
//
//            JsonArray rowArray = pitcherObj.getAsJsonArray("row");
//            if (rowArray == null || rowArray.size() < 7) {
//                return new KboResponseDTO.StartingPitcherFullDTO(
//                        "", "", 0.0, 0, "없음", 0, 0.0,
//                        teamType, gameId, "라인업 발표 전입니다"
//                );
//            }
//
//            // ✅ 데이터 추출
//            String rawHtml = rowArray.get(0).getAsJsonObject().get("Text").getAsString();
//            String eraStr = rowArray.get(1).getAsJsonObject().get("Text").getAsString();
//            String gameCntStr = rowArray.get(3).getAsJsonObject().get("Text").getAsString();
//
//            Element recordEl = Jsoup.parse(rawHtml).selectFirst(".record");
//            String resultStr = "-";
//            if (recordEl != null) {
//                String recordText = recordEl.text().replace("시즌 ", "").trim();
//                resultStr = recordText.isEmpty() ? "없음"
//                        : recordText.contains("VS") ? recordText.split("VS")[0].trim()
//                        : recordText;
//            } else {
//                resultStr = "없음";
//            }
//
//            String qsStr = rowArray.get(5).getAsJsonObject().get("Text").getAsString();
//            String whipStr = rowArray.get(6).getAsJsonObject().get("Text").getAsString();
//
//            Document parsedHtml = Jsoup.parse(rawHtml);
//            Element nameEl = parsedHtml.selectFirst(".name");
//            String name = nameEl != null ? nameEl.text() : "";
//
//            Elements imgs = parsedHtml.select("img");
//            String imgPath = (imgs.size() >= 2) ? imgs.get(1).attr("src") : "";
//            String img = imgPath.startsWith("http") ? imgPath : "https:" + imgPath;
//
//            double era = Double.parseDouble(eraStr);
//            int gameCnt = Integer.parseInt(gameCntStr);
//            int qs = Integer.parseInt(qsStr);
//            double whip = Double.parseDouble(whipStr);
//
//            // ✅ 정상 데이터 리턴 (message는 null)
//            return new KboResponseDTO.StartingPitcherFullDTO(
//                    name, img, era, gameCnt, resultStr, qs, whip,
//                    teamType, gameId,
//            );
//
//        } catch (Exception e) {
//            log.error("[ERROR] API 기반 선발투수 크롤링 실패: gameId={}, teamType={}", gameId, teamType, e);
//            return null;
//        } finally {
//            if (driver != null) driver.quit();
//        }
//    }

    // 오늘 경기의 전체 선발투수 정보
//    public List<KboResponseDTO.StartingPitcherFullDTO> crawlTodayStartingPitchers() {
//        List<KboResponseDTO.StartingPitcherFullDTO> result = new ArrayList<>();
//        WebDriver driver = null;
//
//        try {
//            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//
//            ChromeOptions options = new ChromeOptions();
//            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
//            driver = new ChromeDriver(options);
//
//            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + today;
//            driver.get(url);
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));
//
//            List<WebElement> gameEls = driver.findElements(By.cssSelector("li.game-cont"));
//            for (WebElement gameEl : gameEls) {
//                String gameId = gameEl.getAttribute("g_id");
//                if (gameId == null || gameId.isEmpty()) continue;
//
//                String awayPitId = gameEl.getAttribute("away_p_id");
//                String homePitId = gameEl.getAttribute("home_p_id");
//
//                // ✅ away 팀 선발투수
//                if (awayPitId != null && !awayPitId.isEmpty()) {
//                    KboResponseDTO.StartingPitcherFullDTO awayDto = crawlStartingPitcher(gameId, "away");
//                    if (awayDto != null) {
//                        result.add(awayDto);
//                    } else {
//                        result.add(new KboResponseDTO.StartingPitcherFullDTO(
//                                "", "", 0.0, 0, "없음", 0, 0.0,
//                                "away", gameId, "라인업 발표 전입니다"
//                        ));
//                    }
//                }
//
//                // ✅ home 팀 선발투수
//                if (homePitId != null && !homePitId.isEmpty()) {
//                    KboResponseDTO.StartingPitcherFullDTO homeDto = crawlStartingPitcher(gameId, "home");
//                    if (homeDto != null) {
//                        result.add(homeDto);
//                    } else {
//                        result.add(new KboResponseDTO.StartingPitcherFullDTO(
//                                "", "", 0.0, 0, "없음", 0, 0.0,
//                                "home", gameId, "라인업 발표 전입니다"
//                        ));
//                    }
//                }
//            }
//
//            return result;
//
//        } catch (Exception e) {
//            log.error("[ERROR] 오늘 경기 전체 선발투수 정보 수집 실패", e);
//            return result;
//        } finally {
//            if (driver != null) driver.quit();
//        }
//    }


    // 내일 경기의 전체 선발투수 정보
//    public List<KboResponseDTO.StartingPitcherFullDTO> crawlTomorrowStartingPitchers() {
//        List<KboResponseDTO.StartingPitcherFullDTO> result = new ArrayList<>();
//        WebDriver driver = null;
//
//        try {
//            String targetDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//            System.out.println("[DEBUG] 크롤링 시작: " + targetDate);
//
//            ChromeOptions options = new ChromeOptions();
//            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
//            driver = new ChromeDriver(options);
//
//            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + targetDate;
//            driver.get(url);
//
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));
//
//
//            List<WebElement> gameEls = driver.findElements(By.cssSelector("li.game-cont"));
//            System.out.println("[DEBUG] 경기 수: " + gameEls.size());
//
//            for (WebElement gameEl : gameEls) {
//                String gameId = gameEl.getAttribute("g_id");
//                if (gameId == null || gameId.isEmpty()) continue;
//
//                String awayPitId = gameEl.getAttribute("away_p_id");
//                String homePitId = gameEl.getAttribute("home_p_id");
//
//                System.out.println("[DEBUG] gameId=" + gameId + ", away_p_id=" + awayPitId + ", home_p_id=" + homePitId);
//
//                // ✅ away 선발투수
//                if (awayPitId == null || awayPitId.isEmpty()) {
//                    result.add(new KboResponseDTO.StartingPitcherFullDTO(
//                            "", "", 0.0, 0, "없음", 0, 0.0,
//                            "away", gameId, "라인업 발표 전입니다"
//                    ));
//                } else {
//                    KboResponseDTO.StartingPitcherFullDTO awayDto = crawlStartingPitcher(gameId, "away");
//                    result.add(awayDto != null ? awayDto :
//                            new KboResponseDTO.StartingPitcherFullDTO(
//                                    "", "", 0.0, 0, "없음", 0, 0.0,
//                                    "away", gameId, "라인업 발표 전입니다"
//                            ));
//                }
//
//                // ✅ home 선발투수
//                if (homePitId == null || homePitId.isEmpty()) {
//                    result.add(new KboResponseDTO.StartingPitcherFullDTO(
//                            "", "", 0.0, 0, "없음", 0, 0.0,
//                            "home", gameId, "라인업 발표 전입니다"
//                    ));
//                } else {
//                    KboResponseDTO.StartingPitcherFullDTO homeDto = crawlStartingPitcher(gameId, "home");
//                    result.add(homeDto != null ? homeDto :
//                            new KboResponseDTO.StartingPitcherFullDTO(
//                                    "", "", 0.0, 0, "없음", 0, 0.0,
//                                    "home", gameId, "라인업 발표 전입니다"
//                            ));
//                }
//            }
//
//            return result;
//
//        } catch (Exception e) {
//            log.error("[ERROR] 내일 경기 전체 선발투수 정보 수집 실패", e);
//            return result;
//        } finally {
//            if (driver != null) driver.quit();
//        }
//    }


    // 경기 전체 선발투수 리스트
    public List<KboResponseDTO.StartingPitcherFullDTO> crawlStartingPitchers() {
        List<KboResponseDTO.StartingPitcherFullDTO> result = new ArrayList<>();
        WebDriver driver = null;

        try {
            String targetDate = LocalDate.now()
                    .plusDays(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(options);

            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + targetDate;
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));

            List<WebElement> gameEls = driver.findElements(By.cssSelector("li.game-cont"));
            for (WebElement gameEl : gameEls) {
                String gameId = gameEl.getAttribute("g_id");
                String awayTeamId = gameEl.getAttribute("away_id");
                String homeTeamId = gameEl.getAttribute("home_id");
                String awayTeamName = gameEl.getAttribute("away_nm");   // ⬅️ 팀명 추가
                String homeTeamName = gameEl.getAttribute("home_nm");   // ⬅️ 팀명 추가
                String awayPitId = gameEl.getAttribute("away_p_id");
                String homePitId = gameEl.getAttribute("home_p_id");

                if (awayPitId == null || homePitId == null ||
                        awayPitId.isBlank() || homePitId.isBlank()) {
                    result.add(KboResponseDTO.StartingPitcherFullDTO.empty("away", gameId, awayTeamName));
                    result.add(KboResponseDTO.StartingPitcherFullDTO.empty("home", gameId, homeTeamName));
                    continue;
                }

                JsonObject jsonObj = callPitcherAnalysisApi(awayTeamId, awayPitId, homeTeamId, homePitId);
                if (jsonObj == null) continue;

                for (String teamType : List.of("away", "home")) {
                    int idx = teamType.equals("away") ? 0 : 1;
                    String teamName = teamType.equals("away") ? awayTeamName : homeTeamName;  // ⬅️ 선택

                    JsonObject pitcherObj = jsonObj.getAsJsonArray("rows").get(idx).getAsJsonObject();
                    JsonArray rowArray = pitcherObj.getAsJsonArray("row");

                    String html = rowArray.get(0).getAsJsonObject().get("Text").getAsString();
                    Document parsed = Jsoup.parse(html);
                    String name = parsed.selectFirst(".name").text();
                    String resultStr = parseResultString(parsed);
                    String imgUrl = parseImgUrl(parsed);

                    double era = Double.parseDouble(rowArray.get(1).getAsJsonObject().get("Text").getAsString());
                    int games = Integer.parseInt(rowArray.get(3).getAsJsonObject().get("Text").getAsString());
                    int qs = Integer.parseInt(rowArray.get(5).getAsJsonObject().get("Text").getAsString());
                    double whip = Double.parseDouble(rowArray.get(6).getAsJsonObject().get("Text").getAsString());

                    result.add(new KboResponseDTO.StartingPitcherFullDTO(
                            name, imgUrl, era, games, resultStr, qs, whip,
                            teamType, gameId, teamName));
                }
            }
        } catch (Exception e) {
            log.error("[ERROR] 통합 선발투수 크롤링 실패", e);
        } finally {
            if (driver != null) driver.quit();
        }
        return result;
    }

    // 함수
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
            log.error("[ERROR] API 호출 실패", e);
            return null;
        }
    }

    private String parseResultString(Document doc) {
        Element recordEl = doc.selectFirst(".record");
        if (recordEl == null) return "없음";
        String txt = recordEl.text().replace("시즌 ", "").trim();
        return txt.isEmpty() ? "없음" : txt.contains("VS") ? txt.split("VS")[0].trim() : txt;
    }

    private String parseImgUrl(Document doc) {
        Elements imgs = doc.select("img");
        if (imgs.size() >= 2) {
            String path = imgs.get(1).attr("src");
            return path.startsWith("http") ? path : "https:" + path;
        }
        return "";
    }


}

