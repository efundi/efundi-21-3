/**
 * @typedef {import('../types/LocalizeMixinTypes').NamespaceObject} NamespaceObject
 */
/**
 * `LocalizeManager` manages your translations (includes loading)
 */
export class LocalizeManager {
    constructor({ autoLoadOnLocaleChange, fallbackLocale }?: {
        autoLoadOnLocaleChange?: boolean | undefined;
        fallbackLocale?: string | undefined;
    });
    __delegationTarget: DocumentFragment;
    _autoLoadOnLocaleChange: boolean;
    _fallbackLocale: string;
    /** @type {Object.<string, Object.<string, Object>>} */
    __storage: {
        [x: string]: {
            [x: string]: Object;
        };
    };
    /** @type {Map.<RegExp|string, function>} */
    __namespacePatternsMap: Map<RegExp | string, Function>;
    /** @type {Object.<string, function|null>} */
    __namespaceLoadersCache: {
        [x: string]: Function | null;
    };
    /** @type {Object.<string, Object.<string, Promise.<Object>>>} */
    __namespaceLoaderPromisesCache: {
        [x: string]: {
            [x: string]: Promise<Object>;
        };
    };
    formatNumberOptions: {
        returnIfNaN: string;
    };
    _supportExternalTranslationTools: boolean;
    /**
     * @param {string} value
     */
    set locale(arg: string);
    /**
     * @returns {string}
     */
    get locale(): string;
    _setupTranslationToolSupport(): void;
    /**
     * This value allows for support for Google Translate (or other 3rd parties taking control
     * of the html[lang] attribute).
     *
     * Have the following scenario in mind:
     * 1. locale is initialized by developer via html[data-localize-lang="en-US"] and
     * html[lang="en-US"]. When localize is loaded (note that this also can be after step 2 below),
     * it will sync its initial state from html[data-localize-lang]
     * 2. Google Translate kicks in for the French language. It will set html[lang="fr"].
     * This new language is not one known by us, so we most likely don't have translations for
     * this file. Therefore, we do NOT sync this value to LocalizeManager. The manager should
     * still ask for known resources (in this case for locale 'en-US')
     * 3. locale is changed (think of a language dropdown)
     * It's a bit of a weird case, because we would not expect an end user to do this. If he/she
     * does, make sure that we do not go against Google Translate, so we maintain accessibility
     * (by not altering html[lang]). We detect this by reading _langAttrSetByTranslationTool:
     * when its value is null, we consider Google translate 'not active'.
     *
     * When Google Translate is turned off by the user (html[lang=auto]),
     * `localize.locale` will be synced to html[lang] again
     *
     * Keep in mind that all of the above also works with other tools than Google Translate,
     * but this is the most widely used tool and therefore used as an example.
     */
    _langAttrSetByTranslationTool: string | null | undefined;
    teardown(): void;
    __locale: string | undefined;
    /**
     * @param {string} locale
     */
    _setHtmlLangAttribute(locale: string): void;
    /**
     * @param {string} value
     * @throws {Error} Language only locales are not allowed(Use 'en-GB' instead of 'en')
     */
    __handleLanguageOnly(value: string): void;
    /**
     * @returns {Promise.<Object>}
     */
    get loadingComplete(): Promise<Object>;
    reset(): void;
    /**
     * @param {string} locale
     * @param {string} namespace
     * @param {object} data
     * @throws {Error} Namespace can be added only once, for a given locale
     */
    addData(locale: string, namespace: string, data: object): void;
    /**
     * @param {RegExp|string} pattern
     * @param {function} loader
     */
    setupNamespaceLoader(pattern: RegExp | string, loader: Function): void;
    /**
     * @param {NamespaceObject[]} namespaces
     * @param {Object} [options]
     * @param {string} [options.locale]
     * @returns {Promise.<Object>}
     */
    loadNamespaces(namespaces: NamespaceObject[], { locale }?: {
        locale?: string;
    } | undefined): Promise<Object>;
    /**
     * @param {NamespaceObject} namespaceObj
     * @param {Object} [options]
     * @param {string} [options.locale]
     * @returns {Promise.<Object|void>}
     */
    loadNamespace(namespaceObj: NamespaceObject, { locale }?: {
        locale?: string;
    } | undefined): Promise<Object | void>;
    /**
     * @param {string | string[]} keys
     * @param {Object.<string,?>} [vars]
     * @param {Object} [opts]
     * @param {string} [opts.locale]
     * @returns {string}
     */
    msg(keys: string | string[], vars?: {
        [x: string]: any;
    } | undefined, opts?: {
        locale?: string;
    } | undefined): string;
    _setupHtmlLangAttributeObserver(): void;
    _htmlLangAttributeObserver: MutationObserver | undefined;
    _teardownHtmlLangAttributeObserver(): void;
    /**
     * @param {string} locale
     * @param {string} namespace
     */
    _isNamespaceInCache(locale: string, namespace: string): boolean;
    /**
     * @param {string} locale
     * @param {string} namespace
     */
    _getCachedNamespaceLoaderPromise(locale: string, namespace: string): Promise<Object> | null;
    /**
     * @param {string} locale
     * @param {NamespaceObject} namespaceObj
     * @param {boolean} isDynamicImport
     * @param {string} namespace
     * @returns {Promise.<Object|void>}
     */
    _loadNamespaceData(locale: string, namespaceObj: NamespaceObject, isDynamicImport: boolean, namespace: string): Promise<Object | void>;
    /**
     * @param {NamespaceObject} namespaceObj
     * @param {boolean} isDynamicImport
     * @param {string} namespace
     * @throws {Error} Namespace shall setup properly. Check loader!
     */
    _getNamespaceLoader(namespaceObj: NamespaceObject, isDynamicImport: boolean, namespace: string): Function;
    /**
     * @param {function} loader
     * @param {string} locale
     * @param {string} namespace
     * @param {string} [fallbackLocale]
     * @returns {Promise.<any>}
     * @throws {Error} Data for namespace and (locale or fallback locale) could not be loaded.
     */
    _getNamespaceLoaderPromise(loader: Function, locale: string, namespace: string, fallbackLocale?: string | undefined): Promise<any>;
    /**
     * @param {string} locale
     * @param {string} namespace
     * @param {Promise.<Object>} promise
     */
    _cacheNamespaceLoaderPromise(locale: string, namespace: string, promise: Promise<Object>): void;
    /**
     * @param {string} namespace
     * @returns {function|null}
     */
    _lookupNamespaceLoader(namespace: string): Function | null;
    /**
     * @param {string} locale
     * @returns {string}
     */
    _getLangFromLocale(locale: string): string;
    /**
     * @param {string} type
     * @param {EventListener} listener
     * @param {...Object} options
     */
    addEventListener(type: string, listener: EventListener, ...options: Object[]): void;
    /**
     * @param {string} type
     * @param {EventListener} listener
     * @param {...Object} options
     */
    removeEventListener(type: string, listener: EventListener, ...options: Object[]): void;
    /**
     *  @param {CustomEvent} event
     */
    dispatchEvent(event: CustomEvent): void;
    /**
     * @param {string} newLocale
     * @param {string} oldLocale
     * @returns {undefined}
     */
    _onLocaleChanged(newLocale: string, oldLocale: string): undefined;
    /**
     * @param {string} newLocale
     * @param {string} oldLocale
     * @returns {Promise.<Object>}
     */
    _loadAllMissing(newLocale: string, oldLocale: string): Promise<Object>;
    /**
     * @param {string | string[]} keys
     * @param {string} locale
     * @returns {string | undefined}
     */
    _getMessageForKeys(keys: string | string[], locale: string): string | undefined;
    /**
     * @param {string | undefined} key
     * @param {string} locale
     * @returns {string}
     * @throws {Error} `key`is missing namespace. The format for `key` is "namespace:name"
     *
     */
    _getMessageForKey(key: string | undefined, locale: string): string;
}
export type NamespaceObject = string | import("../types/LocalizeMixinTypes.js").StringToFunctionMap;
