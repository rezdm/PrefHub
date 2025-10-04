package com.prefhub.server.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.prefhub.server.auth.AuthService;
import com.prefhub.server.controllers.AuthController;
import com.prefhub.server.controllers.GameController;
import com.prefhub.server.controllers.RulesController;
import com.prefhub.server.game.GameService;
import com.prefhub.server.game.RulesLoader;
import com.prefhub.server.repository.GameRepository;
import com.prefhub.server.repository.SessionRepository;
import com.prefhub.server.repository.UserRepository;
import com.prefhub.server.repository.impl.FileGameRepository;
import com.prefhub.server.repository.impl.FileSessionRepository;
import com.prefhub.server.repository.impl.FileUserRepository;

public class ServerModule extends AbstractModule {
    private final String storageDirectory;

    public ServerModule(final String storageDirectory) {
        this.storageDirectory = storageDirectory;
    }

    @Override
    protected void configure() {
        // Controllers - new instance per request (not singleton)
        bind(AuthController.class);
        bind(GameController.class);
        bind(RulesController.class);

        // Filters
        bind(com.prefhub.server.web.auth.AuthenticationFilter.class);

        // Services - singletons
        bind(AuthService.class).in(Singleton.class);
        bind(GameService.class).in(Singleton.class);
        bind(RulesLoader.class).in(Singleton.class);

        // Repositories - singletons
        bind(UserRepository.class).to(FileUserRepository.class).in(Singleton.class);
        bind(GameRepository.class).to(FileGameRepository.class).in(Singleton.class);
        bind(SessionRepository.class).to(FileSessionRepository.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

    @Provides
    @Singleton
    public FileUserRepository provideFileUserRepository() {
        return new FileUserRepository(storageDirectory);
    }

    @Provides
    @Singleton
    public FileGameRepository provideFileGameRepository() {
        return new FileGameRepository(storageDirectory);
    }

    @Provides
    @Singleton
    public FileSessionRepository provideFileSessionRepository() {
        return new FileSessionRepository(storageDirectory);
    }
}
