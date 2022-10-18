/**
 * @desc Returns weekday names for locale
 * @param {Object} [options]
 * @param {string} [options.locale] locale
 * @param {string} [options.style=long] long, short or narrow
 * @param {number} [options.firstDayOfWeek=0] 0 (Sunday), 1 (Monday), etc...
 * @returns {string[]} like: ['Sunday', 'Monday', 'Tuesday', ...etc].
 */
export function getWeekdayNames({ locale, style, firstDayOfWeek }?: {
    locale?: string;
    style?: string;
    firstDayOfWeek?: number;
} | undefined): string[];
