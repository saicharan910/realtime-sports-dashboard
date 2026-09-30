const API_BASE_URL = 'http://localhost:8000/api';

export async function fetchMatches() {
    const response = await fetch(`${API_BASE_URL}/matches`);

    if (!response.ok) {
        throw new Error(`Failed to fetch matches: ${response.status}`);
    }

    return response.json();
}