/**
 * Change the symbols for locale 'en-AU', due to bug in Chrome
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {FormatNumberPart[]} formattedParts
 * @param {Object} [options]
 * @param {string} [options.currency]
 * @param {string} [options.currencyDisplay]
 * @returns {FormatNumberPart[]}
 */
export function forceENAUSymbols(formattedParts: FormatNumberPart[], { currency, currencyDisplay }?: {
    currency?: string;
    currencyDisplay?: string;
} | undefined): FormatNumberPart[];
/**
 * Change the symbols for locale 'en-AU', due to bug in Chrome
 */
export type FormatNumberPart = import("../../types/LocalizeMixinTypes").FormatNumberPart;
