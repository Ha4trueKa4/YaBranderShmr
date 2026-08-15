# API requirements for profile and project management

The current frontend can show management UI, but these actions are only local until the API supports the operations below.

## Profile

The contract has no endpoint for changing a user's profile.

- `PATCH /users/{id}` (or `PATCH /users/me`) is required.
- Request body must support at least `name` or `nickname`.
- The response must return the updated user profile, including `id`, `name`/`nickname`, and profile counters.

Without it, the Edit profile modal cannot persist the display name after a page reload.

## Projects

`PATCH /projects/{id}` currently accepts only `is_public` and `data`.

- Add an optional `name: string` field to `PATCH /projects/{id}` so project renaming can be saved.
- The successful response should keep returning the full updated `Project` object.

The existing endpoints are sufficient for these actions:

- `PATCH /projects/{id}` with `is_public` for the Private/Public switch;
- `DELETE /projects/{id}` for deletion after the frontend confirmation.

To integrate them, the frontend also needs the project list endpoint to remain invalidatable/refetchable after each mutation, or to receive the updated project in mutation responses.
