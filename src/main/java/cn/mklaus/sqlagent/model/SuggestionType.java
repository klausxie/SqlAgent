package cn.mklaus.sqlagent.model;

/**
 * Suggestion type enumeration
 */
public enum SuggestionType {
    INDEX_ADDITION,
    INDEX_REMOVAL,
    QUERY_REWRITE,
    JOIN_OPTIMIZATION,
    SUBQUERY_REFACTORING,
    PARTITIONING,
    HINT_USAGE,
    SYNTAX_CORRECTION
}
