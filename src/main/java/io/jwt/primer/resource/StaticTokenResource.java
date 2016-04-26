/*
 * Copyright 2016 Phaneesh Nagaraja <phaneesh.n@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jwt.primer.resource;

import com.codahale.metrics.annotation.Metered;
import com.github.toastshaman.dropwizard.auth.jwt.hmac.HmacSHA512Signer;
import com.google.common.base.Charsets;
import io.jwt.primer.command.DisableStaticCommand;
import io.jwt.primer.command.GenerateStaticCommand;
import io.jwt.primer.command.VerifyStaticCommand;
import io.jwt.primer.config.AerospikeConfig;
import io.jwt.primer.exception.PrimerException;
import io.jwt.primer.model.PrimerError;
import io.jwt.primer.model.StaticTokenResponse;
import io.jwt.primer.model.VerifyStaticResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author phaneesh
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@Singleton
@Api(value = "Static Token Management API for Developer & Merchant API tokens")
public class StaticTokenResource {

    private final HmacSHA512Signer signer;

    private final AerospikeConfig aerospikeConfig;

    public StaticTokenResource(final String privateKey, final AerospikeConfig aerospikeConfig) {
        this.aerospikeConfig = aerospikeConfig;
        this.signer = new HmacSHA512Signer(privateKey.getBytes(Charsets.UTF_8));
    }

    @POST
    @Path("/v1/generate/static/{app}/{id}/{role}")
    @ApiOperation(value = "Generate a static JWT token for given user")
    @ApiResponses({
            @ApiResponse(code = 200, response = StaticTokenResponse.class, message = "Success"),
            @ApiResponse(code = 500, response = PrimerError.class, message = "Error"),
    })
    @Metered
    public StaticTokenResponse generate(@PathParam("app") String app, @PathParam("id") String id, @PathParam("role") String role) throws PrimerException {
        try {
            GenerateStaticCommand generateStaticCommand = new GenerateStaticCommand(signer, aerospikeConfig, id, app, role);
            return generateStaticCommand.queue().get();
        } catch (Exception e) {
            log.error("Error generating token", e);
            throw new PrimerException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "PR000", e.getMessage());
        }
    }

    @POST
    @Path("/v1/disable/static/{app}/{id}")
    @ApiOperation(value = "Disable a static JWT token for given user")
    @ApiResponses({
            @ApiResponse(code = 200, response = StaticTokenResponse.class, message = "Success"),
            @ApiResponse(code = 500, response = PrimerError.class, message = "Error"),
    })
    @Metered
    public StaticTokenResponse disable(@PathParam("id") String id,
                                        @PathParam("app") String app) throws PrimerException {
        try {
            DisableStaticCommand disableCommand = new DisableStaticCommand(aerospikeConfig, app, id);
            return disableCommand.queue().get();
        } catch (Exception e) {
            log.error("Error disabling token", e);
            if(ExceptionUtils.getRootCause(e) instanceof PrimerException) {
                throw (PrimerException)ExceptionUtils.getRootCause(e);
            }
            throw new PrimerException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "PR000", e.getMessage());
        }
    }

    @POST
    @Path("/v1/verify/static/{app}/{id}/{role}")
    @ApiOperation(value = "Verify the static token for a given user")
    @ApiResponses({
            @ApiResponse(code = 200, response = VerifyStaticResponse.class, message = "Success"),
            @ApiResponse(code = 401, response = PrimerError.class, message = "Unauthorized"),
            @ApiResponse(code = 404, response = PrimerError.class, message = "Not Found"),
            @ApiResponse(code = 403, response = PrimerError.class, message = "Forbidden"),
            @ApiResponse(code = 500, response = PrimerError.class, message = "Error")
    })
    @Metered
    public VerifyStaticResponse verify(@HeaderParam("X-Auth-Token") String token, @PathParam("app") String app,
                                 @PathParam("id") String id, @PathParam("role") String role) throws PrimerException {
        try {
            VerifyStaticCommand verifyCommand = new VerifyStaticCommand(aerospikeConfig, token, id, app, role);
            return verifyCommand.queue().get();
        } catch (Exception e) {
            log.error("Error verifying token", e);
            if(ExceptionUtils.getRootCause(e) instanceof PrimerException) {
                throw (PrimerException)ExceptionUtils.getRootCause(e);
            }
            throw new PrimerException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "PR001", "Error");
        }
    }

}