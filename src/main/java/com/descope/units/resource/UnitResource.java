package com.descope.units.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.descope.units.dto.CreateUnitRequest;
import com.descope.units.dto.UnitResponse;
import com.descope.units.dto.UpdateUnitRequest;
import com.descope.units.model.Unit;
import com.descope.units.service.UnitService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST resource for unit management operations.
 *
 * <p>This resource provides endpoints for creating, retrieving, updating, and deleting units.
 */
@Path("/units")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UnitResource {

  private static final Logger logger = LoggerFactory.getLogger(UnitResource.class);

  private final UnitService unitService;

  /**
   * Constructs a UnitResource with the specified service.
   *
   * @param unitService the unit service
   */
  @Inject
  public UnitResource(UnitService unitService) {
    this.unitService = unitService;
  }

  /**
   * Creates a new unit.
   *
   * @param request the create unit request
   * @return the created unit response with HTTP 201 status
   */
  @POST
  public Response createUnit(@Valid CreateUnitRequest request) {
    logger.debug("Received request to create unit with name: {}", request.getName());
    Unit unit = unitService.createUnit(request.getName());
    UnitResponse response = UnitResponse.fromDomain(unit);
    logger.info("Successfully created unit with id: {}", response.getId());
    return Response.status(Response.Status.CREATED).entity(response).build();
  }

  /**
   * Retrieves a unit by its identifier.
   *
   * @param id the unit identifier
   * @return the unit response with HTTP 200 status
   */
  @GET
  @Path("/{id}")
  public Response getUnit(@PathParam("id") String id) {
    logger.debug("Received request to get unit with id: {}", id);
    Unit unit = unitService.getUnitById(id);
    UnitResponse response = UnitResponse.fromDomain(unit);
    logger.debug("Successfully retrieved unit with id: {}", id);
    return Response.ok(response).build();
  }

  /**
   * Updates an existing unit.
   *
   * @param id the unit identifier
   * @param request the update unit request
   * @return the updated unit response with HTTP 200 status
   */
  @PUT
  @Path("/{id}")
  public Response updateUnit(@PathParam("id") String id, @Valid UpdateUnitRequest request) {
    logger.debug("Received request to update unit with id: {}", id);
    Unit unit = unitService.updateUnit(id, request.getName());
    UnitResponse response = UnitResponse.fromDomain(unit);
    logger.info("Successfully updated unit with id: {}", id);
    return Response.ok(response).build();
  }

  /**
   * Deletes a unit by its identifier.
   *
   * @param id the unit identifier
   * @return HTTP 204 No Content status
   */
  @DELETE
  @Path("/{id}")
  public Response deleteUnit(@PathParam("id") String id) {
    logger.debug("Received request to delete unit with id: {}", id);
    unitService.deleteUnit(id);
    logger.info("Successfully deleted unit with id: {}", id);
    return Response.noContent().build();
  }
}
