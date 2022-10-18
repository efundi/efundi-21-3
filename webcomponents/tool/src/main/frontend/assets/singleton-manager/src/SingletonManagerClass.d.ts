export class SingletonManagerClass {
    _map: Map<any, any>;
    /**
     * @param {string} key
     * @param {any} value
     * @throws {Error} Will throw if the key is already defined
     */
    set(key: string, value: any): void;
    /**
     * @param {string} key
     * @returns
     */
    get(key: string): any;
    /**
     * @param {string} key
     */
    has(key: string): boolean;
}
