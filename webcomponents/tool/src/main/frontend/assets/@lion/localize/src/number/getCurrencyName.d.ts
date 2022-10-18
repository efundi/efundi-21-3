/**
 * Based on number, returns currency name like 'US dollar'
 *
 * @typedef {import('../../types/LocalizeMixinTypes').FormatNumberPart} FormatNumberPart
 * @param {string} currencyIso iso code like USD
 * @param {Object} options Intl options are available extended by roundMode
 * @returns {string} currency name like 'US dollar'
 */
export function getCurrencyName(currencyIso: string, options: Object): string;
/**
 * Based on number, returns currency name like 'US dollar'
 */
export type FormatNumberPart = import("../../types/LocalizeMixinTypes.js").FormatNumberPart;
