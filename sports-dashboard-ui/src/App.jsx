import { useCallback, useEffect, useMemo, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client, ReconnectionTimeMode } from '@stomp/stompjs';
import { fetchMatches } from './api/matchesApi';
import './App.css';

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || window.location.origin;
const WS_TOPIC = import.meta.env.VITE_WS_TOPIC || '/topic/scores';

const STATUS_FILTERS = ['ALL', 'LIVE', 'UPCOMING', 'COMPLETED'];

function formatTime(value) {
  if (!value) return 'Time unavailable';

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return 'Time unavailable';
  }

  return new Intl.DateTimeFormat(undefined, {
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function formatDate(value) {
  if (!value) return 'Time unavailable';

  const date = new Date(`${value}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(date);
}

function getTeamInitials(team = '') {
  const words = team
    .trim()
    .split(/\s+/)
    .filter(Boolean);

  if (words.length === 0) return '?';

  if (words.length === 1) {
    return words[0].slice(0, 2).toUpperCase();
  }

  return words
    .slice(0, 2)
    .map(word => word[0])
    .join('')
    .toUpperCase();
}

function getStatusLabel(status) {
  switch (status) {
    case 'LIVE':
      return 'LIVE';
    case 'COMPLETED':
      return 'FINAL';
    case 'UPCOMING':
      return 'UPCOMING';
    default:
      return status || 'UNKNOWN';
  }
}

function sortMatches(matches) {
  const priority = {
    LIVE: 0,
    UPCOMING: 1,
    COMPLETED: 2,
  };

  return [...matches].sort((a, b) => {
    const statusDifference =
      (priority[a.status] ?? 9) - (priority[b.status] ?? 9);

    if (statusDifference !== 0) {
      return statusDifference;
    }

    const dateA = a.matchDate || '';
    const dateB = b.matchDate || '';

    return dateA.localeCompare(dateB);
  });
}

function StatusBadge({ status }) {
  return (
    <span className={`status-badge status-${status?.toLowerCase()}`}>
      {status === 'LIVE' && <span className="live-dot" />}
      {getStatusLabel(status)}
    </span>
  );
}

function TeamMark({ name }) {
  return (
    <span className="team-mark" aria-hidden="true">
      {getTeamInitials(name)}
    </span>
  );
}

function MatchCard({ match, onSelect, featured = false }) {
  return (
    <button
      type="button"
      className={`match-card ${featured ? 'match-card-featured' : ''}`}
      onClick={() => onSelect(match)}
    >
      <div className="match-card-top">
        <div className="competition">
          <span className="competition-dot" />
          {match.series || 'Unknown series'}
        </div>

        <StatusBadge status={match.status} />
      </div>

      <div className="match-teams">
        <div className="team-row">
          <div className="team-name">
            <TeamMark name={match.teamA} />
            <span>{match.teamA}</span>
          </div>

          <span className="team-score">
            {match.score
              ? match.score.split(' - ')[0] || 'No score'
              : (match.status === 'UPCOMING' ? 'Not started' : 'Score unavailable')}
          </span>
        </div>

        <div className="team-row">
          <div className="team-name">
            <TeamMark name={match.teamB} />
            <span>{match.teamB}</span>
          </div>

          <span className="team-score">
            {match.score
              ? match.score.split(' - ')[1] || 'No score'
              : (match.status === 'UPCOMING' ? 'Not started' : 'Score unavailable')}
          </span>
        </div>
      </div>

      <div className="match-card-footer">
        <span>
          {match.status === 'LIVE'
            ? 'Match in progress'
            : formatDate(match.matchDate)}
        </span>

        <span>
          Updated {formatTime(match.lastUpdated)}
          <span className="arrow">&gt;</span>
        </span>
      </div>
    </button>
  );
}

function EmptyState({ filter, search }) {
  return (
    <div className="empty-state">
      <div className="empty-icon">?</div>

      <h3>No matches found</h3>

      <p>
        {search
          ? `No matches match "${search}".`
          : `There are no ${filter.toLowerCase()} matches available.`}
      </p>
    </div>
  );
}

function MatchDetails({ match, onClose }) {
  useEffect(() => {
    if (!match) return undefined;

    const handleKeyDown = event => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [match, onClose]);

  if (!match) return null;

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={event => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <section
        className="match-details"
        role="dialog"
        aria-modal="true"
        aria-labelledby="match-details-title"
      >
        <div className="details-header">
          <div>
            <div className="eyebrow">
              {match.series || 'Unknown series'}
            </div>

            <h2 id="match-details-title">
              {match.teamA}
              <span> vs </span>
              {match.teamB}
            </h2>
          </div>

          <button
            type="button"
            className="close-button"
            onClick={onClose}
            aria-label="Close match details"
          >
            &#10005;
          </button>
        </div>

        <div className="details-status">
          <StatusBadge status={match.status} />

          <span>
            {match.status === 'LIVE'
              ? 'Live match'
              : formatDate(match.matchDate)}
          </span>
        </div>

        <div className="details-score">
          <div className="details-team">
            <TeamMark name={match.teamA} />
            <span>{match.teamA}</span>
          </div>

          <div className="details-main-score">
            {match.score || (match.status === 'UPCOMING' ? 'Not started' : 'Score unavailable')}
          </div>

          <div className="details-team">
            <TeamMark name={match.teamB} />
            <span>{match.teamB}</span>
          </div>
        </div>

        <div className="details-grid">
          <div>
            <span>Competition</span>
            <strong>{match.series || 'Unknown series'}</strong>
          </div>

          <div>
            <span>Date</span>
            <strong>{formatDate(match.matchDate)}</strong>
          </div>

          <div>
            <span>Status</span>
            <strong>{getStatusLabel(match.status)}</strong>
          </div>

          <div>
            <span>Last updated</span>
            <strong>{formatTime(match.lastUpdated)}</strong>
          </div>
        </div>

        <div className="details-note">
          <span className="details-note-indicator" />
          Match information is supplied by the live sports data
          provider and updates as new data becomes available.
        </div>
      </section>
    </div>
  );
}

function App() {
  const [matches, setMatches] = useState([]);
  const [selectedMatchId, setSelectedMatchId] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [wsStatus, setWsStatus] = useState('CONNECTING');
  const [staleCheckAt, setStaleCheckAt] = useState(() => Date.now());

  const selectedMatch = useMemo(
    () => matches.find(match => match.id === selectedMatchId) ?? null,
    [matches, selectedMatchId]
  );

  const loadMatches = useCallback(async signal => {
    try {
      setError('');

      const data = await fetchMatches(signal);

      setMatches(Array.isArray(data) ? data : []);
    } catch (err) {
      if (err?.name === 'AbortError') {
        return;
      }

      console.error('Match API error:', err);

      setError(
        'Unable to load match data. The server may be temporarily unavailable.'
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      loadMatches(controller.signal);
    }, 0);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [loadMatches]);

  /*
   * REST refresh is intentionally retained as a fallback.
   * The backend currently persists provider updates, while the WebSocket
   * channel can deliver immediate changes when the server publishes them.
   */
  useEffect(() => {
    const refresh = () => {
      loadMatches();
    };

    const interval = window.setInterval(refresh, 60_000);

    return () => window.clearInterval(interval);
  }, [loadMatches]);

  useEffect(() => {
    const stompClient = new Client({
      webSocketFactory: () =>
        new SockJS(`${WS_BASE_URL}/ws-sports`),

      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      reconnectDelay: 2000,
      maxReconnectDelay: 30000,
      connectionTimeout: 10000,

      onConnect: () => {
        setWsStatus('CONNECTED');
        loadMatches();

        stompClient.subscribe(WS_TOPIC, message => {
          try {
            const updatedMatch = JSON.parse(message.body);

            setMatches(previous => {
              const index = previous.findIndex(
                match => match.id === updatedMatch.id
              );

              if (index === -1) {
                return [updatedMatch, ...previous];
              }

              const next = [...previous];
              next[index] = updatedMatch;

              return next;
            });

          } catch (err) {
            console.error(
              'Invalid WebSocket match payload:',
              err
            );
          }
        });
      },

      onDisconnect: () => {
        setWsStatus('DISCONNECTED');
      },

      onStompError: frame => {
        console.error('STOMP error:', frame);
        setWsStatus('ERROR');
      },

      onWebSocketError: error => {
        console.error('WebSocket error:', error);
        setWsStatus('ERROR');
      },

      onWebSocketClose: () => {
        setWsStatus('DISCONNECTED');
      },
    });

    stompClient.activate();

    return () => {
      stompClient.deactivate();
    };
  }, [loadMatches]);


  const latestDataUpdate = useMemo(() => {
    const timestamps = matches
      .map(match => match.lastUpdated)
      .filter(Boolean)
      .map(value => new Date(value).getTime())
      .filter(value => !Number.isNaN(value));

    if (timestamps.length === 0) return null;
    return new Date(Math.max(...timestamps));
  }, [matches]);

  const latestLiveUpdate = useMemo(() => {
    const timestamps = matches
      .filter(match => match.status === 'LIVE')
      .map(match => match.lastUpdated)
      .filter(Boolean)
      .map(value => new Date(value).getTime())
      .filter(value => !Number.isNaN(value));

    if (timestamps.length === 0) return null;
    return new Date(Math.max(...timestamps));
  }, [matches]);

  const isStale = latestLiveUpdate
    ? staleCheckAt - latestLiveUpdate.getTime() >= 90000
    : false;

  useEffect(() => {
    if (!latestLiveUpdate) return undefined;

    const delay = Math.max(0,
      latestLiveUpdate.getTime() + 90000 - Date.now()
    );

    const timer = window.setTimeout(() => {
      setStaleCheckAt(Date.now());
    }, delay);

    return () => window.clearTimeout(timer);
  }, [latestLiveUpdate]);


  const counts = useMemo(
    () => ({
      ALL: matches.length,
      LIVE: matches.filter(match => match.status === 'LIVE').length,
      UPCOMING: matches.filter(
        match => match.status === 'UPCOMING'
      ).length,
      COMPLETED: matches.filter(
        match => match.status === 'COMPLETED'
      ).length,
    }),
    [matches]
  );

  const visibleMatches = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();

    const filtered = matches.filter(match => {
      const statusMatches =
        filter === 'ALL' || match.status === filter;

      if (!statusMatches) return false;

      if (!normalizedSearch) return true;

      return [
        match.teamA,
        match.teamB,
        match.series,
      ]
        .filter(Boolean)
        .some(value =>
          value.toLowerCase().includes(normalizedSearch)
        );
    });

    return sortMatches(filtered);
  }, [matches, filter, search]);

  const liveMatches = visibleMatches.filter(
    match => match.status === 'LIVE'
  );

  const upcomingMatches = visibleMatches.filter(
    match => match.status === 'UPCOMING'
  );

  const completedMatches = visibleMatches.filter(
    match => match.status === 'COMPLETED'
  );

  const statusText = {
    CONNECTED: 'Live updates',
    CONNECTING: 'Connecting...',
    DISCONNECTED: 'Reconnecting...',
    ERROR: 'Live updates unavailable',
  }[wsStatus];


  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="brand">
            <div className="brand-mark">C</div>

            <div>
              <div className="brand-name">
                Cricket Match Center
              </div>

              <div className="brand-subtitle">
                Live scores &#183; Fixtures &#183; Results
              </div>
            </div>
          </div>

          <div className="system-status">
            <span
              className={`system-dot system-${wsStatus.toLowerCase()}`}
            />

            <span>{isStale && wsStatus === 'CONNECTED' ? 'Live updates delayed' : statusText}</span>

            {latestDataUpdate && (
              <span className="refresh-time">Updated {formatTime(latestDataUpdate)}
              </span>
            )}
          </div>
        </div>
      </header>

      <main className="page">
        <section className="page-heading">
          <div>
            <div className="section-kicker">
              CRICKET
            </div>

            <h1>Cricket Matches</h1>

            <p>
              Follow live matches, upcoming fixtures and recent
              results in one place.
            </p>
          </div>

          <button
            type="button"
            className="refresh-button"
            onClick={() => loadMatches()}
          >
            Refresh
          </button>
        </section>

        {error && (
          <div className="error-banner">
            <strong>Data unavailable</strong>
            <span>{error}</span>
            <button
              type="button"
              onClick={() => loadMatches()}
            >
              Retry
            </button>
          </div>
        )}

        <section className="toolbar">
          <div className="status-tabs">
            {STATUS_FILTERS.map(status => (
              <button
                key={status}
                type="button"
                className={
                  filter === status
                    ? 'status-tab active'
                    : 'status-tab'
                }
                onClick={() => setFilter(status)}
              >
                {status === 'LIVE' && (
                  <span className="tab-live-dot" />
                )}

                {status === 'COMPLETED'
                  ? 'RESULTS'
                  : status}

                <span className="tab-count">
                  {counts[status]}
                </span>
              </button>
            ))}
          </div>

          <div className="search-wrapper">
            <span className="search-icon">&#128269;</span>

            <input
              type="search"
              value={search}
              onChange={event =>
                setSearch(event.target.value)
              }
              placeholder="Search teams or competitions"
              aria-label="Search teams or competitions"
            />
          </div>
        </section>

        {loading ? (
          <div className="loading-state">
            <div className="loading-line" />
            <div className="loading-line short" />
            <div className="loading-line" />
          </div>
        ) : visibleMatches.length === 0 ? (
          <EmptyState filter={filter} search={search} />
        ) : (
          <>
            {liveMatches.length > 0 && (
              <section className="match-section">
                <div className="section-heading">
                  <div>
                    <span className="live-heading-dot" />
                    <h2>Live now</h2>
                  </div>

                  <span>
                    {liveMatches.length} match
                    {liveMatches.length !== 1
                      ? 'es'
                      : ''}
                  </span>
                </div>

                <div className="live-grid">
                  {liveMatches.map((match, index) => (
                    <MatchCard
                      key={match.id}
                      match={match}
                      featured={index === 0}
                      onSelect={match => setSelectedMatchId(match.id)}
                    />
                  ))}
                </div>
              </section>
            )}

            {upcomingMatches.length > 0 && (
              <section className="match-section">
                <div className="section-heading">
                  <div>
                    <h2>Upcoming</h2>
                  </div>

                  <span>
                    {upcomingMatches.length} scheduled
                  </span>
                </div>

                <div className="match-list">
                  {upcomingMatches.map(match => (
                    <MatchCard
                      key={match.id}
                      match={match}
                      onSelect={match => setSelectedMatchId(match.id)}
                    />
                  ))}
                </div>
              </section>
            )}

            {completedMatches.length > 0 && (
              <section className="match-section">
                <div className="section-heading">
                  <div>
                    <h2>Results</h2>
                  </div>

                  <span>
                    {completedMatches.length} completed
                  </span>
                </div>

                <div className="match-list">
                  {completedMatches.map(match => (
                    <MatchCard
                      key={match.id}
                      match={match}
                      onSelect={match => setSelectedMatchId(match.id)}
                    />
                  ))}
                </div>
              </section>
            )}
          </>
        )}

        <footer className="footer">
          <span>Cricket Match Center</span>

          <span className="footer-separator">|</span>

          <a
            href="https://sportscore.com/"
            target="_blank"
            rel="noreferrer"
          >
            Powered by SportScore
          </a>
        </footer>
      </main>

      <MatchDetails
        match={selectedMatch}
        onClose={() => setSelectedMatchId(null)}
      />
    </div>
  );
}

export default App;
