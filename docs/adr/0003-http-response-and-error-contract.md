# ADR-0003: HTTP Response and Error Contract

## Status

Accepted

## Context

QuickCommerce will expose HTTP APIs to consumer, rider, seller, store, warehouse, catalog, and administration clients. These clients need predictable machine-readable result codes and human-readable messages, while HTTP status codes must retain their normal transport meaning.

The response contract must not make the `identity` module or any other business module responsible for a concern that belongs to the HTTP adapter. It must also preserve module-local business exception ownership and remain practical if a module is extracted into a service later.

## Decision

Use two deliberately different HTTP body contracts:

1. Successful JSON business responses use a small application envelope.
2. Error responses use RFC 9457 Problem Details with a stable application error code extension.

The HTTP status code remains authoritative for transport outcome. It will not be duplicated as an application `status` field inside the success envelope.

### Successful JSON response

```json
{
  "code": "AUTH_OTP_SENT",
  "message": "OTP sent to ******5364 via SMS.",
  "data": null,
  "meta": {
    "traceId": "01J00000000000000000000000"
  }
}
```

Rules:

- `code` is a stable, machine-readable, uppercase identifier owned by the producing module.
- `message` is human-readable and must not be used for client decisions or localization keys.
- `data` contains the successful result and may be `null` when the operation has no result body.
- `meta` contains transport metadata such as `traceId` and pagination information; it must not contain another module's entity or persistence model.
- Numeric application statuses such as `status: 12` are prohibited because their meaning is opaque and they duplicate HTTP semantics.
- The envelope is used for JSON business responses, not as a universal wrapper for every HTTP response.

### Error response

Errors use `application/problem+json`:

```json
{
  "type": "https://api.minutemart.com/problems/auth-otp-expired",
  "title": "OTP verification failed",
  "status": 400,
  "detail": "The OTP has expired.",
  "code": "AUTH_OTP_EXPIRED",
  "traceId": "01J00000000000000000000000"
}
```

Rules:

- `status` appears in Problem Details because it is part of the standard error representation; it is still consistent with the HTTP status line.
- `code` is the stable machine-readable application error code.
- `detail` is safe for the client and must not expose secrets, SQL, stack traces, provider credentials, or internal topology.
- Validation failures may add an `errors` extension containing field-level details.
- Authentication, authorization, validation, domain, infrastructure, and unexpected errors must map to appropriate HTTP statuses without leaking internal exception types.

### HTTP responses without an envelope

The following responses intentionally have no success envelope:

- `204 No Content`
- File and image downloads
- Streaming or server-sent events
- Health and readiness endpoints
- Provider-specific webhook acknowledgements where the provider contract requires another body

## Ownership and module boundaries

The response envelope is an HTTP transport convention, not a business capability. It does not belong to `identity`, `catalog`, `ordering`, or another business module.

- Each module's `web` adapter maps its own use-case result into the success envelope.
- Each module's `web` adapter maps its own business exceptions into Problem Details and owns the corresponding error codes.
- Generic framework failures may be handled by an application-level HTTP fallback, but that fallback must not know module-specific business exceptions.
- No global business exception hierarchy or shared business error package is allowed.
- The wire schema is shared by documented convention; module implementation DTOs do not need to be shared Java types.

Example ownership:

```text
identity/web     -> AUTH_OTP_SENT, AUTH_OTP_EXPIRED
inventory/web    -> INVENTORY_STOCK_UNAVAILABLE
ordering/web     -> ORDER_CANNOT_BE_CANCELLED
delivery/web     -> DELIVERY_RIDER_NOT_ASSIGNED
```

Business exceptions remain inside their owning module. A consumer of an in-process module contract must receive a contract result or a translated failure, never another module's domain exception, entity, or repository type.

## Initial HTTP status guidance

The exact status is selected by the owning web adapter, but the baseline is:

| Situation | HTTP status |
| --- | --- |
| Successful read or command with a response | `200 OK` |
| Resource created | `201 Created` |
| Accepted for asynchronous processing | `202 Accepted` |
| Successful command with no body | `204 No Content` |
| Malformed request or invalid transport input | `400 Bad Request` |
| Missing or invalid authentication | `401 Unauthorized` |
| Authenticated but not permitted | `403 Forbidden` |
| Resource not found | `404 Not Found` |
| State or business conflict | `409 Conflict` |
| Rate limit exceeded | `429 Too Many Requests` |
| Unexpected server failure | `500 Internal Server Error` |

## Alternatives considered

### One envelope for every response

Rejected because it wraps protocol-specific responses such as `204`, downloads, streams, health checks, and webhooks without adding useful meaning.

### One custom envelope for both success and errors

Rejected in favor of RFC 9457 Problem Details. Standard error semantics improve interoperability with Spring clients, API tooling, gateways, and future extracted services while still allowing stable application `code` and `traceId` extensions.

### Numeric application status codes

Rejected because numeric values such as `12` are difficult to discover, document, evolve, and debug across independently owned modules.

### Shared `common` or `identity` response package

Rejected because the envelope is not identity behavior and a global business package would become a coupling and dumping ground. The wire contract is documented centrally, while web adapters keep implementation ownership locally.

## Consequences

- Clients get predictable success data and stable machine-readable codes.
- HTTP semantics remain usable by browsers, gateways, monitoring, and service clients.
- Module boundaries remain intact; identity does not become the owner of all API behavior.
- Error handling remains compatible with module-local exception ownership and future service extraction.
- Each module must keep its error codes stable and document breaking changes.
- Repeating a tiny response mapping type in module web adapters is acceptable to avoid a shared technical dependency; consistency is enforced by contract review and API documentation.

## Enforcement

- New JSON business endpoints follow the success envelope described here.
- New error handlers produce `application/problem+json` and a stable `code`.
- Module web tests verify HTTP status, response code, and error representation.
- Business exceptions remain package-private or module-local unless deliberately exposed as a named module contract.
- This ADR is the source of truth for the response shape; future extraction must preserve the wire contract or explicitly version it.
