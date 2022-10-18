/**
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {FormatNumberPart[]} formattedParts
 * @param {Object} [options]
 * @param {string} [options.currency]
 * @param {string} [options.currencyDisplay]
 * @returns {FormatNumberPart[]}
 */
export function forceTryCurrencyCode(formattedParts: FormatNumberPart[], { currency, currencyDisplay }?: {
    currency?: string;
    currencyDisplay?: string;
} | undefined): FormatNumberPart[];
export type FormatNumberPart = import("../../types/LocalizeMixinTypes").FormatNumberPart;
