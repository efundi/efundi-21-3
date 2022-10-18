/**
 * Formats a number based on locale and options. It uses Intl for the formatting.
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {number} number Number to be formatted
 * @param {Object} [options] Intl options are available extended by roundMode and returnIfNaN
 * @param {string} [options.roundMode]
 * @param {string} [options.returnIfNaN]
 * @param {string} [options.locale]
 * @param {string} [options.localeMatcher]
 * @param {string} [options.numberingSystem]
 * @param {string} [options.style]
 * @param {string} [options.currency]
 * @param {string} [options.currencyDisplay]
 * @param {boolean}[options.useGrouping]
 * @param {number} [options.minimumIntegerDigits]
 * @param {number} [options.minimumFractionDigits]
 * @param {number} [options.maximumFractionDigits]
 * @param {number} [options.minimumSignificantDigits]
 * @param {number} [options.maximumSignificantDigits]
 * @returns {string}
 */
export function formatNumber(number: number, options?: {
    roundMode?: string;
    returnIfNaN?: string;
    locale?: string;
    localeMatcher?: string;
    numberingSystem?: string;
    style?: string;
    currency?: string;
    currencyDisplay?: string;
    useGrouping?: boolean;
    minimumIntegerDigits?: number;
    minimumFractionDigits?: number;
    maximumFractionDigits?: number;
    minimumSignificantDigits?: number;
    maximumSignificantDigits?: number;
} | undefined): string;
/**
 * Formats a number based on locale and options. It uses Intl for the formatting.
 */
export type FormatNumberPart = import("../../types/LocalizeMixinTypes.js").FormatNumberPart;
