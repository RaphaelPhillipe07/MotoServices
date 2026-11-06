package br.com.motoservices.dao;

import br.com.motoservices.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PecaDAO extends JpaRepository<Peca, Long> {

    Optional<Peca> findByCodigo(String codigo);

    /**
     * Compara a quantidade em estoque com o mínimo <strong>da própria peça</strong>
     * (comparação coluna-a-coluna), e não com um valor informado.
     */
    @Query("SELECT p FROM Peca p WHERE p.quantidadeEstoque < p.estoqueMinimo")
    List<Peca> findAbaixoDoEstoqueMinimo();
}
