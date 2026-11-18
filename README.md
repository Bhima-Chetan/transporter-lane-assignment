# Transporter Assignment on Lanes

A Spring Boot service that decides which transporter should run each lane, so that every lane is covered, the total cost is as low as possible, and no more than `maxTransporters` transporters are used.

- **Live API:** `https://<your-app>.onrender.com` (the root opens Swagger UI)
- **Swagger UI:** `https://<your-app>.onrender.com/swagger-ui/index.html`
- **Demo video:** `<loom link>`

The live instance runs on Render's free tier, so the first request after it has been idle for a while can take up to a minute while it wakes up.

## How I read the problem

The spec lists three objectives that pull against each other (cheapest vs. most transporters), so I ranked them:

1. **Every lane is covered.** This is a hard rule. If it can't be done within `maxTransporters`, the API returns `422` and tells you the smallest limit that would work.
2. **Lowest total cost** among plans that stay within the limit.
3. **Use as many transporters as possible** as the tie-breaker between plans that cost the same, including when two transporters quoted the same price on a lane.

Each lane is given to exactly one transporter. `selectedTransporters` only lists transporters that actually won a lane, so with a generous limit it can be shorter than `maxTransporters`. Adding a transporter that wins nothing doesn't save money, and pushing a lane onto a more expensive transporter just to use one more of them would break rule 2.

## The algorithm

Picking at most *k* transporters so that each lane goes to the cheapest of them is the p-median problem, which is NP-hard. There's no formula for it, so the service runs an exact branch-and-bound search ([BranchAndBoundSearch.java](src/main/java/com/transporter/assignment/optimizer/BranchAndBoundSearch.java)):

- It walks through the transporters one at a time and branches on *take it* or *skip it*, until *k* are taken.
- Before going deeper it works out the best cost the branch could still reach: for every lane, the lower of the best quote already taken and the best quote among transporters not decided yet. If that can't beat the best plan found so far, or some lane can no longer be covered, the whole branch is dropped.
- At the end of a branch each lane goes to its cheapest selected transporter. When several selected transporters tie on a lane, a small bipartite matching ([LaneWinners.java](src/main/java/com/transporter/assignment/optimizer/LaneWinners.java)) spreads those lanes so the most transporters end up with work.

The answer is exact, not a heuristic. [AssignmentOptimizerCrossCheckTest](src/test/java/com/transporter/assignment/optimizer/AssignmentOptimizerCrossCheckTest.java) checks it against plain brute force on 500 random inputs built to contain lots of tied quotes.

Timings on dense random data (every transporter quotes every lane), on a laptop:

| Transporters × lanes | maxTransporters | Time |
|---|---|---|
| 30 × 30 | 10 | ~50 ms |
| 40 × 40 | 5 | ~80 ms |
| 50 × 50 | 4 | ~45 ms |
| 100 × 50 | 3 | ~25 ms |

Very large inputs with high limits would take too long to solve exactly, so the search stops after `assignment.search-step-limit` steps (5 million, set in `application.properties`) and returns `422` rather than tying up the server. For example, 60 × 60 with a limit of 15 hits that cap in about half a second. If real data ever got that big, I'd replace `BranchAndBoundSearch` with an integer-programming solver such as OR-Tools CP-SAT; nothing else in the service would need to change.

## Results on the provided data

| Data | maxTransporters | Total cost | Transporters used |
|---|---|---|---|
| Test case 1 (5 lanes) | 1 | 96,940 | 4 |
| | 2 | 62,938 | 1, 4 |
| | **3** | **60,139** | **1, 4, 5** |
| | 4 or more | 59,896 | 1, 3, 4, 5 |
| Quote sheet CSV (9 lanes) | 3 | 134,876 | 1, 4, 6 |
| | 4 | 127,844 | 1, 3, 4, 7 |
| | 5 or more | 125,045 | 1, 3, 4, 5, 7 |
| Test case 2 | 2 | not possible, needs 3 | |
| | 3 | 7,500 | 1, 2, 3 |
| | 4 | 7,450 | 1, 2, 3, 4 |
| API spec example (10 lanes) | 3 | not possible, needs 5 | |
| | 5 or more | 48,000 | 1, 2, 3, 4, 5 |

All of these are pinned down by unit tests.

### About the sample responses in the spec

The sample outputs in the assignment document don't match their inputs, so I treated them as examples of the response format rather than expected values:

- **Test case 1** expects 75,000 with transporters 4, 6 and 3. The lanes it lists add up to 69,001, lane 3 goes to transporter 2 (who isn't in the selected list), and choosing 3, 4 and 6 properly would cost 64,677. The cheapest plan with three transporters is 1, 4 and 5 at **60,139**.
- **The API 2 example** gives lanes 7, 9 and 10 to transporters 1 and 2, who never quoted them. With that input every lane needs at least five transporters, so for `maxTransporters: 3` this service returns a `422` saying exactly that.

## API

Base path: `/api/v1/transporters`. The OpenAPI document is at `/v3/api-docs`.

| Method | Path | What it does |
|---|---|---|
| POST | `/input` | Submit lanes and quotes as JSON. Replaces anything submitted before. |
| POST | `/input/csv` | Same thing from the CSV quote sheet (multipart form, field `file`). |
| POST | `/assignment` | Returns the cheapest plan for `{"maxTransporters": n}`. |

Test case 1 is loaded when the app starts, so `/assignment` works straight away.

### Submit input

```http
POST /api/v1/transporters/input
Content-Type: application/json

{
  "lanes": [
    {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
    {"id": 2, "origin": "Delhi", "destination": "Bangalore"}
  ],
  "transporters": [
    {"id": 1, "name": "Transporter T1", "laneQuotes": [{"laneId": 1, "quote": 5000}, {"laneId": 2, "quote": 6000}]},
    {"id": 2, "name": "Transporter T2", "laneQuotes": [{"laneId": 1, "quote": 5500}]}
  ]
}
```

```json
{"status": "success", "message": "Input data saved successfully."}
```

Swagger's *Examples* dropdown has the full test case 1, test case 2 and spec example bodies ready to send.

### Submit the CSV quote sheet

The format is the sheet that came with the assignment: a header row `Trade Lane,T1,T2,...` followed by one row per lane with a quote under each transporter.

```csv
Trade Lane,T1,T2,T3
Lane 1,20835,48844,39020
Lane 2,10512,,20648
```

Lanes and transporters get their ids from their position, so the first lane row is lane 1 and column `T1` is transporter 1. An empty cell means that transporter didn't quote the lane. A byte-order mark and Windows line endings (as Excel exports) are fine.

```bash
curl -F "file=@src/main/resources/samples/transporter_assignment_data.csv" \
     http://localhost:8080/api/v1/transporters/input/csv
```

```json
{"status": "success", "message": "Input data saved successfully. Loaded 9 lanes and 7 transporters."}
```

### Get the assignment

```http
POST /api/v1/transporters/assignment
Content-Type: application/json

{"maxTransporters": 3}
```

```json
{
  "status": "success",
  "totalCost": 60139,
  "assignments": [
    {"laneId": 1, "transporterId": 5},
    {"laneId": 2, "transporterId": 1},
    {"laneId": 3, "transporterId": 4},
    {"laneId": 4, "transporterId": 4},
    {"laneId": 5, "transporterId": 4}
  ],
  "selectedTransporters": [1, 4, 5]
}
```

### Errors

Every error has the same shape. `details` appears when there are individual problems to list:

```json
{
  "status": "error",
  "message": "Input data is not valid",
  "details": [
    "Transporter 3 quotes lane 12, which does not exist",
    "Lane 6 has no quotes from any transporter, so it can never be covered"
  ]
}
```

| Status | When |
|---|---|
| 400 | Malformed JSON, missing or invalid fields, duplicate lane or transporter ids, quotes for lanes that don't exist, a lane nobody quoted, a broken CSV |
| 404 | `/assignment` is called before any input exists (only possible if the startup sample is removed) |
| 405 / 415 | Wrong HTTP method or content type |
| 422 | `maxTransporters` is too low to cover every lane (the message says how many are needed), or the input is too large to solve within the step limit |

## Running it locally

You need Java 21. The Maven wrapper downloads Maven for you.

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Then open http://localhost:8080.

With Docker:

```bash
docker build -t transporter-assignment .
docker run -p 8080:8080 transporter-assignment
```

### Tests

```bash
./mvnw test
```

There are 54 tests:

- Optimizer: every row of the results table above, tie handling, infeasible limits, the step limit, plus the randomized brute-force cross-check.
- Validator and CSV parser: one test per rule.
- Service: replacing data, and keeping the old data when new input is rejected.
- API: full HTTP round trips through MockMvc, covering success, validation errors, malformed JSON, 405, 415 and 422, the CSV upload and the generated OpenAPI document.

### Postman

Import `postman/transporter-assignment.postman_collection.json`. It has every sample input, the CSV upload and two assignment calls. Change the `baseUrl` variable to point at the live URL.

## Deploying to Render (free)

1. Push this repository to GitHub.
2. On Render: **New → Web Service**, pick the repository. Render finds the `Dockerfile` by itself.
3. Choose the **Free** instance type and deploy. No environment variables are needed; the app picks up Render's `PORT`.

## Project layout

```
src/main/java/com/transporter/assignment
├── controller   REST endpoints, plus the redirect from / to Swagger
├── service      input validation, CSV parsing, orchestration
├── optimizer    branch-and-bound search and tie spreading
├── repository   in-memory store for the current input
├── model        lanes, transporters, quotes, the resulting plan
├── dto          request and response bodies
├── exception    error types and the global exception handler
└── config       OpenAPI setup and the startup sample loader
```

## Decisions and trade-offs

- **In-memory storage.** The spec works with one dataset at a time and doesn't ask for persistence, so the latest input lives in memory and test case 1 is reloaded on restart. Moving to a database would only touch `TransporterInputRepository`.
- **New input replaces old input** instead of merging into it, which matches "submit input, then fetch results".
- **Validation happens in two layers.** Bean Validation checks the shape of each field. `TransporterInputValidator` checks rules that span the whole input (unique ids, known lanes, every lane quoted) and reports all problems in a single response.
- **Quotes are whole numbers**, as in all the provided data.
