/** Angel Fund goal shown on the thermometer (dollars). */
export const ANGEL_GOAL = 2000;

/**
 * Client-side gift bounds, paired with the server's PaymentService.MIN/MAX_DONATION_CENTS.
 * The server is authoritative; these keep a donor away from its error paths.
 */
export const ANGEL_MIN_DOLLARS = 1;
export const ANGEL_MAX_DOLLARS = 10000;
