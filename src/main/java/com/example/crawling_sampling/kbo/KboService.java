package com.example.crawling_sampling.kbo;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // 상대 선발투수 라인업
    // 선택한 팀이 홈팀일 경우 원정팀 선발투수를 보여주고, 선택한 팀이 원정팀일 경우 홈팀의 선발투수를 보여준다
    public KboResponseDTO.StartingPitcherFullDTO crawlStartingPitcher(String gameId, String teamType) {
        WebDriver driver = null;
        try {
            String targetDate = gameId.substring(0, 8);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(options);

            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + targetDate;
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));

            WebElement gameEl = driver.findElement(By.cssSelector("li.game-cont[g_id='" + gameId + "']"));
            if (gameEl == null) {
                log.warn("[WARN] gameId={} 해당 경기 요소를 찾을 수 없습니다", gameId);
                return null;
            }

            String awayTeamId = gameEl.getAttribute("away_id");
            String homeTeamId = gameEl.getAttribute("home_id");
            String awayPitId = gameEl.getAttribute("away_p_id");
            String homePitId = gameEl.getAttribute("home_p_id");

            // ✅ 선발투수 발표 전 (둘 중 하나라도 없음)
            if (awayPitId.isEmpty() || homePitId.isEmpty()) {
                return new KboResponseDTO.StartingPitcherFullDTO(
                        "", "", 0.0, 0, "없음", 0, 0.0,
                        teamType, gameId, "라인업 발표 전입니다"
                );
            }

            // ✅ 전력분석 API 요청
            String apiUrl = "https://www.koreabaseball.com/ws/Schedule.asmx/GetPitcherRecordAnalysis";
            Connection.Response response = Jsoup.connect(apiUrl)
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

            JsonObject jsonObj = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject pitcherObj = jsonObj.getAsJsonArray("rows")
                    .get(teamType.equals("away") ? 0 : 1)
                    .getAsJsonObject();

            JsonArray rowArray = pitcherObj.getAsJsonArray("row");
            if (rowArray == null || rowArray.size() < 7) {
                return new KboResponseDTO.StartingPitcherFullDTO(
                        "", "", 0.0, 0, "없음", 0, 0.0,
                        teamType, gameId, "라인업 발표 전입니다"
                );
            }

            // ✅ 데이터 추출
            String rawHtml = rowArray.get(0).getAsJsonObject().get("Text").getAsString();
            String eraStr = rowArray.get(1).getAsJsonObject().get("Text").getAsString();
            String gameCntStr = rowArray.get(3).getAsJsonObject().get("Text").getAsString();

            Element recordEl = Jsoup.parse(rawHtml).selectFirst(".record");
            String resultStr = "-";
            if (recordEl != null) {
                String recordText = recordEl.text().replace("시즌 ", "").trim();
                resultStr = recordText.isEmpty() ? "없음"
                        : recordText.contains("VS") ? recordText.split("VS")[0].trim()
                        : recordText;
            } else {
                resultStr = "없음";
            }

            String qsStr = rowArray.get(5).getAsJsonObject().get("Text").getAsString();
            String whipStr = rowArray.get(6).getAsJsonObject().get("Text").getAsString();

            Document parsedHtml = Jsoup.parse(rawHtml);
            Element nameEl = parsedHtml.selectFirst(".name");
            String name = nameEl != null ? nameEl.text() : "";

            Elements imgs = parsedHtml.select("img");
            String imgPath = (imgs.size() >= 2) ? imgs.get(1).attr("src") : "";
            String img = imgPath.startsWith("http") ? imgPath : "https:" + imgPath;

            double era = Double.parseDouble(eraStr);
            int gameCnt = Integer.parseInt(gameCntStr);
            int qs = Integer.parseInt(qsStr);
            double whip = Double.parseDouble(whipStr);

            // ✅ 정상 데이터 리턴 (message는 null)
            return new KboResponseDTO.StartingPitcherFullDTO(
                    name, img, era, gameCnt, resultStr, qs, whip,
                    teamType, gameId
            );

        } catch (Exception e) {
            log.error("[ERROR] API 기반 선발투수 크롤링 실패: gameId={}, teamType={}", gameId, teamType, e);
            return null;
        } finally {
            if (driver != null) driver.quit();
        }
    }

    // 오늘 경기의 전체 선발투수 정보
    public List<KboResponseDTO.StartingPitcherFullDTO> crawlTodayStartingPitchers() {
        List<KboResponseDTO.StartingPitcherFullDTO> result = new ArrayList<>();
        WebDriver driver = null;

        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(options);

            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + today;
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));

            List<WebElement> gameEls = driver.findElements(By.cssSelector("li.game-cont"));
            for (WebElement gameEl : gameEls) {
                String gameId = gameEl.getAttribute("g_id");
                if (gameId == null || gameId.isEmpty()) continue;

                String awayPitId = gameEl.getAttribute("away_p_id");
                String homePitId = gameEl.getAttribute("home_p_id");

                // ✅ away 팀 선발투수
                if (awayPitId != null && !awayPitId.isEmpty()) {
                    KboResponseDTO.StartingPitcherFullDTO awayDto = crawlStartingPitcher(gameId, "away");
                    if (awayDto != null) {
                        result.add(awayDto);
                    } else {
                        result.add(new KboResponseDTO.StartingPitcherFullDTO(
                                "", "", 0.0, 0, "없음", 0, 0.0,
                                "away", gameId, "라인업 발표 전입니다"
                        ));
                    }
                }

                // ✅ home 팀 선발투수
                if (homePitId != null && !homePitId.isEmpty()) {
                    KboResponseDTO.StartingPitcherFullDTO homeDto = crawlStartingPitcher(gameId, "home");
                    if (homeDto != null) {
                        result.add(homeDto);
                    } else {
                        result.add(new KboResponseDTO.StartingPitcherFullDTO(
                                "", "", 0.0, 0, "없음", 0, 0.0,
                                "home", gameId, "라인업 발표 전입니다"
                        ));
                    }
                }
            }

            return result;

        } catch (Exception e) {
            log.error("[ERROR] 오늘 경기 전체 선발투수 정보 수집 실패", e);
            return result;
        } finally {
            if (driver != null) driver.quit();
        }
    }


    // 내일 경기의 전체 선발투수 정보
    public List<KboResponseDTO.StartingPitcherFullDTO> crawlTomorrowStartingPitchers() {
        List<KboResponseDTO.StartingPitcherFullDTO> result = new ArrayList<>();
        WebDriver driver = null;

        try {
            String targetDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            System.out.println("[DEBUG] 크롤링 시작: " + targetDate);

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(options);

            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + targetDate;
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));


            List<WebElement> gameEls = driver.findElements(By.cssSelector("li.game-cont"));
            System.out.println("[DEBUG] 경기 수: " + gameEls.size());

            for (WebElement gameEl : gameEls) {
                String gameId = gameEl.getAttribute("g_id");
                if (gameId == null || gameId.isEmpty()) continue;

                String awayPitId = gameEl.getAttribute("away_p_id");
                String homePitId = gameEl.getAttribute("home_p_id");

                System.out.println("[DEBUG] gameId=" + gameId + ", away_p_id=" + awayPitId + ", home_p_id=" + homePitId);

                // ✅ away 선발투수
                if (awayPitId == null || awayPitId.isEmpty()) {
                    result.add(new KboResponseDTO.StartingPitcherFullDTO(
                            "", "", 0.0, 0, "없음", 0, 0.0,
                            "away", gameId, "라인업 발표 전입니다"
                    ));
                } else {
                    KboResponseDTO.StartingPitcherFullDTO awayDto = crawlStartingPitcher(gameId, "away");
                    result.add(awayDto != null ? awayDto :
                            new KboResponseDTO.StartingPitcherFullDTO(
                                    "", "", 0.0, 0, "없음", 0, 0.0,
                                    "away", gameId, "라인업 발표 전입니다"
                            ));
                }

                // ✅ home 선발투수
                if (homePitId == null || homePitId.isEmpty()) {
                    result.add(new KboResponseDTO.StartingPitcherFullDTO(
                            "", "", 0.0, 0, "없음", 0, 0.0,
                            "home", gameId, "라인업 발표 전입니다"
                    ));
                } else {
                    KboResponseDTO.StartingPitcherFullDTO homeDto = crawlStartingPitcher(gameId, "home");
                    result.add(homeDto != null ? homeDto :
                            new KboResponseDTO.StartingPitcherFullDTO(
                                    "", "", 0.0, 0, "없음", 0, 0.0,
                                    "home", gameId, "라인업 발표 전입니다"
                            ));
                }
            }

            return result;

        } catch (Exception e) {
            log.error("[ERROR] 내일 경기 전체 선발투수 정보 수집 실패", e);
            return result;
        } finally {
            if (driver != null) driver.quit();
        }
    }


    // 타자 라인업
    public List<KboResponseDTO.HitterLineupDTO> crawlTodayHitterLineups() {
        List<KboResponseDTO.HitterLineupDTO> result = new ArrayList<>();
        WebDriver driver = null;

        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + today;

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
            driver = new ChromeDriver(options);
            driver.get(url);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));

            List<WebElement> gameElements = driver.findElements(By.cssSelector("li.game-cont"));
            for (int i = 0; i < gameElements.size(); i++) {
                try {
                    WebElement game = driver.findElements(By.cssSelector("li.game-cont")).get(i);
                    String gameId = game.getAttribute("g_id");
                    String homeTeam = game.getAttribute("home_nm");
                    String awayTeam = game.getAttribute("away_nm");
                    String lineupCk = game.getAttribute("lineup_ck");

                    driver.get("https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + today);

                    Thread.sleep(1000); // JS로 테이블 로딩되는 시간 약간 필요

                    if (!"1".equals(lineupCk)) {
                        // 라인업 발표 전
                        result.add(new KboResponseDTO.HitterLineupDTO(
                                "away", awayTeam,
                                Collections.emptyList(),
                                "라인업 발표 전입니다"
                        ));
                        result.add(new KboResponseDTO.HitterLineupDTO(
                                "home", homeTeam,
                                Collections.emptyList(),
                                "라인업 발표 전입니다"
                        ));
                        continue;
                    }

                    // 라인업 발표된 경우
                    result.add(parseLineup(driver, "#tblAwayLineUp", "away", awayTeam));
                    result.add(parseLineup(driver, "#tblHomeLineUp", "home", homeTeam));
                } catch (StaleElementReferenceException e) {
                    log.warn("[WARN] Stale element encountered, skipping game index={}", i);
                    continue;
                }
            }

            return result;

        } catch (Exception e) {
            log.error("[ERROR] 타자 라인업 수집 중 오류 발생", e);
            return result;
        } finally {
            if (driver != null) driver.quit();
        }
    }

    private KboResponseDTO.HitterLineupDTO parseLineup(WebDriver driver, String tableSelector, String teamType, String teamName) {
        List<KboResponseDTO.HitterLineupDTO.HitterInfo> hitters = new ArrayList<>();
        try {
            WebElement table = driver.findElement(By.cssSelector(tableSelector));
            List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 3) continue;
                int order = Integer.parseInt(cols.get(0).getText().trim());
                String position = cols.get(1).getText().trim();
                String playerName = cols.get(2).getText().trim();

                hitters.add(new KboResponseDTO.HitterLineupDTO.HitterInfo(order, position, playerName));
            }

        } catch (Exception e) {
            log.warn("[WARN] 라인업 파싱 실패: teamType={}, teamName={}", teamType, teamName);
        }

        return new KboResponseDTO.HitterLineupDTO(teamType, teamName, hitters, null);
    }


    public List<KboResponseDTO.PlayerSimpleDTO> crawlTeamPlayersWithId(String teamCode) {
        List<KboResponseDTO.PlayerSimpleDTO> result = new ArrayList<>();
        try {
            String url = "https://www.koreabaseball.com/Team/Roster.aspx?code=" + teamCode;
            Document doc = Jsoup.connect(url).get();

            Elements rows = doc.select("table.tData tbody tr");

            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 3) continue;

                // 포지션 필터링
                String position = cols.get(2).text();
                if (!position.contains("투수") &&
                        !position.contains("포수") &&
                        !position.contains("내야수") &&
                        !position.contains("외야수")) {
                    continue;
                }

                String name = cols.get(1).text();
                String playerId = null;

                // a 태그의 href에서 playerId 추출
                Element linkEl = cols.get(1).selectFirst("a[href*=playerId]");
                if (linkEl != null) {
                    String href = linkEl.attr("href");
                    Matcher matcher = Pattern.compile("playerId=(\\d+)").matcher(href);
                    if (matcher.find()) {
                        playerId = matcher.group(1);
                    }
                }

                result.add(new KboResponseDTO.PlayerSimpleDTO(teamCode, name, playerId));
            }

        } catch (Exception e) {
            log.error("[ERROR] 팀 선수 정보 크롤링 실패: teamCode={}", teamCode, e);
        }

        return result;
    }

    public List<KboResponseDTO.PlayerSimpleDTO> crawlAllTeamsPlayers() {
        List<String> teamCodes = List.of("OB", "LT", "HH", "SS", "HT", "SK", "LG", "KT", "WO", "NC");
        List<KboResponseDTO.PlayerSimpleDTO> allPlayers = new ArrayList<>();

        for (String code : teamCodes) {
            allPlayers.addAll(crawlTeamPlayersWithId(code));
        }

        return allPlayers;
    }


}

