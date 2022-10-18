/**
 * For Dutch and Belgian amounts the currency should be at the end of the string
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {FormatNumberPart[]} formattedParts
 * @returns {FormatNumberPart[]}
 */
export function forceCurrencyToEnd(formattedParts: FormatNumberPart[]): FormatNumberPart[];
/**
 * For Dutch and Belgian amounts the currency should be at the end of the string
 */
export type FormatNumberPart = import("../../types/LocalizeMixinTypes").FormatNumberPart;
