package com.example.crawling_sampling.kbo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public class KboResponseDTO {


        @Data
        public static class KboRankDTO {
            private String date; // 오늘 날짜
            private int rank; // 순위
            private String teamName; // 팀명
            private int gamesPlayed; // 경기 수
            private int wins; // 승
            private int losses; // 패
            private int draws; // 무
            private double winRate; // 승률
            private String gamesBehind; // 게임차 (예: "-" 또는 "1.5")
            private String last10; // 최근 10경기 (예: 6승 4패)
            private String streak; // 연속 (예: 2승, 3패)
            private String homeRecord; // 홈 성적 (예: 18승 10패)
            private String awayRecord; // 원정 성적 (예: 12승 13패)


            public KboRankDTO(int rank, String teamName, int gamesPlayed, int wins, int losses, int draws,
                              double winRate, String gamesBehind, String last10, String streak,
                              String homeRecord, String awayRecord) {
                this.date = LocalDate.now().toString();;
                this.rank = rank;
                this.teamName = teamName;
                this.gamesPlayed = gamesPlayed;
                this.wins = wins;
                this.losses = losses;
                this.draws = draws;
                this.winRate = winRate;
                this.gamesBehind = gamesBehind;
                this.last10 = last10;
                this.streak = streak;
                this.homeRecord = homeRecord;
                this.awayRecord = awayRecord;
            }
        }


    @Data
    public static class GameDetailDTO {

        /* ===== 경기 헤더 ===== */
        private String gameId;          // 20250615W0000
        private String date;            // 2025-06-15
        private String time;            // 17:00
        private String stadium;         // 잠실
        private String homeTeam;        // 두산 베어스
        private String awayTeam;        // 키움 히어로즈
        private int    homeScore;       // 3
        private int    awayScore;       // 2
        private String winningPitcher;  // 곽빈
        private String losingPitcher;   // 정현우
        private String savePitcher;     // 김택연 (없으면 null)

        /* ===== 상세 기록 ===== */
        private List<BatterRecordDTO> homeBatters;
        private List<BatterRecordDTO> awayBatters;
        private List<PitcherRecordDTO> homePitchers;
        private List<PitcherRecordDTO> awayPitchers;

        /* ---------- 타자 기록 ---------- */
        @Data
        public static class BatterRecordDTO {
            private int    order;     // 타순 (1~9)
            private String player;    // 선수명
            private int    atBat;     // 타수
            private int    hit;       // 안타
            private int    rbi;       // 타점
            private int    run;       // 득점
            private double avg;       // 타율
            // ↓ 원하는 경우 이닝별 결과(문자열)도 저장
            private String result1; private String result2; private String result3;
            private String result4; private String result5; private String result6;
            private String result7; private String result8; private String result9;

            public BatterRecordDTO(int order, String player, int atBat, int hit, int rbi, int run, double avg, String result1, String result2, String result3, String result4, String result5, String result6, String result7, String result8, String result9) {
                this.order = order;
                this.player = player;
                this.atBat = atBat;
                this.hit = hit;
                this.rbi = rbi;
                this.run = run;
                this.avg = avg;
                this.result1 = result1;
                this.result2 = result2;
                this.result3 = result3;
                this.result4 = result4;
                this.result5 = result5;
                this.result6 = result6;
                this.result7 = result7;
                this.result8 = result8;
                this.result9 = result9;
            }
        }

        /* ---------- 투수 기록 ---------- */
        @Data
        public static class PitcherRecordDTO {
            private String player;      // 선수명
            private String role;        // 선발/구원
            private String decision;    // 승/패/세/홀드/없음
            private int    win;         // 승
            private int    loss;        // 패
            private int    save;        // 세
            private double innings;     // 이닝(4.1 → 4⅓)
            private int    batters;     // 타자 수
            private int    pitches;     // 투구 수
            private int    atBat;       // 타수
            private int    hits;        // 피안타
            private int    homeRuns;    // 피홈런
            private int    walksHbp;    // 4사구
            private int    strikeOuts;  // 삼진
            private int    runs;        // 실점
            private int    earned;      // 자책
            private double era;         // 평균자책점


            public PitcherRecordDTO(String player, String role, String decision, int win, int loss, int save, double innings, int batters, int pitches, int atBat, int hits, int homeRuns, int walksHbp, int strikeOuts, int runs, int earned, double era) {
                this.player = player;
                this.role = role;
                this.decision = decision;
                this.win = win;
                this.loss = loss;
                this.save = save;
                this.innings = innings;
                this.batters = batters;
                this.pitches = pitches;
                this.atBat = atBat;
                this.hits = hits;
                this.homeRuns = homeRuns;
                this.walksHbp = walksHbp;
                this.strikeOuts = strikeOuts;
                this.runs = runs;
                this.earned = earned;
                this.era = era;
            }
        }

        public GameDetailDTO(String gameId, String date, String time, String stadium, String homeTeam, String awayTeam, int homeScore, int awayScore, String winningPitcher, String losingPitcher, String savePitcher, List<BatterRecordDTO> homeBatters, List<BatterRecordDTO> awayBatters, List<PitcherRecordDTO> homePitchers, List<PitcherRecordDTO> awayPitchers) {
            this.gameId = gameId;
            this.date = date;
            this.time = time;
            this.stadium = stadium;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
            this.homeScore = homeScore;
            this.awayScore = awayScore;
            this.winningPitcher = winningPitcher;
            this.losingPitcher = losingPitcher;
            this.savePitcher = savePitcher;
            this.homeBatters = homeBatters;
            this.awayBatters = awayBatters;
            this.homePitchers = homePitchers;
            this.awayPitchers = awayPitchers;
        }
    }




    }

