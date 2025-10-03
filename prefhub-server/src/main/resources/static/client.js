class PrefHubClient {
    constructor() {
        this.baseUrl = window.location.origin;
        this.authToken = localStorage.getItem('authToken');
        this.currentGameId = localStorage.getItem('currentGameId');
        this.currentUsername = localStorage.getItem('username');
        this.pollingInterval = null;

        this.init();
    }

    init() {
        this.log('Клиент инициализирован', 'info');

        if (this.authToken) {
            this.showMainSection();
            if (this.currentGameId) {
                $('#game-id').val(this.currentGameId);
                this.startPolling();
            }
        } else {
            this.showAuthSection();
        }
    }

    log(message, type = 'info') {
        const timestamp = new Date().toLocaleTimeString('ru-RU');
        const logEntry = $('<div>')
            .addClass(`log-entry log-${type}`)
            .html(`<span class="log-time">[${timestamp}]</span>${message}`);

        $('#log-output').prepend(logEntry);

        // Keep only last 100 entries
        const entries = $('#log-output .log-entry');
        if (entries.length > 100) {
            entries.slice(100).remove();
        }
    }

    async apiCall(endpoint, method = 'GET', body = null, requireAuth = true) {
        try {
            const options = {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                }
            };

            if (requireAuth && this.authToken) {
                options.headers['Authorization'] = `Bearer ${this.authToken}`;
            }

            if (body) {
                options.body = JSON.stringify(body);
            }

            this.log(`${method} ${endpoint}`, 'info');
            const response = await fetch(this.baseUrl + endpoint, options);
            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Request failed');
            }

            return data;
        } catch (error) {
            this.log(`Ошибка: ${error.message}`, 'error');
            throw error;
        }
    }

    showAuthSection() {
        $('#auth-section').show();
        $('#main-section').hide();
    }

    showMainSection() {
        $('#auth-section').hide();
        $('#main-section').show();
        $('#username-display').text(this.currentUsername);
    }

    async register() {
        const username = $('#auth-username').val().trim();
        const password = $('#auth-password').val().trim();

        if (!username || !password) {
            this.log('Заполните все поля', 'warning');
            return;
        }

        try {
            await this.apiCall('/api/auth/register', 'POST', { username, password }, false);
            this.log('Регистрация успешна! Теперь войдите', 'success');
        } catch (error) {
            this.log(`Ошибка регистрации: ${error.message}`, 'error');
        }
    }

    async login() {
        const username = $('#auth-username').val().trim();
        const password = $('#auth-password').val().trim();

        if (!username || !password) {
            this.log('Заполните все поля', 'warning');
            return;
        }

        try {
            const data = await this.apiCall('/api/auth/login', 'POST', { username, password }, false);
            this.authToken = data.token;
            this.currentUsername = username;

            localStorage.setItem('authToken', this.authToken);
            localStorage.setItem('username', username);

            this.log('Вход выполнен успешно', 'success');
            this.showMainSection();
        } catch (error) {
            this.log(`Ошибка входа: ${error.message}`, 'error');
        }
    }

    async logout() {
        try {
            await this.apiCall('/api/auth/logout', 'POST');
            this.authToken = null;
            this.currentUsername = null;
            this.currentGameId = null;

            localStorage.removeItem('authToken');
            localStorage.removeItem('username');
            localStorage.removeItem('currentGameId');

            this.stopPolling();
            this.log('Выход выполнен', 'success');
            this.showAuthSection();
        } catch (error) {
            this.log(`Ошибка выхода: ${error.message}`, 'error');
        }
    }

    async createGame() {
        const gameId = $('#game-id').val().trim();

        if (!gameId) {
            this.log('Введите ID игры', 'warning');
            return;
        }

        try {
            const data = await this.apiCall('/api/games/create', 'POST', { gameId });
            this.currentGameId = gameId;
            localStorage.setItem('currentGameId', gameId);

            this.log(`Игра "${gameId}" создана`, 'success');
            this.displayGameState(data);
            this.startPolling();
        } catch (error) {
            this.log(`Ошибка создания игры: ${error.message}`, 'error');
        }
    }

    async joinGame() {
        const gameId = $('#game-id').val().trim();

        if (!gameId) {
            this.log('Введите ID игры', 'warning');
            return;
        }

        try {
            const data = await this.apiCall('/api/games/join', 'POST', { gameId });
            this.currentGameId = gameId;
            localStorage.setItem('currentGameId', gameId);

            this.log(`Присоединились к игре "${gameId}"`, 'success');
            this.displayGameState(data);
            this.startPolling();
        } catch (error) {
            this.log(`Ошибка присоединения: ${error.message}`, 'error');
        }
    }

    async listGames() {
        try {
            const data = await this.apiCall('/api/games/list', 'GET');
            this.log('Список игр получен', 'success');

            if (Array.isArray(data) && data.length > 0) {
                const gamesList = data.map(g => `- ${g.gameId} (${g.players?.length || 0} игроков)`).join('\n');
                this.log(`Доступные игры:\n${gamesList}`, 'info');
            } else {
                this.log('Нет доступных игр', 'info');
            }
        } catch (error) {
            this.log(`Ошибка получения списка игр: ${error.message}`, 'error');
        }
    }

    async getGameState() {
        if (!this.currentGameId) {
            return;
        }

        try {
            const data = await this.apiCall(`/api/games/state?gameId=${this.currentGameId}`, 'GET');
            this.displayGameState(data);
        } catch (error) {
            // Silently fail during polling
            if (!this.pollingInterval) {
                this.log(`Ошибка получения состояния: ${error.message}`, 'error');
            }
        }
    }

    displayGameState(state) {
        $('#game-state').show();

        let html = '';

        // Basic info
        html += `<p><strong>Игра:</strong> ${state.gameId || 'N/A'}</p>`;
        html += `<p><strong>Раунд:</strong> ${state.roundNumber || 0}</p>`;
        html += `<p><strong>Фаза:</strong> <span class="game-phase">${state.phase || 'N/A'}</span></p>`;

        if (state.nextActionDescription) {
            html += `<p><strong>Следующее действие:</strong> ${state.nextActionDescription}</p>`;
        }

        if (state.yourTurn) {
            html += `<p class="your-turn">⚡ ВАШ ХОД! ⚡</p>`;
        }

        // Hand
        if (state.hand && state.hand.length > 0) {
            html += `<p><strong>Ваши карты:</strong> `;
            html += state.hand.map(card => this.formatCard(card)).join(' ');
            html += `</p>`;
        }

        // Widow
        if (state.widow && state.widow.length > 0) {
            html += `<p><strong>Прикуп:</strong> `;
            html += state.widow.map(card => this.formatCard(card)).join(' ');
            html += `</p>`;
        }

        // Contract and trump
        if (state.contract) {
            html += `<p><strong>Контракт:</strong> ${state.contract.displayName || state.contract}</p>`;
            if (state.trumpSuit) {
                html += `<p><strong>Козырь:</strong> ${state.trumpSuit.symbol} ${state.trumpSuit.russianName}</p>`;
            }
        }

        // Declarer
        if (state.declarerUsername) {
            html += `<p><strong>Разыгрывающий:</strong> ${state.declarerUsername}</p>`;
        }

        // Bids
        if (state.bids && Object.keys(state.bids).length > 0) {
            html += `<p><strong>Заявки:</strong></p><ul>`;
            for (const [player, bid] of Object.entries(state.bids)) {
                html += `<li>${player}: ${bid}</li>`;
            }
            html += `</ul>`;
        }

        // Current trick
        if (state.currentTrick && Object.keys(state.currentTrick).length > 0) {
            html += `<div class="current-trick">`;
            html += `<strong>Текущая взятка:</strong> `;
            for (const [player, card] of Object.entries(state.currentTrick)) {
                html += `<div class="trick-card">`;
                html += `<div class="trick-card-value">${this.formatCard(card)}</div>`;
                html += `<div class="trick-card-player">${player}</div>`;
                html += `</div>`;
            }
            html += `</div>`;
        }

        // Scores
        if (state.scores && Object.keys(state.scores).length > 0) {
            html += `<table class="scores-table">`;
            html += `<tr><th>Игрок</th><th>Очки</th><th>Пуля</th><th>Гора</th></tr>`;
            for (const [player, score] of Object.entries(state.scores)) {
                const bullets = state.bullets?.[player] || 0;
                const mountains = state.mountains?.[player] || 0;
                html += `<tr><td>${player}</td><td>${score}</td><td>${bullets}</td><td>${mountains}</td></tr>`;
            }
            html += `</table>`;
        }

        // Other players
        if (state.otherPlayers && state.otherPlayers.length > 0) {
            html += `<p><strong>Другие игроки:</strong> ${state.otherPlayers.join(', ')}</p>`;
        }

        $('#game-info').html(html);

        // Update action controls
        this.updateActionControls(state);
    }

    updateActionControls(state) {
        $('#bid-controls').hide();
        $('#play-controls').hide();
        $('#next-round-controls').hide();

        if (!state.yourTurn) {
            return;
        }

        if (state.phase === 'BIDDING') {
            $('#bid-controls').show();
        } else if (state.phase === 'PLAYING') {
            $('#play-controls').show();
            this.renderHandCards(state.hand || []);
        } else if (state.phase === 'ROUND_ENDED') {
            $('#next-round-controls').show();
        }
    }

    renderHandCards(hand) {
        const container = $('#hand-cards');
        container.empty();

        hand.forEach(card => {
            const button = $('<button>')
                .addClass('card-button')
                .text(this.formatCard(card))
                .click(() => this.playCard(card));
            container.append(button);
        });
    }

    formatCard(card) {
        if (!card || !card.rank || !card.suit) return '?';

        const rankSymbol = this.getRankSymbol(card.rank);
        const suitSymbol = this.getSuitSymbol(card.suit);

        return rankSymbol + suitSymbol;
    }

    getRankSymbol(rank) {
        const symbols = {
            'SEVEN': '7', 'EIGHT': '8', 'NINE': '9', 'TEN': '10',
            'JACK': 'В', 'QUEEN': 'Д', 'KING': 'К', 'ACE': 'Т'
        };
        return symbols[rank] || rank;
    }

    getSuitSymbol(suit) {
        const symbols = {
            'SPADES': '♠', 'CLUBS': '♣', 'DIAMONDS': '♦', 'HEARTS': '♥'
        };
        return symbols[suit] || suit;
    }

    async placeBid() {
        const contract = $('#bid-select').val();

        try {
            await this.apiCall('/api/games/bid', 'POST', {
                gameId: this.currentGameId,
                contract: contract
            });
            this.log(`Заявка сделана: ${contract}`, 'success');
            await this.getGameState();
        } catch (error) {
            this.log(`Ошибка заявки: ${error.message}`, 'error');
        }
    }

    async playCard(card) {
        const cardStr = `${card.rank}_${card.suit}`;

        try {
            await this.apiCall('/api/games/play', 'POST', {
                gameId: this.currentGameId,
                card: cardStr
            });
            this.log(`Карта сыграна: ${this.formatCard(card)}`, 'success');
            await this.getGameState();
        } catch (error) {
            this.log(`Ошибка игры картой: ${error.message}`, 'error');
        }
    }

    async nextRound() {
        try {
            await this.apiCall('/api/games/next-round', 'POST', {
                gameId: this.currentGameId
            });
            this.log('Следующий раунд начат', 'success');
            await this.getGameState();
        } catch (error) {
            this.log(`Ошибка начала раунда: ${error.message}`, 'error');
        }
    }

    startPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
        }

        this.log('Начато автообновление (каждые 5 сек)', 'info');
        this.pollingInterval = setInterval(() => {
            this.getGameState();
        }, 5000);

        // Initial fetch
        this.getGameState();
    }

    stopPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
            this.log('Автообновление остановлено', 'info');
        }
    }
}

// Initialize client when page loads
let client;
$(document).ready(() => {
    client = new PrefHubClient();
});
