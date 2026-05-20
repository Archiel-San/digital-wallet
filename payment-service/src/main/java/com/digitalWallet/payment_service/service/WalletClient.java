package com.digitalWallet.payment_service.service;

import com.digitalWallet.payment_service.dtos.requests.InternalTransactionRequest;
import com.digitalWallet.payment_service.dtos.responses.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletClient {

    private final RestTemplate restTemplate;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    // ── Helper: get current JWT and build Authorization header ───────────────
    private HttpHeaders getAuthHeaders() {
        // Grab the JWT from the current security context
        AbstractOAuth2TokenAuthenticationToken<?> authentication =
                (AbstractOAuth2TokenAuthenticationToken<?>)
                        SecurityContextHolder.getContext().getAuthentication();

        String token = authentication.getToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);  // adds "Authorization: Bearer eyJ..."
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public WalletResponse getWalletByKeycloakId(String keycloakId) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(getAuthHeaders());

            return restTemplate.exchange(
                    walletServiceUrl + "/api/internal/wallets/by-keycloak/" + keycloakId,
                    HttpMethod.GET,
                    entity,
                    WalletResponse.class
            ).getBody();

        } catch (Exception e) {
            log.error("Failed to get wallet for keycloakId={}: {}", keycloakId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Wallet service unavailable");
        }
    }

    public void debit(Long walletId, BigDecimal amount,
                      String description, String referenceId) {
        try {
            HttpEntity<InternalTransactionRequest> entity = new HttpEntity<>(
                    new InternalTransactionRequest(amount, description, referenceId),
                    getAuthHeaders()
            );

            restTemplate.exchange(
                    walletServiceUrl + "/api/internal/wallets/" + walletId + "/debit",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

        } catch (HttpClientErrorException e) {
            log.error("Debit failed for walletId={}: {}", walletId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Debit error for walletId={}: {}", walletId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Wallet service unavailable");
        }
    }

    public void credit(Long walletId, BigDecimal amount,
                       String description, String referenceId) {
        try {
            HttpEntity<InternalTransactionRequest> entity = new HttpEntity<>(
                    new InternalTransactionRequest(amount, description, referenceId),
                    getAuthHeaders()
            );

            restTemplate.exchange(
                    walletServiceUrl + "/api/internal/wallets/" + walletId + "/credit",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

        } catch (Exception e) {
            log.error("Credit error for walletId={}: {}", walletId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Wallet service unavailable");
        }
    }
}