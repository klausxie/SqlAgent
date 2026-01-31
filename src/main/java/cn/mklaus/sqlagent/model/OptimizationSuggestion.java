package cn.mklaus.sqlagent.model;

/**
 * Optimization suggestion model
 */
public class OptimizationSuggestion {
    private SuggestionType type;
    private String title;
    private String description;
    private String sqlSnippet;
    private ImpactLevel impact;
    private ComplexityLevel complexity;

    public OptimizationSuggestion() {
        this.impact = ImpactLevel.MEDIUM;
        this.complexity = ComplexityLevel.MEDIUM;
    }

    public SuggestionType getType() {
        return type;
    }

    public void setType(SuggestionType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSqlSnippet() {
        return sqlSnippet;
    }

    public void setSqlSnippet(String sqlSnippet) {
        this.sqlSnippet = sqlSnippet;
    }

    public ImpactLevel getImpact() {
        return impact;
    }

    public void setImpact(ImpactLevel impact) {
        this.impact = impact;
    }

    public ComplexityLevel getComplexity() {
        return complexity;
    }

    public void setComplexity(ComplexityLevel complexity) {
        this.complexity = complexity;
    }

    @Override
    public String toString() {
        return "OptimizationSuggestion{" +
                "type=" + type +
                ", title='" + title + '\'' +
                ", impact=" + impact +
                '}';
    }
}
