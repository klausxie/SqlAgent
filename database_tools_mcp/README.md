# Database Tools MCP Server

Model Context Protocol (MCP) server for database metadata and SQL analysis tools.

## Features

- **get_table_metadata**: Get table metadata (columns, indexes, row count) for MySQL and PostgreSQL
- **explain_sql**: Get SQL execution plan with cost estimates
- **parse_sql**: Parse SQL and extract tables, columns, and query type

## Configuration

Set the following environment variables:

### For MySQL:
- `DB_TYPE`: "mysql"
- `DB_HOST`: Database host
- `DB_PORT`: Database port (default: 3306)
- `DB_USER`: Database user
- `DB_PASSWORD`: Database password
- `DB_NAME`: Database name

### For PostgreSQL:
- `DB_TYPE`: "postgresql"
- `DB_HOST`: Database host
- `DB_PORT`: Database port (default: 5432)
- `DB_USER`: Database user
- `DB_PASSWORD`: Database password
- `DB_NAME`: Database name

## Usage

Run with uv:
```bash
uv run main.py
```

Or run with Python:
```bash
python main.py
```
