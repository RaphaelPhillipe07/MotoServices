package br.com.motoservices.dao;

import br.com.motoservices.model.Peca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PecaDAO extends JpaRepository<Peca, Long> {

    Optional<Peca> findByCodigo(String codigo);

    Page<Peca> findByDescricaoContainingIgnoreCaseOrCodigoContainingIgnoreCase(
            String descricao, String codigo, Pageable pageable);

    /**
     * Compara a quantidade em estoque com o mínimo <strong>da própria peça</strong>
     * (comparação coluna-a-coluna), e não com um valor informado.
     */
    @Query("SELECT p FROM Peca p WHERE p.quantidadeEstoque < p.estoqueMinimo")
    Page<Peca> findAbaixoDoEstoqueMinimo(Pageable pageable);

    /**
     * Baixa atômica: o banco só decrementa se ainda houver saldo suficiente,
     * evitando lost update em confirmações concorrentes de OS.
     * O {@code versao} é incrementado manualmente porque bulk updates não passam pelo ciclo do Hibernate.
     *
     * @return 1 quando a baixa ocorre, 0 quando não há saldo suficiente
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Peca p SET p.quantidadeEstoque = p.quantidadeEstoque - :quantidade, "
            + "p.versao = p.versao + 1 "
            + "WHERE p.id = :pecaId AND p.quantidadeEstoque >= :quantidade")
    int baixarEstoque(@Param("pecaId") Long pecaId, @Param("quantidade") int quantidade);
}
