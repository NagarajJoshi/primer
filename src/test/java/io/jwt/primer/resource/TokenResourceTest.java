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

import io.dropwizard.testing.junit.ResourceTestRule;
import io.jwt.primer.BaseTest;
import io.jwt.primer.model.RefreshResponse;
import io.jwt.primer.model.ServiceUser;
import io.jwt.primer.model.TokenResponse;
import io.jwt.primer.model.VerifyResponse;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;

import static org.junit.Assert.assertNotNull;

/**
 * @author phaneesh
 */
public class TokenResourceTest extends BaseTest {

    @ClassRule
    public static ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(tokenResource)
            .build();

    @Test
    public void testGenerateToken() {
        Entity<ServiceUser> serviceUserEntity = Entity.json(
                ServiceUser.builder()
                        .id("test")
                        .name("test")
                        .role("user")
                .build());
        TokenResponse result = resources.client().target("/v1/generate/test").request()
                .header("X-User-Id", "test")
                .post(serviceUserEntity, TokenResponse.class);
        assertNotNull(result.getToken());
        assertNotNull(result.getRefreshToken());
    }

    @Test
    public void testVerifyToken() {
        Entity<ServiceUser> serviceUserEntity = Entity.json(
                ServiceUser.builder()
                        .id("test")
                        .name("test")
                        .role("user")
                        .build());
        TokenResponse result = resources.client().target("/v1/generate/test").request()
                .header("X-User-Id", "test")
                .post(serviceUserEntity, TokenResponse.class);
        assertNotNull(result.getToken());
        assertNotNull(result.getRefreshToken());
        VerifyResponse verifyResponse = resources.client().target("/v1/verify/test/test").request()
                .header("X-Auth-Token", result.getToken())
                .post(serviceUserEntity, VerifyResponse.class);
        assertNotNull(verifyResponse.getToken());
    }


    @Test
    public void testRefreshToken() {
        Entity<ServiceUser> serviceUserEntity = Entity.json(
                ServiceUser.builder()
                        .id("test1")
                        .name("test1")
                        .role("user")
                        .build());
        TokenResponse result = resources.client().target("/v1/generate/test").request()
                .header("X-User-Id", "test1")
                .post(serviceUserEntity, TokenResponse.class);
        assertNotNull(result.getToken());
        assertNotNull(result.getRefreshToken());
        RefreshResponse refreshResponse = resources.client().target("/v1/refresh/test/test1").request()
                .header("X-Auth-Token", result.getToken())
                .header("X-Refresh-Token", result.getRefreshToken())
                .post(null, RefreshResponse.class);
        assertNotNull(refreshResponse.getToken());
        assertNotNull(refreshResponse.getRefreshToken());
    }


}
