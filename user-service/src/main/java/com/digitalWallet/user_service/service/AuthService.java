package com.digitalWallet.user_service.service;

import com.digitalWallet.user_service.dtos.requests.LoginRequest;
import com.digitalWallet.user_service.dtos.requests.RefreshTokenRequest;
import com.digitalWallet.user_service.dtos.requests.RegisterRequest;
import com.digitalWallet.user_service.dtos.responses.LoginResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Autowired
    public Keycloak keycloakAdminClient;

    @Value("${kc.server-url}")
    private String serverUrl;

    @Value("${kc.realm}")
    private String realm;

    @Value("${kc.client-id}")
    private String clientId;

    @Value("${kc.client-secret}")
    private String clientSecret;


    // ── Register: create user in Keycloak only ───────────────────────────────
    // DB sync happens on first login via /api/users/me
    public void register(RegisterRequest request){
        RealmResource realmResource = keycloakAdminClient.realm(realm);
        /// Check if email already exists in Keycloak
        List<UserRepresentation> existing = realmResource.users().searchByEmail(request.email(), true);

        if (!existing.isEmpty()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email Ja Em Uso");
        }

        /// Build Keycloak user
        UserRepresentation user = getUserRepresentation(request);

        Response response = realmResource.users().create(user);

        if(response.getStatus() != 201){
            log.error("Keycloak user Creation failed: {}", response.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to Create User"
            );
        }

        String keycloakUserId = extractCreatedId(response);
        assignRole(realmResource, keycloakUserId, "ROLE_USER");

        log.info("User Registered in Keycloak {}", keycloakUserId);

    }

    private static UserRepresentation getUserRepresentation(RegisterRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.email());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        //gostei, keycloak simplifica
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));
        return user;
    }

    public LoginResponse login(LoginRequest loginRequest){
        try {
            Keycloak userClient = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.PASSWORD)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(loginRequest.email())
                    .password(loginRequest.password())
                    .build();


            AccessTokenResponse tokenResponse = userClient.tokenManager().getAccessToken();

            return new LoginResponse(tokenResponse.getToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn());
        }
        catch (Exception e){
            log.warn("Login Failed for {}: {}", loginRequest.email(), e.getLocalizedMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Credentials");
        }
    }

    private String extractCreatedId(Response response){
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf("/")+1);
    }

    private void assignRole(RealmResource realm, String userId, String roleName){
        try {
            RoleRepresentation role = realm.roles().get(roleName).toRepresentation();
            realm.users().get(userId).roles().realmLevel().add(List.of(role));
        }
        catch (Exception e){
            log.warn("Could not assign Role {}: {}", roleName, e.getLocalizedMessage());
            // Don't fail registration just because role assignment failed
        }
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        try {
            // Use RestTemplate — simplest and most reliable for form posts
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", request.refreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    serverUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                    entity,
                    JsonNode.class
            );

            JsonNode json = response.getBody();

            if (json == null || json.get("access_token") == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            return new LoginResponse(
                    json.get("access_token").asText(),
                    json.get("refresh_token").asText(),
                    json.get("expires_in").asLong()
            );

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Refresh failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }



}
