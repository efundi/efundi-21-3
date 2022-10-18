/**
 * Function with all fixes on localize
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {FormatNumberPart[]} formattedParts
 * @param {Object} options
 * @param {string} [options.style]
 * @param {string} [options.currency]
 * @param {string} [options.currencyDisplay]
 * @param {string} _locale
 * @returns {FormatNumberPart[]}
 */
export function normalizeIntl(formattedParts: FormatNumberPart[], options: {
    style?: string;
    currency?: string;
    currencyDisplay?: string;
} | undefined, _locale: string): FormatNumberPart[];
/**
 * Function with all fixes on localize
 */
export type FormatNumberPart = import("../../types/LocalizeMixinTypes.js").FormatNumberPart;
