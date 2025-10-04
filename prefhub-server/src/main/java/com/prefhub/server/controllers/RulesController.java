package com.prefhub.server.controllers;

import com.google.inject.Inject;
import com.prefhub.server.game.RulesLoader;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

/**
 * Rules management controller using JAX-RS.
 */
@Path("/api/rules")
@Produces(MediaType.APPLICATION_JSON)
public class RulesController {
    private final RulesLoader rulesLoader;

    @Inject
    public RulesController(final RulesLoader rulesLoader) {
        this.rulesLoader = rulesLoader;
    }

    @GET
    @Path("/list")
    public Map<String, String> listRules() {
        return rulesLoader.getAvailableRulesList();
    }
}
