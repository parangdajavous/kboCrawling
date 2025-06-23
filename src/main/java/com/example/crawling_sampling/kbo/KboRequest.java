package com.example.crawling_sampling.kbo;

import lombok.Data;

import java.util.List;

public class KboRequest {

    @Data
    public static class LineupSaveDTO {
        private String teamType;     // "away" or "home"
        private String teamName;
        private List<HitterInfo> hitters;

        @Data
        public static class HitterInfo {
            private int order;
            private String position;
            private String playerName;
            private List<MatchUpDTO> matchUps;
            private double sessionAvg;  // 타자의 시즌 타율

            @Data
            public static class MatchUpDTO {
                private Integer ab;  // 타수
                private Integer h;  // 안타
                private Integer hr;  // 홈런
                private Integer bb;  // 볼넷
                private double avg;  // 선발투수에 대한 타율
                private double ops;  // 출루율 + 장타율

                public MatchUpDTO(Integer ab, Integer h, Integer hr, Integer bb, double avg, double ops) {
                    this.ab = ab;
                    this.h = h;
                    this.hr = hr;
                    this.bb = bb;
                    this.avg = avg;
                    this.ops = ops;
                }
            }


            public HitterInfo(int order, String position, String playerName, double sessionAvg, List<MatchUpDTO> matchUps) {
                this.order = order;
                this.position = position;
                this.playerName = playerName;
                this.sessionAvg = sessionAvg;
                this.matchUps = matchUps;
            }

        }

        public LineupSaveDTO(String teamType, String teamName, List<HitterInfo> hitters) {
            this.teamType = teamType;
            this.teamName = teamName;
            this.hitters = hitters;
        }
    }
}