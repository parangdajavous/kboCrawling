package com.example.crawling_sampling.kbo;

import lombok.Data;

public class KboResponseDTO {

    @Data
    public static class PitcherStatsDTO {
        private double era;
        private int g;
        private int w;
        private int l;
        private int qs;
        private double whip;

        public PitcherStatsDTO(double era, int g, int w, int l, int qs, double whip) {
            this.era = era;
            this.g = g;
            this.w = w;
            this.l = l;
            this.qs = qs;
            this.whip = whip;
        }

    }

    /* PlayerInfo DTO */
    @Data
    public static class PlayerInfo {
        private String name;
        private String playerId;


        public PlayerInfo(String name, String playerId) {
            this.name = name;
            this.playerId = playerId;
        }
    }

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

    @Data
    public static class HitterStatsDTO {
        private int h;
        private int ab;
        private double avg;
        private double dps;

        public HitterStatsDTO(int h, int ab, double avg, double dps) {
            this.h = h;
            this.ab = ab;
            this.avg = avg;
            this.dps = dps;
        }
    }

}

