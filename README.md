# Digital Wallet API

Documentacao dos endpoints principais do backend da Digital Wallet.

Em ambiente Docker, os endpoints devem ser chamados pelo **API Gateway**:

```text
http://localhost:8085
```

Os servicos internos usam Eureka para discovery e nao devem ser chamados diretamente em uso normal.

## Autenticacao

Os endpoints protegidos exigem um JWT no header:

```http
Authorization: Bearer <accessToken>
```

Fluxo recomendado:

1. Registar utilizador em `POST /api/auth/register`
2. Fazer login em `POST /api/auth/login`
3. Usar o `accessToken` retornado nos endpoints protegidos
4. Usar o `refreshToken` em `POST /api/auth/refresh` quando precisar renovar sessao

## Auth

### Registar utilizador

```http
POST /api/auth/register
```

Endpoint publico. Cria o utilizador no Keycloak.

Request:

```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

Validacoes:

- `email`: obrigatorio e deve ser email valido
- `password`: obrigatorio, minimo 8 caracteres
- `firstName`: obrigatorio
- `lastName`: obrigatorio

Resposta de sucesso:

```http
201 Created
```

Possiveis erros:

- `409 Conflict`: email ja existe
- `500 Internal Server Error`: falha ao criar utilizador no Keycloak

### Login

```http
POST /api/auth/login
```

Endpoint publico. Autentica o utilizador no Keycloak e retorna tokens.

Request:

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

Resposta:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 300
}
```

Possiveis erros:

- `401 Unauthorized`: credenciais invalidas

### Renovar token

```http
POST /api/auth/refresh
```

Endpoint publico. Gera um novo `accessToken` usando o `refreshToken`.

Request:

```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

Resposta:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 300
}
```

Possiveis erros:

- `401 Unauthorized`: refresh token invalido ou expirado

## Users

### Obter o utilizador autenticado

```http
GET /api/users/me
```

Endpoint protegido. Sincroniza o utilizador com a base de dados local na primeira chamada, usando os dados do JWT.

Headers:

```http
Authorization: Bearer <accessToken>
```

Resposta:

```json
{
  "id": 1,
  "keycloakId": "uuid-do-utilizador-no-keycloak",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": null,
  "createdAt": "2026-05-19T10:30:00"
}
```

Possiveis erros:

- `401 Unauthorized`: token ausente, invalido ou expirado

## Wallets

### Obter a minha wallet

```http
GET /api/wallets/me
```

Endpoint protegido. Cria a wallet automaticamente na primeira chamada, caso ainda nao exista.

Headers:

```http
Authorization: Bearer <accessToken>
```

Resposta:

```json
{
  "id": 1,
  "keycloakId": "uuid-do-utilizador-no-keycloak",
  "walletStatus": "ACTIVE",
  "balance": 100.00,
  "createdAt": "2026-05-19T10:30:00"
}
```

### Obter o extrato da minha wallet

```http
GET /api/wallets/me/ledger
```

Endpoint protegido. Retorna o historico de movimentos da wallet autenticada.

Headers:

```http
Authorization: Bearer <accessToken>
```

Resposta:

```json
[
  {
    "id": 1,
    "ledgerType": "CREDIT",
    "amount": 100.00,
    "balanceAfter": 100.00,
    "description": "Deposito inicial",
    "createdAt": "2026-05-19T10:35:00"
  }
]
```

### Depositar na minha wallet

```http
POST /api/wallets/me/deposit
```

Endpoint protegido. Usado para testes; em producao, movimentos financeiros devem passar pelo Payment Service.

Headers:

```http
Authorization: Bearer <accessToken>
```

Request:

```json
{
  "amount": 50.00,
  "description": "Deposito de teste"
}
```

Validacoes:

- `amount`: obrigatorio, minimo `0.01`
- `description`: opcional

Resposta:

```json
{
  "id": 1,
  "keycloakId": "uuid-do-utilizador-no-keycloak",
  "walletStatus": "ACTIVE",
  "balance": 150.00,
  "createdAt": "2026-05-19T10:30:00"
}
```

## Payments

### Fazer transferencia

```http
POST /api/payments/transfer
```

Endpoint protegido. Transfere saldo da wallet do utilizador autenticado para outro utilizador.

Headers:

```http
Authorization: Bearer <accessToken>
```

Request:

```json
{
  "receiverKeycloakId": "uuid-do-destinatario-no-keycloak",
  "amount": 25.00,
  "description": "Pagamento de teste"
}
```

Validacoes:

- `receiverKeycloakId`: obrigatorio
- `amount`: obrigatorio, minimo `0.01`
- `description`: opcional

Resposta:

```json
{
  "id": 1,
  "senderKeycloakId": "uuid-do-remetente-no-keycloak",
  "receiverKeycloakId": "uuid-do-destinatario-no-keycloak",
  "amount": 25.00,
  "description": "Pagamento de teste",
  "status": "COMPLETED",
  "failureReason": null,
  "createdAt": "2026-05-19T10:40:00"
}
```

Possiveis erros:

- `401 Unauthorized`: token ausente, invalido ou expirado
- `400 Bad Request`: request invalido
- `500 Internal Server Error`: falha no processamento da transferencia

### Obter historico de pagamentos

```http
GET /api/payments/history
```

Endpoint protegido. Retorna as transacoes onde o utilizador autenticado participou.

Headers:

```http
Authorization: Bearer <accessToken>
```

Resposta:

```json
[
  {
    "id": 1,
    "senderKeycloakId": "uuid-do-remetente-no-keycloak",
    "receiverKeycloakId": "uuid-do-destinatario-no-keycloak",
    "amount": 25.00,
    "description": "Pagamento de teste",
    "status": "COMPLETED",
    "failureReason": null,
    "createdAt": "2026-05-19T10:40:00"
  }
]
```

## Endpoints internos

Estes endpoints pertencem ao Wallet Service e sao usados para comunicacao interna entre servicos.

No gateway, `/api/internal/**` deve permanecer bloqueado para chamadas externas.

### Buscar wallet por Keycloak ID

```http
GET /api/internal/wallets/by-keycloak/{keycloakId}
```

Retorna ou cria a wallet associada ao `keycloakId`.

### Debitar wallet

```http
POST /api/internal/wallets/{walletId}/debit
```

Request:

```json
{
  "amount": 25.00,
  "description": "Debito de transferencia",
  "referenceId": "TRANSFER-123"
}
```

Resposta:

```http
200 OK
```

### Creditar wallet

```http
POST /api/internal/wallets/{walletId}/credit
```

Request:

```json
{
  "amount": 25.00,
  "description": "Credito de transferencia",
  "referenceId": "TRANSFER-123"
}
```

Resposta:

```http
200 OK
```

## Ordem sugerida para testar no Postman

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. Copiar o `accessToken`
4. `GET /api/users/me`
5. `GET /api/wallets/me`
6. `POST /api/wallets/me/deposit`
7. Criar/logar outro utilizador e copiar o `keycloakId`
8. `POST /api/payments/transfer`
9. `GET /api/payments/history`
10. `GET /api/wallets/me/ledger`
