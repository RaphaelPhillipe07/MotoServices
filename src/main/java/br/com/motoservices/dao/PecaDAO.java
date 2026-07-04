package br.com.motoservices.dao;

import br.com.motoservices.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PecaDAO extends JpaRepository<Peca, Long> {

    Optional<Peca> findByCodigo(String codigo);

    List<Peca> findByQuantidadeEstoqueLessThan(Integer estoqueMinimo);
}
