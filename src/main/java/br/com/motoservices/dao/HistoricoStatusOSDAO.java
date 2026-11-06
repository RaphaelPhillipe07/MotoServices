package br.com.motoservices.dao;

import br.com.motoservices.model.HistoricoStatusOS;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricoStatusOSDAO extends JpaRepository<HistoricoStatusOS, Long> {

    Page<HistoricoStatusOS> findByOrdemDeServicoId(Long ordemDeServicoId, Pageable pageable);
}
