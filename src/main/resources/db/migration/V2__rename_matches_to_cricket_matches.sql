alter table matches rename to cricket_matches;

alter index idx_matches_status rename to idx_cricket_matches_status;
alter index idx_matches_match_date rename to idx_cricket_matches_match_date;
alter index idx_matches_series rename to idx_cricket_matches_series;
