# Feature Specification: Harden the inputs

**Feature Branch**: `feat/hardening-api`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "Harden the operations already delivered with input validation. An unreadable request, or one with an extra, repeated or wrongly typed field, is rejected whole and the response does not quote the text sent or an internal detail. The number of shares goes from 1 to 2147483647; an amount that does not fit that integer is not stored truncated. Amount and price are positive, with up to 8 integer digits and 2 decimals half-up. The account is 1 to 20 digits. The ticker is 1 to 10 letters or digits. The order id is an integer from 1 to 2147483647. The search is trimmed at both ends, allows up to 100 characters, allows no control characters or the null character, and is compared literally without depending on the machine locale. The message names the field, is stable and does not repeat the value sent. There are no sessions, attempt limits or retry key. The rejection for insufficient cash or shares is still stored. Account 10001, the PAMP search and a valid order keep answering as before."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reject a request that cannot be interpreted (Priority: P1)

A person, or an automated client, sends a broken request: it is not a data document, it is empty, it carries a type other than the expected one, it repeats a field or it includes data the operation does not ask for. The operation rejects it, stores nothing and does not return the text received or an internal diagnosis.

**Why this priority**: An unreadable request or one with extra fields is the entry point for junk data. If it is processed, it can store an order different from the intent or show an internal detail.

**Independent Test**: Send a body that cannot be read, an empty body, a number where text goes, text where a size goes, the same field twice and a field the operation does not define. In every case no new order appears and the response does not quote the body sent.

**Acceptance Scenarios**:

1. **Given** a body that is not a valid data document, **When** an order is sent, **Then** nothing is stored and the response reports an unreadable request, without the body received and without an internal detail.
2. **Given** an empty body, a document that is not an object, or a submission that is not a data document, **When** it is sent, **Then** it is rejected like an unreadable request.
3. **Given** a field with a type other than the expected one, such as a size as non-numeric text or a ticker that is not text, **When** it is sent, **Then** the field is named, nothing is stored and the value is not reinterpreted.
4. **Given** a field the operation does not define, **When** it is sent with an otherwise valid request, **Then** the whole request is rejected and the extra field is not ignored.
5. **Given** the same field repeated with two values, **When** it is sent, **Then** it is rejected and neither value is picked.

---

### User Story 2 - Reject numbers that do not fit the model (Priority: P2)

A person sends a size, amount, price or id that is negative, zero when a positive is required, or so large it cannot be stored whole. The operation rejects the whole request. It does not keep the part that fits or turn it into another number.

**Why this priority**: A number that overflows or changes sign can reserve or fill a quantity different from the one requested. That moves extra money or shares.

**Independent Test**: Send a size `-1`, a size `0`, a size greater than `2147483647`, a negative amount or price, an amount or price with 9 integer digits, and an order id `0` or negative. None stores an order or changes the portfolio. A size of `2147483647`, if the rest of the request is valid and the account covers it, is not rejected for its size.

**Acceptance Scenarios**:

1. **Given** a negative size, amount or price, **When** an order is sent, **Then** it is not stored and the response names that field.
2. **Given** a zero size, or a zero or missing price on a limit order, **When** it is sent, **Then** it is not stored.
3. **Given** a size greater than `2147483647`, or an amount or price with more than 8 integer digits, **When** it is sent, **Then** a smaller figure or a number of another sign is not stored.
4. **Given** an amount that, divided by the applicable price, gives more shares than `2147483647`, **When** it is sent, **Then** no order is stored, not even a business-rejected one, with a smaller quantity or another sign.
5. **Given** an order id that is not an integer from `1` to `2147483647`, **When** it is cancelled, **Then** no other order is acted on.
6. **Given** an existing short position, **When** the portfolio is queried, **Then** the reported negative quantity is kept. This feature does not forbid that result.

---

### User Story 3 - Treat text as data, not as an instruction (Priority: P3)

A person sends, in the account, the ticker or the search, a text that looks like an instruction to read, change or delete data. That text is not executed. If it does not match the field format, it is rejected. If it is an allowed search text, it is compared literally and, at most, returns no rows. In no case does it change data a valid request would not change, nor does the response show an internal error.

**Why this priority**: The account, the ticker and the search come from the caller. Without a closed format, a hostile text can alter the query or leak an internal error.

**Independent Test**: Ask for the portfolio of `10001' OR '1'='1`, create an order with ticker `PAMP'; DROP TABLE orders;--` and search `pamp' OR 1=1`. The account and the ticker are rejected. The search modifies no data and returns no internal error. The portfolio of `10001` and the `PAMP` search keep answering as before.

**Acceptance Scenarios**:

1. **Given** an account number with letters, spaces, signs or more than 20 digits, **When** it is used for portfolio, placement or cancellation, **Then** it is rejected and no other account is picked.
2. **Given** a ticker that is empty, has spaces, signs or more than 10 letters or digits, **When** an order is sent, **Then** nothing is stored.
3. **Given** a search text that is empty, longer than 100 characters, with control characters or with a null character, **When** it is searched, **Then** there is no list and no internal error is shown.
4. **Given** a search text of up to 100 characters, made of letters, digits, spaces and the usual punctuation of a name, **When** it is searched, **Then** it is compared as literal text, case-insensitively and without depending on the machine locale. A text that looks like an instruction changes no data and does not widen the result beyond the literal matches.
5. **Given** account `10001` and ticker `PAMP`, **When** they are queried in their valid form, **Then** the result is the one of the previous feature.

---

### Edge Cases

- Trimming spaces does not turn one identifier into another. An account or ticker with leading or trailing spaces is rejected.
- A search text can be trimmed at both ends. After that, the 100-character maximum and the ban on control characters and the null character apply.
- More than 2 decimals on an amount or price is not junk: it is rounded half-up to 2 decimals, like every other amount. This feature does not reject it for the number of decimals.
- `BUY` and `SELL` are the only sides of a new order. `MARKET` and `LIMIT` are the only types. Any other text, including case or space variants, is rejected. A new peso deposit or withdrawal still stores no order.
- A new order still requires exactly one form of quantity: shares or amount. Both, or neither, are rejected.
- When the error belongs to a field, the response names that field, uses the same text for the same case and does not include the value sent. An account, instrument or order that does not exist is reported without repeating the value sent.
- A buy or sell that does not fit for insufficient cash or shares is still stored as rejected. This feature does not turn that case into an input rejection.
- A valid request of the reference account does not start failing because of these rules.
- Whoever knows the account number trades. This feature adds no sessions, permissions or attempt limit.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every input of an operation MUST be validated before reading or storing the state of an account. Invalid input MUST NOT store an order, MUST NOT change balances and MUST NOT pick another similar record.
- **FR-002**: A body that is unreadable, empty, not a data document, or not an object MUST be rejected. The response MUST NOT include the body received or an internal detail.
- **FR-003**: A field with a type other than the expected one, a repeated field or a field the operation does not define MUST reject the whole request. The system MUST NOT ignore the extra field, MUST NOT pick one of the repeated values and MUST NOT turn text into a number or a number into text.
- **FR-004**: When the error belongs to a field, the response MUST name that field. The text MUST be stable for the same case and MUST NOT include the value sent or an internal diagnosis. A missing account, instrument or order MUST be reported without repeating the value sent.
- **FR-005**: A number of shares MUST be an integer from `1` to `2147483647`. An amount or price MUST be greater than zero and MUST have at most 8 integer digits. Extra decimals MUST be rounded half-up to 2. A value that is negative, zero when the field is required, non-numeric or with more than 8 integer digits MUST be rejected whole, without storing part of the number or changing its sign.
- **FR-006**: If an amount, divided by the applicable price, does not fit an integer from `1` to `2147483647`, the request MUST be rejected without storing an order, including a business-rejected order.
- **FR-007**: An order id MUST be an integer from `1` to `2147483647`. Zero, negative, text or any other number outside that range MUST be rejected without acting on a different order.
- **FR-008**: An account number MUST be 1 to 20 digits, with no other characters. An order ticker MUST be 1 to 10 letters or digits, with no spaces or signs. A value outside that format MUST be rejected. Leading or trailing spaces on the account or the ticker MUST be rejected and MUST NOT be trimmed.
- **FR-009**: A search text MUST be trimmed only at both ends, MUST have 1 to 100 characters after that trim and MUST NOT contain control characters or the null character. The accepted text MUST be compared literally, case-insensitively and without depending on the machine locale, and MUST NOT be executed as an instruction. An invalid search or one without matches MUST NOT modify data or return an internal error.
- **FR-010**: A side other than buy or sell, or a type other than market or limit, MUST be rejected. The operation MUST still require exactly one form between number of shares and amount. A new peso deposit or withdrawal MUST still store no order.
- **FR-011**: A query or order that was already valid MUST stay valid. The negative quantity of a short position reported by the portfolio MUST be kept. An order that does not fit for insufficient cash or shares MUST still be stored as rejected.

### Key Entities

- **Input value**: Each value that arrives in an operation: account, order id, search text, ticker, side, type, size, amount and price. It has a closed format or a bounded text. Outside that format it does not reach the account state.
- **Input rejection**: The response to a value that cannot be accepted. It names the field when there is one, does not repeat the value sent and does not change orders or balances.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the requests that are unreadable, wrongly typed, with an extra field or with a repeated field create no order, and the response contains neither the body sent nor an internal detail.
- **SC-002**: 100% of sizes, amounts, prices and ids that are negative, zero when not allowed, or above the maximum leave no new order and do not change the portfolio. No amount that does not fit the integer of shares is stored with a smaller size.
- **SC-003**: 100% of account, ticker and search texts that look like an instruction modify no data and produce no internal error. Out-of-format account and ticker are rejected. An allowed search only returns literal matches.
- **SC-004**: The portfolio of `10001`, the `PAMP` search and a valid order of that account keep completing as before this feature.

## Assumptions

- This feature builds on the behaviour already delivered for portfolio, search and orders. It does not change the scale of amounts: they stay at 2 decimals half-up, and the new 8-integer-digit cap is the maximum a price can already be stored with.
- The `2147483647` share cap is the largest integer that can already be stored as a size. This feature applies it at the edge and forbids storing part of the number.
- The limits of 20 digits for the account and 10 characters for the ticker are the ones that already identify the reference accounts and instruments. This feature does not widen them.
- The 100-character length of the search is a new cap of this feature. The text may include letters, digits, spaces and a name's punctuation, because the reference names use it. The closed ticker format does not apply to it.
- Sessions, permissions, attempt limits and the retry key are out of scope.
- How each text is bound as data and how these formats are enforced is chosen during planning. That choice does not change the observable rules of this spec.
