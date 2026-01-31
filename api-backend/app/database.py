"""
Conexão com PostgreSQL usando psycopg2 puro (sem ORM).
"""

import os

from psycopg2.pool import SimpleConnectionPool
from psycopg2.extras import RealDictCursor

# Database config
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "localhost"),
    "port": int(os.getenv("DB_PORT", "5432")),
    "database": os.getenv("DB_NAME", "teste_intuitive_care"),
    "user": os.getenv("DB_USER", "postgres"),
    "password": os.getenv("DB_PASSWORD", "postgres"),
}


class DatabasePool:
    """Singleton class to manage database connection pool."""

    _instance = None
    _pool = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def get_pool(self):
        """Obtém ou cria connection pool."""
        if DatabasePool._pool is None:
            DatabasePool._pool = SimpleConnectionPool(
                minconn=1,
                maxconn=10,
                cursor_factory=RealDictCursor,  # Retorna dicts
                **DB_CONFIG
            )
        return DatabasePool._pool

    def close_pool(self):
        """Fecha connection pool."""
        if DatabasePool._pool:
            DatabasePool._pool.closeall()
            DatabasePool._pool = None


_db_pool = DatabasePool()


def get_pool():
    """Obtém ou cria connection pool."""
    return _db_pool.get_pool()


def close_pool():
    """Fecha connection pool."""
    _db_pool.close_pool()


def get_db():
    """
    Dependency para FastAPI - retorna conexão do pool.

    Uso:
        @app.get("/items")
        def get_items(conn = Depends(get_db)):
            with conn.cursor() as cur:
                cur.execute("SELECT * FROM items")
                return cur.fetchall()
    """
    pool = get_pool()
    conn = pool.getconn()
    try:
        yield conn
    finally:
        pool.putconn(conn)
