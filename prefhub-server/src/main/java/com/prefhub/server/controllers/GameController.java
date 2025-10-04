package com.prefhub.server.controllers;

import com.prefhub.core.model.Card;
import com.prefhub.core.model.Contract;
import com.prefhub.core.model.PlayerView;
import com.google.inject.Inject;
import com.prefhub.server.game.GameService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * Game management controller using JAX-RS.
 * Handles game creation, joining, and state management.
 */
@Path("/games")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GameController {
    private final GameService gameService;

    @Inject
    public GameController(final GameService gameService) {
        this.gameService = gameService;
    }

    @POST
    @Path("/create")
    public PlayerView createGame(CreateGameRequest request, @Context ContainerRequestContext requestContext) {
        final String username = (String) requestContext.getProperty("username");
        gameService.createGame(request.gameId(), request.ruleId());
        gameService.joinGame(request.gameId(), username);
        return gameService.getPlayerView(request.gameId(), username);
    }

    @POST
    @Path("/join")
    public PlayerView joinGame(JoinGameRequest request, @Context ContainerRequestContext requestContext) {
        final String username = (String) requestContext.getProperty("username");
        gameService.joinGame(request.gameId(), username);
        return gameService.getPlayerView(request.gameId(), username);
    }

    @GET
    @Path("/list")
    public Object listGames() {
        return gameService.getAllGames();
    }

    @GET
    @Path("/state")
    public PlayerView getGameState(@QueryParam("gameId") String gameId, @Context ContainerRequestContext requestContext) {
        final String username = (String) requestContext.getProperty("username");
        return gameService.getPlayerView(gameId, username);
    }

    @POST
    @Path("/bid")
    public PlayerView placeBid(BidRequest request, @Context ContainerRequestContext requestContext) {
        final String username = (String) requestContext.getProperty("username");
        gameService.placeBid(request.gameId(), username, Contract.valueOf(request.contract()));
        return gameService.getPlayerView(request.gameId(), username);
    }

    @GET
    @Path("/available-bids")
    public Object getAvailableBids(@QueryParam("gameId") String gameId) {
        return gameService.getAvailableBids(gameId);
    }

    @POST
    @Path("/exchange")
    public PlayerView exchangeWidow(ExchangeRequest request, @Context ContainerRequestContext requestContext) {
        final String username = (String) requestContext.getProperty("username");
        gameService.exchangeWidow(request.gameId(), username, request.cards());
        return gameService.getPlayerView(request.gameId(), username);
    }

    @POST
    @Path("/play")
    public PlayerView playCard(PlayCardRequest request, @Context ContainerRequestContext requestContext) {
        final String username = (String) requestContext.getProperty("username");
        gameService.playCard(request.gameId(), username, request.card());
        return gameService.getPlayerView(request.gameId(), username);
    }

    @POST
    @Path("/next-round")
    public PlayerView nextRound(NextRoundRequest request, @Context ContainerRequestContext requestContext) {
        final String username = (String) requestContext.getProperty("username");
        gameService.startNextRound(request.gameId());
        return gameService.getPlayerView(request.gameId(), username);
    }

    // Request DTOs
    public record CreateGameRequest(String gameId, String ruleId) {}
    public record JoinGameRequest(String gameId) {}
    public record BidRequest(String gameId, String contract) {}
    public record ExchangeRequest(String gameId, List<Card> cards) {}
    public record PlayCardRequest(String gameId, Card card) {}
    public record NextRoundRequest(String gameId) {}
}
