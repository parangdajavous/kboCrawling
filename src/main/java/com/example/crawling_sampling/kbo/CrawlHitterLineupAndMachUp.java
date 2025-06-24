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
import java.util.List;

public class CrawlHitterLineupAndMachUp {


    // 선발투수
    public void startingPitchers() {
        /* WebDriverManager.chromedriver().setup() 로 드라이버 경로 자동 설정 */
        WebDriverManager.chromedriver().setup(); // ① 크롬 드라이버 준비

        List<KboRequest.SaveDTO> dtoList = new ArrayList<>();
        WebDriver driver = null;

        try {
            /* 1) GameCenter 접속 (내일 날짜) */
//            String targetDate = LocalDate.now()
//                    .plusDays(1)
//                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            ChromeOptions opts = new ChromeOptions()
                    .addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");

            driver = new ChromeDriver(opts);
            driver.get("https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?"); //gameDate=+ targetDate

            /* 경기 리스트(li.game-cont) 로딩 대기 (최대 10초) */
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));

            /* 2) 경기 목록 순회 */
            for (WebElement gameEl : driver.findElements(By.cssSelector("li.game-cont"))) {

                /* ─ 경기·팀·투수 식별자 추출 ─ */
                String gameIdStr = gameEl.getAttribute("g_id");
                String gameId = gameIdStr;
                String awayTeamId = gameEl.getAttribute("away_id");
                String homeTeamId = gameEl.getAttribute("home_id");
                String awayPitId = gameEl.getAttribute("away_p_id");
                String homePitId = gameEl.getAttribute("home_p_id");

                // 선발투수 미확정 시 skip
                if (awayPitId == null || homePitId == null ||
                        awayPitId.isBlank() || homePitId.isBlank())
                    continue;

                /* 3) 내부 API 호출 → 선발투수 시즌 스탯(JSON) */
                JsonObject api = callPitcherAnalysisApi(
                        awayTeamId, awayPitId, homeTeamId, homePitId);
                if (api == null) continue;

                /* 3. away / home 투수 2명 파싱 */
                for (String teamType : List.of("away", "home")) {
                    int idx = teamType.equals("away") ? 0 : 1; // rows[0]=away, rows[1]=home
                    Integer pitId = Integer.parseInt(teamType.equals("away") ? awayPitId : homePitId);

                    JsonArray row = api.getAsJsonArray("rows")
                            .get(idx).getAsJsonObject()
                            .getAsJsonArray("row");

                    // 이름·이미지·전적 파싱 (row[0] 은 HTML 덩어리)
                    Document html = Jsoup.parse(row.get(0).getAsJsonObject().get("Text").getAsString());
                    String imgUrl = parseImgUrl(html);
                    String resultS = parseResultString(html);

                    /* DTO 적재 */
                    dtoList.add(new KboRequest.SaveDTO(
                            gameId,
                            pitId,
                            imgUrl,
                            Double.parseDouble(row.get(1).getAsJsonObject().get("Text").getAsString()),
                            Integer.parseInt(row.get(3).getAsJsonObject().get("Text").getAsString()),
                            resultS,
                            Integer.parseInt(row.get(5).getAsJsonObject().get("Text").getAsString()),
                            Double.parseDouble(row.get(6).getAsJsonObject().get("Text").getAsString())
                    ));

                    System.out.println("✔ " + teamType + " 투수(" + pitId + ") 크롤링 완료");
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] 선발투수 크롤링 실패: " + e.getMessage());
            e.printStackTrace();

        } finally {
            if (driver != null) driver.quit();
        }

        /* DB 저장 전 데이터 확인 */
        for (KboRequest.SaveDTO dto : dtoList) {
            System.out.println("▶ gameId=" + dto.getGameId()
                    + ", playerId=" + dto.getPlayerId()
                    + "profilImgUrl+" + dto.getProfileUrl()
                    + ", ERA=" + dto.getERA()
                    + ", games=" + dto.getGameCount()
                    + ", result=" + dto.getResult()
                    + ", QS=" + dto.getQS()
                    + ", WHIP=" + dto.getWHIP());
        }

        /* 6) 배치 저장 (중복 처리 로직은 Repository에서) */
//        if (!dtoList.isEmpty()) {
//            startingPitcherRepository.saveAll(dtoList);
//            System.out.println("✅ 선발투수 " + dtoList.size() + "건 저장 완료");
//        } else {
//            System.out.println("🏷️  저장할 선발투수 데이터가 없습니다.");
//        }
    }


    /*------------------ Util함수로 뺄 예정 ------------------*/
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

    // HTML <span class="record">...</span> 에서 시즌 전적 문자열 추출
    // ex) "시즌 10승 3패 VS 상대 ..." → "10승 3패"
    private String parseResultString(Document doc) {
        Element recordEl = doc.selectFirst(".record");
        if (recordEl == null) return "없음";
        String txt = recordEl.text().replace("시즌 ", "").trim();
        return txt.isEmpty() ? "없음" : txt.contains("VS") ? txt.split("VS")[0].trim() : txt;
    }

    // HTML 내 두 번째 <img> 태그(src) → 프로필 이미지 URL 반환
    // '//' 로 시작하면 "https:" 접두어 추가
    private String parseImgUrl(Document doc) {
        Elements imgs = doc.select("img");
        if (imgs.size() >= 2) {
            String path = imgs.get(1).attr("src");
            return path.startsWith("http") ? path : "https:" + path;
        }
        return "";
    }


    // 타자 라인업
    public void crawlHitterLineupWithMatchup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);

        List<KboRequest.HitterSaveDTO> result = new ArrayList<>();

        try {
            // ① 오늘 날짜로 URL 생성
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String url = "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=" + today;
            driver.get(url);

            // ② 경기 정보 로딩 대기
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.game-cont")));

            // ③ 각 경기별 라인업 + 선발투수 정보 파싱
            List<WebElement> games = driver.findElements(By.cssSelector("li.game-cont"));
            for (WebElement game : games) {
                String gameId = game.getAttribute("g_id");
                int homeTeamId = Integer.parseInt(game.getAttribute("home_id"));
                int awayTeamId = Integer.parseInt(game.getAttribute("away_id"));
                String homeTeam = game.getAttribute("home_nm");
                String awayTeam = game.getAttribute("away_nm");
                String homePitcher = game.getAttribute("home_p_nm");
                String awayPitcher = game.getAttribute("away_p_nm");
                String lineupCk = game.getAttribute("lineup_ck");

                driver.get(url); // 페이지 리로딩 (다시 로딩 안 하면 다음 table 못 읽을 수 있음)
                Thread.sleep(1000);

                if (!"1".equals(lineupCk)) {
                    System.out.println("라인업 발표 전: " + homeTeam + " vs " + awayTeam);
                    continue;
                }

                // away 타자 vs home 선발투수
                KboRequest.HitterSaveDTO awayDto = parseLineupWithMatchup(
                        driver, "#tblAwayLineUp", "away", awayTeam, homeTeam, homePitcher, gameId, awayTeamId
                );
                result.add(awayDto);

                // home 타자 vs away 선발투수
                KboRequest.HitterSaveDTO homeDto = parseLineupWithMatchup(
                        driver, "#tblHomeLineUp", "home", homeTeam, awayTeam, awayPitcher, gameId, homeTeamId
                );
                result.add(homeDto);
            }

            // ④ 저장 or 출력
            System.out.println("타자 라인업 저장 완료: " + result.size() + "건");
            // lineupRepo.saveAll(result); // TODO: 저장처리 연결

        } catch (Exception e) {
            System.out.println("[ERROR] 타자 라인업 크롤링 실패");
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    /**
     * 라인업 테이블 파싱 + 선발투수와의 맞대결 전적 크롤링
     */
    private KboRequest.HitterSaveDTO parseLineupWithMatchup(
            WebDriver driver,
            String tableSelector,
            String teamType,
            String teamName,
            String opponentTeam,
            String opponentPitcher,
            String gameId,
            Integer teamId
    ) {
        List<KboRequest.HitterSaveDTO.HitterInfo> hitters = new ArrayList<>();

        try {
            WebElement table = driver.findElement(By.cssSelector(tableSelector));
            List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.size() < 3) continue;

                int order = Integer.parseInt(cols.get(0).getText().trim());
                String position = cols.get(1).getText().trim();
                String playerName = cols.get(2).getText().trim();

                List<KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO> matchUps = new ArrayList<>();

                // 맞대결 스탯 크롤링
                if (opponentPitcher != null && !"없음".equals(opponentPitcher)) {
                    try {
                        KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO stats = crawlMatchup(
                                driver, opponentTeam, opponentPitcher, teamName, playerName
                        );
                        if (stats != null) matchUps.add(stats);
                    } catch (Exception e) {
                        System.out.println("[WARN] 맞대결 조회 실패: " + playerName);
                    }
                }

                // TODO: 시즌 타율 추출 로직 추가 필요 (현재는 예시로 0.000)
                double seasonAvg = 0.000;

                hitters.add(new KboRequest.HitterSaveDTO.HitterInfo(
                        teamId,
                        order,
                        playerName,
                        position,
                        matchUps,
                        seasonAvg
                ));
            }

        } catch (Exception e) {
            System.out.println("[WARN] 라인업 파싱 실패");
        }

        // 현재는 HPredictionPer 예측값 없음 → null 처리
        return new KboRequest.HitterSaveDTO(gameId, hitters, null);
    }

    /**
     * 선발투수 vs 타자 맞대결 전적 크롤링 메서드 (한 번의 요청당 한 명 기준)
     */
    private KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO crawlMatchup(
            WebDriver driver,
            String pitcherTeamNm, String pitcherNm,
            String hitterTeamNm, String hitterNm) throws InterruptedException {

        driver.get("https://www.koreabaseball.com/Record/Etc/HitVsPit.aspx");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        new Select(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("cphContents_cphContents_cphContents_ddlPitcherTeam")))).selectByVisibleText(pitcherTeamNm);
        new Select(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("cphContents_cphContents_cphContents_ddlPitcherPlayer")))).selectByVisibleText(pitcherNm);
        new Select(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("cphContents_cphContents_cphContents_ddlHitterTeam")))).selectByVisibleText(hitterTeamNm);
        new Select(wait.until(ExpectedConditions.elementToBeClickable(
                By.id("cphContents_cphContents_cphContents_ddlHitterPlayer")))).selectByVisibleText(hitterNm);

        driver.findElement(By.id("cphContents_cphContents_cphContents_btnSearch")).click();

        wait.until(d -> {
            Document doc = Jsoup.parse(d.getPageSource());
            Element row = doc.selectFirst("table.tData.tt tbody tr");
            return row != null && row.select("td").size() >= 14;
        });

        Document doc = Jsoup.parse(driver.getPageSource());
        Element row = doc.selectFirst("table.tData.tt tbody tr");
        if (row == null) return null;

        Elements td = row.select("td");

        Integer ab = parseIntSafe(td.get(2).text());
        Integer h = parseIntSafe(td.get(3).text());
        double avg = parseDoubleSafe(td.get(0).text());
        double ops = parseDoubleSafe(td.get(13).text());

        return new KboRequest.HitterSaveDTO.HitterInfo.MachUpStatusDTO(ab, h, avg, ops);
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

}
