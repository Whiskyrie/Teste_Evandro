"""
Rotas da API para operadoras - SQL puro com psycopg2.
"""

from decimal import Decimal
from fastapi import APIRouter, Depends, HTTPException, Query
from typing import Annotated

from app.database import get_db
from app.models.schemas import (
    OperadoraBase,
    OperadoraDetail,
    DespesaResponse,
    PaginatedResponse,
)

router = APIRouter(prefix="/api/operadoras", tags=["Operadoras"])


@router.get("", response_model=PaginatedResponse)
def list_operadoras(
    page: Annotated[int, Query(ge=1)] = 1,
    limit: Annotated[int, Query(ge=1, le=100)] = 20,
    search: Annotated[str | None, Query(max_length=100)] = None,
    uf: Annotated[str | None, Query(max_length=2)] = None,
    conn=Depends(get_db),
):
    """
    Lista todas as operadoras com paginação.

    Query params:
    - page: Número da página (default: 1)
    - limit: Itens por página (1-100, default: 20)
    - search: Busca por razão social ou CNPJ
    - uf: Filtro por UF
    """
    with conn.cursor() as cur:
        # Montar WHERE clause dinamicamente
        where_clauses = []
        params = []

        if search:
            where_clauses.append("(razao_social ILIKE %s OR cnpj LIKE %s)")
            search_pattern = f"%{search}%"
            params.extend([search_pattern, search_pattern])

        if uf:
            where_clauses.append("uf = %s")
            params.append(uf.upper())

        where_sql = f"WHERE {' AND '.join(where_clauses)}" if where_clauses else ""

        # Contar total
        cur.execute(f"SELECT COUNT(*) as total FROM operadoras {where_sql}", params)
        total = cur.fetchone()["total"]

        # Buscar dados paginados
        offset = (page - 1) * limit
        cur.execute(
            f"""
            SELECT cnpj, razao_social, registro_ans, modalidade, uf, status_validacao
            FROM operadoras
            {where_sql}
            ORDER BY razao_social
            LIMIT %s OFFSET %s
            """,
            params + [limit, offset],
        )
        operadoras = cur.fetchall()

        # Calcular metadados
        total_pages = (total + limit - 1) // limit
        has_next = page < total_pages
        has_previous = page > 1

        return PaginatedResponse(
            data=[OperadoraBase(**op) for op in operadoras],
            total=total,
            page=page,
            limit=limit,
            total_pages=total_pages,
            has_next=has_next,
            has_previous=has_previous,
        )


@router.get("/{cnpj}", response_model=OperadoraDetail)
def get_operadora_detail(cnpj: str, conn=Depends(get_db)):
    """Retorna detalhes de uma operadora com estatísticas."""
    with conn.cursor() as cur:
        # Buscar operadora
        cur.execute(
            """
            SELECT cnpj, razao_social, registro_ans, modalidade, uf, status_validacao
            FROM operadoras
            WHERE cnpj = %s
            """,
            (cnpj,),
        )
        operadora = cur.fetchone()

        if not operadora:
            raise HTTPException(status_code=404, detail="Operadora não encontrada")

        # Buscar estatísticas
        cur.execute(
            """
            SELECT 
                COALESCE(SUM(valor_despesas), 0) as total_despesas,
                COUNT(*) as qtd_trimestres
            FROM despesas_consolidadas
            WHERE cnpj_operadora = %s
            """,
            (cnpj,),
        )
        stats = cur.fetchone()

        return OperadoraDetail(
            **operadora,
            total_despesas=Decimal(str(stats["total_despesas"])),
            qtd_trimestres=stats["qtd_trimestres"],
        )


@router.get("/{cnpj}/despesas", response_model=list[DespesaResponse])
def get_operadora_despesas(cnpj: str, conn=Depends(get_db)):
    """Retorna histórico de despesas de uma operadora."""
    with conn.cursor() as cur:
        # Verificar se operadora existe
        cur.execute("SELECT 1 FROM operadoras WHERE cnpj = %s", (cnpj,))
        if not cur.fetchone():
            raise HTTPException(status_code=404, detail="Operadora não encontrada")

        # Buscar despesas com JOIN
        cur.execute(
            """
            SELECT 
                d.trimestre,
                d.ano,
                d.valor_despesas,
                d.cnpj_operadora,
                o.razao_social,
                o.uf
            FROM despesas_consolidadas d
            JOIN operadoras o ON d.cnpj_operadora = o.cnpj
            WHERE d.cnpj_operadora = %s
            ORDER BY d.ano DESC, d.trimestre DESC
            """,
            (cnpj,),
        )
        despesas = cur.fetchall()

        return [DespesaResponse(**d) for d in despesas]
