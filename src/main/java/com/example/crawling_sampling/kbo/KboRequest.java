package com.example.crawling_sampling.kbo;

import lombok.Data;

import java.util.List;

public class KboRequest {

    @Data
    public static class HitterSaveDTO {

        private String gameId; // 경기ID (ex. 20250624SKOB0)
        private List<HitterInfo> hitters;
        private Integer HPredictionPer; // 안타 예측값

        @Data
        public static class HitterInfo {
            private String teamName;
            private Integer hitterOrder; // 타순
            private String name; // 선수명
            private String position; // 포지션 (내야수, 외야수, ...)
            private List<MachUpStatusDTO> machUpStatuses;  // 맞대결 전적 스탯 리스트
            private Double seasonAVG; // 타자의 시즌 타율

            // 🔽 임시 필드: 누구와 맞붙었는지
            private String vsPitcherName;
            private String vsPitcherTeam;

            @Data
            public static class MachUpStatusDTO {
                private Integer AB; // 선발투수와 맞대결 전적: 타수
                private Integer H; // 선발투수와 맞대결 전적: 안타수
                private Double AVG; // 선발투수와 맞대결 전적: 타울
                private Double OPS; // 선발투수와 맞대결 전적: 출루율 + 장타율

                public MachUpStatusDTO(Integer AB, Integer H, Double AVG, Double OPS) {
                    this.AB = AB;
                    this.H = H;
                    this.AVG = AVG;
                    this.OPS = OPS;
                }
            }


            public HitterInfo(String teamName, Integer hitterOrder, String name, String position, List<MachUpStatusDTO> machUpStatuses, Double seasonAVG, String vsPitcherName, String vsPitcherTeam) {
                this.teamName = teamName;
                this.hitterOrder = hitterOrder;
                this.name = name;
                this.position = position;
                this.machUpStatuses = machUpStatuses;
                this.seasonAVG = seasonAVG;
                this.vsPitcherName = vsPitcherName;
                this.vsPitcherTeam = vsPitcherTeam;
            }
        }


        public HitterSaveDTO(String gameId, List<HitterInfo> hitters, Integer HPredictionPer) {
            this.gameId = gameId;
            this.hitters = hitters;
            this.HPredictionPer = HPredictionPer;
        }
    }


    // 선발투수
    @Data
    public static class SaveDTO {

        private String gameId; // 경기ID
        private Integer playerId; // 선수ID
        private List<PitcherDTO> pitchers;


        @Data
        public static class PitcherDTO{
            private String teamName;
            private String playerName;
            private String profileUrl; // 선수 프로필 이미지 경로
            private double ERA;  // 평균 자책점
            private Integer gameCount;  // 출전 경기수
            private String result;  // 전적 요약 (예: 10승 3패)
            private Integer QS;  // 퀄리티 스타트 횟수
            private double WHIP;  // 이닝당 허용 출루율

            public PitcherDTO(String teamName, String playerName, String profileUrl, double ERA, Integer gameCount, String result, Integer QS, double WHIP) {
                this.teamName = teamName;
                this.playerName = playerName;
                this.profileUrl = profileUrl;
                this.ERA = ERA;
                this.gameCount = gameCount;
                this.result = result;
                this.QS = QS;
                this.WHIP = WHIP;
            }
        }

        public SaveDTO(String gameId, Integer playerId, List<PitcherDTO> pitchers) {
            this.gameId = gameId;
            this.playerId = playerId;
            this.pitchers = pitchers;
        }
    }
}