const API_BASE_URL =
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api';

export async function fetchMatches(signal) {
    const response = await fetch(`${API_BASE_URL}/matches`, {
        signal,
        headers: {
            Accept: 'application/json',
        },
    });

    if (!response.ok) {
        throw new Error(`Failed to fetch matches: ${response.status}`);
    }

    const data = await response.json();

    if (Array.isArray(data)) {
        return data;
    }

    if (Array.isArray(data.value)) {
        return data.value;
    }

    throw new Error('Unexpected matches API response format');
}