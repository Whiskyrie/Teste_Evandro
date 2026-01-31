"""
FastAPI Application - Backend REST API para análise de despesas de operadoras.

Trade-off: FastAPI + psycopg2 (sem ORM)
- Justificativa:
  - FastAPI: Performance, validação automática, docs OpenAPI
  - psycopg2 puro: Compatível Python 3.14, mais rápido que ORM
  - SQL direto: Schema já existe (Parte 3), queries simples
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from app.routes import operadoras, estatisticas
from app.database import close_pool


@asynccontextmanager
async def lifespan(_app: FastAPI):
    """Lifecycle events."""
    print("Starting FastAPI server...")
    yield
    print("Shutting down...")
    close_pool()


# Criar aplicação
app = FastAPI(
    title="Intuitive Care - API de Operadoras",
    description="API REST para consulta de operadoras e despesas",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS (desenvolvimento)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(operadoras.router)
app.include_router(estatisticas.router)


@app.get("/", tags=["Health"])
def health_check():
    """Health check endpoint."""
    return {"status": "ok", "service": "Intuitive Care API", "version": "1.0.0"}


@app.get("/favicon.ico", include_in_schema=False)
def favicon():
    """Retorna 204 para evitar logs de favicon 404."""
    from fastapi.responses import Response

    return Response(status_code=204)
