package com.example.crawling_sampling.kbo;

import com.example.crawling_sampling.utils.SeleniumUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class KboService {

    public List<KboResponseDTO.KboRankDTO> getTeamRanks() {

        List<KboResponseDTO.KboRankDTO> result = new ArrayList<>();
        WebDriver driver = SeleniumUtil.initDriver();

        try {
            driver.get("https://www.koreabaseball.com/Record/TeamRank/TeamRank.aspx");

            // JS-렌더링된 표가 나타날 때까지 최대 5초 대기
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement table = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("#cphContents_cphContents_cphContents_udpRecord > table")));

            List<WebElement> rows = table.findElements(By.cssSelector("tbody tr"));

            for (WebElement row : rows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                if (cells.size() < 12) continue;              // 빈 행(헤더) 방어

                KboResponseDTO.KboRankDTO dto = new KboResponseDTO.KboRankDTO(
                        Integer.parseInt(cells.get(0).getText().trim()),          // rank
                        cells.get(1).getText().trim(),                            // teamName
                        Integer.parseInt(cells.get(2).getText().trim()),          // gamesPlayed
                        Integer.parseInt(cells.get(3).getText().trim()),          // wins
                        Integer.parseInt(cells.get(4).getText().trim()),          // losses
                        Integer.parseInt(cells.get(5).getText().trim()),          // draws
                        Double.parseDouble(cells.get(6).getText()
                                .trim()
                                .replaceFirst("^\\.", "0.")),        // winRate (.623 → 0.623)
                        cells.get(7).getText().trim(),                            // gamesBehind
                        cells.get(8).getText().trim(),                            // last10
                        cells.get(9).getText().trim(),                            // streak
                        cells.get(10).getText().trim(),                           // homeRecord
                        cells.get(11).getText().trim()                            // awayRecord
                );

                // DTO 생성자에서 date가 LocalDate.now()로 자동 세팅되지만
                // 한 번 계산한 today로 통일하고 싶다면 주석 해제
                // dto.setDate(today);

                result.add(dto);
            }

        } catch (Exception e) {
            log.error("KBO 팀 순위 크롤링 실패", e);
        } finally {
            driver.quit();
        }

        return result;
    }


    /* =============================================================
       1) 날짜별 gameId 리스트 수집
       ------------------------------------------------------------- */
    public List<String> getGameIds(LocalDate date) {
        String year = String.valueOf(date.getYear());
        String month = String.format("%02d", date.getMonthValue());

        List<String> ids = new ArrayList<>();
        WebDriver driver = SeleniumUtil.initDriver();
        try {
            /* 1-A. 일정/결과 메인 진입 */
            System.out.println("Attempting to navigate to KBO website..."); // 디버깅용 메시지
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            System.out.println("Navigation command sent. Current URL: " + driver.getCurrentUrl()); // 현재 URL 확인


            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60)); // 대기 시간 넉넉하게

            /* 1-B. 년·월 드롭다운 선택 (select 태그) */
            // 년도 선택
            WebElement yearSelectElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ddlYear")));
            Select selectYear = new Select(yearSelectElement);
            selectYear.selectByVisibleText(year);

            // 월 선택
            WebElement monthSelectElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ddlMonth")));
            Select selectMonth = new Select(monthSelectElement);
            selectMonth.selectByVisibleText(month);

            // 월 변경 후 Ajax 로딩 대기 (리스트 table 다시 그려짐)
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".loading-indicator"))); // 로딩 인디케이터 사라질 때까지
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tblScheduleList"))); // 테이블이 존재할 때까지

            /* 1-C. 날짜 행(tr)에 day 텍스트 포함된 곳 찾기 (Jsoup으로 파싱) */
            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);

            Elements rows = doc.select("#tblScheduleList tbody tr"); // ID 셀렉터는 #을 붙여야 합니다.

            for (Element row : rows) {
                // thElement는 날짜를 포함하는 셀 (<td>일 수도 있으니 유연하게)
                // KBO 웹사이트 HTML 구조상 <td class="day">로 날짜가 표시되므로, th 대신 td.day를 사용하는 것이 정확합니다.
                Element dateCell = row.selectFirst("td.day"); // 날짜 셀을 찾습니다.

                if (dateCell != null) { // 날짜 셀이 있는 행만 처리 (경기가 있는 날짜의 첫 행)
                    String thText = dateCell.text().trim(); // 날짜 텍스트 (예: "06.01(일)")

                    // 요청된 날짜 (month.day)와 일치하는지 확인
                    String expectedDatePrefix = String.format("%02d.%02d", date.getMonthValue(), date.getDayOfMonth());

                    if (thText.startsWith(expectedDatePrefix)) {
                        // 해당 날짜에 속하는 모든 경기 행 처리
                        // 같은 날짜의 경기는 rowspan으로 묶여있으므로, 첫째 행만 dateCell을 가집니다.
                        // 따라서, 이 if 블록 안에서는 해당 날짜의 모든 경기 행을 처리할 수 있어야 합니다.
                        // Jsoup으로 이미 모든 tr을 가져왔으니, 여기서 특정 일자의 경기만 골라내는 로직이 필요합니다.

                        // KBO 웹사이트의 tbody tr 구조가 변경된 것 같습니다.
                        // 이제 각 <tr>이 하나의 경기를 나타내는 것으로 보이며,
                        // 날짜는 첫 경기의 <td>에 rowspan으로 포함되어 있습니다.
                        // 따라서 날짜를 검사하는 로직은 이미 괜찮습니다.
                        // 중요한 것은 각 경기 (tr) 내에서 "리뷰" 버튼을 찾는 것입니다.

                        Optional<String> gid = row.select("a").stream()
                                .filter(a -> "리뷰".equals(a.text()))
                                .map(a -> a.attr("href"))
                                .filter(href -> href != null && href.contains("gameId="))
                                // ===== 이 부분부터 새로운 파싱 로직을 적용합니다 =====
                                .map(href -> {
                                    try {
                                        URI uri = new URI(href);
                                        Map<String, String> queryParams = new LinkedHashMap<>();
                                        String query = uri.getQuery();
                                        if (query != null) {
                                            // HTML 엔티티 (&amp;)가 있다면 먼저 &로 변환 후 디코딩
                                            query = URLDecoder.decode(query.replace("&amp;", "&"), StandardCharsets.UTF_8);
                                            String[] pairs = query.split("&");
                                            for (String pair : pairs) {
                                                int idx = pair.indexOf("=");
                                                if (idx != -1) {
                                                    queryParams.put(pair.substring(0, idx), pair.substring(idx + 1));
                                                }
                                            }
                                        }
                                        return queryParams.get("gameId");
                                    } catch (URISyntaxException e) {
                                        // 파싱 오류 발생 시 null 반환하여 필터링되도록
                                        log.warn("URI 파싱 오류: {}", href, e);
                                        return null;
                                    }
                                })
                                .filter(java.util.Objects::nonNull) // null이 아닌 gameId만 통과
                                // ===== 여기까지 새로운 파싱 로직 적용 =====
                                .findFirst(); // 해당 경기 줄에서 첫 번째 gameId만 가져옴

                        gid.ifPresent(ids::add);
                    }
                }
                // 참고: 날짜 셀이 없는 <tr>은 같은 날짜의 두 번째 경기 이후의 행일 수 있습니다.
                // 현재 로직은 날짜 셀이 있는 첫 <tr>에서만 gameId를 찾도록 되어 있어,
                // 같은 날짜의 다른 경기는 놓칠 수 있습니다. 이 부분은 아래에서 다시 설명하겠습니다.
            }

            // ... (Ids.isEmpty() 경고 로직)

        } finally {
            // ... (드라이버 종료)
        }
        return ids;
    }


    /* =============================================================
       2) gameId 하나 → 리뷰 + 타자 + 투수 기록
       ------------------------------------------------------------- */
    public KboResponseDTO.GameDetailDTO getGameDetail(String gameId) {
        WebDriver driver = null; // WebDriver 인스턴스를 try 블록 밖에서 선언 및 null 초기화
        KboResponseDTO.GameDetailDTO gameDetail = null;

        try {
            driver = SeleniumUtil.initDriver(); // 드라이버 초기화

            // 1. gameId에서 gameDate 추출 (ex: "20250615WOOB0" -> "20250615")
            String gameDate = gameId.substring(0, 8);

            // 2. 직접 경기 상세 페이지 URL 생성 (section=REVIEW 포함)
            String gameDetailUrl = String.format("https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx?gameDate=%s&gameId=%s&section=REVIEW", gameDate, gameId);

            // 3. 생성된 URL로 직접 이동
            driver.get(gameDetailUrl);
            log.info("경기 상세 페이지로 직접 이동: {}", driver.getCurrentUrl());

            // WebDriverWait 대기 시간을 30초로 늘립니다. (필요시 40초 이상으로 조정)
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30)); // 대기 시간 30초로 변경

            // 페이지 로딩 완료를 나타내는 document.readyState 'complete'를 기다립니다.
            // .score_box 로딩 전 페이지의 기본 구조가 완전히 로드되도록 합니다.
            wait.until(webDriver -> ((org.openqa.selenium.JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").equals("complete"));
            log.info("Document readyState 'complete' 상태 확인.");


            /* 2-A. 점수 박스 등장 대기 */
            // 페이지가 로드되고 .score_box가 나타날 때까지 기다립니다.
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".score_box")));
            log.info(".score_box 요소 로딩 완료.");

            // Selenium으로 현재 페이지 HTML 소스 가져와 Jsoup으로 파싱
            Document doc = Jsoup.parse(driver.getPageSource());

            Element scoreBox = doc.selectFirst(".score_box");
            if (scoreBox == null) {
                log.error("경기 상세 페이지에서 .score_box를 찾을 수 없습니다. gameId: {}", gameId);
                return null; // 또는 예외를 던져도 됩니다.
            }

            String dateStr = safeText(scoreBox, ".date").replace(".", "-");
            String timeStr = safeText(scoreBox, ".time");
            String stadium = safeText(scoreBox, ".place");
            String home = safeText(scoreBox, ".home .team");
            String away = safeText(scoreBox, ".away .team");
            int hScore = parseInt(safeText(scoreBox, ".home .score"));
            int aScore = parseInt(safeText(scoreBox, ".away .score"));

            // 투수 정보는 URL에 section=REVIEW가 포함되어 있으므로, 페이지 로드 후 바로 파싱 가능
            String winningPitcher = safeText(doc, "#lblWinPitcher");
            String losingPitcher = safeText(doc, "#lblLosePitcher");
            String savePitcher = safeText(doc, "#lblSavePitcher");
            if (savePitcher.isBlank()) savePitcher = null;

            /* 2-B. 타자·투수 테이블 파싱 (리뷰 탭 클릭 없이 바로 진행) */
            // section=REVIEW 덕분에 이 테이블들이 이미 로드되어 있을 것입니다.
            List<KboResponseDTO.GameDetailDTO.BatterRecordDTO> awayBat = parseBatters(doc, "#tblAwayHitter");
            List<KboResponseDTO.GameDetailDTO.BatterRecordDTO> homeBat = parseBatters(doc, "#tblHomeHitter");
            List<KboResponseDTO.GameDetailDTO.PitcherRecordDTO> awayPit = parsePitchers(doc, "#tblAwayPitcher");
            List<KboResponseDTO.GameDetailDTO.PitcherRecordDTO> homePit = parsePitchers(doc, "#tblHomePitcher");

            /* 2-C. DTO 생성 후 반환 */
            gameDetail = new KboResponseDTO.GameDetailDTO(
                    gameId, dateStr, timeStr, stadium,
                    home, away, hScore, aScore,
                    winningPitcher, losingPitcher, savePitcher,
                    homeBat, awayBat, homePit, awayPit
            );

        } catch (TimeoutException e) {
            log.error("경기 상세 웹 페이지 로딩 타임아웃 오류 (getGameDetail: {}). 네트워크 지연 또는 셀렉터 오류일 수 있습니다. 현재 URL: {}", gameId, driver.getCurrentUrl(), e);
            throw new RuntimeException("페이지 로딩 타임아웃 오류", e);
        } catch (Exception e) {
            log.error("경기 상세 크롤링 실패: {}. 예상치 못한 오류 발생. 현재 URL: {}", gameId, driver.getCurrentUrl(), e);
            throw new RuntimeException("경기 상세 정보 크롤링 중 오류 발생", e);
        } finally {
            if (driver != null) {
                SeleniumUtil.quitDriver(driver); // 드라이버 종료
                log.info("Selenium WebDriver 종료 완료."); // 종료 로그 추가
            }
        }
        return gameDetail;
    }

    /* ---------------- 타자 테이블 파싱 ---------------- */
    private List<KboResponseDTO.GameDetailDTO.BatterRecordDTO> parseBatters(Document doc, String css) {
        List<KboResponseDTO.GameDetailDTO.BatterRecordDTO> list = new ArrayList<>();
        Element table = doc.selectFirst(css);
        if (table == null) {
            log.warn("타자 테이블을 찾을 수 없습니다: {}", css);
            return list;
        }

        // KBO 웹사이트의 타자 테이블 컬럼 순서 (2025-06-16 기준):
        // 0: 타순, 1: 선수명, 2: 타수, 3: 안타, 4: 2루타, 5: 3루타, 6: 홈런, 7: 타점, 8: 득점, 9: 볼넷, 10: 삼진,
        // 11: 희생번트, 12: 희생플라이, 13: 병살타, 14: AVG, 15: SLG
        // 16~24: 1회~9회 결과 (총 9개)
        // 25: 10회 이후 결과 (연장전) - DTO에는 9회까지만 있으므로 25번 인덱스는 현재 스킵

        for (Element tr : table.select("tbody tr")) {
            Elements tds = tr.select("td");
            // `TOTAL` 행을 제외하고, 최소한의 컬럼 수 (타순 ~ 타율/장타율 + 9이닝 결과까지) 확인
            // KBO 사이트 타자 테이블은 16개 (기본 스탯) + 9개 (이닝별 결과) = 총 25개 컬럼이 있을 수 있습니다.
            // DTO에는 9회 결과까지 있으므로 최소 16+9=25개 필요. TOTAL 행은 `td` 개수가 다릅니다.
            if (tds.size() < 25) {
                continue;
            }

            try {
                int order = parseInt(tds.get(0).text()); // 타순
                String player = tds.get(1).text(); // 선수명
                int atBat = parseInt(tds.get(2).text()); // 타수
                int hit = parseInt(tds.get(3).text()); // 안타
                int rbi = parseInt(tds.get(7).text()); // 타점
                int run = parseInt(tds.get(8).text()); // 득점
                double avg = parseDouble(tds.get(14).text()); // 타율

                // 이닝별 결과 (1회 ~ 9회)
                // KBO 웹사이트에서 이닝별 결과는 16번 인덱스부터 시작합니다.
                String result1 = tds.size() > 15 ? tds.get(16).text() : ""; // 1회
                String result2 = tds.size() > 16 ? tds.get(17).text() : ""; // 2회
                String result3 = tds.size() > 17 ? tds.get(18).text() : ""; // 3회
                String result4 = tds.size() > 18 ? tds.get(19).text() : ""; // 4회
                String result5 = tds.size() > 19 ? tds.get(20).text() : ""; // 5회
                String result6 = tds.size() > 20 ? tds.get(21).text() : ""; // 6회
                String result7 = tds.size() > 21 ? tds.get(22).text() : ""; // 7회
                String result8 = tds.size() > 22 ? tds.get(23).text() : ""; // 8회
                String result9 = tds.size() > 23 ? tds.get(24).text() : ""; // 9회

                list.add(new KboResponseDTO.GameDetailDTO.BatterRecordDTO(
                        order, player, atBat, hit, rbi, run, avg,
                        result1, result2, result3, result4, result5,
                        result6, result7, result8, result9
                ));
            } catch (NumberFormatException e) {
                log.warn("타자 기록 숫자 파싱 오류 (행: {}): {}", tr.text(), e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                log.warn("타자 기록 인덱스 오류 (행: {}). 테이블 구조가 변경되었을 수 있습니다: {}", tr.text(), e.getMessage());
            }
        }
        return list;
    }

    /* ---------------- 투수 테이블 파싱 ---------------- */
    private List<KboResponseDTO.GameDetailDTO.PitcherRecordDTO> parsePitchers(Document doc, String css) {
        List<KboResponseDTO.GameDetailDTO.PitcherRecordDTO> list = new ArrayList<>();
        Element table = doc.selectFirst(css);
        if (table == null) {
            log.warn("투수 테이블을 찾을 수 없습니다: {}", css);
            return list;
        }

        // KBO 웹사이트 투수 기록 컬럼 순서 (2025-06-16 기준):
        // 0: 선수명, 1: 기록 (선발/구원, 승/패/세/홀드 등 포함), 2: ERA, 3: 이닝, 4: 안타, 5: 홈런, 6: 볼넷,
        // 7: 사구, 8: 삼진, 9: 실점, 10: 자책, 11: 투구수, 12: 타자수, 13: 피안타율, 14: 피출루율, 15: 피장타율, 16: WHIP

        for (Element tr : table.select("tbody tr")) {
            Elements tds = tr.select("td");
            // `TOTAL` 행을 제외하고, 최소한의 컬럼 수 (17개) 확인
            if (tds.size() < 17) {
                continue;
            }

            try {
                String player = tds.get(0).text(); // 선수명
                String recordText = tds.get(1).text(); // 기록 (예: 선발 승, 구원 홀드, 구원 패 등)

                // role, decision, win, loss, save 파싱
                String role = "구원"; // 기본값 구원
                String decision = "없음";
                int win = 0, loss = 0, save = 0;

                if (recordText.contains("선발")) {
                    role = "선발";
                }
                if (recordText.contains("승")) {
                    decision = "승";
                    win = 1;
                } else if (recordText.contains("패")) {
                    decision = "패";
                    loss = 1;
                } else if (recordText.contains("세")) { // 세이브
                    decision = "세";
                    save = 1;
                } else if (recordText.contains("홀드")) {
                    decision = "홀드";
                }

                double era = parseDouble(tds.get(2).text()); // ERA
                double innings = parseInnings(tds.get(3).text()); // 이닝 (4.1 -> 4.333)
                int hits = parseInt(tds.get(4).text()); // 피안타
                int homeRuns = parseInt(tds.get(5).text()); // 피홈런
                int walksHbp = parseInt(tds.get(6).text()) + parseInt(tds.get(7).text()); // 볼넷(6) + 사구(7)
                int strikeOuts = parseInt(tds.get(8).text()); // 삼진
                int runs = parseInt(tds.get(9).text()); // 실점
                int earned = parseInt(tds.get(10).text()); // 자책
                int pitches = parseInt(tds.get(11).text()); // 투구수
                int batters = parseInt(tds.get(12).text()); // 타자 수

                list.add(new KboResponseDTO.GameDetailDTO.PitcherRecordDTO(
                        player, role, decision, win, loss, save, innings, batters,
                        pitches, 0, hits, homeRuns, walksHbp, strikeOuts, runs, earned, era
                ));
                // atBat은 투수 기록 테이블에 직접적으로 "타수"로 명시되지 않으므로 0으로 설정하거나 계산 로직 추가 필요.
                // KBO 사이트 투수 기록 테이블에는 `타자수` (battersFaced)가 있습니다.
            } catch (NumberFormatException e) {
                log.warn("투수 기록 숫자 파싱 오류 (행: {}): {}", tr.text(), e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                log.warn("투수 기록 인덱스 오류 (행: {}). 테이블 구조가 변경되었을 수 있습니다: {}", tr.text(), e.getMessage());
            }
        }
        return list;
    }

    /* ---------------- 안전 파서 & 유틸 (Jsoup Element 사용) ---------------- */
    private int parseInt(String text) {
        try {
            return Integer.parseInt(text.trim().replace(",", "")); // 콤마 제거
        } catch (NumberFormatException e) {
            return 0; // 파싱 실패 시 기본값 0 반환
        }
    }

    private double parseDouble(String text) {
        try {
            // Java 코드의 replaceFirst("^\\.", "0.")과 동일하게 처리
            String cleanedText = text.trim().replaceFirst("^\\.", "0.").replace(",", ""); // 콤마 제거
            return Double.parseDouble(cleanedText);
        } catch (NumberFormatException e) {
            return 0.0; // 파싱 실패 시 기본값 0.0 반환
        }
    }

    // Jsoup Element로부터 안전하게 텍스트를 가져오는 유틸리티
    private String safeText(Element parentElement, String cssSelector) {
        Element element = parentElement.selectFirst(cssSelector);
        return (element != null) ? element.text().trim() : "";
    }

    // 이닝 문자열 (예: "4.1")을 double (예: 4.333)으로 변환
    private double parseInnings(String inningsText) {
        try {
            if (inningsText.contains(".")) {
                String[] parts = inningsText.split("\\.");
                double wholeInnings = Double.parseDouble(parts[0]);
                double partialInnings = Double.parseDouble(parts[1]);
                // KBO에서는 0.1 이닝은 1/3, 0.2 이닝은 2/3로 처리
                // 0.1 -> 1/3 = 0.333...
                // 0.2 -> 2/3 = 0.666...
                if (partialInnings == 1) {
                    return wholeInnings + (1.0 / 3.0);
                } else if (partialInnings == 2) {
                    return wholeInnings + (2.0 / 3.0);
                }
            }
            return Double.parseDouble(inningsText);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            log.warn("이닝 파싱 오류: '{}' - {}", inningsText, e.getMessage());
            return 0.0;
        }
    }


}
