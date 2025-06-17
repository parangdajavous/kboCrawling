package com.example.crawling_sampling.kbo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
public class KboController {


    private final KboService kboService;

    /* 전체 투수 목록 + 스탯 한번에 */
    @GetMapping("/api/pitchers")
    public ResponseEntity<?> getAllPitchers() {
        List<KboResponseDTO.PitcherFullDTO> respDTO = kboService.crawlAllPitchers();
        return ResponseEntity.ok(respDTO);
    }

    /* 전체 타자 목록 + 스탯 한번에 */
    @GetMapping("/api/hitters")
    public ResponseEntity<?> getAllHitters() {
        List<KboResponseDTO.HitterFullDTO> respDTO = kboService.crawlAllHitters();
        return ResponseEntity.ok(respDTO);
    }


    /* 맞대결 전적 선수 스탯*/
    /* localhost:8080/api/matchup?pitcherTeam=한화&pitcher=폰세&hitterTeam=두산&hitter=양의지 */
    @GetMapping("/api/matchup")
    public ResponseEntity<?> getMatchup(
            @RequestParam String pitcherTeam,
            @RequestParam String pitcher,
            @RequestParam String hitterTeam,
            @RequestParam String hitter) {

        KboResponseDTO.MatchupStatsDTO respDTO = kboService.crawlMatchup(pitcherTeam, pitcher, hitterTeam, hitter);
        if (respDTO == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "맞대결 기록이 없습니다");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        return ResponseEntity.ok(respDTO);
    }



}
