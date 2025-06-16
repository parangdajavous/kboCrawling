package com.example.crawling_sampling.kbo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class KboController {


    private final KboService kboService;

    @GetMapping("/api/rank")
    public ResponseEntity<List<KboResponseDTO.KboRankDTO>> getGameResult() {
        List<KboResponseDTO.KboRankDTO> respDTO = kboService.getTeamRanks();
        return ResponseEntity.ok(respDTO);
    }

    // 날짜별 gameId 목록 조회
    @GetMapping("/api/ids/{yyyyMMdd}")
    public ResponseEntity<List<String>> getGameIds(@PathVariable String yyyyMMdd) {
        LocalDate date = LocalDate.parse(yyyyMMdd, DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<String> ids = kboService.getGameIds(date);
        return ResponseEntity.ok(ids);
    }

    // gameId 하나에 대한 상세 경기 기록 (리뷰 탭 포함)
    @GetMapping("/api/game/{gameId}")
    public ResponseEntity<KboResponseDTO.GameDetailDTO> getGameDetail(@PathVariable String gameId) {
        KboResponseDTO.GameDetailDTO detail = kboService.getGameDetail(gameId);
        return ResponseEntity.ok(detail);
    }
}
