# Research: Harden the inputs

## Strict reading of the document

**Decision**: Enable in `application.yml` the reading options that Spring Boot 4.1.2 already exposes over Jackson 3.1.7:

- `spring.jackson.deserialization.fail-on-unknown-properties: true`
- `spring.jackson.deserialization.fail-on-trailing-tokens: true`
- `spring.jackson.read.strict-duplicate-detection: true`
- `spring.jackson.datatype.enum.fail-on-numbers-for-enums: true`

Boot's standard customizer turns `FAIL_ON_UNKNOWN_PROPERTIES` off. It has to be turned back on. Repeated keys come from `StreamReadFeature.STRICT_DUPLICATE_DETECTION`, in the `spring.jackson.read` map.

Coercions have no property. A `JsonMapperBuilderCustomizer` (`org.springframework.boot.jackson.autoconfigure`) sets `CoercionAction.Fail` when a scalar arrives in another shape: text where a number goes, a number where text goes, a number where a side or a type goes, and a boolean, array or object in any of those fields. An empty body, an array at the root and a content type that is not a data document get the same rejection as an unreadable request.

**Rationale**: Bean Validation does not see a repeated key, an unknown field or a number that does not fit an integer: the reader has already failed or already picked a value. These flags exist in the project jars (`spring-boot-jackson` 4.1.2 and `jackson-databind` 3.1.7) and add no dependency.

**Alternatives considered**: Validation annotations on `PlaceOrderRequest`. They do not reject the extra field or the repeated key. Reading the body as a tree and validating it by hand duplicates the reader the adapter already has.

## Stable text, without the value sent

**Decision**: `ApiExceptionHandler` stops returning `exception.getMessage()` for read failures, content type failures and malformed arguments. The body is still `ApiError`, with HTTP 400, except an unsupported content type, which answers 415 (revised on 2026-11-13).

- Unreadable request, empty, not an object, or with trailing tokens: `The request cannot be read.`.
- Content type other than `application/json`: 415, code `UNSUPPORTED_MEDIA_TYPE` and an `Accept` header with the types the operation reads.
- A field the operation does not define: `The request contains a field that is not allowed.`. That key is not named, because the caller picks the name and it may be the hostile text itself.
- One of our fields repeated or wrongly typed: `The size field is duplicated.` or `The size field is not valid.`, and the same for ticker, side, type, amount and price. The name is the model's, not the value.
- Missing account, instrument or order: `Account not found.`, `Instrument not found.`, `Order not found.`. Before this feature those texts concatenated the value sent.

If the reader does not point to one of our fields, the unreadable-request text is used. Jackson's message is never copied: it includes the fragment read and the internal path.

**Rationale**: The spec asks for a stable text, the field name when it is one of ours, and nothing of the value or the internal diagnosis. The `{ "message" }` envelope is already the one of feature 001.

**Alternatives considered**: Problem Details. It changes the body the tests and the generated contract already consume. Returning Jackson's message and trimming it is fragile: the reader's text is not a contract.

## Format before the account

**Decision**: Account, ticker, order id and search are parsed in `InputText`, in the domain, and throw `InvalidRequestException` with a fixed text. Controllers receive the path and the search as text. The order id is no longer declared as `Long`, so that the framework does not convert a huge number and no other order is queried.

Rules:

- Account: 1 to 20 digits, the whole text. It is not trimmed. `10001` is still the reference account. Leading zeros are kept, because they make a different string.
- Ticker: 1 to 10 letters or digits, the whole text, untrimmed and with its case unchanged. `PAMP` is looked up as today. `pamp` has a valid format and, if there is no such instrument, answers that it does not exist, without repeating the text.
- Order: an integer from 1 to 2147483647. Zero, negative, text, spaces and a larger number are rejected with `The order is not valid.`. Leading zeros are accepted if the numeric value is in range (`0001` is order 1).
- Search: only the ends are trimmed. After that, 1 to 100 Unicode characters, without ISO control characters and without the null character. Empty: `The search has no text.`. Too long: `The search exceeds the maximum length.`. With a control: `The search is not valid.`.

The search comparison is still in memory, over the catalogue already loaded, with `Locale.ROOT`. The text is not concatenated into a query. That way a text that looks like an instruction is not executed and does not depend on the machine locale.

**Rationale**: The spec requires validating before reading or storing, and never picking a similar record. Parsing the id as `Long` lets through values the order integer cannot store and, when conversion fails, the default message includes the path text.

**Alternatives considered**: Regular expressions only in the controller. The use case would be unprotected if another adapter called it. Rejecting the search when it has signs: the reference names use punctuation, and the spec allows it.

## Amount, price and size

**Decision**: Amount and price are read as text, not as a bare number, as today. If the text is not a decimal number, the field is not valid. If it is, it is first rounded half-up to 2 decimals and then required to be greater than zero with at most 8 integer digits. `99999999.994` becomes `99999999.99` and is accepted. `99999999.995` becomes `100000000.00` and is rejected for the maximum. A zero result after rounding is rejected for not being greater than zero.

`MoneyMath.sharesFromAmount` stops using `intValue()`. It computes the integer quotient and, if it is greater than 2147483647, throws an input rejection. The use case does not store that order. Quotient 0 is still the existing business rejection: it is stored `REJECTED`. A missing, zero or negative limit price is still rejected before storing. A side other than `BUY` or `SELL`, a type other than `MARKET` or `LIMIT`, and both forms of quantity or neither, are still rejected.

The domain texts, without the value sent:

- `Size must be an integer from 1 to 2147483647.`
- `Amount must be greater than zero.`
- `Amount exceeds the maximum allowed.`
- `Price is required and must be greater than zero.`
- `Price exceeds the maximum allowed.`
- `Exactly one quantity is required: shares or amount.`
- `Side must be BUY or SELL.`
- `Order type is required.`

**Rationale**: `NUMERIC(10, 2)` stores 8 integer digits and 2 decimals. Rounding before counting the integer digits validates the number that would be stored. `intValue()` keeps the low 32 bits and may persist a small or negative size; the spec forbids it.

**Alternatives considered**: Rejecting every amount with more than 2 decimals. The spec says those decimals are rounded. Raising the cap to 12 integer digits. The current column does not store them, and this feature does not change the schema.

## Contract and tests

**Decision**: No OpenAPI is written in `specs`. The document is still the one springdoc generates at startup. [contracts/inputs.md](contracts/inputs.md) fixes the observable texts and rejections. Domain and service tests come first and must fail before implementation. `HardenInputsIT` covers the HTTP document. The existing scenarios for `10001`, `PAMP` and a valid order are not loosened.

**Rationale**: Feature 001 forbids a hand-written copy of the contract. The error texts are part of what is observable and today they are not in that generated document in a stable way, because they come from exceptions.

**Alternatives considered**: Annotating the error schema so springdoc publishes each message. It publishes type names, not the catalogue of phrases. It does not replace the tests.
