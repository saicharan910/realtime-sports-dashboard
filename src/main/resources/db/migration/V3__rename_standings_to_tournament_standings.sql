alter table standings rename to tournament_standings;

alter index idx_standings_series rename to idx_tournament_standings_series;
alter index idx_standings_series_points rename to idx_tournament_standings_series_points;

alter table tournament_standings
    rename constraint pk_standings to pk_tournament_standings;

alter table tournament_standings
    rename constraint uk_standings_series_team to uk_tournament_standings_series_team;
