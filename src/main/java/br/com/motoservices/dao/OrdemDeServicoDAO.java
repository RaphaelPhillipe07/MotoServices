package br.com.motoservices.dao;

import br.com.motoservices.model.OrdemDeServico;
import br.com.motoservices.model.enums.StatusOS;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrdemDeServicoDAO extends JpaRepository<OrdemDeServico, Long> {

    @EntityGraph(attributePaths = {"cliente", "moto"})
    Page<OrdemDeServico> findByClienteId(Long clienteId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"cliente", "moto"})
    Page<OrdemDeServico> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "moto"})
    Page<OrdemDeServico> findByStatus(StatusOS status, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "moto"})
    @Query("SELECT o FROM OrdemDeServico o WHERE o.cliente.id = :clienteId AND o.status = :status")
    Page<OrdemDeServico> buscarPorClienteEStatus(@Param("clienteId") Long clienteId,
                                                 @Param("status") StatusOS status,
                                                 Pageable pageable);

    /**
     * Filtros opcionais e combináveis. O {@code EntityGraph} evita N+1 de cliente/moto.
     */
    @EntityGraph(attributePaths = {"cliente", "moto"})
    @Query("SELECT o FROM OrdemDeServico o WHERE "
            + "(:clienteId IS NULL OR o.cliente.id = :clienteId) AND "
            + "(:motoId IS NULL OR o.moto.id = :motoId) AND "
            + "(:status IS NULL OR o.status = :status)")
    Page<OrdemDeServico> buscarComFiltros(@Param("clienteId") Long clienteId,
                                          @Param("motoId") Long motoId,
                                          @Param("status") StatusOS status,
                                          Pageable pageable);

    /**
     * OS encerradas no período, pelo snapshot gravado no fechamento.
     */
    @EntityGraph(attributePaths = {"cliente", "moto"})
    @Query("SELECT o FROM OrdemDeServico o WHERE o.status = :status "
            + "AND o.dataFechamento BETWEEN :inicio AND :fim ORDER BY o.dataFechamento")
    List<OrdemDeServico> buscarFechadasPorPeriodo(@Param("status") StatusOS status,
                                                  @Param("inicio") LocalDateTime inicio,
                                                  @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(o.totalPecas), 0) AS totalPecas, "
            + "COALESCE(SUM(o.totalMaoDeObra), 0) AS totalMaoDeObra, "
            + "COALESCE(SUM(o.descontoAplicado), 0) AS descontoAplicado, "
            + "COALESCE(SUM(o.totalFinal), 0) AS totalFinal, "
            + "COUNT(o) AS quantidadeOs "
            + "FROM OrdemDeServico o WHERE o.status = :status "
            + "AND o.dataFechamento BETWEEN :inicio AND :fim")
    FaturamentoProjection faturamentoPorPeriodo(@Param("status") StatusOS status,
                                                @Param("inicio") LocalDateTime inicio,
                                                @Param("fim") LocalDateTime fim);

    @Query("SELECT s.descricao AS descricao, TYPE(s) AS tipo, COUNT(s) AS quantidade, "
            + "COALESCE(SUM(s.valorBaseMaoDeObra), 0) AS totalBaseMaoDeObra "
            + "FROM OrdemDeServico o JOIN o.servicos s WHERE o.status = :status "
            + "AND o.dataFechamento BETWEEN :inicio AND :fim "
            + "GROUP BY s.descricao, TYPE(s) "
            + "ORDER BY COUNT(s) DESC, COALESCE(SUM(s.valorBaseMaoDeObra), 0) DESC")
    List<ServicoRankingProjection> servicosMaisRealizados(@Param("status") StatusOS status,
                                                          @Param("inicio") LocalDateTime inicio,
                                                          @Param("fim") LocalDateTime fim,
                                                          Pageable pageable);

    @Query("SELECT o.cliente.id AS clienteId, o.cliente.nome AS clienteNome, "
            + "COUNT(o) AS quantidadeOs, COALESCE(SUM(o.totalFinal), 0) AS totalFinal "
            + "FROM OrdemDeServico o WHERE o.status = :status "
            + "AND o.dataFechamento BETWEEN :inicio AND :fim "
            + "GROUP BY o.cliente.id, o.cliente.nome "
            + "ORDER BY COUNT(o) DESC, COALESCE(SUM(o.totalFinal), 0) DESC")
    List<ClienteRankingProjection> clientesMaisAtendidos(@Param("status") StatusOS status,
                                                         @Param("inicio") LocalDateTime inicio,
                                                         @Param("fim") LocalDateTime fim,
                                                         Pageable pageable);

    interface FaturamentoProjection {
        BigDecimal getTotalPecas();

        BigDecimal getTotalMaoDeObra();

        BigDecimal getDescontoAplicado();

        BigDecimal getTotalFinal();

        Long getQuantidadeOs();
    }

    interface ServicoRankingProjection {
        String getDescricao();

        Class<?> getTipo();

        Long getQuantidade();

        BigDecimal getTotalBaseMaoDeObra();
    }

    interface ClienteRankingProjection {
        Long getClienteId();

        String getClienteNome();

        Long getQuantidadeOs();

        BigDecimal getTotalFinal();
    }
}
