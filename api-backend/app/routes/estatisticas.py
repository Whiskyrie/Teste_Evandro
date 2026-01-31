"""
Rotas da API para estatísticas - SQL puro com psycopg2.
"""

from decimal import Decimal
from fastapi import APIRouter, Depends

from app.database import get_db
from app.models.schemas import EstatisticasGerais, TopOperadora, DespesasPorUF

router = APIRouter(prefix="/api/estatisticas", tags=["Estatísticas"])


@router.get("", response_model=EstatisticasGerais)
def get_estatisticas_gerais(conn=Depends(get_db)):
    """
    Retorna estatísticas agregadas do sistema.

    Trade-off: Cálculo on-demand (sem cache)
    - Justificativa: Dados atualizados apenas via imports manuais
    - Volume pequeno: 732 operadoras, 2148 despesas
    - Performance: < 500ms (aceitável para dashboard)
    """
    with conn.cursor() as cur:
        # 1. Total de operadoras
        cur.execute("SELECT COUNT(*) as total FROM operadoras")
        total_operadoras = cur.fetchone()["total"]

        # 2. Total e média de despesas
        cur.execute(
            """
            SELECT 
                COALESCE(SUM(valor_despesas), 0) as total,
                COALESCE(AVG(valor_despesas), 0) as media
            FROM despesas_consolidadas
            """
        )
        totais = cur.fetchone()
        total_despesas = Decimal(str(totais["total"]))
        media_despesas = Decimal(str(totais["media"]))

        # 3. Top 5 operadoras por volume de despesas
        cur.execute(
            """
            SELECT 
                o.cnpj,
                o.razao_social,
                o.uf,
                SUM(d.valor_despesas) as total_despesas
            FROM operadoras o
            JOIN despesas_consolidadas d ON o.cnpj = d.cnpj_operadora
            GROUP BY o.cnpj, o.razao_social, o.uf
            ORDER BY total_despesas DESC
            LIMIT 5
            """
        )
        top_5 = cur.fetchall()

        # 4. Distribuição por UF (Top 10)
        cur.execute(
            """
            SELECT 
                o.uf,
                SUM(d.valor_despesas) as total_despesas,
                COUNT(DISTINCT o.cnpj) as qtd_operadoras,
                AVG(d.valor_despesas) as media_por_operadora
            FROM operadoras o
            JOIN despesas_consolidadas d ON o.cnpj = d.cnpj_operadora
            GROUP BY o.uf
            ORDER BY total_despesas DESC
            LIMIT 10
            """
        )
        despesas_uf = cur.fetchall()

        return EstatisticasGerais(
            total_operadoras=total_operadoras,
            total_despesas=total_despesas,
            media_despesas_por_operadora=media_despesas,
            top_5_operadoras=[
                TopOperadora(
                    cnpj=op["cnpj"],
                    razao_social=op["razao_social"],
                    uf=op["uf"],
                    total_despesas=Decimal(str(op["total_despesas"])),
                )
                for op in top_5
            ],
            despesas_por_uf=[
                DespesasPorUF(
                    uf=d["uf"],
                    total_despesas=Decimal(str(d["total_despesas"])),
                    qtd_operadoras=d["qtd_operadoras"],
                    media_por_operadora=Decimal(str(d["media_por_operadora"])),
                )
                for d in despesas_uf
            ],
        )
