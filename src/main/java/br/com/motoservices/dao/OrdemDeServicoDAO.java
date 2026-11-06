package br.com.motoservices.dao;

import br.com.motoservices.model.OrdemDeServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrdemDeServicoDAO extends JpaRepository<OrdemDeServico, Long> {

    @EntityGraph(attributePaths = {"cliente", "moto"})
    Page<OrdemDeServico> findByClienteId(Long clienteId, Pageable pageable);

    @Query("SELECT o FROM OrdemDeServico o WHERE o.dataAbertura BETWEEN :inicio AND :fim")
    List<OrdemDeServico> buscarPorPeriodo(@Param("inicio") LocalDateTime inicio,
                                          @Param("fim") LocalDateTime fim);
}
