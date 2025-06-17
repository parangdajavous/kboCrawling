package com.example.crawling_sampling.kbo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class KboController {


    private final KboService kboService;


    /* 단일 투수 */
    @GetMapping("/api/pitchers/{playerId}")
    public ResponseEntity<KboResponseDTO.PitcherStatsDTO> getPitcher(
            @PathVariable String playerId) {
        KboResponseDTO.PitcherStatsDTO respDTO = kboService.crawlPitcherDetail(playerId);
        return ResponseEntity.ok(respDTO);
    }

    /* 전체 투수 목록 + 스탯 한번에 */
    @GetMapping("/api/pitchers")
    public ResponseEntity<List<KboResponseDTO.PitcherFullDTO>> getAllPitchers() {
        List<KboResponseDTO.PitcherFullDTO> respDTO = kboService.crawlAllPitchers();
        return ResponseEntity.ok(respDTO);
    }

    /* (선택) playerId 목록만 보고 싶다면 */
    @GetMapping("/api/pitchers/players")
    public ResponseEntity<List<KboResponseDTO.PlayerInfo>> getPlayerList() {
        List<KboResponseDTO.PlayerInfo> respDTO = kboService.crawlPitcherPlayerList();
        return ResponseEntity.ok(respDTO);
    }


    @GetMapping("/api/hitters")
    public ResponseEntity<List<KboResponseDTO.HitterStatsDTO>> getAllHitters() {
        List<KboResponseDTO.HitterStatsDTO> respDTO = kboService.crawlAllHitters();
        return ResponseEntity.ok(respDTO);
    }

}
