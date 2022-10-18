/**
 * @desc Returns month names for locale
 * @param {Object} [options]
 * @param {string} [options.locale] locale
 * @param {string} [options.style=long] long, short or narrow
 * @returns {string[]} like: ['January', 'February', ...etc].
 */
export function getMonthNames({ locale, style }?: {
    locale?: string;
    style?: string;
} | undefined): string[];
