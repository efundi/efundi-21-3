/**
 * Formats date based on locale and options
 *
 * @param {Date} date
 * @param {Object} [options] Intl options are available
 * @param {string} [options.locale]
 * @param {string} [options.localeMatcher]
 * @param {string} [options.formatMatcher]
 * @param {boolean}[options.hour12]
 * @param {string} [options.numberingSystem]
 * @param {string} [options.calendar]
 * @param {string} [options.timeZone]
 * @param {string} [options.timeZoneName]
 * @param {string} [options.weekday]
 * @param {string} [options.era]
 * @param {string} [options.year]
 * @param {string} [options.month]
 * @param {string} [options.day]
 * @param {string} [options.hour]
 * @param {string} [options.minute]
 * @param {string} [options.second]
 * @returns {string}
 */
export function formatDate(date: Date, options?: {
    locale?: string;
    localeMatcher?: string;
    formatMatcher?: string;
    hour12?: boolean;
    numberingSystem?: string;
    calendar?: string;
    timeZone?: string;
    timeZoneName?: string;
    weekday?: string;
    era?: string;
    year?: string;
    month?: string;
    day?: string;
    hour?: string;
    minute?: string;
    second?: string;
} | undefined): string;
