/**
 * Contract Freeze: Data Transform Interface
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#data-flow
 *
 * The Keystone backend returns snake_case JSON. The frontend uses camelCase.
 * This transform layer bridges the two.
 */

/**
 * Transform service — converts between backend snake_case and frontend camelCase.
 *
 * Implementation must handle:
 * - Recursive key conversion at any nesting depth
 * - Arrays of objects
 * - Null/undefined values
 * - Date strings (ISO 8601)
 * - Special fields that should not be transformed (e.g., checksum hashes)
 */
export interface DataTransformer {
  /**
   * Transform a snake_case object to camelCase.
   * Applied to all API response data before it reaches view components.
   */
  toCamelCase<T = unknown>(input: Record<string, unknown>): T;

  /**
   * Transform a camelCase object to snake_case.
   * Applied to request bodies before sending to the backend.
   */
  toSnakeCase<T = unknown>(input: Record<string, unknown>): T;

  /**
   * Transform an array of snake_case objects.
   */
  toCamelCaseArray<T = unknown>(input: Record<string, unknown>[]): T[];

  /**
   * Deeply convert all keys in an object tree.
   */
  deepTransformKeys(
    input: unknown,
    transformKey: (key: string) => string,
  ): unknown;
}

/**
 * Converts a snake_case string to camelCase.
 * Used by the DataTransformer implementation.
 *
 * @example
 * snakeToCamel('last_analyzed') // 'lastAnalyzed'
 * snakeToCamel('overall_score') // 'overallScore'
 */
export function snakeToCamel(key: string): string {
  return key.replace(/_([a-z])/g, (_, char) => char.toUpperCase());
}

/**
 * Converts a camelCase string to snake_case.
 * Used by the DataTransformer implementation.
 *
 * @example
 * camelToSnake('lastAnalyzed') // 'last_analyzed'
 * camelToSnake('overallScore') // 'overall_score'
 */
export function camelToSnake(key: string): string {
  return key.replace(/[A-Z]/g, (char) => `_${char.toLowerCase()}`);
}
