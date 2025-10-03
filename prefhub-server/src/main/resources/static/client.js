class PrefHubClient {
    constructor() {
        this.baseUrl = window.location.origin;
        this.authToken = localStorage.getItem('authToken')?.trim() || null;
        this.currentGameId = localStorage.getItem('currentGameId')?.trim() || null;
        this.currentUsername = localStorage.getItem('username')?.trim() || null;
        this.pollingInterval = null;
        this.currentRules = null;
        this.availableRules = null;

        this.init();
    }

    async init() {
        this.log('Клиент инициализирован', 'info');

        if (this.authToken) {
            this.showMainSection();
            await this.loadAvailableRules();
            if (this.currentGameId) {
                $('#game-id').val(this.currentGameId);
                this.startPolling();
            }
        } else {
            this.showAuthSection();
        }
    }

    async loadAvailableRules() {
        try {
            this.availableRules = await this.apiCall('/api/rules/list', 'GET', null, false);
            this.displayAvailableRules();
        } catch (error) {
            this.log(`Не удалось загрузить правила: ${error.message}`, 'warning');
        }
    }

    displayAvailableRules() {
        if (!this.availableRules) return;

        const select = $('#rule-select');
        select.empty();
        select.append('<option value="">По умолчанию (Ленинградка)</option>');

        for (const [ruleId, description] of Object.entries(this.availableRules)) {
            select.append(`<option value="${ruleId}">${description}</option>`);
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
                const cleanToken = this.authToken.trim().replace(/[\x00-\x1F\x80-\xFF]/g, '');
                options.headers['Authorization'] = `Bearer ${cleanToken}`;
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
            await this.loadAvailableRules();
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
        const ruleId = $('#rule-select').val();

        if (!gameId) {
            this.log('Введите ID игры', 'warning');
            return;
        }

        try {
            const data = await this.apiCall('/api/games/create', 'POST', { gameId, ruleId });
            this.currentGameId = gameId;
            localStorage.setItem('currentGameId', gameId);

            this.log(`Игра "${gameId}" создана с правилами: ${ruleId || 'default'}`, 'success');
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

    async displayGameState(state) {
        $('#game-state').show();

        // Store rules
        if (state.rules) {
            this.currentRules = state.rules;
        }

        let html = '';

        // Basic info
        html += `<p><strong>Игра:</strong> ${state.gameId || 'N/A'}</p>`;
        html += `<p><strong>Раунд:</strong> ${state.roundNumber || 0}</p>`;
        html += `<p><strong>Фаза:</strong> <span class="game-phase">${state.phase || 'N/A'}</span></p>`;

        // Display rules info
        if (this.currentRules) {
            html += `<p><strong>Правила:</strong> ${this.currentRules.name}</p>`;
            html += `<details><summary>Подробнее о правилах</summary>`;
            html += `<div class="rules-details">`;
            html += `<p>${this.currentRules.description}</p>`;
            html += `<p>Выход из распасов: ${this.formatMizerExit(this.currentRules.mizerExitType)}</p>`;
            html += `<p>6 пик: ${this.currentRules.mandatory6SpadesWhist ? 'обязательный вист' : 'не обязательный'}</p>`;
            html += `<p>Пол-вист: ${this.currentRules.allowHalfWhist ? 'разрешён' : 'запрещён'}</p>`;
            html += `<p>Десятерная: ${this.currentRules.tenGameMode === 'CHECKED' ? 'проверяется' : 'вистуется'}</p>`;
            html += `<p>Вист: ${this.currentRules.whistType === 'OPEN' ? 'открытый' : 'закрытый'}</p>`;
            if (this.currentRules.poolEnabled) {
                html += `<p>Пуля: включена (размер: ${this.currentRules.poolSize})</p>`;
            }
            html += `</div></details>`;
        }

        if (state.nextActionDescription) {
            html += `<p><strong>Следующее действие:</strong> ${state.nextActionDescription}</p>`;
        }

        const yourTurn = state.yourTurn || state.isYourTurn;
        if (yourTurn) {
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
        await this.updateActionControls(state);
    }

    async updateActionControls(state) {
        $('#bid-controls').hide();
        $('#play-controls').hide();
        $('#next-round-controls').hide();
        $('#widow-exchange-controls').hide();

        const yourTurn = state.yourTurn || state.isYourTurn;
        if (!yourTurn) {
            return;
        }

        if (state.phase === 'BIDDING') {
            await this.renderBiddingControls();
            $('#bid-controls').show();
        } else if (state.phase === 'WIDOW_EXCHANGE') {
            this.renderWidowExchange(state.hand || [], state.widow || []);
            $('#widow-exchange-controls').show();
        } else if (state.phase === 'PLAYING') {
            this.renderHandCards(state.hand || []);
            $('#play-controls').show();
        } else if (state.phase === 'ROUND_ENDED') {
            $('#next-round-controls').show();
        }
    }

    async renderBiddingControls() {
        try {
            const availableBids = await this.apiCall(`/api/games/available-bids?gameId=${this.currentGameId}`, 'GET');
            const container = $('#bid-buttons');
            container.empty();

            if (availableBids && availableBids.length > 0) {
                availableBids.forEach(bid => {
                    const button = $('<button>')
                        .addClass('bid-button')
                        .text(bid.displayName || bid)
                        .click(() => this.placeBid(bid.contract || bid));
                    container.append(button);
                });
            }

            // Always show PASS button
            const passButton = $('<button>')
                .addClass('bid-button pass-button')
                .text('ПАС')
                .click(() => this.placeBid('PASS'));
            container.append(passButton);
        } catch (error) {
            this.log(`Ошибка загрузки заявок: ${error.message}`, 'error');
        }
    }

    renderWidowExchange(hand, widow) {
        const container = $('#widow-exchange');
        container.empty();

        const selectedCards = [];

        container.append('<h3>Прикуп:</h3>');
        const widowDiv = $('<div>').addClass('widow-cards');
        widow.forEach(card => {
            const cardSpan = $('<span>')
                .addClass('card-display')
                .text(this.formatCard(card));
            widowDiv.append(cardSpan);
        });
        container.append(widowDiv);

        container.append('<h3>Выберите 2 карты для сброса:</h3>');
        const handDiv = $('<div>').addClass('exchange-hand');

        const allCards = [...hand, ...widow];
        allCards.forEach(card => {
            const button = $('<button>')
                .addClass('card-button')
                .text(this.formatCard(card))
                .click(function() {
                    const cardStr = `${card.rank}_${card.suit}`;
                    const index = selectedCards.indexOf(cardStr);
                    if (index > -1) {
                        selectedCards.splice(index, 1);
                        $(this).removeClass('selected');
                    } else if (selectedCards.length < 2) {
                        selectedCards.push(cardStr);
                        $(this).addClass('selected');
                    }
                    exchangeButton.prop('disabled', selectedCards.length !== 2);
                });
            handDiv.append(button);
        });
        container.append(handDiv);

        const exchangeButton = $('<button>')
            .text('Сбросить')
            .prop('disabled', true)
            .click(() => this.exchangeWidow(selectedCards));
        container.append(exchangeButton);
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

    async placeBid(contract) {
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

    async exchangeWidow(cards) {
        try {
            await this.apiCall('/api/games/exchange', 'POST', {
                gameId: this.currentGameId,
                cards: cards
            });
            this.log(`Сброс выполнен`, 'success');
            await this.getGameState();
        } catch (error) {
            this.log(`Ошибка сброса: ${error.message}`, 'error');
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

    formatMizerExit(type) {
        switch (type) {
            case 'FLAT_6': return '6-6-6-6...';
            case 'ESCALATING_678': return '6-7-8-8-8...';
            case 'ESCALATING_677': return '6-7-7-7-7...';
            default: return type;
        }
    }
}

// Initialize client when page loads
let client;
$(document).ready(() => {
    client = new PrefHubClient();
});
