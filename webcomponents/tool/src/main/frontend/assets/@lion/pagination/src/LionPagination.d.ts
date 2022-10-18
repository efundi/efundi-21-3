declare const LionPagination_base: typeof LitElement & import("@open-wc/dedupe-mixin").Constructor<import("@lion/localize/types/LocalizeMixinTypes").LocalizeMixinHost> & Pick<typeof import("@lion/localize/types/LocalizeMixinTypes").LocalizeMixinHost, "prototype" | "localizeNamespaces" | "waitForLocalizeNamespaces"> & Pick<typeof LitElement, "prototype" | "render" | "styles" | "getStyles" | "properties" | "observedAttributes" | "createProperty">;
/**
 * @typedef {import('@lion/core').TemplateResult} TemplateResult
 */
/**
 * `LionPagination` is a class for custom Pagination element (`<lion-pagination>` web component).
 *
 * @customElement lion-pagination
 */
export class LionPagination extends LionPagination_base {
    static get styles(): import("@lion/core").CSSResult[];
    static get properties(): {
        current: {
            type: NumberConstructor;
            reflect: boolean;
        };
        count: {
            type: NumberConstructor;
            reflect: boolean;
        };
    };
    /** @param {number} value */
    set current(arg: number);
    /** @returns {number} */
    get current(): number;
    __current: number | undefined;
    /** @private */
    private __visiblePages;
    count: number;
    /**
     * Go next in pagination
     * @public
     */
    public next(): void;
    /**
     * Go to first page
     * @public
     */
    public first(): void;
    /**
     * Go to the last page
     * @public
     */
    public last(): void;
    /**
     * Go to the specific page
     * @param {number} pageNumber
     * @public
     */
    public goto(pageNumber: number): void;
    /**
     * Go back in pagination
     * @public
     */
    public previous(): void;
    /**
     * Set desired page in the pagination and fire the current changed event.
     * @param {Number} page page number to be set
     * @private
     */
    private __fire;
    /**
     * Calculate nav list based on current page selection.
     * @returns {(number|'...')[]}
     * @private
     */
    private __calculateNavList;
    /**
     * Get previous or next button template.
     * This method can be overridden to apply customized template in wrapper.
     * @param {String} label namespace label i.e. next or previous
     * @returns {TemplateResult} icon template
     * @protected
     */
    protected _prevNextIconTemplate(label: string): TemplateResult;
    /**
     * Get next or previous button template.
     * This method can be overridden to apply customized template in wrapper.
     * @param {String} label namespace label i.e. next or previous
     * @param {Number} pageNumber page number to be set
     * @param {String} namespace namespace prefix for translations
     * @returns {TemplateResult} nav item template
     * @protected
     */
    protected _prevNextButtonTemplate(label: string, pageNumber: number, namespace?: string): TemplateResult;
    /**
     * Get disabled button template.
     * This method can be overridden to apply customized template in wrapper.
     * @param {String} label namespace label i.e. next or previous
     * @returns {TemplateResult} nav item template
     * @protected
     */
    protected _disabledButtonTemplate(label: string): TemplateResult;
    /**
     * Render navigation list
     * @returns {TemplateResult[]} nav list template
     * @protected
     */
    protected _renderNavList(): TemplateResult[];
}
export type TemplateResult = import("@lion/core").TemplateResult;
import { LitElement } from "@lion/core";
export {};
