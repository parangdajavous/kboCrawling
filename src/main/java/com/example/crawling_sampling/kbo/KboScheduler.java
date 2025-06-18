package com.example.crawling_sampling.kbo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KboScheduler {


    private final KboService kboService;
    // private final PitcherRepository pitcherRepo; // TODO: 나중에 추가
    // private final HitterRepository hitterRepo;   // TODO: 나중에 추가
    // private final MatchupRepository matchupRepo; // TODO: 나중에 추가

    /**
     * 매일 새벽 3시: 투수·타자 전체 스탯 갱신
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    // @Async               // ← 나중에 비동기로 돌리고 싶으면 주석 해제
    public void updateDailyStats() {
        log.info("투수·타자 스탯 갱신 시작");

        List<KboResponseDTO.PitcherFullDTO> pitchers = kboService.crawlAllPitchers();
        List<KboResponseDTO.HitterFullDTO> hitters = kboService.crawlAllHitters();

        // TODO: DB 저장 로직
        // pitcherRepo.saveAll(mapToEntities(pitchers));
        // hitterRepo.saveAll(mapToEntities(hitters));

        log.info("갱신 완료: 투수 {}명, 타자 {}명", pitchers.size(), hitters.size());
    }

    /**
     * 매일 새벽 3시: 맞대결 전적 갱신
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    // @Async // 비동기 처리 원할 경우 활성화
    public void refreshMatchupCache() {
        log.info("맞대결 전적 갱신 시작");

        String pitcherTeam = "한화", pitcher = "폰세";
        String hitterTeam = "두산", hitter = "양의지";

        try {
            var dto = kboService.crawlMatchup(pitcherTeam, pitcher, hitterTeam, hitter);
            if (dto != null) {
                // TODO: 캐시에 저장하거나 DB에 저장
                // matchupRepo.saveOrUpdate(dto); // 예시
                log.info("맞대결 업데이트 완료: {}", dto);
            } else {
                log.warn("맞대결 데이터 없음: {} vs {}", pitcher, hitter);
            }
        } catch (Exception e) {
            log.error("맞대결 갱신 실패", e);
        }
    }


}
