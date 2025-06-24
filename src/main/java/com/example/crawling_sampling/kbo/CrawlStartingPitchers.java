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
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CrawlStartingPitchers {

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

    public static void main(String[] args) {
        System.out.println("한글 출력 테스트");
        System.out.println(System.getProperty("file.encoding"));
        CrawlStartingPitchers crawl = new CrawlStartingPitchers();
        crawl.startingPitchers();
    }
}







