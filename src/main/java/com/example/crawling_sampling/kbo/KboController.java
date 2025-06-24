package com.example.crawling_sampling.kbo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class KboController {


    private final KboService kboService;


    // 경기 전체 선발투수 정보
    @GetMapping("/api/tomorrow-starting-pitchers")
    public ResponseEntity<?> getTomorrowStartingPitchers() {
        List<KboResponseDTO.StartingPitcherFullDTO> respDTO = kboService.crawlStartingPitchers();
        return ResponseEntity.ok(respDTO);
    }

//    // 타자 라인업 (선발투수와 맞대결 전적 포함)
//    @GetMapping("/api/today-hitter-lineups")
//    public ResponseEntity<?> getTodayHitterLineups() {
//        List<KboResponseDTO.HitterLineupDTO> respDTO = kboService.crawlTodayHitterLineups();
//        return ResponseEntity.ok(respDTO);
//    }


    /* 맞대결 전적 선수 스탯*/
    /* localhost:8080/api/matchup?pitcherTeam=한화&pitcher=폰세&hitterTeam=두산&hitter=양의지 */
//    @GetMapping("/api/matchup")
//    public ResponseEntity<?> getMatchup(
//            @RequestParam String pitcherTeam,
//            @RequestParam String pitcher,
//            @RequestParam String hitterTeam,
//            @RequestParam String hitter) {
//
//        KboResponseDTO.MatchupStatsDTO respDTO = kboService.crawlMatchup(pitcherTeam, pitcher, hitterTeam, hitter);
//
//        if (respDTO == null) {
//            Map<String, String> error = new HashMap<>();
//            error.put("message", "맞대결 기록이 없습니다");
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
//        }
//        return ResponseEntity.ok(respDTO);
//    }


    // 상대 선발투수 정보만 반환
//    @GetMapping("/api/starting-pitcher")
//    public ResponseEntity<?> getOpponentPitcher(
//            @RequestParam("gameId") String gameId,
//            @RequestParam("teamType") String teamType
//    ) {
//        String opponentType = teamType.equalsIgnoreCase("home") ? "away" : "home";
//
//        KboResponseDTO.StartingPitcherFullDTO opponent =
//                kboService.crawlStartingPitcher(gameId, opponentType);
//
//        if (opponent == null) {
//            Map<String, String> error = new HashMap<>();
//            error.put("message", "해당 경기에서 상대 선발투수를 찾을 수 없습니다.");
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
//        }
//
//        return ResponseEntity.ok(opponent);
//    }

    // 수요일 ~ 일요일 경기 전체 선발투수 정보
//    @GetMapping("/api/today-starting-pitchers")
//    public ResponseEntity<?> getTodayStartingPitchers() {
//        List<KboResponseDTO.StartingPitcherFullDTO> respDTO = kboService.crawlTodayStartingPitchers();
//        return ResponseEntity.ok(respDTO);
//    }

}
