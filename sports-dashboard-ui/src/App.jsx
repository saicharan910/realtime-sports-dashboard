import React, { useEffect, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export default function App() {
  const [matches, setMatches] = useState([]);
  const [selectedMatch, setSelectedMatch] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [wsStatus, setWsStatus] = useState('Connecting...');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('http://localhost:8000/api/matches')
      .then(res => res.json())
      .then(data => {
        setMatches(data);
        setLoading(false);
      })
      .catch(err => {
        console.error("API error:", err);
        setLoading(false);
      });

    const socket = new SockJS('http://localhost:8000/ws-sports');
    const stompClient = new Client({
      webSocketFactory: () => socket,
      onConnect: () => {
        setWsStatus('Connected');
        stompClient.subscribe('/topic/scores', (message) => {
          const updatedMatch = JSON.parse(message.body);
          setMatches(prev => {
            const index = prev.findIndex(m => m.id === updatedMatch.id);
            if (index !== -1) {
              const copy = [...prev];
              copy[index] = updatedMatch;
              return copy;
            }
            return [updatedMatch, ...prev];
          });
        });
      },
      onStompError: () => setWsStatus('Error'),
      onWebSocketClose: () => setWsStatus('Disconnected')
    });

    stompClient.activate();
    return () => stompClient.deactivate();
  }, []);

  const filteredMatches = matches.filter(m => {
    if (filter === 'ALL') return true;
    return m.status && m.status.toUpperCase() === filter;
  });

  return (
    <div style={{ background: '#ffffff', color: '#111827', minHeight: '100vh', fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif', fontSize: '14px' }}>

      {/* Clean White Professional Header */}
      <header style={{ borderBottom: '1px solid #e5e7eb', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#ffffff' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '18px', fontWeight: '600', color: '#111827' }}>Cricket Match Center // Enterprise Edition</h1>
          <span style={{ fontSize: '12px', color: '#6b7280' }}>Real-time live scores, past results, upcoming schedules & win probability analytics</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', color: '#4b5563' }}>
          <span style={{
            width: '8px', height: '8px', borderRadius: '50%',
            background: wsStatus === 'Connected' ? '#10b981' : '#ef4444',
            display: 'inline-block'
          }}></span>
          <span style={{ fontWeight: '500' }}>{wsStatus}</span>
        </div>
      </header>

      {/* Main Container */}
      <main style={{ maxWidth: '1050px', margin: '32px auto', padding: '0 16px' }}>

        {/* Filter Bar */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h2 style={{ fontSize: '16px', fontWeight: '600', margin: 0, color: '#111827' }}>Fixtures & Results Stream</h2>
          <div style={{ display: 'flex', gap: '6px' }}>
            {['ALL', 'LIVE', 'UPCOMING', 'COMPLETED'].map(tab => (
              <button
                key={tab}
                onClick={() => setFilter(tab)}
                style={{
                  background: filter === tab ? '#111827' : '#f3f4f6',
                  color: filter === tab ? '#ffffff' : '#374151',
                  border: 'none',
                  padding: '6px 14px',
                  borderRadius: '6px',
                  fontSize: '12px',
                  fontWeight: '500',
                  cursor: 'pointer'
                }}
              >
                {tab}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div style={{ padding: '32px', textAlign: 'center', color: '#6b7280', border: '1px solid #e5e7eb', borderRadius: '8px' }}>Loading match schedules...</div>
        ) : filteredMatches.length === 0 ? (
          <div style={{ padding: '32px', textAlign: 'center', color: '#6b7280', border: '1px solid #e5e7eb', borderRadius: '8px' }}>
            No matches available for status: {filter}
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {filteredMatches.map(m => (
              <div
                key={m.id}
                onClick={() => setSelectedMatch(m)}
                style={{
                  background: '#ffffff',
                  border: '1px solid #e5e7eb',
                  borderRadius: '8px',
                  padding: '20px',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.02)',
                  transition: 'border-color 0.15s ease'
                }}
                onMouseEnter={e => e.currentTarget.style.borderColor = '#9ca3af'}
                onMouseLeave={e => e.currentTarget.style.borderColor = '#e5e7eb'}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                    <span style={{
                      fontSize: '11px', fontWeight: '600', padding: '2px 8px', borderRadius: '4px',
                      background: m.status === 'LIVE' ? '#fee2e2' : m.status === 'COMPLETED' ? '#d1fae5' : '#fef3c7',
                      color: m.status === 'LIVE' ? '#dc2626' : m.status === 'COMPLETED' ? '#059669' : '#d97706'
                    }}>
                      {m.status === 'LIVE' ? 'LIVE' : m.status}
                    </span>
                    <span style={{ fontSize: '12px', color: '#6b7280' }}>{m.series}</span>
                  </div>
                  <div style={{ fontWeight: '600', fontSize: '16px', color: '#111827', marginBottom: '4px' }}>
                    {m.teamA} vs {m.teamB}
                  </div>
                  <div style={{ fontSize: '13px', color: '#4b5563' }}>
                    {m.score}
                  </div>
                </div>

                <div style={{ textAlign: 'right', fontSize: '12px', color: '#6b7280' }}>
                  <div>Date: {m.matchDate || 'Scheduled'}</div>
                  <div style={{ marginTop: '4px', color: '#2563eb', fontWeight: '500' }}>Live Commentary Log →</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Match Details & Timestamped Commentary Modal */}
      {selectedMatch && (
        <div style={{ position: 'fixed', top: 0, left: 0, width: '100%', height: '100%', background: 'rgba(0, 0, 0, 0.4)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000 }}>
          <div style={{ background: '#ffffff', borderRadius: '12px', width: '700px', maxWidth: '90%', maxHeight: '85vh', overflowY: 'auto', padding: '28px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #e5e7eb', paddingBottom: '16px', marginBottom: '20px' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '600', color: '#111827' }}>{selectedMatch.teamA} vs {selectedMatch.teamB}</h3>
                <span style={{ fontSize: '12px', color: '#6b7280' }}>{selectedMatch.series} | Date: {selectedMatch.matchDate}</span>
              </div>
              <button onClick={() => setSelectedMatch(null)} style={{ background: '#f3f4f6', border: 'none', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', fontWeight: '500', fontSize: '12px' }}>Close</button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <span style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '4px' }}>MATCH STATUS & SCORE</span>
                <div style={{ background: '#f9fafb', padding: '12px', borderRadius: '6px', border: '1px solid #e5e7eb', fontWeight: '500' }}>
                  {selectedMatch.score}
                </div>
              </div>

              <div>
                <span style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '4px' }}>WIN PROBABILITY TELEMETRY MODEL</span>
                <div style={{ background: '#f0fdf4', color: '#166534', padding: '12px', borderRadius: '6px', border: '1px solid #bbf7d0', fontWeight: '500', fontSize: '13px' }}>
                  {selectedMatch.winProbability || "Calculating probabilistic model..."}
                </div>
              </div>

              <div>
                <span style={{ fontSize: '12px', fontWeight: '600', color: '#374151', display: 'block', marginBottom: '4px' }}>TIMESTAMPED COMMENTARY STREAM</span>
                <div style={{ background: '#f9fafb', padding: '14px', borderRadius: '6px', border: '1px solid #e5e7eb', whiteSpace: 'pre-line', fontSize: '13px', lineHeight: '1.6', color: '#1f2937', fontFamily: 'monospace', maxHeight: '280px', overflowY: 'auto' }}>
                  {selectedMatch.commentary || "No commentary logs captured yet."}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}