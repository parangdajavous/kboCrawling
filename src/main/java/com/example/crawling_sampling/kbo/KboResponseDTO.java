package com.example.crawling_sampling.kbo;

import lombok.Data;

public class KboResponseDTO {

    /* 투수 - 선수이름 + playerId + 스탯 */
    @Data
    public static class PitcherFullDTO {
        private String name;
        private String playerId;

        private double era;
        private int g;
        private int w;
        private int l;
        private int qs;
        private double whip;

        public PitcherFullDTO(String name, String playerId,
                              double era, int g, int w, int l, int qs, double whip) {
            this.name = name;
            this.playerId = playerId;
            this.era = era;
            this.g = g;
            this.w = w;
            this.l = l;
            this.qs = qs;
            this.whip = whip;
        }
    }


    /* 타자 - 선수이름 + playerId + 스탯 */
    @Data
    public static class HitterFullDTO {
        private String name;
        private String playerId;
        private int h;
        private int ab;
        private double avg;
        private double ops;

        public HitterFullDTO(String name, String playerId, int h, int ab, double avg, double ops) {
            this.name = name;
            this.playerId = playerId;
            this.h = h;
            this.ab = ab;
            this.avg = avg;
            this.ops = ops;
        }
    }


    /* 맞대결 전적 */
    @Data
    public static class MatchupStatsDTO {
        private String pitcher;  // 투수
        private String hitter;  // 타자
        private Integer ab;  // 타수
        private Integer h;  // 안타
        private Integer hr;  // 홈런
        private Integer bb;  // 볼넷
        private double avg;  // 타율
        private double ops;  // 출루율 + 장타율

        public MatchupStatsDTO(String pitcher, String hitter, Integer ab, Integer h, Integer hr, Integer bb, double avg, double ops) {
            this.pitcher = pitcher;
            this.hitter = hitter;
            this.ab = ab;
            this.h = h;
            this.hr = hr;
            this.bb = bb;
            this.avg = avg;
            this.ops = ops;
        }
    }


    /* 오늘 경기의 선발투수 정보 */
    @Data
    public static class StartingPitcherFullDTO {
        private String name;
        private String profil_img;
        private double era;
        private Integer gameCount;
        private String gameResult;
        private Integer qs;
        private double whip;
        private String teamType;
        private String gameId;

        public StartingPitcherFullDTO(String name, String profil_img,
                                      double era, Integer gameCount, String gameResult,
                                      Integer qs, double whip, String teamType, String gameId) {
            this.name = name;
            this.profil_img = profil_img;
            this.era = era;
            this.gameCount = gameCount;
            this.gameResult = gameResult;
            this.qs = qs;
            this.whip = whip;
            this.teamType = teamType;
            this.gameId = gameId;
        }
    }


}

