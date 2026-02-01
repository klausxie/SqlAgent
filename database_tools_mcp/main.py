"""
Database Tools MCP Server

MCP server providing database metadata and SQL analysis tools for MySQL and PostgreSQL.
"""

from typing import Any, Dict, List, Optional
import os
import logging
import json

import pymysql
import psycopg2
import sqlparse
from mcp.server.fastmcp import FastMCP

# Create MCP server instance
mcp = FastMCP("database-tools")

# Database connection configuration from environment
DB_CONFIG = {
    "type": os.getenv("DB_TYPE", "mysql").lower(),
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "3306" if os.getenv("DB_TYPE", "mysql").lower() == "mysql" else "5432")),
    "user": os.getenv("DB_USER", "root"),
    "password": os.getenv("DB_PASSWORD", ""),
    "database": os.getenv("DB_NAME", "test_db"),
}

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("database-tools-mcp")


def get_mysql_connection() -> pymysql.Connection:
    """Get MySQL database connection"""
    return pymysql.connect(
        host=DB_CONFIG["host"],
        port=DB_CONFIG["port"],
        user=DB_CONFIG["user"],
        password=DB_CONFIG["password"],
        database=DB_CONFIG["database"],
        cursorclass=pymysql.cursors.DictCursor
    )


def get_postgresql_connection() -> psycopg2.extensions.connection:
    """Get PostgreSQL database connection"""
    return psycopg2.connect(
        host=DB_CONFIG["host"],
        port=DB_CONFIG["port"],
        user=DB_CONFIG["user"],
        password=DB_CONFIG["password"],
        dbname=DB_CONFIG["database"]
    )


def get_connection():
    """Get database connection based on type"""
    if DB_CONFIG["type"] == "mysql":
        return get_mysql_connection()
    elif DB_CONFIG["type"] == "postgresql":
        return get_postgresql_connection()
    else:
        raise ValueError(f"Unsupported database type: {DB_CONFIG['type']}")


@mcp.tool()
def get_table_metadata(table_name: str) -> Dict[str, Any]:
    """
    Get table metadata including columns, indexes, and row count.

    Args:
        table_name: Name of the table to analyze

    Returns:
        Dictionary containing:
        - tableName: Table name
        - columns: List of column information (name, type, nullable, key)
        - indexes: List of index information
        - rowCount: Estimated row count
    """
    logger.info(f"Getting metadata for table: {table_name}")

    conn = get_connection()
    cursor = None
    try:
        if DB_CONFIG["type"] == "mysql":
            return _get_mysql_table_metadata(conn, cursor, table_name)
        elif DB_CONFIG["type"] == "postgresql":
            return _get_postgresql_table_metadata(conn, cursor, table_name)
        else:
            raise ValueError(f"Unsupported database type: {DB_CONFIG['type']}")
    except Exception as e:
        logger.error(f"Error getting table metadata: {e}")
        return {
            "error": str(e),
            "tableName": table_name
        }
    finally:
        if cursor:
            cursor.close()
        conn.close()


def _get_mysql_table_metadata(conn: pymysql.Connection, cursor, table_name: str) -> Dict[str, Any]:
    """Get MySQL table metadata"""
    cursor = conn.cursor()

    # Get column information
    cursor.execute("""
        SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY,
               COLUMN_DEFAULT, EXTRA, COLUMN_TYPE
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
        ORDER BY ORDINAL_POSITION
    """, (DB_CONFIG["database"], table_name))

    columns = []
    for row in cursor.fetchall():
        columns.append({
            "name": row["COLUMN_NAME"],
            "type": row["DATA_TYPE"],
            "fullType": row["COLUMN_TYPE"],
            "nullable": row["IS_NULLABLE"] == "YES",
            "isPrimaryKey": row["COLUMN_KEY"] == "PRI",
            "isUniqueKey": row["COLUMN_KEY"] == "UNI",
            "default": row["COLUMN_DEFAULT"],
            "extra": row["EXTRA"]
        })

    # Get index information
    cursor.execute("""
        SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE, SEQ_IN_INDEX, INDEX_TYPE
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = %s AND TABLE_NAME = %s
        ORDER BY INDEX_NAME, SEQ_IN_INDEX
    """, (DB_CONFIG["database"], table_name))

    indexes: Dict[str, Any] = {}
    for row in cursor.fetchall():
        index_name = row["INDEX_NAME"]
        if index_name not in indexes:
            indexes[index_name] = {
                "columns": [],
                "unique": row["NON_UNIQUE"] == 0,
                "type": row["INDEX_TYPE"]
            }
        indexes[index_name]["columns"].append(row["COLUMN_NAME"])

    # Get row count (estimate for InnoDB)
    cursor.execute(f"SELECT COUNT(*) as cnt FROM `{table_name}`")
    row_count = cursor.fetchone()["cnt"]

    return {
        "tableName": table_name,
        "database": DB_CONFIG["database"],
        "columns": columns,
        "indexes": [{"name": k, **v} for k, v in indexes.items()],
        "rowCount": row_count
    }


def _get_postgresql_table_metadata(conn, cursor, table_name: str) -> Dict[str, Any]:
    """Get PostgreSQL table metadata"""
    cursor = conn.cursor()

    # Get column information
    cursor.execute("""
        SELECT
            column_name, data_type, is_nullable, column_default,
            character_maximum_length, numeric_precision, numeric_scale
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE table_schema = 'public' AND table_name = %s
        ORDER BY ordinal_position
    """, (table_name,))

    columns = []
    for row in cursor.fetchall():
        column_type = row[1]
        if row[4]:  # character_maximum_length
            column_type = f"{column_type}({row[4]})"
        elif row[5]:  # numeric_precision
            if row[6]:  # numeric_scale
                column_type = f"{column_type}({row[5]},{row[6]})"
            else:
                column_type = f"{column_type}({row[5]})"

        columns.append({
            "name": row[0],
            "type": row[1],
            "fullType": column_type,
            "nullable": row[2] == "YES",
            "default": row[3]
        })

    # Get index information
    cursor.execute("""
        SELECT
            i.indexname, i.indexdef,
            a.attname,
            ix.indisunique,
            ix.indisprimary
        FROM pg_indexes i
        JOIN pg_index ix ON i.indexname = ix.indexrelname::regclass::text
        JOIN pg_attribute a ON a.attnum = ANY(ix.indkey)
        JOIN pg_class c ON c.oid = ix.indrelid
        WHERE c.relname = %s AND i.schemaname = 'public'
        ORDER BY i.indexname, a.attnum
    """, (table_name,))

    indexes: Dict[str, Any] = {}
    for row in cursor.fetchall():
        index_name = row[0]
        if index_name not in indexes:
            indexes[index_name] = {
                "columns": [],
                "unique": row[3],
                "isPrimaryKey": row[4],
                "definition": row[1]
            }
        indexes[index_name]["columns"].append(row[2])

    # Get primary key columns
    cursor.execute("""
        SELECT a.attname
        FROM pg_index i
        JOIN pg_attribute a ON a.attnum = ANY(i.indkey)
        JOIN pg_class c ON c.oid = i.indrelid
        WHERE c.relname = %s AND i.indisprimary = true
    """, (table_name,))

    pk_columns = [row[0] for row in cursor.fetchall()]
    for col in columns:
        col["isPrimaryKey"] = col["name"] in pk_columns

    # Get row count
    cursor.execute(f'SELECT COUNT(*) FROM "{table_name}"')
    row_count = cursor.fetchone()[0]

    return {
        "tableName": table_name,
        "database": DB_CONFIG["database"],
        "columns": columns,
        "indexes": [{"name": k, **v} for k, v in indexes.items()],
        "rowCount": row_count
    }


@mcp.tool()
def explain_sql(sql: str) -> Dict[str, Any]:
    """
    Get SQL execution plan with cost estimates.

    Args:
        sql: SQL query to explain

    Returns:
        Dictionary containing:
        - plan: Execution plan (text or JSON)
        - format: Format of the plan (text, json, etc.)
        - cost: Estimated cost (if available)
    """
    logger.info(f"Explaining SQL: {sql[:100]}...")

    conn = get_connection()
    cursor = None
    try:
        if DB_CONFIG["type"] == "mysql":
            return _explain_mysql_sql(conn, cursor, sql)
        elif DB_CONFIG["type"] == "postgresql":
            return _explain_postgresql_sql(conn, cursor, sql)
        else:
            raise ValueError(f"Unsupported database type: {DB_CONFIG['type']}")
    except Exception as e:
        logger.error(f"Error explaining SQL: {e}")
        return {
            "error": str(e),
            "sql": sql
        }
    finally:
        if cursor:
            cursor.close()
        conn.close()


def _explain_mysql_sql(conn: pymysql.Connection, cursor, sql: str) -> Dict[str, Any]:
    """Get MySQL execution plan"""
    cursor = conn.cursor()

    # Try EXPLAIN FORMAT=JSON first (MySQL 5.6+)
    try:
        cursor.execute(f"EXPLAIN FORMAT=JSON {sql}")
        result = cursor.fetchone()

        if result and "EXPLAIN" in result:
            plan_json = list(result.values())[0]
            return {
                "plan": plan_json,
                "format": "json",
                "sql": sql
            }
    except Exception as e:
        logger.warning(f"EXPLAIN FORMAT=JSON failed, trying traditional format: {e}")

    # Fallback to traditional EXPLAIN
    cursor.execute(f"EXPLAIN {sql}")
    rows = cursor.fetchall()

    return {
        "plan": rows,
        "format": "table",
        "sql": sql
    }


def _explain_postgresql_sql(conn, cursor, sql: str) -> Dict[str, Any]:
    """Get PostgreSQL execution plan"""
    cursor = conn.cursor()

    # Use EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) for detailed plan
    try:
        cursor.execute(f"EXPLAIN (ANALYZE, FORMAT JSON) {sql}")
        result = cursor.fetchone()

        if result and result[0]:
            plan_json = json.dumps(result[0][0], indent=2)
            return {
                "plan": plan_json,
                "format": "json",
                "analyzed": True,
                "sql": sql
            }
    except Exception as e:
        logger.warning(f"EXPLAIN (ANALYZE, FORMAT JSON) failed, trying traditional format: {e}")

    # Fallback to traditional EXPLAIN
    cursor.execute(f"EXPLAIN {sql}")
    rows = cursor.fetchall()

    plan_text = "\n".join([row[0] for row in rows])

    return {
        "plan": plan_text,
        "format": "text",
        "sql": sql
    }


@mcp.tool()
def parse_sql(sql: str) -> Dict[str, Any]:
    """
    Parse SQL and extract table names, column names, and query type.

    Args:
        sql: SQL query to parse

    Returns:
        Dictionary containing:
        - tables: List of table names found in the query
        - queryType: Type of query (SELECT, INSERT, UPDATE, DELETE, etc.)
        - columns: List of column names (extracted from SELECT/INSERT clauses)
        - originalSql: Original SQL query
    """
    logger.info(f"Parsing SQL: {sql[:100]}...")

    try:
        parsed = sqlparse.parse(sql)[0]

        # Extract query type
        query_type = None
        for token in parsed.tokens:
            if token.ttype is not None and token.ttype in sqlparse.tokens.Keyword.DML:
                query_type = str(token).upper()
                break
            elif token.is_keyword:
                normalized = str(token).upper()
                if normalized in ("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "ALTER", "DROP"):
                    query_type = normalized
                    break

        # Extract table names using sqlparse
        tables = _extract_table_names(parsed)

        # Extract column names
        columns = _extract_column_names(parsed)

        return {
            "tables": tables,
            "queryType": query_type,
            "columns": columns,
            "originalSql": sql.strip()
        }
    except Exception as e:
        logger.error(f"Error parsing SQL: {e}")
        return {
            "error": str(e),
            "originalSql": sql.strip()
        }


def _extract_table_names(parsed) -> List[str]:
    """Extract table names from parsed SQL"""
    tables = []

    # Simple extraction - look for identifiers after FROM, JOIN, INTO, UPDATE
    from sqlparse.sql import Identifier, IdentifierList, Table

    tokens = list(parsed.flatten())

    i = 0
    while i < len(tokens):
        token = tokens[i]

        # Look for FROM, JOIN, INTO, UPDATE keywords
        if token.is_keyword and str(token).upper() in ("FROM", "JOIN", "INTO", "UPDATE"):
            # Next token might be a table name
            if i + 1 < len(tokens):
                next_token = tokens[i + 1]
                if isinstance(next_token, Identifier) and not next_token.is_keyword:
                    table_name = _get_identifier_name(next_token)
                    if table_name and table_name not in tables:
                        tables.append(table_name)
            # Skip to next iteration
            i += 2
            continue

        # Check for Identifier tokens that might be table names
        if isinstance(token, Identifier):
            name = _get_identifier_name(token)
            # Heuristic: if it's all lowercase or has underscores, might be a table
            if name and name not in tables and "_" in name or name.islower():
                # But skip common SQL keywords
                if name.upper() not in ("SELECT", "FROM", "WHERE", "JOIN", "ORDER", "GROUP",
                                        "HAVING", "LIMIT", "OFFSET", "AND", "OR", "NOT"):
                    tables.append(name)

        i += 1

    return tables


def _get_identifier_name(identifier) -> Optional[str]:
    """Get the real name from an sqlparse Identifier"""
    if hasattr(identifier, "get_real_name"):
        return identifier.get_real_name()
    return str(identifier).strip('"`[]')


def _extract_column_names(parsed) -> List[str]:
    """Extract column names from parsed SQL"""
    columns = []

    # Get tokens from SELECT clause
    in_select = False
    for token in parsed.tokens:
        if token.is_keyword and str(token).upper() == "SELECT":
            in_select = True
            continue
        if in_select:
            if token.is_keyword and str(token).upper() == "FROM":
                break
            if isinstance(token, (sqlparse.sql.Identifier, sqlparse.sql.IdentifierList)):
                # Extract identifiers
                if isinstance(token, sqlparse.sql.IdentifierList):
                    for identifier in token.get_identifiers():
                        name = _get_identifier_name(identifier)
                        if name and name != "*":
                            columns.append(name)
                else:
                    name = _get_identifier_name(token)
                    if name and name != "*":
                        columns.append(name)

    return columns


@mcp.tool()
def list_tables() -> Dict[str, Any]:
    """
    List all tables in the current database.

    Returns:
        Dictionary containing:
        - database: Database name
        - tables: List of table names
    """
    logger.info("Listing tables")

    conn = get_connection()
    cursor = None
    try:
        if DB_CONFIG["type"] == "mysql":
            cursor = conn.cursor()
            cursor.execute("SHOW TABLES")
            tables = [list(row.values())[0] for row in cursor.fetchall()]
        elif DB_CONFIG["type"] == "postgresql":
            cursor = conn.cursor()
            cursor.execute("""
                SELECT table_name FROM INFORMATION_SCHEMA.TABLES
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
            """)
            tables = [row[0] for row in cursor.fetchall()]
        else:
            raise ValueError(f"Unsupported database type: {DB_CONFIG['type']}")

        return {
            "database": DB_CONFIG["database"],
            "dbType": DB_CONFIG["type"],
            "tables": tables,
            "tableCount": len(tables)
        }
    except Exception as e:
        logger.error(f"Error listing tables: {e}")
        return {
            "error": str(e),
            "database": DB_CONFIG["database"]
        }
    finally:
        if cursor:
            cursor.close()
        conn.close()


def validate_config():
    """Validate database configuration"""
    required_vars = ["DB_TYPE", "DB_HOST", "DB_USER", "DB_PASSWORD", "DB_NAME"]
    missing = [var for var in required_vars if not os.getenv(var)]

    if missing:
        logger.warning(f"Missing environment variables: {', '.join(missing)}")
        logger.warning("Please set database configuration environment variables")


def main():
    """Main entry point"""
    validate_config()
    logger.info(f"Database Tools MCP server starting")
    logger.info(f"Database type: {DB_CONFIG['type']}")
    logger.info(f"Connected to: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")


if __name__ == "__main__":
    main()
    mcp.run()
