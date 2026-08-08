export type ApiResult<Data, Error> =
    | { ok: true; data: Data }
    | { ok: false; error: Error };
