"""
Pydantic schemas para validação de requests e responses.
"""

from decimal import Decimal
from pydantic import BaseModel, Field


# ============================================================================
# OPERADORAS
# ============================================================================


class OperadoraBase(BaseModel):
    cnpj: str
    razao_social: str
    registro_ans: str | None = None
    modalidade: str | None = None
    uf: str | None = None
    status_validacao: str | None = None


class OperadoraDetail(OperadoraBase):
    total_despesas: Decimal = Field(default=Decimal("0"))
    qtd_trimestres: int = Field(default=0)


class DespesaResponse(BaseModel):
    trimestre: int
    ano: int
    valor_despesas: Decimal
    cnpj_operadora: str
    razao_social: str
    uf: str | None


# ============================================================================
# PAGINAÇÃO
# ============================================================================


class PaginatedResponse(BaseModel):
    data: list[OperadoraBase]
    total: int
    page: int
    limit: int
    total_pages: int
    has_next: bool
    has_previous: bool


# ============================================================================
# ESTATÍSTICAS
# ============================================================================


class TopOperadora(BaseModel):
    cnpj: str
    razao_social: str
    uf: str | None
    total_despesas: Decimal


class DespesasPorUF(BaseModel):
    uf: str | None
    total_despesas: Decimal
    qtd_operadoras: int
    media_por_operadora: Decimal


class EstatisticasGerais(BaseModel):
    total_operadoras: int
    total_despesas: Decimal
    media_despesas_por_operadora: Decimal
    top_5_operadoras: list[TopOperadora]
    despesas_por_uf: list[DespesasPorUF]
