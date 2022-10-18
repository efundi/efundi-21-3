/**
 * When in some locales there is no space between currency and amount it is added
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {FormatNumberPart[]} formattedParts
 * @param {Object} [options]
 * @param {string} [options.currency]
 * @param {string} [options.currencyDisplay]
 * @returns {FormatNumberPart[]}
 */
export function forceSpaceBetweenCurrencyCodeAndNumber(formattedParts: FormatNumberPart[], { currency, currencyDisplay }?: {
    currency?: string;
    currencyDisplay?: string;
} | undefined): FormatNumberPart[];
/**
 * When in some locales there is no space between currency and amount it is added
 */
export type FormatNumberPart = import("../../types/LocalizeMixinTypes").FormatNumberPart;
